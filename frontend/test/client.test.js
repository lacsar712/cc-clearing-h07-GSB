import { describe, it, expect, vi, beforeEach } from 'vitest'

globalThis.localStorage = { getItem: () => null, setItem: vi.fn(), removeItem: vi.fn() }
globalThis.window = { location: { pathname: '/', href: '' } }
import axios from 'axios'

const errorSpy = vi.fn()
vi.mock('element-plus', () => ({
  ElMessage: { error: (...args) => errorSpy(...args), success: vi.fn() }
}))

import api from '../src/api/client.js'

function mockAdapter(status, data) {
  return (config) =>
    Promise.reject(Object.assign(new Error('Request failed with status code ' + status), {
      config,
      response: {
        status,
        statusText: '',
        headers: {},
        config,
        data
      },
      isAxiosError: true
    }))
}

describe('axios interceptor error copy', () => {
  beforeEach(() => {
    errorSpy.mockClear()
  })

  it('shows permission copy (not validation copy) for 403', async () => {
    const client = axios.create()
    // reuse the same interceptor logic through the exported instance with a per-request adapter
    api.defaults.adapter = mockAdapter(403, { code: 'FORBIDDEN', message: 'operator role required' })

    await expect(api.post('/obligations', {})).rejects.toBeTruthy()
    expect(errorSpy).toHaveBeenCalledTimes(1)
    const shown = errorSpy.mock.calls[0][0]
    expect(shown).toContain('无此操作权限')
    expect(shown).not.toContain('输入不合法')
  })

  it('shows validation copy for 400', async () => {
    api.defaults.adapter = mockAdapter(400, {
      code: 'VALIDATION_ERROR',
      message: 'validation failed',
      details: ['currency: must not be blank']
    })

    await expect(api.post('/obligations', {})).rejects.toBeTruthy()
    expect(errorSpy).toHaveBeenCalledTimes(1)
    const shown = errorSpy.mock.calls[0][0]
    expect(shown).toContain('输入不合法')
    expect(shown).not.toContain('无此操作权限')
  })
})
