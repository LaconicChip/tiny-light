<script setup>
import { ref, computed, onMounted } from 'vue'

const props = defineProps({
  lights: Array,
  year: Number,
})
const emit = defineEmits(['select'])

const pathD = 'M-30,300 C140,190 300,390 460,275 S760,380 920,315 S1100,230 1230,295'
const W = 1200, H = 600, TD = 365

/* 确定性伪随机：同一 day 永远得到同一偏移/大小，避免刷新时星点乱跳 */
function seededRand(seed) {
  const x = Math.sin(seed * 12.9898) * 43758.5453
  return x - Math.floor(x)
}

function dayOfYear(dateStr) {
  const [y, m, d] = dateStr.split('-').map(Number)
  const date = new Date(y, m - 1, d)
  const start = new Date(y, 0, 0)
  return Math.floor((date - start) / 86400000)
}

const now = new Date()
const todayDoy = dayOfYear(`${now.getFullYear()}-${now.getMonth() + 1}-${now.getDate()}`)

const pathCoreRef = ref(null)
const starsGroupRef = ref(null)
const tooltipRef = ref(null)
const geometry = ref([])
const monthLabels = ref([])
const todayPos = ref(null)

/* 几何只算一次（不依赖数据），存入 geometry */
onMounted(() => {
  const pe = pathCoreRef.value
  if (!pe) return
  const tl = pe.getTotalLength()
  const geo = []
  for (let day = 1; day <= TD; day++) {
    const t = (day - 1) / (TD - 1)
    const pt = pe.getPointAtLength(t * tl)
    const wave = Math.sin(day * 0.75 + day * day * 0.0008) * 25
    const noise = (seededRand(day) - 0.5) * 40
    const off = wave + noise
    const t2 = Math.min(day / (TD - 1), 1)
    const pt2 = pe.getPointAtLength(t2 * tl)
    const dx = pt2.x - pt.x, dy = pt2.y - pt.y
    const l = Math.sqrt(dx * dx + dy * dy) || 1
    const nx = -dy / l, ny = dx / l
    const x = pt.x + nx * off
    const y = pt.y + ny * off + (seededRand(day + 1000) - 0.5) * 15
    const rec = todayDoy > 0 ? Math.min(day / todayDoy, 1) : 0
    geo.push({
      day, x, y, rec,
      pd: seededRand(day + 2000) * 4,
      rSeed: seededRand(day + 3000),
    })
  }
  geometry.value = geo

  /* 月份标签位置 */
  const monthNames = ['一月','二月','三月','四月','五月','六月','七月','八月','九月','十月','十一月','十二月']
  const monthStartDay = [1, 32, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335]
  const labels = []
  monthNames.forEach((name, i) => {
    const day = monthStartDay[i]
    if (day > TD) return
    const t = (day - 1) / (TD - 1)
    const pt = pe.getPointAtLength(t * tl)
    labels.push({ name, x: pt.x, y: pt.y })
  })
  monthLabels.value = labels

  /* 今日星位置 */
  const todayGeo = geo.find(g => g.day === todayDoy)
  if (todayGeo) todayPos.value = { x: todayGeo.x, y: todayGeo.y }
})

/* 合并几何 + 真实点亮数据 */
const stars = computed(() => {
  const lightMap = new Map()
  for (const l of props.lights) {
    lightMap.set(dayOfYear(l.lightDate), l)
  }
  return geometry.value.map(g => {
    const isLit = lightMap.has(g.day)
    const isToday = g.day === todayDoy
    let r
    if (isLit) {
      r = 2 + g.rSeed * 2 + g.rec * 2.5
      if (isToday) r = 7
    } else {
      r = 0.8 + g.rSeed * 1.2
    }
    return { ...g, isLit, isToday, r, light: lightMap.get(g.day) || null }
  })
})

const litCount = computed(() => props.lights.length)

