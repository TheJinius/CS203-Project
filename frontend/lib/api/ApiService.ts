import { getSession } from 'next-auth/react'

export class ApiService {
  private static baseUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

  static async getAuthHeaders(): Promise<Record<string, string>> {
    try {
      console.log('🔍 Fetching auth session...');
      console.log('🔧 Environment check:', {
        region: process.env.NEXT_PUBLIC_AWS_REGION,
        userPoolId: process.env.NEXT_PUBLIC_COGNITO_USER_POOL_ID ? 'SET' : 'MISSING',
        clientId: process.env.NEXT_PUBLIC_COGNITO_CLIENT_ID ? 'SET' : 'MISSING',
        apiUrl: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'
      });
      
      // Check if user is logged in first
      try {
        const session = await getSession();
        console.log('✅ Current user found:', session?.user?.name);
      } catch (userError) {
        console.error('❌ No current user found:', userError);
        throw new Error('User not authenticated - please log in');
      }
      
      // Get the session
      const session = await getSession();
      console.log('📋 Session details:', {
        credentials: !!session.credentials,
        tokens: !!session.tokens,
        accessToken: !!session.tokens?.accessToken,
        idToken: !!session.tokens?.idToken,
      });

      if (!session.tokens) {
        console.error('❌ No tokens in session');
        throw new Error('No authentication tokens available - please log in again');
      }

      const accessToken = session.tokens.accessToken?.toString();
      
      if (!accessToken) {
        console.error('❌ No access token found');
        throw new Error('No authentication token available');
      }

      console.log('✅ Access token found, length:', accessToken.length);
      console.log('🔐 Token preview:', accessToken.substring(0, 50) + '...');

      return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`,
      };
    } catch (error) {
      console.error('❌ Failed to get auth headers:', error);
      
      if (error instanceof Error) {
        throw error; // Re-throw the specific error
      }
      
      throw new Error('Authentication failed - please log in');
    }
  }

  static async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    try {
      console.log(`🚀 API Request: ${options.method || 'GET'} ${endpoint}`);
      
      const headers = await this.getAuthHeaders();
      console.log('📤 Headers prepared:', {
        'Content-Type': headers['Content-Type'],
        'Authorization': 'Bearer [REDACTED]'
      });
      
      const fullUrl = `${this.baseUrl}${endpoint}`;
      console.log('🌐 Full URL:', fullUrl);
      
      const response = await fetch(fullUrl, {
        ...options,
        headers: {
          ...headers,
          ...options.headers,
        },
      });

      console.log(`📥 Response: ${response.status} ${response.statusText}`);

      if (!response.ok) {
        const responseText = await response.text();
        console.error('❌ Error response body:', responseText);
        
        if (response.status === 401) {
          throw new Error('🔐 Authentication failed - please log in again');
        }
        if (response.status === 403) {
          throw new Error('🚫 Access denied - insufficient permissions');
        }
        
        let errorData;
        try {
          errorData = JSON.parse(responseText);
        } catch {
          errorData = { message: responseText };
        }
        
        throw new Error(errorData.message || `HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();
      console.log('✅ API Success for:', endpoint);
      return data;
      
    } catch (error) {
      console.error(`💥 API Error [${endpoint}]:`, error);
      throw error;
    }
  }

  // Tariff API methods
  static async searchTariffs(searchData: {
    reporter: string;
    partner: string;
    tlCode: string;
    year: number;
  }) {
    console.log('🔍 Searching tariffs with data:', searchData);
    return this.request('/api/tariffs/search', {
      method: 'POST',
      body: JSON.stringify(searchData),
    });
  }

  static async calculateTariff(calculationData: {
    reporterCode: string;
    partnerCode: string;
    productCode: string;
    tariffId: number;
    amountOfProduct: number;
    currency: string;
  }) {
    console.log('🧮 Calculating tariff with data:', calculationData);
    return this.request('/api/tariffs/calculate', {
      method: 'POST',
      body: JSON.stringify(calculationData),
    });
  }

  static async getAllTariffs() {
    return this.request('/api/tariffs');
  }

  static async getProducts() {
    return this.request('/api/products');
  }

  static async getCountries() {
    return this.request('/api/countries');
  }
}