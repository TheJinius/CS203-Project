from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from neo4j import GraphDatabase
import os
import csv
import math
from dotenv import load_dotenv
from pathlib import Path
from typing import Optional, Dict, List, Tuple

# ========================
# Configuration
# ========================
load_dotenv(dotenv_path=Path(__file__).resolve().parent / ".env")

NEO4J_URI = os.getenv("NEO4J_URI", "bolt://localhost:7687")
NEO4J_USER = os.getenv("NEO4J_USER", "neo4j")
NEO4J_PASS = os.getenv("NEO4J_PASSWORD")

print(NEO4J_USER, NEO4J_PASS, NEO4J_URI)

driver = GraphDatabase.driver(NEO4J_URI, auth=(NEO4J_USER, NEO4J_PASS))

# ========================
# Graph Projection Setup
# ========================
def ensure_graph_projection():
    """
    Ensure the shippingGraph projection exists.
    Creates it if it doesn't exist.
    This should be called on application startup.
    """
    session = None
    try:
        session = driver.session()
        
        # Check if projection exists
        result = session.run(
            "CALL gds.graph.exists('shippingGraph') YIELD exists RETURN exists"
        )
        record = result.single()
        
        if record and record["exists"]:
            print("✅ Graph projection 'shippingGraph' already exists")
            return True
        
        # Create projection if it doesn't exist
        print("Creating graph projection 'shippingGraph'...")
        session.run("""
            CALL gds.graph.project(
                'shippingGraph',
                ['ShippingNode'],
                {
                    ShippingLane: {
                        type: 'ShippingLane',
                        orientation: 'NATURAL',
                        properties: {
                            distance_km: {
                                property: 'distance_km',
                                defaultValue: 0.0
                            }
                        }
                    }
                }
            )
        """)
        print("✅ Graph projection 'shippingGraph' created successfully")
        return True
        
    except Exception as e:
        print(f"❌ Error ensuring graph projection: {e}")
        return False
    finally:
        if session:
            session.close()

# ========================
# Transport Configuration
# ========================
# Shipping costs and speeds
SHIPPING_COST_PER_KM = 0.50  # USD per km
SHIPPING_SPEED_KMH = 37.0    # ~20 knots average
SHIPPING_CO2_PER_KM = 0.01   # kg CO2 per km (container ship)

# Air transport costs and speeds  
AIR_COST_PER_KM = 4.50       # USD per km (much more expensive)
AIR_SPEED_KMH = 800.0        # Average cargo plane speed
AIR_CO2_PER_KM = 0.50        # kg CO2 per km (significantly higher)

# Risk scoring
PIRACY_RISK_GRID_FILE = Path(__file__).resolve().parent / "piracy_risk_grid.csv"

