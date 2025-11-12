import { getSession, signOut } from "next-auth/react";

const API_BASE_ROUTE = process.env.NEXT_PUBLIC_API_BASE_ROUTE || "http://localhost:8080/api"
console.log(API_BASE_ROUTE);

// Helper function to get current auth headers
async function getAuthHeaders() {
  const session = await getSession();
  
  // Check if there's a token refresh error
  if (session?.error === "RefreshAccessTokenError") {
    // Force sign out if refresh failed
    await signOut({ callbackUrl: '/login' });
    throw new Error("Session expired. Please sign in again.");
  }
  
  const token = session?.accessToken;
  
  return {
    "Content-Type": "application/json",
    "Authorization": token ? `Bearer ${token}` : ""
  };
}

export async function searchTariffs(params: {
  reporter: string;
  partner: string;
  tlCode: string;
  year: number;
}) {
  console.log('🔍 Searching tariffs with data:', params);
  
  // Map frontend field names to backend expected names  
  const backendRequest = {
    reporterCode: params.reporter,
    partnerCode: params.partner,
    productCode: params.tlCode, // Map tlCode -> productCode
    year: params.year,
  };

  try {
    const headers = await getAuthHeaders();
    
    const response = await fetch(`${API_BASE_ROUTE}/tariffs/search`, {
      method: "POST",
      headers,
      body: JSON.stringify(backendRequest), // Send mapped data
    })
    const data = await response.json()
    return { ok: response.ok, data }
  } catch (error) {
    console.error('Search tariffs error:', error);
    return { ok: false, data: { error: (error as Error).message } };
  }
}

export async function calculateTariff(params: {
  reporterCode: string;
  partnerCode: string;
  productCode: string;
  tariffId: number;
  amountOfProduct: number;
  productValueDollars?: number;
  currency: string;
}) {
  console.log('🧮 Calculating tariff with data:', params);
  
  try {
    const headers = await getAuthHeaders();
    
    const response = await fetch(`${API_BASE_ROUTE}/tariffs/calculate`, {
      method: "POST",
      headers,
      body: JSON.stringify(params),
    })
    const data = await response.json()
    return { ok: response.ok, data }
  } catch (error) {
    console.error('Calculate tariff error:', error);
    return { ok: false, data: { error: (error as Error).message } };
  }
}

export async function getExchangeRate() {
  try {
    const headers = await getAuthHeaders();
    
    const response = await fetch(`${API_BASE_ROUTE}/exchange-rates`, {
      method: "GET",
      headers,
    })
    const data = await response.json()
    console.log('📊 Exchange rate data:', data)
    return { ok: response.ok, data }
  } catch (error) {
    console.error('Get exchange rate error:', error);
    return { ok: false, data: { error: (error as Error).message } };
  }
}

export async function searchProducts(query: string, limit: number = 5) {
  console.log('🔍 Searching products with query:', query);
  
  try {
    const headers = await getAuthHeaders();
    
    const response = await fetch(`${API_BASE_ROUTE}/products/search?q=${encodeURIComponent(query)}&limit=${limit}`, {
      method: "GET",
      headers,
    })
    const data = await response.json()
    return { ok: response.ok, data }
  } catch (error) {
    console.error('Search products error:', error);
    return { ok: false, data: { error: (error as Error).message } };
  }
}

export async function getCountries() {
  console.log('🌍 Fetching countries from database');
  
  try {
    const headers = await getAuthHeaders();
    
    const response = await fetch(`${API_BASE_ROUTE}/admin/tariffs/countries`, {
      method: "GET",
      headers,
    })
    const data = await response.json()
    return { ok: response.ok, data }
  } catch (error) {
    console.error('Get countries error:', error);
    return { ok: false, data: { error: (error as Error).message } };
  }
}

