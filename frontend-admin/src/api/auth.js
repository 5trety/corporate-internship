import http from './http'

export function login(username, password) {
  const body = new URLSearchParams()
  body.set('username', username)
  body.set('password', password)

  return http.post('/login', body, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

export function logout() {
  return http.post('/logout')
}

export function fetchMenus() {
  return http.get('/menus')
}