# ========================
# Risk Grid Loader
# ========================
class RiskGridManager:
    def __init__(self, csv_file: Path):
        self.risk_grid = {}
        self.grid_size = 1.0  # degrees
        self._load_risk_grid(csv_file)
    
    def _load_risk_grid(self, csv_file: Path):
        """Load piracy risk grid from CSV."""
        if not csv_file.exists():
            print(f"Warning: Risk grid file not found: {csv_file}")
            return
        
        with open(csv_file, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                min_lat = float(row['min_lat'])
                min_lon = float(row['min_lon'])
                risk_score = float(row['risk_score'])
                self.risk_grid[(min_lat, min_lon)] = risk_score
        
        print(f"Loaded {len(self.risk_grid)} risk grid cells")
    
    def get_risk_score(self, lat: float, lon: float) -> float:
        """Get risk score for a coordinate."""
        grid_lat = math.floor(lat / self.grid_size) * self.grid_size
        grid_lon = math.floor(lon / self.grid_size) * self.grid_size
        return self.risk_grid.get((grid_lat, grid_lon), 0.0)
    
    def calculate_route_risk(self, coordinates: List[Tuple[float, float]]) -> float:
        """Calculate total risk score for a route."""
        if not coordinates:
            return 0.0
        
        total_risk = 0.0
        for lon, lat in coordinates:  # GeoJSON format is [lon, lat]
            total_risk += self.get_risk_score(lat, lon)
        
        # Average risk per point
        return total_risk / len(coordinates) if coordinates else 0.0

risk_manager = RiskGridManager(PIRACY_RISK_GRID_FILE)

# ========================
# Utility Functions
# ========================
def haversine_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Calculate great circle distance between two points in km."""
    R = 6371  # Earth's radius in km
    
    lat1_rad = math.radians(lat1)
    lat2_rad = math.radians(lat2)
    delta_lat = math.radians(lat2 - lat1)
    delta_lon = math.radians(lon2 - lon1)
    
    a = math.sin(delta_lat/2)**2 + math.cos(lat1_rad) * math.cos(lat2_rad) * math.sin(delta_lon/2)**2
    c = 2 * math.asin(math.sqrt(a))
    
    return R * c

def split_line_at_antimeridian(coords):
    """
    Splits a line (list of [lon, lat]) into one or more segments
    so that no segment crosses the 180° meridian.
    Returns a list of coordinate lists (MultiLineString-compatible).
    """
    if not coords or len(coords) < 2:
        return [coords]

    segments = []
    current = [coords[0]]

    for i in range(1, len(coords)):
        lon1, lat1 = coords[i - 1]
        lon2, lat2 = coords[i]

        # detect crossing over ±180
        if abs(lon1 - lon2) > 180:
            # calculate crossing longitude and interpolate latitude
            if lon1 > 0:
                lon_cross = 180
                lon2 += 360
            else:
                lon_cross = -180
                lon1 += 360

            lat_cross = lat1 + (lat2 - lat1) * (lon_cross - lon1) / (lon2 - lon1)
            # add crossing point
            current.append((lon_cross, lat_cross))
            segments.append(current)
            # start new segment
            current = [((-lon_cross), lat_cross)]

        current.append((lon2 if lon2 <= 180 else lon2 - 360, lat2))

    segments.append(current)
    return segments

def calculate_route_metrics(coordinates: List[Tuple[float, float]], 
                            transport_type: str) -> Dict:
    """Calculate distance, cost, time, CO2, and risk for a route."""
    if len(coordinates) < 2:
        return {
            "distance_km": 0,
            "cost_usd": 0,
            "time_hours": 0,
            "co2_kg": 0,
            "risk_score": 0,
            "transport_type": transport_type
        }
    
    # Calculate total distance
    total_distance = 0.0
    for i in range(len(coordinates) - 1):
        lon1, lat1 = coordinates[i]
        lon2, lat2 = coordinates[i + 1]
        total_distance += haversine_distance(lat1, lon1, lat2, lon2)
    
    # Get transport parameters
    if transport_type == "air":
        cost_per_km = AIR_COST_PER_KM
        speed_kmh = AIR_SPEED_KMH
        co2_per_km = AIR_CO2_PER_KM
    else:  # shipping
        cost_per_km = SHIPPING_COST_PER_KM
        speed_kmh = SHIPPING_SPEED_KMH
        co2_per_km = SHIPPING_CO2_PER_KM
    
    # Calculate metrics
    cost = total_distance * cost_per_km
    time = total_distance / speed_kmh
    co2 = total_distance * co2_per_km
    
    # Calculate risk (only for shipping routes)
    risk = 0.0
    if transport_type == "shipping":
        risk = risk_manager.calculate_route_risk(coordinates)
    
    return {
        "distance_km": round(total_distance, 2),
        "cost_usd": round(cost, 2),
        "time_hours": round(time, 2),
        "co2_kg": round(co2, 2),
        "risk_score": round(risk, 2),
        "transport_type": transport_type
    }

def create_geojson_feature(coordinates: List[Tuple[float, float]], 
                          properties: Dict) -> Dict:
    """Create a GeoJSON feature with proper geometry handling."""
    segments = split_line_at_antimeridian(coordinates)
    
    if len(segments) == 1:
        geometry = {
            "type": "LineString",
            "coordinates": segments[0]
        }
    else:
        geometry = {
            "type": "MultiLineString",
            "coordinates": segments
        }
    
    return {
        "type": "Feature",
        "geometry": geometry,
        "properties": properties
    }


# ========================
# FastAPI App
# ========================
# FastAPI App
# ========================
app = FastAPI(title="Multi-Modal Transport Route API", version="2.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Startup event to ensure graph projection exists
@app.on_event("startup")
async def startup_event():
    """Initialize graph projection on application startup."""
    print("=" * 60)
    print("Application Starting - Checking Graph Projection")
    print("=" * 60)
    ensure_graph_projection()
    print("=" * 60)

# ========================
# Neo4j Queries
# ========================
SHIPPING_QUERY_DISTANCE = """
WITH point({latitude: $src_lat, longitude: $src_lon}) AS srcPoint,
     point({latitude: $dst_lat, longitude: $dst_lon}) AS dstPoint

MATCH (src:ShippingNode)
WITH src, srcPoint, dstPoint
ORDER BY point.distance(point({latitude: src.lat, longitude: src.lon}), srcPoint) ASC
LIMIT 1
WITH src, dstPoint

MATCH (dst:ShippingNode)
WITH src, dst
ORDER BY point.distance(point({latitude: dst.lat, longitude: dst.lon}), dstPoint) ASC
LIMIT 1

CALL gds.shortestPath.dijkstra.stream("shippingGraph", {
  sourceNode: src,
  targetNode: dst,
  relationshipWeightProperty: 'distance_km'
})
YIELD totalCost, path
RETURN
  src.node_id AS source_node_id,
  dst.node_id AS dest_node_id,
  totalCost AS total_distance_km,
  [node IN nodes(path) | [node.lon, node.lat]] AS coordinates
"""

# ========================
# Route Calculation Functions
# ========================
def get_shipping_route(src_lat: float, src_lon: float, 
                      dst_lat: float, dst_lon: float) -> Optional[Dict]:
    """Get shipping route from Neo4j graph."""
    with driver.session() as session:
        result = session.run(
            SHIPPING_QUERY_DISTANCE,
            {
                "src_lat": src_lat,
                "src_lon": src_lon,
                "dst_lat": dst_lat,
                "dst_lon": dst_lon,
            },
        )
        record = result.single()
        if not record:
            return None
        
        coordinates = record["coordinates"]
        return {
            "coordinates": coordinates,
            "source_node_id": record["source_node_id"],
            "dest_node_id": record["dest_node_id"]
        }

def get_air_route(src_lat: float, src_lon: float, 
                 dst_lat: float, dst_lon: float) -> Dict:
    """Generate direct air route (straight line)."""
    return {
        "coordinates": [[src_lon, src_lat], [dst_lon, dst_lat]],
        "source": "air",
        "dest": "air"
    }

def calculate_optimized_routes(src_lat: float, src_lon: float,
                               dst_lat: float, dst_lon: float,
                               time_constraint_hours: Optional[float] = None) -> Dict:
    """
    Calculate 4 optimized routes:
    1. Lowest cost route
    2. Fastest route
    3. Lowest risk route
    4. Lowest CO2 emissions route
    """
    
    # Get both shipping and air routes
    shipping_route = get_shipping_route(src_lat, src_lon, dst_lat, dst_lon)
    air_route = get_air_route(src_lat, src_lon, dst_lat, dst_lon)
    
    routes = []
    
    # Calculate metrics for shipping route
    if shipping_route:
        shipping_metrics = calculate_route_metrics(
            shipping_route["coordinates"], 
            "shipping"
        )
        routes.append({
            "route": shipping_route,
            "metrics": shipping_metrics,
            "type": "shipping"
        })
    
    # Calculate metrics for air route
    air_metrics = calculate_route_metrics(
        air_route["coordinates"],
        "air"
    )
    routes.append({
        "route": air_route,
        "metrics": air_metrics,
        "type": "air"
    })
    
    # Filter by time constraint if provided
    if time_constraint_hours:
        routes = [r for r in routes if r["metrics"]["time_hours"] <= time_constraint_hours]
    
    if not routes:
        return {
            "error": "No routes satisfy the time constraint",
            "time_constraint_hours": time_constraint_hours
        }
    
    # Find optimal routes for each criterion
    optimal_routes = {}
    
    # 1. Lowest cost
    cost_optimal = min(routes, key=lambda r: r["metrics"]["cost_usd"])
    optimal_routes["cost_optimized"] = {
        "coordinates": cost_optimal["route"]["coordinates"],
        "metrics": cost_optimal["metrics"],
        "optimization": "cost"
    }
    
    # 2. Fastest (lowest time)
    time_optimal = min(routes, key=lambda r: r["metrics"]["time_hours"])
    optimal_routes["time_optimized"] = {
        "coordinates": time_optimal["route"]["coordinates"],
        "metrics": time_optimal["metrics"],
        "optimization": "time"
    }
    
    # 3. Lowest risk (prefer shipping with low risk, air has 0 risk)
    risk_optimal = min(routes, key=lambda r: r["metrics"]["risk_score"])
    optimal_routes["risk_optimized"] = {
        "coordinates": risk_optimal["route"]["coordinates"],
        "metrics": risk_optimal["metrics"],
        "optimization": "risk"
    }
    
    # 4. Lowest CO2
    co2_optimal = min(routes, key=lambda r: r["metrics"]["co2_kg"])
    optimal_routes["co2_optimized"] = {
        "coordinates": co2_optimal["route"]["coordinates"],
        "metrics": co2_optimal["metrics"],
        "optimization": "co2"
    }
    
    return optimal_routes

# ========================
# API Routes
# ========================
@app.post("/optimal-routes")
async def optimal_routes(request: Request):
    """
    Calculate optimal routes for cost, time, risk, and CO2 emissions.
    
    Request body:
    {
        "src_lat": float,
        "src_lon": float,
        "dst_lat": float,
        "dst_lon": float,
        "time_constraint_hours": float (optional)
    }
    
    Returns GeoJSON FeatureCollection with 4 optimized routes.
    """
    data = await request.json()
    
    required = ["src_lat", "src_lon", "dst_lat", "dst_lon"]
    if not all(k in data for k in required):
        return JSONResponse(
            {"error": f"Missing parameters. Required: {required}"}, 
            status_code=400
        )
    
    time_constraint = data.get("time_constraint_hours")
    
    try:
        optimal = calculate_optimized_routes(
            data["src_lat"],
            data["src_lon"],
            data["dst_lat"],
            data["dst_lon"],
            time_constraint
        )
        
        if "error" in optimal:
            return JSONResponse(optimal, status_code=400)
        
        # Build GeoJSON FeatureCollection
        features = []
        for opt_type, route_data in optimal.items():
            feature = create_geojson_feature(
                route_data["coordinates"],
                {
                    "optimization_type": opt_type,
                    **route_data["metrics"]
                }
            )
            features.append(feature)
        
        geojson = {
            "type": "FeatureCollection",
            "features": features,
            "metadata": {
                "source": {"lat": data["src_lat"], "lon": data["src_lon"]},
                "destination": {"lat": data["dst_lat"], "lon": data["dst_lon"]},
                "time_constraint_hours": time_constraint
            }
        }
        
        return JSONResponse(geojson)
        
    except Exception as e:
        return JSONResponse(
            {"error": str(e)},
            status_code=500
        )

@app.post("/shortest-route")
async def shortest_route(request: Request):
    """Legacy endpoint - returns shortest shipping route."""
    data = await request.json()

    required = ["src_lat", "src_lon", "dst_lat", "dst_lon"]
    if not all(k in data for k in required):
        return JSONResponse(
            {"error": f"Missing parameters. Required: {required}"}, 
            status_code=400
        )

    shipping_route = get_shipping_route(
        data["src_lat"],
        data["src_lon"],
        data["dst_lat"],
        data["dst_lon"]
    )
    
    if not shipping_route:
        return JSONResponse({"error": "No route found"}, status_code=404)
    
    metrics = calculate_route_metrics(shipping_route["coordinates"], "shipping")
    
    feature = create_geojson_feature(
        shipping_route["coordinates"],
        {
            **metrics,
            "source_node_id": shipping_route["source_node_id"],
            "dest_node_id": shipping_route["dest_node_id"]
        }
    )
    
    geojson = {
        "type": "FeatureCollection",
        "features": [feature]
    }

    return JSONResponse(geojson)

@app.get("/")
def root():
    return {
        "message": "Multi-Modal Transport Route API",
        "version": "2.0",
        "endpoints": {
            "/optimal-routes": "POST - Get 4 optimized routes (cost, time, risk, CO2)",
            "/shortest-route": "POST - Get shortest shipping route (legacy)",
            "/health": "GET - Health check"
        }
    }

@app.get("/health")
def health():
    """Health check endpoint."""
    session = None
    try:
        # Test Neo4j connection
        session = driver.session()
        session.run("RETURN 1")
        
        # Check if graph projection exists
        result = session.run(
            "CALL gds.graph.exists('shippingGraph') YIELD exists RETURN exists"
        )
        record = result.single()
        graph_exists = record["exists"] if record else False
        
        # If projection doesn't exist, try to create it
        if not graph_exists:
            print("⚠️ Graph projection missing during health check - attempting to recreate...")
            ensure_graph_projection()
            # Check again
            result = session.run(
                "CALL gds.graph.exists('shippingGraph') YIELD exists RETURN exists"
            )
            record = result.single()
            graph_exists = record["exists"] if record else False
        
        return {
            "status": "healthy",
            "neo4j": "connected",
            "risk_grid_loaded": len(risk_manager.risk_grid) > 0,
            "risk_grid_cells": len(risk_manager.risk_grid),
            "shipping_graph_exists": graph_exists
        }
    except Exception as e:
        return JSONResponse(
            {
                "status": "unhealthy",
                "error": str(e)
            },
            status_code=503
        )
    finally:
        if session:
            session.close()

