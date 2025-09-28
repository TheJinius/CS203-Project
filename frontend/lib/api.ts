import { getSession } from "next-auth/react";

const API_BASE_ROUTE = process.env.NEXT_PUBLIC_API_BASE_ROUTE || "http://localhost:8080/api"

// Helper function to get current auth headers
async function getAuthHeaders() {
  const session = await getSession();
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

  const headers = await getAuthHeaders();
  
  const response = await fetch(`${API_BASE_ROUTE}/tariffs/search`, {
    method: "POST",
    headers,
    body: JSON.stringify(backendRequest), // Send mapped data
  })
  const data = await response.json()
  return { ok: response.ok, data }
}

export async function calculateTariff(params: {
  reporterCode: string;
  partnerCode: string;
  productCode: string;
  tariffId: number;
  amountOfProduct: number;
  currency: string;
}) {
  console.log('🧮 Calculating tariff with data:', params);
  
  const headers = await getAuthHeaders();
  
  const response = await fetch(`${API_BASE_ROUTE}/tariffs/calculate`, {
    method: "POST",
    headers,
    body: JSON.stringify(params),
  })
  const data = await response.json()
  return { ok: response.ok, data }
}

export async function getExchangeRate() {
  const headers = await getAuthHeaders();
  
  const response = await fetch(`${API_BASE_ROUTE}/exchange-rates`, {
    method: "GET",
    headers,
  })
  const data = await response.json()
  console.log('📊 Exchange rate data:', data)
  return { ok: response.ok, data }
}