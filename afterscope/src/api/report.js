import request from '../utils/request'

/** 6.1 月度统计总览 */
export function fetchMonthlySummary(month) {
  return request({
    url: '/report/monthly-summary',
    method: 'get',
    params: { month }
  })
}

/** 6.2 每日明细 */
export function fetchDailyDetail(month) {
  return request({
    url: '/report/daily',
    method: 'get',
    params: { month }
  })
}

/** 6.3 月度店铺排行 */
export function fetchStoreRanking(month, params) {
  return request({
    url: '/report/store-ranking',
    method: 'get',
    params: { month, ...params }
  })
}

/** 6.4 通过率趋势 */
export function fetchRateTrend(months) {
  return request({
    url: '/report/rate-trend',
    method: 'get',
    params: { months }
  })
}

/** 6.5 导出报表 */
export function exportReport(params) {
  return request({
    url: '/report/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
