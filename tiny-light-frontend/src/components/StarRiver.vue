<script setup>
import { computed } from 'vue'

const props = defineProps({
  lights: Array,
  year: Number,
})

const emit = defineEmits(['select'])

function formatDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const days = computed(() => {
  const lightMap = new Map()
  for (const l of props.lights) {
    lightMap.set(l.lightDate, l)
  }
  const list = []
  const start = new Date(props.year, 0, 1)
  const end = new Date(props.year, 11, 31)
  for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
    const dateStr = formatDate(d)
    list.push({
      date: dateStr,
      light: lightMap.get(dateStr) || null,
    })
  }
  return list
})

const litCount = computed(() => props.lights.length)
</script>

<template>
  <section class="star-river">
    <div class="river-meta">
      <span class="meta-year">{{ year }}</span>
      <span class="meta-sep">·</span>
      <span class="meta-count">已点亮 <em>{{ litCount }}</em> 天</span>
    </div>

    <div class="dots">
      <button
        v-for="(d, i) in days"
        :key="d.date"
        type="button"
        :class="['dot', { lit: d.light }]"
        :style="{ '--i': i }"
        :title="d.date + (d.light ? ` · ${d.light.mood || ''}` : '')"
        :disabled="!d.light"
        @click="d.light && emit('select', d.light)"
      />
    </div>

    <p class="river-foot">每一个圆点是一天，金色是当时点亮的微光。</p>
  </section>
</template>

<style scoped>
.star-river {
  position: relative;
}
.river-meta {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 18px;
  font-family: var(--font-display);
  color: var(--text-soft);
}
.meta-year {
  font-size: 22px;
  color: var(--gold);
  letter-spacing: 0.02em;
}
.meta-sep {
  color: var(--text-faint);
}
.meta-count {
  font-size: 14px;
  letter-spacing: 0.06em;
}
.meta-count em {
  font-style: normal;
  color: var(--gold);
  font-weight: 500;
  margin: 0 2px;
}
.dots {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  /* 让最后一行左对齐，不要被 space-between 拉散 */
  justify-content: flex-start;
}
.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: none;
  background: var(--dot-empty);
  cursor: default;
  padding: 0;
  transition: transform 200ms cubic-bezier(0.34, 1.56, 0.64, 1), background 150ms ease;
}
.dot.lit {
  background: var(--dot-lit);
  box-shadow: 0 0 6px var(--gold-glow);
  cursor: pointer;
}
.dot.lit:hover {
  transform: scale(1.4);
  box-shadow: 0 0 10px var(--gold-glow);
}
.river-foot {
  margin: 18px 0 0;
  font-size: 12px;
  color: var(--text-faint);
  font-family: var(--font-display);
  letter-spacing: 0.06em;
  text-align: right;
}
</style>
