import re
import csv
from bs4 import BeautifulSoup

def parse_coordinate(coord_str):
    """
    Convert coordinate format from DD:MM.MMN/S/E/W to decimal degrees.
    Examples:
    - "01:43.35N" -> 1.7225
    - "101:26.18E" -> 101.4363
    - "14:32.4S" -> -14.54
    - "040:39.1E" -> 40.6517
    """
    # Match pattern like "01:43.35N" or "101:26.18E"
    match = re.match(r'(\d+):(\d+\.?\d*)([NSEW])', coord_str.strip())
    if not match:
        return None
    
    degrees = int(match.group(1))
    minutes = float(match.group(2))
    direction = match.group(3)
    
    # Convert to decimal degrees
    decimal = degrees + (minutes / 60.0)
    
    # Apply sign based on direction
    if direction in ['S', 'W']:
        decimal = -decimal
    
    return decimal

def extract_coordinates_from_html(html_file):
    """
    Extract piracy attack coordinates from ICC HTML file.
    Returns a list of tuples: (event_id, lat, long)
    """
    with open(html_file, 'r', encoding='utf-8') as f:
        html_content = f.read()
    
    soup = BeautifulSoup(html_content, 'html.parser')
    events = []
    event_id = 1
    
    # Find all table rows with class "wpgmaps_mlist_row"
    rows = soup.find_all('tr', class_='wpgmaps_mlist_row')
    
    for row in rows:
        # Get the narration cell which contains "Posn:"
        narration_cell = row.find('td', class_='wpgmza_table_custom_field_66')
        
        if narration_cell:
            narration_text = narration_cell.get_text()
            
            # Find "Posn:" followed by coordinates
            # Pattern: "Posn: 01:43.35N – 101:26.18E" or "Posn: 01:43.35N - 101:26.18E"
            posn_match = re.search(r'Posn:\s*([0-9:\.]+[NS])\s*[–\-]\s*([0-9:\.]+[EW])', narration_text)
            
            if posn_match:
                lat_str = posn_match.group(1)
                lon_str = posn_match.group(2)
                
                lat = parse_coordinate(lat_str)
                lon = parse_coordinate(lon_str)
                
                if lat is not None and lon is not None:
                    events.append((event_id, lat, lon))
                    event_id += 1
                else:
                    print(f"Warning: Could not parse coordinates: {lat_str}, {lon_str}")
    
    return events

def save_to_csv(events, output_file):
    """
    Save events to CSV file with columns: event_id, lat, long
    """
    with open(output_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['event_id', 'lat', 'long'])
        writer.writerows(events)
    
    print(f"Successfully extracted {len(events)} piracy events to {output_file}")

if __name__ == "__main__":
    html_file = "piracy_html.html"
    output_file = "piracy_coordinates.csv"
    
    print(f"Extracting coordinates from {html_file}...")
    events = extract_coordinates_from_html(html_file)
    
    if events:
        save_to_csv(events, output_file)
        print(f"\nFirst 5 events:")
        for event in events[:5]:
            print(f"  Event {event[0]}: Lat {event[1]:.4f}, Long {event[2]:.4f}")
    else:
        print("No coordinates found in the HTML file.")
