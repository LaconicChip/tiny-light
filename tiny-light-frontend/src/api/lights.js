import axios from 'axios'

// 【临时共享模式】全设备统一用户，方便多终端测试。
// 恢复多用户时改回：从 localStorage 取 userId，没有就生成随机 ID。
const SHARED_USER_ID = 'shared-user'
export function getUserId() {
  return SHARED_USER_ID
}

// axios 实例：所有请求自动加 /api 前缀，走 Vite 代理转发到后端 8080
const api = axios.create({
  baseURL: '/api',
})

// 创建一条微光（防重复：一人一天一颗）
export function createLight({ content, mood }) {
  return api.post('/lights', { userId: getUserId(), content, mood })
}

// 查今日微光
export function getToday() {
  return api.get('/lights/today', { params: { userId: getUserId() } })
}

// 查年度星光河
export function getRiver(year) {
  return api.get('/lights/river', { params: { userId: getUserId(), year } })
}

// 查那年今日
export function getOnThisDay() {
  return api.get('/lights/on-this-day', { params: { userId: getUserId() } })
}

// 查某条详情
export function getLightById(id) {
  return api.get(`/lights/${id}`, { params: { userId: getUserId() } })
}

// 编辑今天的微光
export function updateLight(id, { content, mood }) {
  return api.put(`/lights/${id}`, { userId: getUserId(), content, mood })
}

// 删除今天的微光
export function deleteLight(id) {
  return api.delete(`/lights/${id}`, { params: { userId: getUserId() } })
}

// 查统计
export function getStats() {
  return api.get('/lights/stats', { params: { userId: getUserId() } })
}
