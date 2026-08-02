<script setup>
import { computed } from 'vue'

const props = defineProps({
  lights: Array,
})
const emit = defineEmits(['select'])

const monthEng = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC']

/* 散落卡片最多 3 张（最近 3 年），位置/旋转按设计预览 */
const positions = [
  { top: '5px', left: '0%', right: 'auto', rot: -3, delay: '0.1s' },
  { top: '60px', left: '34%', right: 'auto', rot: 2.5, delay: '0.3s' },
  { top: '10px', left: 'auto', right: '2%', rot: -1.5, delay: '0.5s' },
]

const items = computed(() => {
  if (!props.lights?.length) return []
  // 最近 3 条，按年份倒序
  const sorted = [...props.lights].sort((a, b) => b.lightDate.localeCompare(a.lightDate))
  return sorted.slice(0, 3).map((l, i) => {
    const [y, m, d] = l.lightDate.split('-')
    const pos = positions[i] || positions[positions.length - 1]
    return {
      id: l.id,
      year: y,
      dateLabel: `${monthEng[parseInt(m) - 1]} ${d}`,
      content: l.content,
      mood: l.mood,
      pos,
      raw: l,
    }
  })
})
</script>

<template>
  <template v-if="items.length">
    <h2 class="memories-header">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
      往年今日
    </h2>
    <div class="memories-scatter">
      <div
        v-for="it in items"
        :key="it.id"
        class="memory-card"
        :style="{
          top: it.pos.top,
          left: it.pos.left,
          right: it.pos.right,
          '--cr': it.pos.rot + 'deg',
          'transition-delay': it.pos.delay,
        }"
        @click="emit('select', it.raw)"
      >
        <div class="memory-year">
          <svg width="8" height="8" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="12" r="4"/></svg>
          {{ it.year }} · {{ it.dateLabel }}
        </div>
        <div class="memory-text">{{ it.content }}</div>
        <div class="memory-mood">
          <svg width="8" height="8" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="12" r="5"/></svg>
          {{ it.mood }}
        </div>
      </div>
    </div>
  </template>
</template>

<style scoped>
.memories-header {
  font-family: var(--font-sans);
  font-size: clamp(1.1rem, 2.5vw, 1.6rem);
  font-weight: 300;
  color: var(--text-dark);
  letter-spacing: 0.18em;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 10px;
  opacity: 0;
  transform: translate3d(0, 18px, 0);
  transition: all 0.9s var(--ease-out);
}
.memories-header.visible {
  opacity: 1;
  transform: translate3d(0, 0, 0);
}
.memories-header svg { width: 18px; height: 18px; color: var(--gold-1); opacity: 0.5; }

.memories-scatter { position: relative; min-height: 240px; max-width: 800px; }

.memory-card {
  position: absolute;
  width: clamp(190px, 38%, 240px);
  background: var(--platinum-1);
  border-radius: 5px;
  padding: 22px 18px 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05), 0 10px 28px rgba(0, 0, 0, 0.04);
  opacity: 0;
  transform: translate3d(0, 25px, 0) rotate(var(--cr, 0deg));
  transition: all 0.8s var(--ease-out);
  will-change: transform; /* 仅 3 张卡：常驻合成层，消除 hover 瞬间临时建层的首帧延迟 */
  cursor: pointer;
}
.memory-card.visible {
  opacity: 1;
  transform: translate3d(0, 0, 0) rotate(var(--cr, 0deg));
}
.memory-card:hover {
  transform: translate3d(0, -6px, 0) rotate(0deg) scale(1.03);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06), 0 16px 40px rgba(0, 0, 0, 0.07), 0 0 30px rgba(237, 206, 110, 0.08);
  z-index: 5;
}
/* 金色图钉 */
.memory-card::before {
  content: '';
  position: absolute;
  top: -7px;
  left: 50%;
  transform: translateX(-50%);
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: radial-gradient(circle at 40% 35%, var(--gold-3), var(--gold-1));
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.12);
}
.memory-year {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  color: var(--gold-1);
  letter-spacing: 0.18em;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 5px;
}
.memory-text {
  font-family: var(--font-sans);
  font-size: 0.88rem;
  font-weight: 300;
  color: var(--text-dark);
  line-height: 1.85;
  letter-spacing: 0.02em;
}
.memory-mood {
  margin-top: 12px;
  font-size: 0.65rem;
  font-weight: 300;
  color: var(--text-dark-faint);
  letter-spacing: 0.08em;
  display: flex;
  align-items: center;
  gap: 4px;
}

@media (max-width: 768px) {
  .memories-scatter { min-height: auto; display: flex; flex-direction: column; gap: 16px; align-items: center; }
  .memory-card {
    position: relative !important;
    top: auto !important;
    left: auto !important;
    right: auto !important;
    width: 100%;
    max-width: 300px;
    --cr: 0deg;
  }
  .memory-card:nth-child(odd) { --cr: -0.8deg; }
  .memory-card:nth-child(even) { --cr: 0.8deg; }
}
</style>
