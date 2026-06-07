import axios from 'axios'
const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截
service.interceptors.request.use(config => {
  // 统一token头
  config.headers['token'] = localStorage.getItem('token') || ''
  return config
})

// 响应拦截
service.interceptors.response.use(res=>res.data, err=>{
  console.error('接口异常',err)
  return Promise.reject(err)
})
export default service