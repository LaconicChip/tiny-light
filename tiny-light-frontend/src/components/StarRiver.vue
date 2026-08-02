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
const scrollWrapRef = ref(null)
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

  /* 手机端：默认滚动到当前月份（否则默认在 1 月，用户看不到自己最近的星）
     viewBox 宽 1200，当前月份 x 坐标 / 1200 = 比例，乘以 scrollWidth 减去视口一半居中 */
  if (window.matchMedia('(hover: none) and (pointer: coarse)').matches && scrollWrapRef.value) {
    const wrap = scrollWrapRef.value
    requestAnimationFrame(() => {
      const targetX = todayGeo ? todayGeo.x : 600
      const scrollTarget = (targetX / 1200) * wrap.scrollWidth - wrap.clientWidth / 2
      wrap.scrollLeft = Math.max(0, scrollTarget)
    })
  }
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
    // 只有今日星和最近7天的亮星才 pulse，避免 109 颗亮星同时跑 CSS 动画（GPU 大户）
    const isRecent = isLit && (todayDoy - g.day <= 7) && (todayDoy - g.day >= 0)
    let r
    if (isLit) {
      r = 3 + g.rSeed * 3 + g.rec * 3.5  // 加大星点:原 2+2+2.5 → 3+3+3.5
      if (isToday) r = 10  // 今日星:原 7 → 10
    } else {
      r = 1.2 + g.rSeed * 1.8  // 未点亮:原 0.8+1.2 → 1.2+1.8
    }
    return { ...g, isLit, isToday, isRecent, r, light: lightMap.get(g.day) || null }
  })
})

const litCount = computed(() => props.lights.length)

/* tooltip + 点击：事件委托到 starsGroup（原 365×4=1460 个监听器 → 3 个）
   currentHoverEl 跟踪当前 hover 的 g 元素，只在切换 star 时才 find stars 数组，
   mousemove 内部不 find，避免高频查询开销 */
let currentHoverEl = null
let currentHoverStar = null
let tipRaf = null
let tipPendingX = 0, tipPendingY = 0
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
  // 跟随鼠标定位，始终在右下方，边缘溢出时限制在视口内
  positionTooltip(e.clientX, e.clientY)
}
/* tooltip 紧贴鼠标（12px偏移），根据可用空间智能选择方向：
   - 右侧空间够：显示在鼠标右侧，否则左侧（始终紧贴鼠标）
   - 下方空间够：显示在鼠标下方，否则上方（始终紧贴鼠标）
   不用 clamp 到视口边缘（会离鼠标太远），翻转方向时也紧贴鼠标另一侧 */
