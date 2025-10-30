# Multi-Modal Transport Route API Documentation

## Overview
This API provides optimal routing solutions for cargo transportation, supporting both **shipping** and **air transport** modes. It calculates routes optimized for different criteria: cost, time, risk, and environmental impact (CO2 emissions).

## Base URL
```
http://localhost:8000
```

## Transport Modes

### Shipping
- **Cost**: $0.50 per km
- **Speed**: 37 km/h (~20 knots)
- **CO2 Emissions**: 0.01 kg per km
- **Risk**: Variable (based on piracy risk grid)
- **Route**: Uses Neo4j graph database with actual shipping lanes

### Air Transport
- **Cost**: $4.50 per km (9x more expensive than shipping)
- **Speed**: 800 km/h
- **CO2 Emissions**: 0.50 kg per km (50x more than shipping)
- **Risk**: 0 (no piracy risk)
- **Route**: Direct straight line from source to destination

---

## API Endpoints

### 1. Get Optimal Routes (NEW)
**`POST /optimal-routes`**

Returns 4 optimized routes based on different criteria.

#### Request Body
```json
{
  "src_lat": 1.3521,
  "src_lon": 103.8198,
  "dst_lat": 40.7128,
  "dst_lon": -74.0060,
  "time_constraint_hours": 200
}
```

**Parameters:**
- `src_lat` (required): Source latitude
- `src_lon` (required): Source longitude
- `dst_lat` (required): Destination latitude
- `dst_lon` (required): Destination longitude
- `time_constraint_hours` (optional): Maximum delivery time in hours

