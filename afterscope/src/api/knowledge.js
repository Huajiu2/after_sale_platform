import request from '../utils/request'

/** 3.1 知识库文档列表（分页） */
export function fetchKnowledgeList(params) {
  return request({
    url: '/knowledge/list',
    method: 'get',
    params
  })
}

/** 3.2 上传知识库文档 */
export function uploadKnowledgeDoc(data) {
  return request({
    url: '/knowledge/upload',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 3.3 文档切片详情 */
export function fetchDocChunks(docId, params) {
  return request({
    url: `/knowledge/chunks/${docId}`,
    method: 'get',
    params
  })
}

/** 3.4 重新向量化文档 */
export function reVectorizeDoc(docId) {
  return request({
    url: `/knowledge/re-vectorize/${docId}`,
    method: 'post'
  })
}

/** 3.5 删除文档 */
export function deleteKnowledgeDoc(docId) {
  return request({
    url: `/knowledge/${docId}`,
    method: 'delete'
  })
}
