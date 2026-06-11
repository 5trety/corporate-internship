import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true
})

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
