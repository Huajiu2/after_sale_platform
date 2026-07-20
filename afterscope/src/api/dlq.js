import request from '../utils/request'

/** 4.1 死信消息列表（分页） */
export function fetchDlqList(params) {
  return request({
    url: '/dlq/list',
    method: 'get',
    params
  })
}

/** 4.2 单条死信重试 */
export function retryDlq(id) {
  return request({
    url: `/dlq/retry/${id}`,
    method: 'post'
  })
}

/** 4.3 批量重试死信 */
export function batchRetryDlq(data) {
  return request({
    url: '/dlq/batch-retry',
    method: 'post',
    data
  })
}

/** 4.4 删除死信消息 */
export function deleteDlq(id) {
  return request({
    url: `/dlq/${id}`,
    method: 'delete'
  })
}