function positionTooltip(mx, my) {
  const tip = tooltipRef.value
  if (!tip) return
  const tipW = tip.offsetWidth || 240
  const tipH = tip.offsetHeight || 60
  const gap = 12  // tooltip 与鼠标的间距
  // 水平：右侧空间够则右，否则左（紧贴鼠标）
  const left = mx + gap + tipW <= window.innerWidth - 8
    ? mx + gap
    : mx - tipW - gap
  // 垂直：下方空间够则下，否则上（紧贴鼠标）
  const top = my + gap + tipH <= window.innerHeight - 8
    ? my + gap
    : my - tipH - gap
  tip.style.left = Math.max(8, left) + 'px'
  tip.style.top = Math.max(8, top) + 'px'
}
/* rAF 节流跟随鼠标，避免高频 mousemove 触发过多 style 写入 */
function moveTooltip(e) {
  tipPendingX = e.clientX
  tipPendingY = e.clientY
  if (tipRaf === null) {
    tipRaf = requestAnimationFrame(() => {
      tipRaf = null
      positionTooltip(tipPendingX, tipPendingY)
    })
  }
}
function hideTooltip() {
  const tip = tooltipRef.value
  if (tip) tip.classList.remove('show')
  if (tipRaf) { cancelAnimationFrame(tipRaf); tipRaf = null }
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

  <div class="river-scroll-outer">
    <div class="river-scroll-wrap" ref="scrollWrapRef">
    <svg class="river-svg" viewBox="0 80 1200 420" preserveAspectRatio="xMidYMid meet" xmlns="http://www.w3.org/2000/svg">
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
      <filter id="riverBlur" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="12"/></filter>
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
        <circle v-if="s.isLit" class="star-glow" :cx="s.x" :cy="s.y" :r="s.r * 3.5" fill="rgba(237,206,110,0.08)" opacity="0"/>
        <g :class="{ 'pulse-wrap': s.isRecent }">
          <circle class="star-body" :cx="s.x" :cy="s.y" :r="s.r"/>
        </g>
        <g v-if="s.isLit && s.r > 3" opacity="0.25">
          <line :x1="s.x - s.r * 2" :y1="s.y" :x2="s.x + s.r * 2" :y2="s.y" stroke="rgba(248,227,154,0.2)" stroke-width="0.4"/>
          <line :x1="s.x" :y1="s.y - s.r * 2" :x2="s.x" :y2="s.y + s.r * 2" stroke="rgba(248,227,154,0.2)" stroke-width="0.4"/>
        </g>
      </g>
    </g>
    <g class="month-labels" font-family="'Geist','PingFang SC','Noto Sans SC',sans-serif" fill="#5a5248" font-size="16" opacity="0.55" font-weight="300" letter-spacing="3">
      <text v-for="(m, i) in monthLabels" :key="i" :x="m.x" :y="m.y + 70" text-anchor="middle">{{ m.name }}</text>
    </g>
    <g v-if="todayPos">
      <circle class="today-pulse-1" :cx="todayPos.x" :cy="todayPos.y" fill="none" stroke="rgba(237,206,110,0.3)" stroke-width="1.2"/>
      <circle class="today-pulse-2" :cx="todayPos.x" :cy="todayPos.y" fill="none" stroke="rgba(248,227,154,0.12)" stroke-width="0.9"/>
      <text :x="todayPos.x" :y="todayPos.y - 28" text-anchor="middle" fill="#edce6e" font-size="14" font-family="'Geist Mono',monospace" opacity="0.7" letter-spacing="2">TODAY</text>
    </g>
    </svg>
    </div>
    <div class="river-scroll-hint">
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
      横向滑动查看全年
    </div>
  </div>

  <p class="river-footer">
    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="2"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/></svg>
    每一颗星，都是你曾被照亮的日子
  </p>

  <Teleport to="body">
    <div class="tooltip" ref="tooltipRef">
      <div class="tip-date"></div>
      <div class="tip-text"></div>
    </div>
  </Teleport>
</template>

<style scoped>
.river-header {
  margin-bottom: 12px;
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

/* 横向滚动长卷容器：桌面端 width:100% 自适应；手机端 SVG 设 min-width 横向滑动 */
.river-scroll-outer {
  position: relative;  /* 给 hint 的 absolute 定位做参照 */
  width: 100%;
}
.river-scroll-wrap {
  position: relative;
  width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;  /* Firefox 隐藏滚动条 */
  -ms-overflow-style: none;
}
.river-scroll-wrap::-webkit-scrollbar { display: none; }  /* Chrome/Safari 隐藏滚动条 */
/* 滑动提示：absolute 定位在容器水平居中，垂直在 SVG 底部区域。
   不在 scroll-wrap 内，不受横向滚动影响，常驻显示 */
.river-scroll-hint {
  display: none;  /* 桌面端不显示 */
  position: absolute;
  bottom: 8px;  /* 靠近 SVG 底部 */
  left: 50%;
  transform: translateX(-50%);
  align-items: center;
  gap: 6px;
  font-family: var(--font-sans);
  font-size: 0.7rem;
  font-weight: 300;
  color: var(--gold-3);
  letter-spacing: 0.12em;
  opacity: 0.6;
  pointer-events: none;  /* 不阻挡 SVG 点击 */
  z-index: 2;
}
.river-scroll-hint svg { width: 12px; height: 12px; }
@keyframes hintNudge {
  0%, 100% { transform: translate3d(0, 0, 0); }
  50% { transform: translate3d(4px, 0, 0); }
}
.river-scroll-hint svg { animation: hintNudge 2s ease-in-out infinite; }

/* 手机端：SVG 设固定宽度，横向滑动查看全年 */
@media (hover: none) and (pointer: coarse) {
  .river-scroll-wrap {
    /* 左右渐变遮罩：暗示可滑动，淡出边缘。mask-image 不跟随滚动，比伪元素可靠 */
    mask-image: linear-gradient(to right, transparent 0, #000 24px, #000 calc(100% - 24px), transparent 100%);
    -webkit-mask-image: linear-gradient(to right, transparent 0, #000 24px, #000 calc(100% - 24px), transparent 100%);
  }
  .river-scroll-wrap .river-svg {
    min-width: 1200px;  /* 加大:900→1200,保证月份字和星点清晰可点 */
  }
  .river-scroll-hint {
    display: flex;  /* 手机端常驻显示滑动提示(absolute 定位,不受滚动影响) */
  }
}
.river-star { cursor: pointer; transition: transform 0.35s var(--ease-spring); transform-origin: center; transform-box: fill-box; }
.river-star .star-body { transition: r 0.35s var(--ease-spring); }
/* 移除 SVG filter（goldGlow）：109 颗亮星每帧重算 feGaussianBlur 是 GPU 大户。
   star-glow circle 默认隐藏，仅 hover 时淡入，避免每颗星都有光圈在浅色背景上突兀。
   pointer-events:all 确保透明状态下仍接收鼠标事件，作为星点的命中区域（star-body 半径仅 3-9px 太小）。 */
.river-star.lit .star-body { fill: var(--gold-2); }
.river-star .star-glow { transition: opacity 0.3s ease; pointer-events: all; }
.river-star.lit:hover .star-glow { opacity: 1; fill: rgba(248,227,154,0.18); }
/* 脉动动画只挂到今日星+最近7天亮星（~8颗），避免 109 颗亮星同时跑 CSS 动画。
   动画挂到无滤镜的包裹组：每帧只调组透明度。 */
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
/* 移除 flowDash 动画：stroke-dashoffset 是 SVG paint 属性，每帧触发 repaint（非合成层）。
   25s 慢但持续重绘，是隐性性能税。星点动态已足够，虚线静态即可。 */
.river-path-core { fill: none; stroke: url(#riverGradCore); stroke-width: 1.5; stroke-linecap: round; opacity: 0.2; stroke-dasharray: 3 8; }

/* 今日星脉冲环：配合今日星半径 10 加大（原 r 8;20;8 → r 12;30;12） */
.today-pulse-1 { animation: todayPulse1 3s ease-in-out infinite; }
@keyframes todayPulse1 {
  0%, 100% { r: 12; opacity: 0.5; }
  50% { r: 30; opacity: 0; }
}
.today-pulse-2 { animation: todayPulse2 3s ease-in-out 0.5s infinite; }
@keyframes todayPulse2 {
  0%, 100% { r: 18; opacity: 0.3; }
  50% { r: 38; opacity: 0; }
}
.river-footer {
  margin-top: 6px;
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
