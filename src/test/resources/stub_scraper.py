#!/usr/bin/env python3
"""
Stub Python scraper for testing PythonScraperService.
This script mimics the behavior of the real web_scraper.py
but doesn't actually scrape any data.
"""
import sys
import os

def main():
    if len(sys.argv) < 2:
        print("ERROR: Country code required", file=sys.stderr)
        sys.exit(1)
    
    country_code = sys.argv[1]
    
    # Simulate successful scraping
    print(f"Starting scrape for {country_code}")
    print("Downloading data...")
    print("Processing data...")
    
    # Return success with filename
    filename = f"test_{country_code.lower()}_data.csv"
    print(f"SUCCESS: {filename}")
    
    # Create a dummy CSV file in the expected location
    target_dir = os.path.join("src", "main", "resources", "data", "test_data")
    os.makedirs(target_dir, exist_ok=True)
    
    filepath = os.path.join(target_dir, filename)
    with open(filepath, 'w') as f:
        # Write CSV header that matches WITS format (16 columns before HSDataCleaner adds 9 more)
        f.write('"Reporter","ReporterName","Partner","PartnerName","Year","TL","TLS","Duty Type","Duty Code","AV Duty Rate","Specific Duty Rate","TrfLineDescription","DutyTypeDescription","Duty Nature","AvMethod","Note"\n')
        # Write a sample data row
        f.write(f'"SGP","Singapore","WLD","World","2023","01010000","Horses, asses, mules and hinnies, live.","AV","R0","5%","","Live horses","Ad valorem duty","Bound (UR)","Simple average","Test data"\n')
    
    sys.exit(0)

if __name__ == "__main__":
    main()