/* tooltip + 点击：事件委托到 starsGroup（原 365×4=1460 个监听器 → 3 个）
   currentHoverEl 跟踪当前 hover 的 g 元素，只在切换 star 时才 find stars 数组，
   mousemove 内部不 find，避免高频查询开销 */
let currentHoverEl = null
let currentHoverStar = null
function showTooltip(s, e) {
  if (!s.isLit || !s.light) return
  const tip = tooltipRef.value
  if (!tip) return
  const [, m, d] = s.light.lightDate.split('-')
  const dateEl = tip.querySelector('.tip-date')
  const textEl = tip.querySelector('.tip-text')
  // SVG 是常量，走 innerHTML 安全；mood 来自用户数据，用 textContent 追加防 XSS
  if (dateEl) {
    dateEl.innerHTML = `<svg width="8" height="8" viewBox="0 0 16 16" fill="currentColor"><path d="M8 0l1.8 5.5L16 8l-6.2 2.5L8 16l-1.8-5.5L0 8l6.2-2.5z"/></svg> ${parseInt(m)}月${parseInt(d)}日 · `
    const moodSpan = document.createElement('span')
    moodSpan.textContent = s.light.mood || ''
    dateEl.appendChild(moodSpan)
  }
  if (textEl) textEl.textContent = s.light.content || ''
  tip.classList.add('show')
  moveTooltip(e)
}
function moveTooltip(e) {
  const tip = tooltipRef.value
  if (!tip || !tip.classList.contains('show')) return
  tip.style.left = Math.min(e.clientX + 16, window.innerWidth - 260) + 'px'
  tip.style.top = (e.clientY - 10) + 'px'
}
function hideTooltip() {
  const tip = tooltipRef.value
  if (tip) tip.classList.remove('show')
}

/* 点击：开详情 + 涟漪 */
function onGroupClick(e) {
  const g = e.target.closest('.river-star')
  if (!g) return
  const day = parseInt(g.dataset.day)
  const s = stars.value.find(it => it.day === day)
  if (!s || !s.isLit || !s.light) return
  emit('select', s.light)
  createRipple(s.x, s.y)
}
function onGroupMove(e) {
  const g = e.target.closest('.river-star')
  if (g !== currentHoverEl) {
    // 切换 star：隐藏旧 tooltip，显示新的
    hideTooltip()
    currentHoverEl = g
    if (g) {
      const day = parseInt(g.dataset.day)
      currentHoverStar = stars.value.find(it => it.day === day) || null
      if (currentHoverStar) showTooltip(currentHoverStar, e)
    } else {
      currentHoverStar = null
    }
  } else if (currentHoverStar) {
    // 同一 star 内移动：只更新 tooltip 位置
    moveTooltip(e)
  }
}
function onGroupLeave() {
  hideTooltip()
  currentHoverEl = null
  currentHoverStar = null
}
function createRipple(x, y) {
  const g = starsGroupRef.value
  if (!g) return
  for (let i = 0; i < 2; i++) {
    const r = document.createElementNS('http://www.w3.org/2000/svg', 'circle')
    r.setAttribute('cx', x)
    r.setAttribute('cy', y)
    r.setAttribute('r', '3')
    r.setAttribute('fill', 'none')
    r.setAttribute('stroke', i === 0 ? '#edce6e' : '#f8e39a')
    r.setAttribute('stroke-width', i === 0 ? '1.2' : '0.6')
    r.setAttribute('opacity', i === 0 ? '0.7' : '0.35')
    g.appendChild(r)
    let rr = 3, o = parseFloat(r.getAttribute('opacity'))
    const sp = 1 + i * 0.4, fs = 0.012 + i * 0.006
    function anim() {
      rr += sp; o -= fs
      if (o <= 0) { r.remove(); return }
      r.setAttribute('r', rr)
      r.setAttribute('opacity', o)
      requestAnimationFrame(anim)
    }
    requestAnimationFrame(anim)
  }
}
</script>

