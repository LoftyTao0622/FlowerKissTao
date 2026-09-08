type SessionReset = () => void

const resetters = new Set<SessionReset>()

/** 注册已实例化的私有 store；Pinia store 是单例，因此每个 reset 只会注册一次。 */
export function registerSessionReset(reset: SessionReset) {
  resetters.add(reset)
}

/** 登录态失效时同步清空全部已实例化的私有 store。 */
export function resetSessionStores() {
  resetters.forEach((reset) => reset())
}
