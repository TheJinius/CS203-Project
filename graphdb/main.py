from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from neo4j import GraphDatabase
import os
from dotenv import load_dotenv
from pathlib import Path

# ========================
# Configuration
# ========================
load_dotenv(dotenv_path=Path(__file__).resolve().parent / ".env")

NEO4J_URI = os.getenv("NEO4J_URI", "bolt://localhost:7687")
NEO4J_USER = os.getenv("NEO4J_USER", "neo4j")
NEO4J_PASS = os.getenv("NEO4J_PASSWORD")

print(NEO4J_USER, NEO4J_PASS, NEO4J_URI)

driver = GraphDatabase.driver(NEO4J_URI, auth=(NEO4J_USER, NEO4J_PASS))

app = FastAPI(title="Shipping Route API", version="1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins="*",
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ========================
# Cypher Query
# ========================

QUERY = """
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
  [node IN nodes(path) | node.node_id] AS route_node_ids,
  [node IN nodes(path) | [node.lon, node.lat]] AS coordinates
"""

# ========================
# API Routes
# ========================

def split_line_at_antimeridian(coordinates):
    """
    Split a LineString at the International Date Line (antimeridian) to prevent wrapping.
    Returns a list of MultiLineString segments.
    """
    if not coordinates or len(coordinates) < 2:
        return coordinates
    
    segments = []
    current_segment = [coordinates[0]]
    
    for i in range(1, len(coordinates)):
        prev_lon = coordinates[i - 1][0]
        curr_lon = coordinates[i][0]
        
        # Check if we're crossing the antimeridian (180° / -180°)
        lon_diff = curr_lon - prev_lon
        
        # If the longitude difference is greater than 180°, we're wrapping
        if abs(lon_diff) > 180:
            # Add current segment
            current_segment.append(coordinates[i])
            segments.append(current_segment)
            # Start new segment
            current_segment = [coordinates[i]]
        else:
            current_segment.append(coordinates[i])
    
    # Add the last segment
    if len(current_segment) > 1:
        segments.append(current_segment)
    
    return segments

@app.post("/shortest-route")
async def shortest_route(request: Request):
    data = await request.json()

    required = ["src_lat", "src_lon", "dst_lat", "dst_lon"]
    if not all(k in data for k in required):
        return JSONResponse(
            {"error": f"Missing parameters. Required: {required}"}, status_code=400
        )

    with driver.session() as session:
        result = session.run(
            QUERY,
            {
                "src_lat": data["src_lat"],
                "src_lon": data["src_lon"],
                "dst_lat": data["dst_lat"],
                "dst_lon": data["dst_lon"],
            },
        )
        record = result.single()
        if not record:
            return JSONResponse({"error": "No route found"}, status_code=404)

        # Get coordinates and split at antimeridian if needed
        coordinates = record["coordinates"]
        segments = split_line_at_antimeridian(coordinates)
        
        # Build GeoJSON response with MultiLineString if split, otherwise LineString
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
        
        geojson = {
            "type": "FeatureCollection",
            "features": [
                {
                    "type": "Feature",
                    "geometry": geometry,
                    "properties": {
                        "source_node_id": record["source_node_id"],
                        "dest_node_id": record["dest_node_id"],
                        "total_distance_km": record["total_distance_km"],
                        "route_node_ids": record["route_node_ids"]
                    }
                }
            ]
        }

        return JSONResponse(geojson)

@app.get("/")
def root():
    return {"message": "Shipping Route API running."}
