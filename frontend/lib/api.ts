import { getSession } from "next-auth/react";

const API_BASE_ROUTE = process.env.NEXT_PUBLIC_API_BASE_ROUTE || "http://localhost:8080/api"
const session = await getSession();
const token = session?.accessToken;

export async function searchTariffs(params: {
  reporter: string;
  partner: string;
  tlCode: string;
  year: number;
}) {
  const response = await fetch(`${API_BASE_ROUTE}/tariffs/search`, {
    method: "POST",
    headers: { 
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`
     },
    body: JSON.stringify(params),
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
  const response = await fetch(`${API_BASE_ROUTE}/tariffs/calculate`, {
    method: "POST",
    headers: { 
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`
     },
    body: JSON.stringify(params),
  })
  const data = await response.json()
  return { ok: response.ok, data }
}

export async function getExchangeRate() {
    const response = await fetch(`${API_BASE_ROUTE}/exchange-rates`, {
    method: "GET",
    headers: { 
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`
     },
  })
  const data = await response.json()
  console.log(data)
  return { ok: response.ok, data }
}