<template>
  <div class="river-header">
    <h2 class="river-title">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 16.8l-6.2 4.5 2.4-7.4L2 9.4h7.6z"/></svg>
      {{ year }} · 星光河
    </h2>
    <p class="river-sub">这一年，你点亮的每一颗星 · 已点亮 {{ litCount }} 天</p>
  </div>

  <svg class="river-svg" viewBox="0 0 1200 600" preserveAspectRatio="xMidYMid meet" xmlns="http://www.w3.org/2000/svg">
    <defs>
      <linearGradient id="riverGrad" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" stop-color="rgba(201,154,53,0.06)"/>
        <stop offset="30%" stop-color="rgba(237,206,110,0.12)"/>
        <stop offset="50%" stop-color="rgba(248,227,154,0.18)"/>
        <stop offset="70%" stop-color="rgba(237,206,110,0.12)"/>
        <stop offset="100%" stop-color="rgba(201,154,53,0.06)"/>
      </linearGradient>
      <linearGradient id="riverGradCore" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" stop-color="rgba(201,154,53,0.15)"/>
        <stop offset="50%" stop-color="rgba(248,227,154,0.35)"/>
        <stop offset="100%" stop-color="rgba(201,154,53,0.15)"/>
      </linearGradient>
      <filter id="riverBlur" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="22"/></filter>
      <filter id="goldGlow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="3" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
      <filter id="goldGlowStrong" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="6" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
    </defs>
    <path class="river-path-glow" :d="pathD"/>
    <path class="river-path-core" :d="pathD" ref="pathCoreRef"/>
    <g ref="starsGroupRef"
      @click="onGroupClick"
      @mousemove="onGroupMove"
      @mouseleave="onGroupLeave"
    >
      <g
        v-for="s in stars"
        :key="s.day"
        :class="['river-star', s.isLit ? 'lit' : 'dim']"
        :data-day="s.day"
        :style="{ '--pd': s.pd + 's' }"
      >
        <circle v-if="s.isLit" class="star-glow" :cx="s.x" :cy="s.y" :r="s.r * 3" fill="rgba(237,206,110,0.05)"/>
        <g :class="{ 'pulse-wrap': s.isLit }">
          <circle class="star-body" :cx="s.x" :cy="s.y" :r="s.r"/>
          <circle v-if="s.isLit" class="star-body-strong" :cx="s.x" :cy="s.y" :r="s.r"/>
        </g>
        <g v-if="s.isLit && s.r > 3" opacity="0.25">
          <line :x1="s.x - s.r * 2" :y1="s.y" :x2="s.x + s.r * 2" :y2="s.y" stroke="rgba(248,227,154,0.2)" stroke-width="0.4"/>
          <line :x1="s.x" :y1="s.y - s.r * 2" :x2="s.x" :y2="s.y + s.r * 2" stroke="rgba(248,227,154,0.2)" stroke-width="0.4"/>
        </g>
      </g>
    </g>
    <g class="month-labels" font-family="'Geist','PingFang SC','Noto Sans SC',sans-serif" fill="#5a5248" font-size="11" opacity="0.55" font-weight="300" letter-spacing="3">
      <text v-for="(m, i) in monthLabels" :key="i" :x="m.x" :y="m.y + 55" text-anchor="middle">{{ m.name }}</text>
    </g>
    <g v-if="todayPos">
      <circle class="today-pulse-1" :cx="todayPos.x" :cy="todayPos.y" fill="none" stroke="rgba(237,206,110,0.3)" stroke-width="1"/>
      <circle class="today-pulse-2" :cx="todayPos.x" :cy="todayPos.y" fill="none" stroke="rgba(248,227,154,0.12)" stroke-width="0.7"/>
      <text :x="todayPos.x" :y="todayPos.y - 22" text-anchor="middle" fill="#edce6e" font-size="9" font-family="'Geist Mono',monospace" opacity="0.7" letter-spacing="2">TODAY</text>
    </g>
  </svg>

  <p class="river-footer">
    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="2"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/></svg>
    每一颗星，都是你曾被照亮的日子
  </p>

  <div class="tooltip" ref="tooltipRef">
    <div class="tip-date"></div>
    <div class="tip-text"></div>
  </div>
