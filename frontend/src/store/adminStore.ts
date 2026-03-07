const ADMIN_KEY_PREFIX = 'potlog-admin-'

export function getStoredAdminPassword(numericId: string): string | null {
  return localStorage.getItem(ADMIN_KEY_PREFIX + numericId)
}

export function setStoredAdminPassword(numericId: string, password: string): void {
  localStorage.setItem(ADMIN_KEY_PREFIX + numericId, password)
}

/** 是否有权限修改（管理员或非 adminOnly 的 session） */
export function canModify(numericId: string, adminOnly?: boolean): boolean {
  if (!adminOnly) return true
  return getStoredAdminPassword(numericId) !== null
}

export function setAdmin(numericId: string, password: string): void {
  setStoredAdminPassword(numericId, password)
}
