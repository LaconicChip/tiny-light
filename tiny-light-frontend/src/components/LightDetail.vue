<script setup>
import { onMounted, onUnmounted } from 'vue'
import { PhX } from '@phosphor-icons/vue'

const props = defineProps({
  light: Object,
})
const emit = defineEmits(['close'])

function onKey(e) {
  if (e.key === 'Escape') emit('close')
}
onMounted(() => window.addEventListener('keydown', onKey))
onUnmounted(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <Transition name="modal">
    <div class="overlay" @click.self="$emit('close')">
      <div class="modal" role="dialog" aria-modal="true">
        <button class="close-btn" @click="$emit('close')" aria-label="关闭">
          <PhX :size="18" weight="regular" />
        </button>

        <div class="modal-inner">
          <p class="date-stamp">{{ light.lightDate }}</p>
          <p class="content">{{ light.content }}</p>
          <p v-if="light.mood" class="mood">
            <span class="mood-dot" />
            <span>{{ light.mood }}</span>
          </p>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(45, 36, 24, 0.32);
  backdrop-filter: blur(10px) saturate(120%);
  -webkit-backdrop-filter: blur(10px) saturate(120%);
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
  background: var(--bg-paper);
  border-left: 2px solid var(--gold);
  border-radius: 0 12px 12px 0;
  box-shadow:
    0 24px 60px rgba(45, 36, 24, 0.18),
    0 6px 16px rgba(45, 36, 24, 0.08);
}
.modal-inner {
  padding: 36px 32px 28px;
}
/* 苹果式：spring 进场，critically damped */
.modal-enter-from .modal,
.modal-leave-to .modal {
  transform: scale(0.96) translateY(8px);
  opacity: 0;
}
.modal-enter-active .modal,
.modal-leave-active .modal {
  transition:
    transform 280ms cubic-bezier(0.16, 1, 0.3, 1),
    opacity 200ms ease-out;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-active,
.modal-leave-active {
  transition: opacity 200ms ease-out;
}
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
  color: var(--text-soft);
  cursor: pointer;
  border-radius: 8px;
  transition: color 150ms ease, background 150ms ease;
}
.close-btn:hover {
  color: var(--text);
  background: rgba(201, 169, 97, 0.1);
}
.date-stamp {
  margin: 0 0 18px;
  color: var(--gold);
  font-size: 13px;
  font-family: var(--font-display);
  letter-spacing: 0.12em;
}
.content {
  margin: 0 0 16px;
  font-size: 18px;
  line-height: 1.95;
  color: var(--text);
  font-family: var(--font-display);
  letter-spacing: 0.02em;
}
.mood {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: var(--text-soft);
  font-size: 14px;
  font-family: var(--font-display);
  letter-spacing: 0.06em;
}
.mood-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--gold-lit);
  box-shadow: 0 0 6px var(--gold-glow);
}
@media (prefers-reduced-motion: reduce) {
  .modal-enter-active .modal,
  .modal-leave-active .modal {
    transition: opacity 150ms ease;
    transform: none;
  }
}
</style>
