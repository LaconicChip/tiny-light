<script setup>
import { ref, onMounted } from 'vue'
import { PhSparkle } from '@phosphor-icons/vue'
import LightInput from './components/LightInput.vue'
import StarRiver from './components/StarRiver.vue'
import LightDetail from './components/LightDetail.vue'
import Stats from './components/Stats.vue'
import OnThisDay from './components/OnThisDay.vue'
import {
  getToday, getRiver, getOnThisDay, getStats,
  createLight, updateLight, deleteLight,
} from './api/lights.js'

const todayLight = ref(null)
const riverLights = ref([])
const onThisDayLights = ref([])
const stats = ref(null)
const selectedLight = ref(null)
const errorMsg = ref('')
const year = new Date().getFullYear()
const today = new Date()
const dateLabel = `${today.getMonth() + 1}月${today.getDate()}日`

let errorTimer = null
function showError(msg) {
  errorMsg.value = msg
  clearTimeout(errorTimer)
  errorTimer = setTimeout(() => (errorMsg.value = ''), 3000)
}

async function loadToday() {
  const { data } = await getToday()
  todayLight.value = data.todayLighted ? data.light : null
}

async function loadRiver() {
  const { data } = await getRiver(year)
  riverLights.value = data
}

async function loadOnThisDay() {
  const { data } = await getOnThisDay()
  onThisDayLights.value = data || []
}

async function loadStats() {
  const { data } = await getStats()
  stats.value = data
}

async function refreshAll() {
  await Promise.all([loadToday(), loadRiver(), loadOnThisDay(), loadStats()])
}

async function handleSubmit({ content, mood }) {
  try {
    await createLight({ content, mood })
    await refreshAll()
  } catch (e) {
    showError(e.response?.data?.error || '点亮失败')
  }
}

async function handleUpdate({ content, mood }) {
  if (!todayLight.value) return
  try {
    await updateLight(todayLight.value.id, { content, mood })
    await refreshAll()
  } catch (e) {
    showError(e.response?.data?.error || '保存失败')
  }
}

async function handleDelete() {
  if (!todayLight.value) return
  try {
    await deleteLight(todayLight.value.id)
    await refreshAll()
  } catch (e) {
    showError(e.response?.data?.error || '删除失败')
  }
}

function handleSelect(light) {
  selectedLight.value = light
}

function handleClose() {
  selectedLight.value = null
}

onMounted(async () => {
  try {
    await refreshAll()
  } catch (e) {
    showError('加载失败，请检查后端是否启动')
  }
})
</script>

<template>
  <div class="page">
    <Transition name="error">
      <div v-if="errorMsg" class="error-bar">{{ errorMsg }}</div>
    </Transition>

    <div class="paper">
      <!-- 顶角小标：日期 + 闪光 -->
      <div class="top-mark">
        <span class="date-stamp">{{ dateLabel }}</span>
        <PhSparkle class="corner-star" :size="18" weight="fill" />
      </div>

      <!-- 标题：左对齐，混字号 -->
      <header class="app-header">
        <h1>
          <span class="title-main">今日</span><span class="title-sub">微光</span>
        </h1>
        <p class="subtitle">每天记下一个亮亮的瞬间</p>
      </header>

      <LightInput
        :today-light="todayLight"
        @submit="handleSubmit"
        @update="handleUpdate"
        @delete="handleDelete"
      />

      <div class="reveal-section">
        <div class="section-label" tabindex="0">
          <span class="label-text">今年的微光</span>
          <span class="label-rule" />
          <span class="reveal-hint">触碰展开</span>
        </div>
        <div class="reveal-content">
          <div class="reveal-inner">
            <Stats :stats="stats" />
          </div>
        </div>
      </div>

      <div class="section-label">
        <span class="label-text">星光河</span>
        <span class="label-rule" />
      </div>

      <StarRiver :lights="riverLights" :year="year" @select="handleSelect" />

      <template v-if="onThisDayLights.length">
        <div class="section-label">
          <span class="label-text">那年今日</span>
          <span class="label-rule" />
        </div>

        <OnThisDay :lights="onThisDayLights" @select="handleSelect" />
      </template>

      <footer class="page-foot">
        <span class="foot-star">✦</span>
        <span class="foot-text">愿每个被记下的瞬间都不被辜负</span>
      </footer>
    </div>

    <LightDetail v-if="selectedLight" :light="selectedLight" @close="handleClose" />
  </div>
