export function firstReachableMenu(menus) {
  for (const item of menus) {
    if (item.path) return item.path
    if (item.children?.length) {
      const childPath = firstReachableMenu(item.children)
      if (childPath) return childPath
    }
  }

  return '/dashboard'
}

export function flattenMenuItems(menus) {
  const items = []

  for (const item of menus) {
    if (item.path) {
      items.push({
        id: item.id,
        name: item.name,
        path: item.path
      })
    }

    if (item.children?.length) {
      items.push(...flattenMenuItems(item.children))
    }
  }

  return items
}

export function findMenuByPath(menus, path) {
  return flattenMenuItems(menus).find((item) => item.path === path) || null
}