export async function getAvailableYears() {
  console.log('📅 Fetching available tariff years from database');
  
  try {
    const headers = await getAuthHeaders();
    
    // Fetch all tariffs and extract unique years
    const response = await fetch(`${API_BASE_ROUTE}/admin/tariffs`, {
      method: "GET",
      headers,
    })
    
    if (!response.ok) {
      throw new Error(`Failed to fetch tariffs: ${response.status}`)
    }
    
    const data = await response.json()
    
    // Extract unique years from tariffs
    const years = new Set<number>()
    if (data.tariffs && Array.isArray(data.tariffs)) {
      data.tariffs.forEach((tariff: any) => {
        if (tariff.tariffYear) {
          years.add(tariff.tariffYear)
        }
      })
    }
    
    // Convert to sorted array (newest first)
    const sortedYears = Array.from(years).sort((a, b) => b - a)
    
    return { ok: true, data: { years: sortedYears } }
  } catch (error) {
    console.error('Get available years error:', error);
    return { ok: false, data: { error: (error as Error).message, years: [] } };
  }
}

export async function getShippingRoute(params: {
  src_lat: number;
  src_lon: number;
  dst_lat: number;
  dst_lon: number;
}) {
  console.log('🚢 Getting shipping route:', params);
  
  const GRAPHDB_API = process.env.NEXT_PUBLIC_GRAPHDB_API || "http://localhost:8000";
  
  try {
    const response = await fetch(`${GRAPHDB_API}/shortest-route`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(params),
    })
    const data = await response.json()
    return { ok: response.ok, data }
  } catch (error) {
    console.error('Get shipping route error:', error);
    return { ok: false, data: { error: (error as Error).message } };
  }
}

export async function getOptimalRoutes(params: {
  src_lat: number;
  src_lon: number;
  dst_lat: number;
  dst_lon: number;
  time_constraint_hours?: number;
}) {
  console.log('🚢 Getting optimal routes:', params);
  
  const GRAPHDB_API = process.env.NEXT_PUBLIC_GRAPHDB_API || "http://localhost:8000";
  
  try {
    const response = await fetch(`${GRAPHDB_API}/optimal-routes`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(params),
    })
    const data = await response.json()
    return { ok: response.ok, data }
  } catch (error) {
    console.error('Get optimal routes error:', error);
    return { ok: false, data: { error: (error as Error).message } };
  }
}

// Country code to coordinates mapping (major port cities)
export const COUNTRY_COORDINATES: { [key: string]: { lat: number, lon: number, name: string } } = {
  "702": { lat: 1.29, lon: 103.85, name: "Singapore" },
  "840": { lat: 33.74, lon: -118.27, name: "Los Angeles, USA" },
  "156": { lat: 31.23, lon: 121.47, name: "Shanghai, China" },
  "392": { lat: 35.68, lon: 139.76, name: "Tokyo, Japan" },
  "528": { lat: 51.92, lon: 4.48, name: "Rotterdam, Netherlands" },
  "826": { lat: 51.51, lon: -0.12, name: "London, UK" },
  "276": { lat: 53.55, lon: 9.99, name: "Hamburg, Germany" },
  "036": { lat: -33.87, lon: 151.21, name: "Sydney, Australia" },
  "356": { lat: 18.97, lon: 72.83, name: "Mumbai, India" },
  "124": { lat: 49.28, lon: -123.12, name: "Vancouver, Canada" },
  "076": { lat: -23.96, lon: -46.33, name: "Santos, Brazil" },
  "056": { lat: 51.22, lon: 4.40, name: "Antwerp, Belgium" },
  "144": { lat: 6.93, lon: 79.85, name: "Colombo, Sri Lanka" },
  "158": { lat: 22.62, lon: 120.31, name: "Kaohsiung, Taiwan" },
  "608": { lat: 14.60, lon: 120.98, name: "Manila, Philippines" },
  "554": { lat: -36.84, lon: 174.76, name: "Auckland, New Zealand" },
  "784": { lat: 25.27, lon: 55.30, name: "Dubai, UAE" },
  "634": { lat: 25.29, lon: 51.53, name: "Doha, Qatar" },
  "792": { lat: 39.93, lon: 32.85, name: "Ankara, Turkey" },
  "410": { lat: 37.58, lon: 126.98, name: "Seoul, South Korea" },
  "000": { lat: 22.32, lon: 114.17, name: "World (Any Country)" }, // Generic/any country
}