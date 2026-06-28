/**
 * AI需求预测API
 */
import { createHttpClient } from './http'

const aiHttp = createHttpClient('/ai-api')

export const aiPredictAPI = {
  /**
   * 获取所有预警
   */
  getAllWarnings() {
    return aiHttp.get('/api/warning/all')
  },
  
  /**
   * 获取零件预警
   */
  getPartWarnings(partCode) {
    return aiHttp.get(`/api/warning/part/${partCode}`)
  },
  
  /**
   * 获取预测趋势（固定7天）
   */
  getPredictionTrend(partCode) {
    return aiHttp.get(`/api/predict/trend/${partCode}`)
  },
  
  /**
   * 获取趋势图（固定7天）
   */
  getTrendChart(partCode) {
    return aiHttp.get(`/api/chart/trend/${partCode}`)
  },
  
  /**
   * 获取看板汇总
   */
  getDashboardSummary() {
    return aiHttp.get('/api/dashboard/summary')
  },
  
  /**
   * 获取零件列表
   */
  getAllParts() {
    return aiHttp.get('/api/parts/all')
  },
  
  /**
   * 获取库存汇总
   */
  getInventorySummary() {
    return aiHttp.get('/api/inventory/summary')
  }
}
