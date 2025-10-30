import csv
import math
from collections import defaultdict

class PiracyRiskGrid:
    def __init__(self, grid_size_degrees=1.0):
        """
        Initialize the risk grid calculator.
        
        Args:
            grid_size_degrees: Size of each grid square in degrees (default 1.0 degree)
                              1 degree ≈ 111 km at equator
                              0.5 degree ≈ 55 km
                              0.25 degree ≈ 28 km
        """
        self.grid_size = grid_size_degrees
        self.grid_attacks = defaultdict(list)
        
    def get_grid_cell(self, lat, lon):
        """
        Get the grid cell coordinates for a given lat/lon.
        Returns the bottom-left corner of the grid cell.
        """
        # Floor division to get grid cell
        grid_lat = math.floor(lat / self.grid_size) * self.grid_size
        grid_lon = math.floor(lon / self.grid_size) * self.grid_size
        return (grid_lat, grid_lon)
    
    def get_grid_bounds(self, grid_lat, grid_lon):
        """
        Get the bounding coordinates of a grid cell.
        Returns: (min_lat, max_lat, min_lon, max_lon)
        """
        min_lat = grid_lat
        max_lat = grid_lat + self.grid_size
        min_lon = grid_lon
        max_lon = grid_lon + self.grid_size
        return (min_lat, max_lat, min_lon, max_lon)
    
    def calculate_risk_score(self, attack_count):
        """
        Calculate risk score based on number of attacks.
        
        Risk levels:
        - 0 attacks: 0 (No risk)
        - 1-2 attacks: 1 (Low risk)
        - 3-5 attacks: 2 (Medium risk)
        - 6-10 attacks: 3 (High risk)
        - 11+ attacks: 4 (Very high risk)
        
        You can customize this scoring method.
        """
        if attack_count == 0:
            return 0
        elif attack_count <= 2:
            return 1
        elif attack_count <= 5:
            return 2
        elif attack_count <= 10:
            return 3
        else:
            return 4
    
    def calculate_risk_score_weighted(self, attack_count):
        """
        Alternative: Calculate risk score with more granular weighting.
        Uses logarithmic scaling for better differentiation.
        """
        if attack_count == 0:
            return 0.0
        # Logarithmic scale: log base 2 gives good differentiation
        # Attack count: 1->1, 2->2, 4->3, 8->4, 16->5, etc.
        return round(1 + math.log2(attack_count), 2)
    
    def load_piracy_data(self, csv_file):
        """
        Load piracy coordinates from CSV file.
        """
        with open(csv_file, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                event_id = int(row['event_id'])
                lat = float(row['lat'])
                lon = float(row['long'])
                
                # Get grid cell for this attack
                grid_cell = self.get_grid_cell(lat, lon)
                self.grid_attacks[grid_cell].append(event_id)
        
        print(f"Loaded {sum(len(v) for v in self.grid_attacks.values())} attacks into {len(self.grid_attacks)} grid cells")
    
    def generate_risk_grid(self, output_file, use_weighted_score=True):
        """
        Generate risk grid CSV file.
        
        Output columns:
        - grid_id: Unique identifier for the grid cell
        - min_lat, max_lat, min_lon, max_lon: Boundaries of the grid cell
        - center_lat, center_lon: Center point of the grid cell
        - attack_count: Number of attacks in this cell
        - risk_score: Calculated risk score
        - event_ids: Comma-separated list of event IDs in this cell
        """
        risk_data = []
        grid_id = 1
        
        for (grid_lat, grid_lon), event_ids in sorted(self.grid_attacks.items()):
            attack_count = len(event_ids)
            
            # Calculate risk score
            if use_weighted_score:
                risk_score = self.calculate_risk_score_weighted(attack_count)
            else:
                risk_score = self.calculate_risk_score(attack_count)
            
            # Get grid bounds
            min_lat, max_lat, min_lon, max_lon = self.get_grid_bounds(grid_lat, grid_lon)
            
            # Calculate center point
            center_lat = (min_lat + max_lat) / 2
            center_lon = (min_lon + max_lon) / 2
            
            risk_data.append({
                'grid_id': grid_id,
                'min_lat': round(min_lat, 4),
                'max_lat': round(max_lat, 4),
                'min_lon': round(min_lon, 4),
                'max_lon': round(max_lon, 4),
                'center_lat': round(center_lat, 4),
                'center_lon': round(center_lon, 4),
                'attack_count': attack_count,
                'risk_score': risk_score,
                'event_ids': ','.join(map(str, sorted(event_ids)))
            })
            grid_id += 1
        
        # Write to CSV
        with open(output_file, 'w', newline='', encoding='utf-8') as f:
            fieldnames = ['grid_id', 'min_lat', 'max_lat', 'min_lon', 'max_lon', 
                         'center_lat', 'center_lon', 'attack_count', 'risk_score', 'event_ids']
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(risk_data)
        
        print(f"Generated risk grid with {len(risk_data)} cells")
        print(f"Saved to {output_file}")
        
        # Print statistics
        self.print_statistics(risk_data)
    
    def print_statistics(self, risk_data):
        """Print summary statistics about the risk grid."""
        print("\n=== Risk Grid Statistics ===")
        print(f"Total grid cells with attacks: {len(risk_data)}")
        print(f"Total attacks: {sum(cell['attack_count'] for cell in risk_data)}")
        print(f"Grid size: {self.grid_size} degrees (~{round(self.grid_size * 111)} km at equator)")
        
        # Risk score distribution
        risk_distribution = defaultdict(int)
        for cell in risk_data:
            risk_distribution[cell['risk_score']] += 1
        
        print("\nRisk Score Distribution:")
        for score in sorted(risk_distribution.keys()):
            print(f"  Risk Score {score}: {risk_distribution[score]} cells")
        
        # Top risk areas
        print("\nTop 10 Highest Risk Areas:")
        sorted_cells = sorted(risk_data, key=lambda x: x['attack_count'], reverse=True)[:10]
        for cell in sorted_cells:
            print(f"  Grid {cell['grid_id']}: {cell['attack_count']} attacks at "
                  f"({cell['center_lat']:.2f}, {cell['center_lon']:.2f}) - Risk Score: {cell['risk_score']}")


def main():
    # Configuration
    GRID_SIZE = 1.0  # 1 degree grid (~111 km at equator)
    # Try different sizes: 0.5 (55km), 0.25 (28km), 2.0 (222km)
    
    INPUT_FILE = "piracy_coordinates.csv"
    OUTPUT_FILE = "piracy_risk_grid.csv"
    USE_WEIGHTED_SCORE = False  # Set to True for logarithmic scoring
    
    print(f"Creating piracy risk grid with {GRID_SIZE} degree cells...")
    print(f"Input: {INPUT_FILE}")
    print(f"Output: {OUTPUT_FILE}")
    print()
    
    # Create risk grid calculator
    risk_grid = PiracyRiskGrid(grid_size_degrees=GRID_SIZE)
    
    # Load piracy data
    risk_grid.load_piracy_data(INPUT_FILE)
    
    # Generate risk grid CSV
    risk_grid.generate_risk_grid(OUTPUT_FILE, use_weighted_score=USE_WEIGHTED_SCORE)
    
    print("\n✓ Risk grid calculation complete!")


if __name__ == "__main__":
    main()
