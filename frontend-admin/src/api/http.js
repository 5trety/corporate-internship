import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
http.interceptors.request.use(
  (config) => {
    // 如果是 POST/PUT 请求且 data 存在，确保是对象类型
    if ((config.method === 'post' || config.method === 'put') && config.data) {
      console.log('请求数据:', JSON.stringify(config.data, null, 2))
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message =
      error.response?.status === 502
        ? '服务暂不可用，请联系管理员或稍后再试'
        : error.response?.data?.message || error.message || '请求失败'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default http
