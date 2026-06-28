import { describe, expect, it } from 'vitest'
import { findMenuByPath, firstReachableMenu, flattenMenuItems } from '../utils/menu'

describe('firstReachableMenu', () => {
  it('returns the first top-level path when available', () => {
    expect(firstReachableMenu([{ path: '/dashboard' }])).toBe('/dashboard')
  })

  it('returns the first nested path when parent only groups children', () => {
    const menus = [{ path: null, children: [{ path: '/system/user' }] }]

    expect(firstReachableMenu(menus)).toBe('/system/user')
  })

  it('falls back to dashboard when no path exists', () => {
    expect(firstReachableMenu([{ path: null, children: [] }])).toBe('/dashboard')
  })

  it('flattens only menu items that have paths', () => {
    const menus = [
      { id: 1, name: '工作台', path: '/dashboard' },
      {
        id: 2,
        name: '系统管理',
        path: null,
        children: [
          { id: 21, name: '用户管理', path: '/system/user' },
          { id: 22, name: '角色管理', path: '/system/role' }
        ]
      }
    ]

    expect(flattenMenuItems(menus)).toEqual([
      { id: 1, name: '工作台', path: '/dashboard' },
      { id: 21, name: '用户管理', path: '/system/user' },
      { id: 22, name: '角色管理', path: '/system/role' }
    ])
  })

  it('finds a tab source by route path', () => {
    const menus = [{ id: 3, name: '标签页示例', path: '/tabs' }]

    expect(findMenuByPath(menus, '/tabs')).toEqual({
      id: 3,
      name: '标签页示例',
      path: '/tabs'
    })
  })
})
