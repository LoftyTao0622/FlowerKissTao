/**
 * 防止账号切换或请求竞态把旧响应写回 Pinia store。
 *
 * session 代表登录会话；sequence 代表同一会话中最近一次加载。reset 时两个
 * 维度都会失效，因此迟到的请求即使成功也不能修改新账号的数据。
 */
export function createRequestGuard() {
  let session = 0
  const sequences = new Map<string, number>()

  function begin(scope = 'default') {
    const sequence = (sequences.get(scope) ?? 0) + 1
    sequences.set(scope, sequence)
    const token = { session, scope, sequence }
    return token
  }

  function captureSession() {
    return session
  }

  function isSessionCurrent(expectedSession: number) {
    return expectedSession === session
  }

  function isCurrent(token: { session: number; scope: string; sequence: number }) {
    return token.session === session && token.sequence === sequences.get(token.scope)
  }

  function reset() {
    session += 1
    sequences.clear()
  }

  return { begin, captureSession, isSessionCurrent, isCurrent, reset }
}
