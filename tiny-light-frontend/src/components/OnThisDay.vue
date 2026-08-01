<script setup>
import { computed } from 'vue'

const props = defineProps({
  lights: Array,
})

defineEmits(['select'])

const items = computed(() => {
  if (!props.lights?.length) return []
  return props.lights.map(l => {
    const [y, m, d] = l.lightDate.split('-')
    return {
      id: l.id,
      year: y,
      dateLabel: `${parseInt(m, 10)}月${parseInt(d, 10)}日`,
      content: l.content,
      mood: l.mood,
      raw: l,
    }
  })
})
</script>

<template>
  <section v-if="items.length" class="on-this-day">
    <ul class="od-list">
      <li v-for="(it, idx) in items" :key="it.id" class="od-item">
        <button class="od-row" type="button" @click="$emit('select', it.raw)">
          <div class="od-year-col">
            <span class="year-num">{{ it.year }}</span>
            <span class="year-date">{{ it.dateLabel }}</span>
          </div>
          <div class="od-body">
            <p class="od-content">{{ it.content }}</p>
            <p v-if="it.mood" class="od-mood">
              <span class="mood-dot" />
              {{ it.mood }}
            </p>
          </div>
        </button>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.on-this-day {
  /* 无卡片容器：靠列表自身节奏 */
}
.od-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.od-item {
  border-top: 1px dashed var(--hairline);
}
.od-item:first-child {
  border-top: none;
}
.od-row {
  width: 100%;
  display: grid;
  grid-template-columns: 88px 1fr;
  gap: 24px;
  align-items: start;
  text-align: left;
  border: none;
  background: transparent;
  padding: 18px 8px;
  cursor: pointer;
  border-radius: 6px;
  transition: background 150ms ease;
}
.od-row:hover {
  background: rgba(201, 169, 97, 0.06);
}
.od-year-col {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  border-right: 1px solid var(--hairline);
  padding-right: 18px;
  /* 微微下移，让年份与内容首行视觉对齐时更有手账感 */
  padding-top: 2px;
}
.year-num {
  font-size: 28px;
  font-weight: 500;
  font-family: var(--font-display);
  color: var(--gold);
  line-height: 1;
  letter-spacing: 0.01em;
}
.year-date {
  margin-top: 6px;
  font-size: 12px;
  font-family: var(--font-display);
  color: var(--text-soft);
  letter-spacing: 0.08em;
}
.od-body {
  min-width: 0;
  padding-top: 2px;
}
.od-content {
  margin: 0 0 8px;
  font-size: 15px;
  line-height: 1.8;
  color: var(--text);
  font-family: var(--font-display);
  letter-spacing: 0.02em;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.od-mood {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  font-size: 13px;
  color: var(--text-soft);
  font-family: var(--font-body);
  letter-spacing: 0.04em;
}
.mood-dot {
  display: inline-block;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--gold-lit);
}
.empty-hint {
  margin: 0;
  padding: 16px 8px;
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--text-faint);
  letter-spacing: 0.04em;
}
</style>
