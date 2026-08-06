<template>
  <span class="brand-mark" aria-hidden="true">
    <svg viewBox="0 0 48 48" fill="none">
      <path
        d="M24 39V20"
        stroke="currentColor"
        stroke-width="2.5"
        stroke-linecap="round"
      />
      <path
        d="M24 25C14.5 25 9 19.5 9 10c9.5 0 15 5.5 15 15Z"
        fill="currentColor"
        opacity="0.92"
      />
      <path
        d="M24 31c9.5 0 15-5.5 15-15-9.5 0-15 5.5-15 15Z"
        fill="currentColor"
        opacity="0.68"
      />
      <path
        d="M16 39h16"
        stroke="currentColor"
        stroke-width="2.5"
        stroke-linecap="round"
      />
    </svg>
  </span>
</template>

<style scoped>
.brand-mark {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  place-items: center;
  border-radius: 50%;
  background: var(--color-brand-deep);
  color: var(--color-on-brand);
}

svg {
  width: 30px;
  height: 30px;
}
</style>
