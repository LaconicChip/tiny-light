<script setup>
import { onMounted, onUnmounted } from 'vue'

const props = defineProps({
  light: Object,
})
const emit = defineEmits(['close'])

function onKey(e) {
  if (e.key === 'Escape') emit('close')
}
onMounted(() => window.addEventListener('keydown', onKey))
onUnmounted(() => window.removeEventListener('keydown', onKey))

const monthEng = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC']
function formatDate(ds) {
  const [y, m, d] = ds.split('-')
  return `${y} · ${monthEng[parseInt(m) - 1]} ${d}`
}
</script>

<template>
  <div class="overlay" @click.self="$emit('close')">
    <div class="modal" role="dialog" aria-modal="true">
      <button class="close-btn" @click="$emit('close')" aria-label="关闭">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      </button>
      <div class="modal-date">
        <svg width="10" height="10" viewBox="0 0 16 16" fill="currentColor"><path d="M8 0l1.8 5.5L16 8l-6.2 2.5L8 16l-1.8-5.5L0 8l6.2-2.5z"/></svg>
        {{ formatDate(light.lightDate) }}
      </div>
      <p class="modal-content">{{ light.content }}</p>
      <p v-if="light.mood" class="modal-mood">
        <span class="mood-dot"></span>
        {{ light.mood }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(8, 8, 26, 0.55);
  backdrop-filter: blur(20px) saturate(140%);
  -webkit-backdrop-filter: blur(20px) saturate(140%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 20px;
}
.modal {
  position: relative;
  max-width: 460px;
  width: 100%;
  background: rgba(250, 248, 245, 0.82);
  backdrop-filter: blur(22px) saturate(150%) brightness(1.04);
  -webkit-backdrop-filter: blur(22px) saturate(150%) brightness(1.04);
  border: 1px solid rgba(237, 206, 110, 0.32);
  border-radius: 24px;
  padding: 36px 32px 28px;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.4), 0 24px 60px rgba(60, 50, 30, 0.14), 0 8px 24px rgba(60, 50, 30, 0.06), 0 0 80px rgba(237, 206, 110, 0.1);
}
.modal::before {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: 24px;
  padding: 1px;
  background: linear-gradient(135deg, rgba(237,206,110,0.5), transparent 40%, transparent 60%, rgba(237,206,110,0.25));
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}
/* spring 进场 — appear 让组件首次挂载也触发 */
.modal-enter-from .modal, .modal-leave-to .modal {
  transform: scale(0.88) translateY(32px);
  opacity: 0;
}
.modal-enter-active .modal, .modal-leave-active .modal {
  transition: transform 480ms cubic-bezier(0.16, 1, 0.3, 1), opacity 320ms ease-out;
}
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-active, .modal-leave-active { transition: opacity 320ms ease-out; }

.close-btn {
  position: absolute;
  top: 14px;
  right: 16px;
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--text-dark-muted);
  cursor: pointer;
  border-radius: 8px;
  transition: color 0.2s, background 0.2s;
}
.close-btn:hover {
  color: var(--gold-3);
  background: rgba(237, 206, 110, 0.1);
}
.modal-date {
  font-family: var(--font-mono);
  font-size: 0.78rem;
  color: var(--gold-1);
  letter-spacing: 0.2em;
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.modal-date svg { opacity: 0.85; }
.modal-content {
  margin: 0 0 16px;
  font-size: 1.05rem;
  font-weight: 300;
  line-height: 1.95;
  color: var(--text-dark);
  font-family: var(--font-sans);
  letter-spacing: 0.03em;
}
.modal-mood {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-family: var(--font-mono);
  font-size: 0.78rem;
  color: var(--gold-1);
  letter-spacing: 0.15em;
}
.mood-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--gold-1);
  box-shadow: 0 0 8px var(--gold-glow-mid);
}
@media (prefers-reduced-motion: reduce) {
  .modal-enter-active .modal, .modal-leave-active .modal {
    transition: opacity 150ms ease;
    transform: none;
  }
}
</style>
