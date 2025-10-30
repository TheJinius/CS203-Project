from shapely.geometry import LineString, MultiLineString, mapping
import json

def split_antimeridian(coords):
    parts = []
    current = [coords[0]]

    for i in range(1, len(coords)):
        lon1, lat1 = coords[i - 1]
        lon2, lat2 = coords[i]

        # detect crossing
        if abs(lon1 - lon2) > 180:
            # direction
            if lon1 > 0:
                lon_cross = 180
                lon2 += 360
            else:
                lon_cross = -180
                lon1 += 360

            # interpolate latitude where it crosses
            lat_cross = lat1 + (lat2 - lat1) * (lon_cross - lon1) / (lon2 - lon1)
            current.append((lon_cross, lat_cross))
            parts.append(LineString(current))
            current = [((-lon_cross), lat_cross)]

        current.append((lon2 if lon2 <= 180 else lon2 - 360, lat2))

    parts.append(LineString(current))
    return MultiLineString(parts) if len(parts) > 1 else parts[0]

# --- Load your GeoJSON ---
with open("test_route.geojson") as f:
    data = json.load(f)

geometry = data["features"][0]["geometry"]
coords = geometry["coordinates"]

# Handle both LineString and MultiLineString
if geometry["type"] == "LineString":
    lines = [coords]
elif geometry["type"] == "MultiLineString":
    lines = coords
else:
    raise ValueError(f"Unsupported geometry type: {geometry['type']}")

# Apply fix to all lines
fixed_lines = []
for line_coords in lines:
    geom = LineString(line_coords)
    fixed = split_antimeridian(list(geom.coords))
    if isinstance(fixed, MultiLineString):
        fixed_lines.extend(fixed.geoms)
    else:
        fixed_lines.append(fixed)

# Merge back to MultiLineString if needed
final_geom = MultiLineString(fixed_lines) if len(fixed_lines) > 1 else fixed_lines[0]

# --- Write new GeoJSON ---
fixed_geojson = {
    "type": "FeatureCollection",
    "features": [{"type": "Feature", "geometry": mapping(final_geom)}]
}

with open("test_route_fixed.geojson", "w") as f:
    json.dump(fixed_geojson, f)

print("✅ Route fixed and saved as test_route_fixed.geojson")
