import request from '../utils/request'

/** 7.1 查询所有配置（按分组） */
export function fetchConfigList(group) {
  return request({
    url: '/config/list',
    method: 'get',
    params: group ? { group } : {}
  })
}

/** 7.2 更新配置 */
export function updateConfig(data) {
  return request({
    url: '/config/update',
    method: 'put',
    data
  })
}

/** 7.3 批量更新配置 */
export function batchUpdateConfig(data) {
  return request({
    url: '/config/batch-update',
    method: 'put',
    data
  })
}

/** 7.4 恢复默认 */
export function resetConfig(id) {
  return request({
    url: `/config/reset/${id}`,
    method: 'post'
  })
}
