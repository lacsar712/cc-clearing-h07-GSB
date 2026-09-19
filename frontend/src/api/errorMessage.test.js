import { describe, expect, it } from 'vitest'
import { errorMessageFor } from './errorMessage'

describe('errorMessageFor', () => {
  it('forbidden (403) copy differs from validation (400) copy', () => {
    const forbidden = errorMessageFor(403, { code: 'FORBIDDEN', message: 'operator role required' })
    const invalid = errorMessageFor(400, {
      code: 'VALIDATION_ERROR',
      message: 'validation failed',
      details: ['currency: must not be blank']
    })

    expect(forbidden).not.toBe(invalid)
    expect(forbidden).toContain('无权限')
    expect(forbidden).not.toContain('输入不合法')
    expect(invalid).toContain('输入不合法')
  })

  it('validation error includes field details so missing fields are recognizable', () => {
    const msg = errorMessageFor(400, {
      code: 'VALIDATION_ERROR',
      message: 'validation failed',
      details: ['payerMemberId: must not be blank']
    })
    expect(msg).toContain('输入不合法')
    expect(msg).toContain('payerMemberId')
  })

  it('403 never falls back to validation wording even with a validation-looking payload', () => {
    const msg = errorMessageFor(403, { code: 'FORBIDDEN', message: 'operator role required' }, 'fallback')
    expect(msg).toContain('无权限')
    expect(msg).not.toContain('输入不合法')
  })
})