</template>

<style scoped>
.page {
  position: relative;
  min-height: 100vh;
  padding: 56px 24px 80px;
}
.paper {
  max-width: 720px;
  margin: 0 auto;
  position: relative;
}
.error-bar {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: #c44545;
  color: #fff;
  padding: 10px 18px;
  border-radius: 10px;
  font-size: 14px;
  z-index: 200;
  box-shadow: 0 8px 24px rgba(196, 69, 69, 0.32);
}
.error-enter-from,
.error-leave-to {
  opacity: 0;
  transform: translate(-50%, -8px);
}
.error-enter-active,
.error-leave-active {
  transition:
    opacity 200ms ease-out,
    transform 200ms cubic-bezier(0.16, 1, 0.3, 1);
}

/* 顶角小标 */
.top-mark {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}
.date-stamp {
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--text-soft);
  letter-spacing: 0.08em;
}
.corner-star {
  color: var(--gold);
  opacity: 0.7;
}

/* 标题区 */
.app-header {
  margin-bottom: 44px;
}
.app-header h1 {
  margin: 0 0 10px;
  font-family: var(--font-display);
  font-weight: 500;
  line-height: 1.05;
  letter-spacing: 0.02em;
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.title-main {
  font-size: 48px;
  color: var(--text);
}
.title-sub {
  font-size: 48px;
  color: var(--gold);
}
.subtitle {
  margin: 0;
  font-family: var(--font-body);
  font-size: 14px;
  color: var(--text-soft);
  letter-spacing: 0.04em;
}

/* 段落分隔：左侧小标 + 渐隐细线，像手账里的章节注脚 */
.section-label {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 40px 0 20px;
}
.label-text {
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--text-soft);
  letter-spacing: 0.12em;
  white-space: nowrap;
}
.label-rule {
  flex: 1;
  height: 1px;
  background: linear-gradient(to right, var(--hairline), transparent);
}

/* 今年的微光：hover 缓慢展开 */
.reveal-section .section-label {
  cursor: pointer;
  outline: none;
  transition: color 200ms ease;
}
.reveal-section .section-label:hover .label-text,
.reveal-section .section-label:focus-visible .label-text {
  color: var(--gold);
}
.reveal-section .section-label:focus-visible {
  outline: none;
}
.reveal-hint {
  font-family: var(--font-body);
  font-size: 11px;
  color: var(--text-faint);
  letter-spacing: 0.1em;
  opacity: 0.5;
  transition: opacity 200ms ease;
  white-space: nowrap;
}
.reveal-section:hover .reveal-hint,
.reveal-section:focus-within .reveal-hint {
  opacity: 0;
}
.reveal-content {
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows 700ms cubic-bezier(0.16, 1, 0.3, 1);
}
.reveal-inner {
  overflow: hidden;
  opacity: 0;
  transition: opacity 500ms ease;
}
.reveal-section:hover .reveal-content,
.reveal-section:focus-within .reveal-content {
  grid-template-rows: 1fr;
}
.reveal-section:hover .reveal-inner,
.reveal-section:focus-within .reveal-inner {
  opacity: 1;
  transition-delay: 180ms;
}
/* 触屏无 hover：永远展开 */
@media (hover: none) {
  .reveal-content {
    grid-template-rows: 1fr;
  }
  .reveal-inner {
    opacity: 1;
  }
  .reveal-hint {
    display: none;
  }
}
/* 减少动效：瞬切 */
@media (prefers-reduced-motion: reduce) {
  .reveal-content,
  .reveal-inner {
    transition-duration: 1ms !important;
  }
}

/* 页脚签名 */
.page-foot {
  margin-top: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--text-faint);
}
.foot-star {
  color: var(--gold);
  font-size: 12px;
}
.foot-text {
  font-family: var(--font-display);
  font-size: 13px;
  letter-spacing: 0.06em;
}
</style>
