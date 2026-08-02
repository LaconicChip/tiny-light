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
  <Transition name="modal">
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
  </Transition>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(8, 8, 26, 0.6);
  backdrop-filter: blur(12px) saturate(120%);
  -webkit-backdrop-filter: blur(12px) saturate(120%);
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
  background: var(--glass-dark);
  backdrop-filter: blur(24px) saturate(140%);
  -webkit-backdrop-filter: blur(24px) saturate(140%);
  border: 1px solid var(--glass-border);
  border-radius: 20px;
  padding: 36px 32px 28px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.4), 0 0 80px rgba(237, 206, 110, 0.06);
}
.modal::before {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: 20px;
  padding: 1px;
  background: linear-gradient(135deg, rgba(237,206,110,0.3), transparent 40%, transparent 60%, rgba(237,206,110,0.15));
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}
/* spring 进场 */
.modal-enter-from .modal, .modal-leave-to .modal {
  transform: scale(0.96) translateY(8px);
  opacity: 0;
}
.modal-enter-active .modal, .modal-leave-active .modal {
  transition: transform 280ms cubic-bezier(0.16, 1, 0.3, 1), opacity 200ms ease-out;
}
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-active, .modal-leave-active { transition: opacity 200ms ease-out; }

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
  color: var(--platinum-3);
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
  font-size: 0.72rem;
  color: var(--gold-3);
  letter-spacing: 0.2em;
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.modal-date svg { opacity: 0.6; }
.modal-content {
  margin: 0 0 16px;
  font-size: 1.05rem;
  font-weight: 300;
  line-height: 1.95;
  color: var(--platinum-1);
  font-family: var(--font-sans);
  letter-spacing: 0.03em;
}
.modal-mood {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-family: var(--font-mono);
  font-size: 0.72rem;
  color: var(--gold-3);
  letter-spacing: 0.15em;
}
.mood-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--gold-2);
  box-shadow: 0 0 8px var(--gold-glow-mid);
}
@media (prefers-reduced-motion: reduce) {
  .modal-enter-active .modal, .modal-leave-active .modal {
    transition: opacity 150ms ease;
    transform: none;
  }
}
</style>
