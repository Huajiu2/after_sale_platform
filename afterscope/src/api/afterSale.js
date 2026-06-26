import request from '@/utils/request'

export function fetchAfterSaleList(params) {
  return request({
    url: '/after-sale/list',
    method: 'get',
    params
  })
}

export function fetchAfterSaleDetail(ticketNo) {
  return request({
    url: `/after-sale/detail/${ticketNo}`,
    method: 'get'
  })
}

export function submitManualAudit(data) {
  return request({
    url: '/after-sale/manual-audit',
    method: 'post',
    data
  })
}

export function batchAssignTickets(data) {
  return request({
    url: '/after-sale/batch-assign',
    method: 'post',
    data
  })
}

export function batchRetryTickets(data) {
  return request({
    url: '/after-sale/batch-retry',
    method: 'post',
    data
  })
}

export function exportAfterSaleTickets(params) {
  return request({
    url: '/after-sale/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function fetchStores(params) {
  return request({
    url: '/basic/stores',
    method: 'get',
    params
  })
}

export function fetchAfterSaleTypes() {
  return request({
    url: '/basic/after-sale-types',
    method: 'get'
  })
}

export function fetchTicketStatuses() {
  return request({
    url: '/basic/ticket-statuses',
    method: 'get'
  })
}