#### Response
Returns a GeoJSON FeatureCollection with 4 features, each optimized for:
1. **cost_optimized** - Lowest cost route
2. **time_optimized** - Fastest route
3. **risk_optimized** - Lowest piracy risk route
4. **co2_optimized** - Lowest carbon emissions route

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "LineString",
        "coordinates": [[103.8198, 1.3521], [-74.0060, 40.7128]]
      },
      "properties": {
        "optimization_type": "cost_optimized",
        "distance_km": 16234.56,
        "cost_usd": 8117.28,
        "time_hours": 438.77,
        "co2_kg": 162.35,
        "risk_score": 0.85,
        "transport_type": "shipping"
      }
    },
    {
      "type": "Feature",
      "geometry": {
        "type": "LineString",
        "coordinates": [[103.8198, 1.3521], [-74.0060, 40.7128]]
      },
      "properties": {
        "optimization_type": "time_optimized",
        "distance_km": 15789.23,
        "cost_usd": 71051.54,
        "time_hours": 19.74,
        "co2_kg": 7894.62,
        "risk_score": 0.0,
        "transport_type": "air"
      }
    },
    {
      "type": "Feature",
      "geometry": {
        "type": "LineString",
        "coordinates": [[103.8198, 1.3521], [-74.0060, 40.7128]]
      },
      "properties": {
        "optimization_type": "risk_optimized",
        "distance_km": 15789.23,
        "cost_usd": 71051.54,
        "time_hours": 19.74,
        "co2_kg": 7894.62,
        "risk_score": 0.0,
        "transport_type": "air"
      }
    },
    {
      "type": "Feature",
      "geometry": {
        "type": "LineString",
        "coordinates": [[103.8198, 1.3521], [-74.0060, 40.7128]]
      },
      "properties": {
        "optimization_type": "co2_optimized",
        "distance_km": 16234.56,
        "cost_usd": 8117.28,
        "time_hours": 438.77,
        "co2_kg": 162.35,
        "risk_score": 0.85,
        "transport_type": "shipping"
      }
    }
  ],
  "metadata": {
    "source": {"lat": 1.3521, "lon": 103.8198},
    "destination": {"lat": 40.7128, "lon": -74.0060},
    "time_constraint_hours": 200
  }
}
```

---

### 2. Get Shortest Shipping Route (Legacy)
**`POST /shortest-route`**

Returns the shortest shipping route only (distance-optimized).

#### Request Body
```json
{
  "src_lat": 1.3521,
  "src_lon": 103.8198,
  "dst_lat": 40.7128,
  "dst_lon": -74.0060
}
```

#### Response
```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "LineString",
        "coordinates": [[103.8198, 1.3521], [105.2, 2.1], ...]
      },
      "properties": {
        "distance_km": 16234.56,
        "cost_usd": 8117.28,
        "time_hours": 438.77,
        "co2_kg": 162.35,
        "risk_score": 0.85,
        "transport_type": "shipping",
        "source_node_id": "node_123",
        "dest_node_id": "node_456"
      }
    }
  ]
}
```

---

### 3. Health Check
**`GET /health`**

Check API health and Neo4j connection status.

#### Response
```json
{
  "status": "healthy",
  "neo4j": "connected",
  "risk_grid_loaded": true,
  "risk_grid_cells": 35
}
```

---

### 4. API Info
**`GET /`**

Get API information and available endpoints.

#### Response
```json
{
  "message": "Multi-Modal Transport Route API",
  "version": "2.0",
  "endpoints": {
    "/optimal-routes": "POST - Get 4 optimized routes (cost, time, risk, CO2)",
    "/shortest-route": "POST - Get shortest shipping route (legacy)",
    "/health": "GET - Health check"
  }
}
```

---

## Optimization Strategies

### 1. Cost-Optimized Route
- **Goal**: Minimize transportation cost
- **Logic**: Compares `cost_usd` across shipping and air options
- **Typical Result**: Usually shipping (unless time constrained)

### 2. Time-Optimized Route
- **Goal**: Fastest delivery
- **Logic**: Compares `time_hours` across options
- **Typical Result**: Always air transport (800 km/h vs 37 km/h)

### 3. Risk-Optimized Route
- **Goal**: Minimize piracy and security risks
- **Logic**: Compares `risk_score` across options
- **Risk Calculation**: 
  - Air: 0 (no piracy risk)
  - Shipping: Based on piracy incidents in grid cells along route
- **Typical Result**: Air (zero risk) or low-risk shipping lanes

### 4. CO2-Optimized Route
- **Goal**: Minimize carbon emissions
- **Logic**: Compares `co2_kg` across options
- **Typical Result**: Always shipping (0.01 kg/km vs 0.50 kg/km)

---

## Risk Scoring

The API uses a grid-based piracy risk assessment system:

- **Grid Size**: 1° x 1° (approximately 111 km x 111 km at equator)
- **Risk Data**: Based on ICC piracy incident reports
- **Risk Scores**: 
  - 0: No incidents
  - 1-2: Low risk (1-2 incidents)
  - 2-3: Medium risk (3-5 incidents)
  - 3-4: High risk (6-10 incidents)
  - 4+: Very high risk (11+ incidents)

---

## Time Constraints

When a `time_constraint_hours` is provided:
1. Both shipping and air routes are calculated
2. Routes exceeding the time limit are filtered out
3. If no routes satisfy the constraint, an error is returned

**Example:**
```json
{
  "error": "No routes satisfy the time constraint",
  "time_constraint_hours": 24
}
```

---

## Example Use Cases

### 1. Urgent Delivery (Time-Critical)
```json
{
  "src_lat": 1.3521,
  "src_lon": 103.8198,
  "dst_lat": 51.5074,
  "dst_lon": -0.1278,
  "time_constraint_hours": 48
}
```
**Result**: Air transport (only option under 48 hours)

### 2. Cost-Sensitive Shipment
```json
{
  "src_lat": 1.3521,
  "src_lon": 103.8198,
  "dst_lat": 34.0522,
  "dst_lon": -118.2437
}
```
**Result**: Shipping route (significantly cheaper)

### 3. High-Risk Area Avoidance
```json
{
  "src_lat": 12.0,
  "src_lon": 45.0,
  "dst_lat": 15.0,
  "dst_lon": 55.0
}
```
**Result**: May recommend air to avoid piracy hotspots

### 4. Environmental Concerns
```json
{
  "src_lat": 35.6762,
  "src_lon": 139.6503,
  "dst_lat": 37.7749,
  "dst_lon": -122.4194
}
```
**Result**: Shipping route (50x lower CO2 emissions)

---

## Error Handling

### 400 Bad Request
Missing required parameters:
```json
{
  "error": "Missing parameters. Required: ['src_lat', 'src_lon', 'dst_lat', 'dst_lon']"
}
```

### 404 Not Found
No shipping route found (air route still available via `/optimal-routes`):
```json
{
  "error": "No route found"
}
```

### 500 Internal Server Error
Unexpected error:
```json
{
  "error": "Error message here"
}
```

### 503 Service Unavailable
Neo4j connection issues:
```json
{
  "status": "unhealthy",
  "error": "Connection refused"
}
```

---

## Running the API

### Start the Server
```bash
cd graphdb
uvicorn main:app --reload --port 8000
```

### Test the API
```bash
# Health check
curl http://localhost:8000/health

# Get optimal routes
curl -X POST http://localhost:8000/optimal-routes \
  -H "Content-Type: application/json" \
  -d '{
    "src_lat": 1.3521,
    "src_lon": 103.8198,
    "dst_lat": 40.7128,
    "dst_lon": -74.0060
  }'
```

---

## Configuration

Edit constants in `main.py` to adjust transport parameters:

```python
# Shipping
SHIPPING_COST_PER_KM = 0.50  # USD per km
SHIPPING_SPEED_KMH = 37.0     # km/h
SHIPPING_CO2_PER_KM = 0.01    # kg CO2 per km

# Air
AIR_COST_PER_KM = 4.50        # USD per km
AIR_SPEED_KMH = 800.0         # km/h
AIR_CO2_PER_KM = 0.50         # kg CO2 per km
```

---

## Dependencies

- **FastAPI**: Web framework
- **Neo4j**: Graph database for shipping routes
- **Python 3.8+**

Install:
```bash
pip install fastapi uvicorn neo4j python-dotenv
```

---

## Future Enhancements

1. **Hybrid Routes**: Combine shipping + air (e.g., ship to hub, then fly)
2. **Weather Integration**: Adjust routes based on weather conditions
3. **Dynamic Pricing**: Real-time cost updates based on fuel prices
4. **Port Congestion**: Factor in port delays and congestion
5. **Multi-Stop Routes**: Support waypoints and multiple destinations
6. **Cost Breakdown**: Detailed cost analysis (fuel, labor, insurance, etc.)
