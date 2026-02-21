const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

interface RequestOptions extends RequestInit {
  params?: Record<string, string>
}

class HttpClient {
  private baseUrl: string

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl
  }

  private getToken(): string | null {
    return localStorage.getItem('cr-notebook-token')
  }

  private getTenantHeaders(): Record<string, string> {
    const tenantId = localStorage.getItem('cr-notebook-tenant-id')
    const tenantType = localStorage.getItem('cr-notebook-tenant-type')
    if (tenantId && tenantType) {
      return { 'X-Tenant-Id': tenantId, 'X-Tenant-Type': tenantType }
    }
    return {}
  }

  private async request<T>(path: string, options: RequestOptions = {}): Promise<T> {
    const { params, ...init } = options
    let url = `${this.baseUrl}${path}`

    if (params) {
      const searchParams = new URLSearchParams(params)
      url += `?${searchParams.toString()}`
    }

    const token = this.getToken()
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...this.getTenantHeaders(),
      ...(init.headers as Record<string, string> || {}),
    }
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    const response = await fetch(url, { ...init, headers })

    if (response.status === 401) {
      localStorage.removeItem('cr-notebook-token')
      localStorage.removeItem('cr-notebook-user')
      localStorage.removeItem('cr-notebook-tenant-id')
      localStorage.removeItem('cr-notebook-tenant-type')
      window.location.href = '/login'
      throw new Error('登录已过期，请重新登录')
    }

    if (!response.ok) {
      const error = await response.json().catch(() => ({ error: 'Request failed' }))
      throw new Error(error.message || error.error || `HTTP ${response.status}`)
    }

    if (response.status === 204) return undefined as T
    return response.json()
  }

  get<T>(path: string, params?: Record<string, string>) {
    return this.request<T>(path, { method: 'GET', params })
  }

  post<T>(path: string, body?: unknown) {
    return this.request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined })
  }

  put<T>(path: string, body?: unknown) {
    return this.request<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined })
  }

  delete<T>(path: string) {
    return this.request<T>(path, { method: 'DELETE' })
  }
}

export const http = new HttpClient(BASE_URL)