</template>

<style scoped>
.river-header {
  margin-bottom: 24px;
  opacity: 0;
  transform: translate3d(0, 18px, 0);
  transition: all 0.9s var(--ease-out);
}
.river-header.visible {
  opacity: 1;
  transform: translate3d(0, 0, 0);
}
.river-title {
  font-family: var(--font-sans);
  font-size: clamp(1.3rem, 2.8vw, 1.8rem);
  font-weight: 300;
  color: var(--text-dark);
  letter-spacing: 0.18em;
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
}
.river-title svg { width: 20px; height: 20px; color: var(--gold-1); opacity: 0.6; }
.river-sub {
  font-family: var(--font-sans);
  font-size: 0.8rem;
  font-weight: 300;
  color: var(--text-dark-muted);
  letter-spacing: 0.12em;
  margin-top: 6px;
  margin-left: 32px;
}
.river-svg { width: 100%; height: auto; display: block; overflow: visible; contain: layout style paint; }
.river-star { cursor: pointer; transition: transform 0.35s var(--ease-spring); transform-origin: center; transform-box: fill-box; }
.river-star .star-body { transition: r 0.35s var(--ease-spring); }
.river-star.lit .star-body { fill: var(--gold-2); filter: url(#goldGlow); }
/* 预置强发光层：hover 时淡入（opacity 可合成），替代直接切换滤镜（每次 hover 整组重算模糊） */
.star-body-strong { fill: var(--gold-2); filter: url(#goldGlowStrong); opacity: 0; transition: opacity 0.3s ease; pointer-events: none; }
.river-star.lit:hover .star-body-strong { opacity: 1; }
/* 脉动动画挂到无滤镜的包裹组：滤镜结果缓存为纹理，每帧只调组透明度，109 颗亮星不再重算模糊
   不给 will-change：109 颗星各建合成层会超 GPU 预算，反而更卡 */
.pulse-wrap { animation: starPulse 3s ease-in-out infinite; animation-delay: var(--pd, 0s); }
.river-star.dim .star-body { fill: #b0a898; opacity: 0.3; }
.river-star.dim .star-glow { opacity: 0; }
.river-star.dim { cursor: default; }
.river-star:hover { transform: scale(1.8); }
@keyframes starPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}
.river-path-glow { fill: none; stroke: url(#riverGrad); stroke-width: 90; stroke-linecap: round; opacity: 0.1; filter: url(#riverBlur); }
.river-path-core { fill: none; stroke: url(#riverGradCore); stroke-width: 1.5; stroke-linecap: round; opacity: 0.2; stroke-dasharray: 3 8; animation: flowDash 25s linear infinite; }
@keyframes flowDash { to { stroke-dashoffset: -220; } }

/* 今日星脉冲环：原 SMIL <animate> 改 CSS animation（移动端 SMIL 性能不如 CSS）
   复刻原 values: r 8;20;8 + opacity 0.5;0;0.5，dur 3s */
.today-pulse-1 { animation: todayPulse1 3s ease-in-out infinite; }
@keyframes todayPulse1 {
  0%, 100% { r: 8; opacity: 0.5; }
  50% { r: 20; opacity: 0; }
}
.today-pulse-2 { animation: todayPulse2 3s ease-in-out 0.5s infinite; }
@keyframes todayPulse2 {
  0%, 100% { r: 12; opacity: 0.3; }
  50% { r: 26; opacity: 0; }
}
.river-footer {
  margin-top: 12px;
  font-family: var(--font-sans);
  font-size: 0.7rem;
  font-weight: 300;
  color: var(--text-dark-faint);
  letter-spacing: 0.08em;
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: flex-end;
}
</style>
