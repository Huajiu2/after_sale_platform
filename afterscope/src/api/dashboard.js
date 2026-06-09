import request from '../utils/request'

/** 5.1 获取仪表盘统计卡片 */
export function fetchStats() {
  return request.get('/dashboard/stats')
}

/** 5.2 近7日趋势图 */
export function fetchTrend() {
  return request.get('/dashboard/trend')
}

/** 5.3 售后类型占比 */
export function fetchTypeRatio() {
  return request.get('/dashboard/type-ratio')
}

/** 5.4 店铺售后 TOP10 排行 */
export function fetchStoreRanking() {
  return request.get('/dashboard/store-ranking')
}
