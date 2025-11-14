#!/usr/bin/env python3
"""
Stub Python scraper that simulates failure for testing.
"""
import sys

def main():
    print("ERROR: Simulated scraping failure", file=sys.stderr)
    sys.exit(1)

if __name__ == "__main__":
    main()
