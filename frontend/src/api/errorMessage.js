// Maps HTTP error responses to user-facing copy.
// 403 (forbidden / no permission) and 400 (invalid input) must stay
// distinguishable — never collapse one into the other's wording.
export function errorMessageFor(status, payload, fallback) {
  if (status === 403) {
    return '无权限执行此操作：需要操作员权限'
  }
  if (status === 400) {
    if (payload?.code === 'VALIDATION_ERROR') {
      const details = Array.isArray(payload?.details) ? payload.details : []
      return details.length > 0 ? `输入不合法：${details.join('；')}` : '输入不合法'
    }
    // Other 400s are domain/business errors — show the backend message as-is.
    return payload?.message || '输入不合法'
  }
  return payload?.message || fallback || '请求失败'
}
