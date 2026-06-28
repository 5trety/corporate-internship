import axios from 'axios'
import { ElMessage } from 'element-plus'

export const createHttpClient = (baseURL = '/api') => {
  const client = axios.create({
    baseURL,
    timeout: 60000,
    withCredentials: true
  })

  client.interceptors.response.use(
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

  return client
}

const http = createHttpClient('/api')

export default http
