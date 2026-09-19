import { describe, it, expect } from 'vitest'
import { flattenErrorMessage, isPermissionError, isValidationError } from '../src/api/errorMessage.js'

describe('flattenErrorMessage', () => {
  it('uses permission copy for 403, never validation copy', () => {
    const msg = flattenErrorMessage(403, { code: 'FORBIDDEN', message: 'operator role required' })
    expect(msg).toContain('无此操作权限')
    expect(msg).not.toContain('输入不合法')
    expect(isPermissionError(403)).toBe(true)
    expect(isValidationError(403)).toBe(false)
  })

  it('uses session copy for 401, never validation copy', () => {
    const msg = flattenErrorMessage(401, { code: 'UNAUTHORIZED', message: 'invalid or expired token' })
    expect(msg).toMatch(/登录|重新登录/)
    expect(msg).not.toContain('输入不合法')
  })

  it('uses validation copy for 400 and surfaces field details', () => {
    const msg = flattenErrorMessage(
      400,
      {
        code: 'VALIDATION_ERROR',
        message: 'validation failed',
        details: ['currency: must not be blank', 'amount: must not be null']
      }
    )
    expect(msg).toContain('输入不合法')
    expect(msg).toContain('currency: must not be blank')
    expect(isValidationError(400)).toBe(true)
    expect(isPermissionError(400)).toBe(false)
  })

  it('keeps the two failure classes visually distinct', () => {
    const authz = flattenErrorMessage(403, { code: 'FORBIDDEN', message: 'operator role required' })
    const validation = flattenErrorMessage(400, { code: 'VALIDATION_ERROR', message: 'validation failed' })
    expect(authz).not.toBe(validation)
    expect(authz).not.toContain('输入不合法')
    expect(validation).toContain('输入不合法')
  })
})
