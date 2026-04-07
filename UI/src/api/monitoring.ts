import apiClient from './client'

export const monitoringApi = {
  getQpsMetrics: (start: string, end: string) => 
    apiClient.get('/api/v1/monitoring/qps', { params: { start, end } }),
  
  getLatencyMetrics: (start: string, end: string) => 
    apiClient.get('/api/v1/monitoring/latency', { params: { start, end } }),
  
  getTokenMetrics: (start: string, end: string) => 
    apiClient.get('/api/v1/monitoring/tokens', { params: { start, end } }),
  
  getServiceHealth: () => 
    apiClient.get('/api/v1/monitoring/health')
}
