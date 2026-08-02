<script setup>
import { computed } from 'vue'

const props = defineProps({
  stats: Object,
})

const badges = computed(() => {
  if (!props.stats) return []
  return [
    { num: props.stats.currentStreak, label: '连续', cls: 'stat-1' },
    { num: props.stats.longestStreak, label: '最长', cls: 'stat-2' },
    { num: props.stats.yearDays, label: '今年', cls: 'stat-3' },
    { num: props.stats.totalDays, label: '累计', cls: 'stat-4' },
  ]
})
</script>

<template>
  <template v-if="badges.length">
    <div
      v-for="b in badges"
      :key="b.cls"
      :class="['stat-badge', b.cls]"
    >
      <div class="stat-num">{{ b.num }}</div>
      <div class="stat-label">{{ b.label }}</div>
    </div>
  </template>
</template>

<style scoped>
.stat-badge {
  position: absolute;
  background: rgba(250, 248, 245, 0.08);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(237, 206, 110, 0.12);
  border-radius: 16px;
  padding: 16px 20px;
  text-align: center;
  min-width: 88px;
  opacity: 0;
  transform: translate3d(0, 25px, 0) rotate(var(--rot, 0deg));
  transition: opacity 0.8s var(--ease-out), transform 0.8s var(--ease-out), box-shadow 0.3s;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
  cursor: default;
}
.stat-badge.visible {
  opacity: 1;
  transform: translate3d(0, 0, 0) rotate(var(--rot, 0deg));
}
.stat-badge:hover {
  background: rgba(250, 248, 245, 0.12);
  border-color: rgba(237, 206, 110, 0.25);
  transform: translate3d(0, -3px, 0) rotate(0deg) scale(1.04);
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.15), 0 0 20px rgba(237, 206, 110, 0.08);
}
.stat-num {
  font-family: var(--font-mono);
  font-size: 1.8rem;
  font-weight: 300;
  color: var(--gold-2);
  line-height: 1;
  margin-bottom: 4px;
  text-shadow: 0 0 16px rgba(237, 206, 110, 0.2);
}
.stat-label {
  font-size: 0.68rem;
  font-weight: 300;
  color: var(--platinum-2);
  letter-spacing: 0.12em;
  opacity: 0.7;
}
.stat-1 { top: 5px; left: 22%; --rot: -2.5deg; transition-delay: 0.1s; }
.stat-2 { top: 28px; right: 22%; --rot: 2deg; transition-delay: 0.25s; }
.stat-3 { top: 82px; left: 32%; --rot: 1.5deg; transition-delay: 0.4s; }
.stat-4 { top: 72px; right: 30%; --rot: -1deg; transition-delay: 0.55s; }

@media (max-width: 768px) {
  .stat-badge {
    position: relative !important;
    top: auto !important;
    left: auto !important;
    right: auto !important;
    margin: 4px;
  }
}
</style>
