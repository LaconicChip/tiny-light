<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  todayLight: Object,
  dateLabel: String,
})

const emit = defineEmits(['submit', 'update', 'delete'])

const prm = window.matchMedia('(prefers-reduced-motion: reduce)').matches
const touch = window.matchMedia('(hover: none)').matches

/* 7 种心情 + 完整内联 SVG 字符串（stroke 风格，stroke-width 1.5）
   用 v-html 打在 span 上，让 HTML 解析器走内联 SVG 路径，规避 SVG innerHTML 命名空间兼容问题
   注意：v-html 内容不受 scoped 样式影响（无 data-v 属性），尺寸必须内联在 svg 标签上，
   否则 Safari 下无尺寸 svg 会在 shrink-to-fit 容器里塌缩为 0×0（完全不可见） */
const SVG_WRAP = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">'
const moods = [
  { key: '开心', inner: '<circle cx="12" cy="12" r="9"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/>' },
  { key: '平静', inner: '<path d="M11 20A7 7 0 019.8 6.9C15.5 4.9 17 3.5 19 2c1 2 2 4.5 2 8 0 5.5-4.78 10-10 10z"/><path d="M2 21c0-3 1.85-5.36 5.08-6"/>' },
  { key: '感恩', inner: '<path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/>' },
  { key: '疲惫', inner: '<path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z"/>' },
  { key: '感动', inner: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/>' },
  { key: '思念', inner: '<path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>' },
  { key: '期待', inner: '<polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>' },
].map(m => ({ ...m, svg: SVG_WRAP + m.inner + '</svg>' }))

const content = ref('')
const mood = ref('')
const isEditing = ref(false)

watch(() => props.todayLight, (t) => {
  if (t) {
    content.value = t.content
    mood.value = t.mood || ''
    isEditing.value = false
  } else {
    content.value = ''
    mood.value = ''
    isEditing.value = false
  }
}, { immediate: true })

function submit() {
  if (!content.value.trim() || !mood.value) return
  emit('submit', { content: content.value.trim(), mood: mood.value })
  content.value = ''
  mood.value = ''
}
function startEdit() {
  isEditing.value = true
  content.value = props.todayLight.content
  mood.value = props.todayLight.mood || ''
}
function cancelEdit() {
  isEditing.value = false
  content.value = props.todayLight.content
  mood.value = props.todayLight.mood || ''
}
function saveEdit() {
  if (!content.value.trim() || !mood.value) return
  emit('update', { content: content.value.trim(), mood: mood.value })
}
function confirmDelete() {
  if (confirm('确定删除今天的微光吗？删了可以重新点亮。')) emit('delete')
}

/* 3D 倾斜 + 磁吸按钮（仅桌面 + 非减少动效）
   磁吸用事件委托绑在常驻的 card 上，避免按钮在已点亮态不渲染时绑不到监听 */
const cardRef = ref(null)
let tiltRaf = null
let cardMoveHandler = null
let cardLeaveHandler = null
let currentBtn = null

onMounted(() => {
  if (prm || touch) return
  const card = cardRef.value
  if (!card) return

  cardMoveHandler = (e) => {
    // 磁吸：鼠标在点亮按钮上时，按钮被轻微吸引（委托，按钮后渲染也能命中）
    const btn = e.target.closest('.light-btn')
    if (currentBtn && currentBtn !== btn) {
      currentBtn.style.transform = ''
      currentBtn = null
    }
    if (btn && !btn.disabled) {
      currentBtn = btn
      const r = btn.getBoundingClientRect()
      const cx = r.left + r.width / 2
      const cy = r.top + r.height / 2
      btn.style.transform = `translate3d(${(e.clientX - cx) * 0.15}px,${(e.clientY - cy) * 0.15 - 2}px,0) scale(1.02)`
    }
    // 3D 倾斜：仅在卡片可见后生效
    if (!card.classList.contains('visible')) return
    if (tiltRaf) cancelAnimationFrame(tiltRaf)
    tiltRaf = requestAnimationFrame(() => {
      const r = card.getBoundingClientRect()
      const x = (e.clientX - r.left) / r.width - 0.5
      const y = (e.clientY - r.top) / r.height - 0.5
      card.style.transform = `perspective(1000px) rotateX(${-y * 3.5}deg) rotateY(${x * 3.5}deg) rotate(-1.2deg) translate3d(0,0,0)`
      tiltRaf = null
    })
  }
  cardLeaveHandler = () => {
    if (tiltRaf) cancelAnimationFrame(tiltRaf)
    if (card.classList.contains('visible')) {
      card.style.transform = 'perspective(1000px) rotate(-1.2deg) translate3d(0,0,0)'
    }
    if (currentBtn) { currentBtn.style.transform = ''; currentBtn = null }
    tiltRaf = null
  }
  card.addEventListener('mousemove', cardMoveHandler)
  card.addEventListener('mouseleave', cardLeaveHandler)
})

onUnmounted(() => {
  if (tiltRaf) cancelAnimationFrame(tiltRaf)
  const card = cardRef.value
  if (card) {
    card.removeEventListener('mousemove', cardMoveHandler)
    card.removeEventListener('mouseleave', cardLeaveHandler)
  }
})

const showForm = (t) => !t || isEditing.value
</script>

<template>
  <div class="input-card" ref="cardRef">
    <Transition mode="out-in" name="swap">
      <!-- 未点亮 或 编辑态：表单 -->
      <div v-if="showForm(todayLight)" key="form" class="form-state">
        <div class="input-date">
          <svg width="10" height="10" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"><circle cx="8" cy="8" r="6.5"/><path d="M8 4v4l2.5 2.5"/></svg>
          {{ dateLabel }}
        </div>
        <div class="input-prompt">{{ isEditing ? '修改今天的微光' : '今天，是什么照亮了你？' }}</div>
        <textarea
          v-model="content"
          class="input-textarea"
          rows="3"
          maxlength="200"
          placeholder="写下今天的微光——一杯热茶、一个微笑、一阵晚风……"
        />
        <div class="mood-tags">
          <span
            v-for="m in moods"
            :key="m.key"
            :class="['mood-tag', { selected: mood === m.key }]"
            @click="mood = m.key"
          >
            <span class="mood-icon" v-html="m.svg"></span>
            {{ m.key }}
          </span>
        </div>
        <p v-if="content.trim() && !mood" class="mood-hint">先选个心情吧</p>
        <div class="form-actions">
          <button v-if="isEditing" class="ghost-btn" @click="cancelEdit">取消</button>
          <button
            class="light-btn"
            :disabled="!content.trim() || !mood"
            @click="isEditing ? saveEdit() : submit()"
          >
            <svg viewBox="0 0 16 16" fill="currentColor"><path d="M8 0l1.8 5.5L16 8l-6.2 2.5L8 16l-1.8-5.5L0 8l6.2-2.5z"/></svg>
            {{ isEditing ? '保存修改' : '点亮今天' }}
          </button>
        </div>
      </div>
      <!-- 已点亮 + 查看态 -->
      <div v-else key="lit" class="lit-state show">
        <div class="lit-glow"></div>
        <div class="lit-quote">{{ todayLight.content }}</div>
        <div class="lit-meta">
          <svg width="8" height="8" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="12" r="5"/></svg>
          LIT · {{ todayLight.mood }}
        </div>
        <div class="lit-actions">
          <button class="lit-action-btn" @click="startEdit">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            编辑
          </button>
          <button class="lit-action-btn danger" @click="confirmDelete">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
            删除
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.input-card {
  position: relative;
  background: var(--glass-dark);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: 24px;
  padding: clamp(22px, 3.5vw, 36px);
  width: 100%;
  max-width: 480px;
  margin-left: clamp(0px, 3vw, 40px);
  transform: perspective(1000px) rotate(-1.2deg) translate3d(0, 40px, 0);
  opacity: 0;
  transition: opacity 0.9s var(--ease-out), box-shadow 0.4s, transform 0.5s var(--ease-out);
  box-shadow: var(--glass-inner-shadow), var(--glass-outer-shadow), 0 0 60px rgba(237, 206, 110, 0.06);
}
.input-card.visible {
  opacity: 1;
  transform: perspective(1000px) rotate(-1.2deg) translate3d(0, 0, 0);
}
.input-card::before {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: 22px;
  padding: 1px;
  background: linear-gradient(135deg, rgba(237,206,110,0.3), transparent 35%, transparent 65%, rgba(237,206,110,0.15));
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}
.input-date {
  font-family: var(--font-mono);
  font-size: 0.7rem;
  font-weight: 400;
  color: var(--gold-3);
  letter-spacing: 0.2em;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.input-prompt {
  font-family: var(--font-sans);
  font-size: clamp(1rem, 2vw, 1.2rem);
  font-weight: 300;
  color: var(--platinum-1);
  margin-bottom: 20px;
  line-height: 1.7;
  letter-spacing: 0.03em;
}
.input-textarea {
  width: 100%;
  min-height: 80px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(237, 206, 110, 0.1);
  border-radius: 12px;
  padding: 14px;
  color: var(--platinum-1);
  font-family: var(--font-sans);
  font-size: 0.92rem;
  font-weight: 300;
  line-height: 1.8;
  resize: none;
  outline: none;
  transition: border-color 0.3s, box-shadow 0.3s, background 0.3s;
  box-sizing: border-box;
}
.input-textarea::placeholder { color: rgba(245, 242, 235, 0.22); }
.input-textarea:focus {
  border-color: rgba(237, 206, 110, 0.28);
  box-shadow: 0 0 24px rgba(237, 206, 110, 0.06);
  background: rgba(255, 255, 255, 0.05);
}
.mood-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 18px 0 12px;
}
.mood-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px 7px 10px;
  border-radius: 20px;
  font-size: 0.8rem;
  font-family: var(--font-sans);
  font-weight: 300;
  letter-spacing: 0.06em;
  border: 1px solid rgba(237, 206, 110, 0.12);
  background: rgba(237, 206, 110, 0.04);
  color: var(--platinum-3);
  cursor: pointer;
  user-select: none;
  transition: all 0.3s var(--ease-spring);
}
.mood-tag:nth-child(odd) { transform: translateY(2px); }
.mood-tag:nth-child(3n) { transform: translateY(-1px); }
/* v-html 注入的 svg 无 data-v 属性，必须用 :deep() 穿透 scoped 边界才能命中 */
.mood-tag :deep(svg) { width: 14px; height: 14px; opacity: 0.7; transition: opacity 0.3s; }
.mood-tag:hover {
  background: rgba(237, 206, 110, 0.1);
  border-color: rgba(237, 206, 110, 0.3);
  color: var(--gold-3);
  transform: translateY(-3px) scale(1.06);
  box-shadow: 0 4px 16px rgba(237, 206, 110, 0.12);
}
.mood-tag:hover :deep(svg) { opacity: 1; }
.mood-tag.selected {
  background: linear-gradient(135deg, var(--gold-1), var(--gold-2));
  border-color: var(--gold-3);
  color: var(--night-2);
  font-weight: 500;
  box-shadow: 0 4px 20px rgba(237, 206, 110, 0.3);
  transform: translateY(-2px) scale(1.04);
}
.mood-tag.selected :deep(svg) { opacity: 1; }
.mood-hint {
  font-size: 0.72rem;
  color: var(--gold-3);
  letter-spacing: 0.1em;
  margin: 0 0 12px;
  opacity: 0.7;
}
.form-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}
.ghost-btn {
  padding: 12px 22px;
  border-radius: 28px;
  border: 1px solid rgba(237, 206, 110, 0.2);
  background: transparent;
  color: var(--platinum-3);
  font-family: var(--font-sans);
  font-size: 0.84rem;
  font-weight: 300;
  letter-spacing: 0.1em;
  cursor: pointer;
  transition: all 0.3s var(--ease-spring);
}
.ghost-btn:hover {
  border-color: rgba(237, 206, 110, 0.4);
  color: var(--gold-3);
}
.light-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  border-radius: 28px;
  border: none;
  background: linear-gradient(135deg, var(--gold-1), var(--gold-2), var(--gold-3));
  color: var(--night-2);
  font-family: var(--font-sans);
  font-size: 0.88rem;
  font-weight: 500;
  letter-spacing: 0.12em;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.35s var(--ease-spring);
  box-shadow: 0 4px 20px rgba(237, 206, 110, 0.25), 0 0 40px rgba(248, 227, 154, 0.08);
}
.light-btn svg { width: 14px; height: 14px; }
.light-btn::before {
  content: '';
  position: absolute;
  top: 0; left: -100%;
  width: 100%; height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.35), transparent);
  transition: left 0.6s;
}
.light-btn:hover:not(:disabled) {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 8px 30px rgba(237, 206, 110, 0.4), 0 0 60px rgba(248, 227, 154, 0.15);
}
.light-btn:hover:not(:disabled)::before { left: 100%; }
.light-btn:active:not(:disabled) { transform: scale(0.97); }
.light-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* 已点亮态 */
.lit-state { text-align: center; padding: 28px 0 20px; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 280px; }
.lit-glow {
  width: 88px;
  height: 88px;
  margin: 0 auto 28px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--gold-5) 0%, var(--gold-4) 25%, var(--gold-2) 50%, var(--gold-1) 70%, transparent 82%);
  animation: starPulse 3s ease-in-out infinite;
}
@keyframes starPulse {
  0%, 100% { box-shadow: 0 0 28px var(--gold-glow-mid), 0 0 64px rgba(237, 206, 110, 0.16); transform: scale(1); }
  50% { box-shadow: 0 0 44px rgba(237, 206, 110, 0.55), 0 0 88px rgba(237, 206, 110, 0.24); transform: scale(1.06); }
}
.lit-quote {
  font-family: var(--font-sans);
  font-size: 1.25rem;
  font-weight: 300;
  color: var(--platinum-1);
  line-height: 2;
  letter-spacing: 0.05em;
  margin-bottom: 18px;
  max-width: 360px;
}
.lit-quote::before { content: '\201C'; color: var(--gold-3); margin-right: 4px; opacity: 0.6; }
.lit-quote::after { content: '\201D'; color: var(--gold-3); margin-left: 4px; opacity: 0.6; }
.lit-meta {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  color: var(--gold-3);
  letter-spacing: 0.22em;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-bottom: 26px;
}
.lit-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
.lit-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 16px;
  border-radius: 20px;
  border: 1px solid rgba(237, 206, 110, 0.15);
  background: rgba(237, 206, 110, 0.04);
  color: var(--platinum-3);
  font-family: var(--font-sans);
  font-size: 0.74rem;
  font-weight: 300;
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: all 0.3s var(--ease-spring);
}
.lit-action-btn:hover {
  border-color: rgba(237, 206, 110, 0.35);
  color: var(--gold-3);
  background: rgba(237, 206, 110, 0.1);
}
.lit-action-btn.danger:hover {
  border-color: rgba(196, 69, 69, 0.4);
  color: #e07070;
  background: rgba(196, 69, 69, 0.08);
}

/* 表单 ↔ 已点亮 切换过渡（淡入淡出 + 轻微位移） */
.swap-enter-active { transition: opacity 0.32s var(--ease-out), transform 0.32s var(--ease-out); }
.swap-leave-active { transition: opacity 0.24s var(--ease-out), transform 0.24s var(--ease-out); }
.swap-enter-from { opacity: 0; transform: translate3d(0, 12px, 0); }
.swap-leave-to { opacity: 0; transform: translate3d(0, -8px, 0); }

@media (max-width: 768px) {
  .input-card { margin-left: 0; max-width: 100%; transform: perspective(1000px) rotate(-0.6deg) translate3d(0, 40px, 0); }
  .input-card.visible { transform: perspective(1000px) rotate(-0.6deg) translate3d(0, 0, 0); }
}
@media (max-width: 480px) {
  .input-card { padding: 18px; border-radius: 16px; }
}
</style>
