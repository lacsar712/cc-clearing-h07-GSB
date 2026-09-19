// Maps an HTTP error response to user-facing copy.
// Permission failures (401/403) and client validation failures (400) must
// never be shown with the same message — authz must not masquerade as bad input.

export function flattenErrorMessage(status, payload, fallback) {
  const details = Array.isArray(payload?.details) ? payload.details.filter(Boolean) : []
  const serverMessage = payload?.message || fallback

  if (status === 403) {
    const base = '禁止访问：当前账号无此操作权限（需要操作员权限）'
    return serverMessage ? `${base}（${serverMessage}）` : base
  }

  if (status === 401) {
    return '登录状态无效或已过期，请重新登录'
  }

  if (status === 400) {
    const base = '输入不合法，请检查表单字段'
    if (details.length > 0) {
      return `${base}：${details.join('；')}`
    }
    return serverMessage ? `${base}（${serverMessage}）` : base
  }

  return serverMessage || '请求失败'
}

export function isPermissionError(status) {
  return status === 401 || status === 403
}

export function isValidationError(status) {
  return status === 400
}
