<script setup>
import { computed } from 'vue'

const props = defineProps({
  stats: Object,
})

const maxCount = computed(() => {
  const d = props.stats?.moodDistribution || {}
  return Math.max(...Object.values(d), 1)
})
</script>

<template>
  <section v-if="stats" class="stats">
    <div class="stat-row">
      <div class="stat">
        <div class="num">{{ stats.currentStreak }}</div>
        <div class="label">连续</div>
      </div>
      <span class="sep" />
      <div class="stat">
        <div class="num">{{ stats.longestStreak }}</div>
        <div class="label">最长</div>
      </div>
      <span class="sep" />
      <div class="stat">
        <div class="num">{{ stats.yearDays }}</div>
        <div class="label">今年</div>
      </div>
      <span class="sep" />
      <div class="stat">
        <div class="num">{{ stats.totalDays }}</div>
        <div class="label">累计</div>
      </div>
    </div>

    <div
      v-if="stats.moodDistribution && Object.keys(stats.moodDistribution).length"
      class="mood-dist"
    >
      <div class="dist-title">心情分布</div>
      <div
        v-for="(count, mood) in stats.moodDistribution"
        :key="mood"
        class="dist-item"
      >
        <span class="dist-mood">{{ mood }}</span>
        <div class="dist-bar-bg">
          <div class="dist-bar" :style="{ width: (count / maxCount * 100) + '%' }" />
        </div>
        <span class="dist-count">{{ count }}</span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.stats {
  /* 无卡片：直接行内排版，靠空白和细线分隔 */
}
.stat-row {
  display: flex;
  align-items: flex-end;
  gap: 0;
  padding: 4px 0 0;
}
.stat {
  flex: 1;
  text-align: center;
  padding: 0 8px;
}
.stat:first-child {
  text-align: left;
  padding-left: 0;
}
.stat:last-child {
  text-align: right;
  padding-right: 0;
}
.num {
  font-size: 32px;
  font-weight: 500;
  font-family: var(--font-display);
  color: var(--gold);
  line-height: 1;
  letter-spacing: 0.01em;
}
.label {
  font-size: 12px;
  color: var(--text-soft);
  margin-top: 8px;
  font-family: var(--font-body);
  letter-spacing: 0.1em;
}
.sep {
  width: 1px;
  height: 28px;
  background: var(--hairline);
  align-self: flex-end;
  margin-bottom: 20px;
}
.mood-dist {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px dashed var(--hairline);
}
.dist-title {
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--text-soft);
  margin-bottom: 14px;
  letter-spacing: 0.1em;
}
.dist-item {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 10px;
}
.dist-mood {
  width: 44px;
  font-size: 14px;
  font-family: var(--font-display);
  color: var(--text);
  letter-spacing: 0.04em;
}
.dist-bar-bg {
  flex: 1;
  height: 6px;
  background: var(--bg-soft);
  border-radius: 3px;
  overflow: hidden;
}
.dist-bar {
  height: 100%;
  background: linear-gradient(90deg, var(--gold), var(--gold-lit));
  border-radius: 3px;
  transition: width 400ms cubic-bezier(0.16, 1, 0.3, 1);
}
.dist-count {
  width: 24px;
  text-align: right;
  font-size: 13px;
  color: var(--text-soft);
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}
</style>
