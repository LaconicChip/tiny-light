<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import LightInput from './components/LightInput.vue'
import StarRiver from './components/StarRiver.vue'
import LightDetail from './components/LightDetail.vue'
import Stats from './components/Stats.vue'
import OnThisDay from './components/OnThisDay.vue'
import {
  getToday, getRiver, getOnThisDay, getStats,
  createLight, updateLight, deleteLight,
} from './api/lights.js'

/* ===== 业务状态（保持现有逻辑） ===== */
const todayLight = ref(null)
const riverLights = ref([])
const onThisDayLights = ref([])
const stats = ref(null)
const selectedLight = ref(null)
const errorMsg = ref('')
const year = new Date().getFullYear()

/* ===== 日期显示 ===== */
const now = new Date()
const monthEng = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC']
const dayEng = ['SUN','MON','TUE','WED','THU','FRI','SAT']
const heroDate = `${year} · ${String(now.getMonth() + 1).padStart(2, '0')} · ${String(now.getDate()).padStart(2, '0')}`
const inputDateLabel = `${monthEng[now.getMonth()]} ${String(now.getDate()).padStart(2, '0')} · ${dayEng[now.getDay()]}`

/* ===== 背景层 DOM 引用 ===== */
const shootingStarsRef = ref(null)
const particleCanvasRef = ref(null)
const cursorGlowRef = ref(null)
const inputSecRef = ref(null)
const heroEmblemRef = ref(null)
const moonGlowRef = ref(null)
const heroBadgeRef = ref(null)
const heroTitleRef = ref(null)
const heroSubRef = ref(null)

const prm = window.matchMedia('(prefers-reduced-motion: reduce)').matches
const touch = window.matchMedia('(hover: none)').matches

/* ===== API 处理（保持现有逻辑） ===== */
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
    if (!prm) {
      // 从「点亮今天」按钮位置爆发金色粒子
      const btn = document.querySelector('.light-btn')
      if (btn) {
        const r = btn.getBoundingClientRect()
        burstAt(r.left + r.width / 2, r.top + r.height / 2, 18, 0.85)
      }
      shoot(); setTimeout(shoot, 300) // 庆祝流星
    }
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
function handleSelect(light) { selectedLight.value = light }
function handleClose() { selectedLight.value = null }

function scrollToInput() {
  inputSecRef.value?.scrollIntoView({ behavior: 'smooth' })
}

/* ===== 背景动效 1+3：单 canvas 单 rAF（闪烁星 + 十字闪光 + 金尘 + 爆发粒子） =====
   合并原因：原 stars-canvas 在 2000px 高容器内，retina 屏 canvas 实际 4000px 高，
   每帧 clearRect+redraw 数百万像素（视口仅 ~800px 可见），桌面端也卡。
   现在全部画到 particleCanvas（position:fixed 视口大小），一个 rAF 循环按层绘制。
   视觉：星从"滚动滚走"改为"固定背景"（视差设计里远景星固定更自然）。 */
let bgRaf = null
let bgResizeHandler = null
let bgVisibilityHandler = null
let bgPaused = false
let bgLoopFn = null  // 暴露 loop 供滚动暂停后重启
let scrollStopTimer = null
let idleTimer = null  // 静止计时器：3秒无交互后完全停止 canvas
let bgIdle = false    // 是否进入静止休眠
const burstParticles = []
function seededRand(seed) {
  const x = Math.sin(seed * 12.9898) * 43758.5453
  return x - Math.floor(x)
}
function initBackgroundCanvas() {
  if (prm) return
  const cv = particleCanvasRef.value
  if (!cv) return
  const ctx = cv.getContext('2d', { alpha: true })
  // 移动端 DPR 限 1.5（填充率比 2.0 低 44%），桌面端限 2.0
  const isMobile = window.matchMedia('(hover: none) and (pointer: coarse)').matches
  const dpr = Math.min(window.devicePixelRatio || 1, isMobile ? 1.5 : 2)
  // 移动端减少粒子数（60→40），降低每帧计算量
  const particleCount = isMobile ? 40 : 60
  // 闪烁星：150→100（桌面）/ 70（移动），背景慢动画不需要高密度星点
  const starCount = isMobile ? 70 : 100
  let w, h
  // 预生成闪烁星 + 十字闪光（确定性，刷新一致）
  const stars = []
  for (let i = 0; i < starCount; i++) {
    const r1 = seededRand(i + 1), r2 = seededRand(i + 100), r3 = seededRand(i + 200)
    const r4 = seededRand(i + 300), r5 = seededRand(i + 400), r6 = seededRand(i + 500)
    const r7 = seededRand(i + 600), r8 = seededRand(i + 700), r9 = seededRand(i + 800)
    stars.push({
      x: r1, y: r2, sz: r3 * 2.2 + 0.4,
      dur: 2 + r4 * 4, delay: r5 * 5,
      minOp: 0.1 + r6 * 0.15, maxOp: 0.3 + r7 * 0.5,
      gold: r8 < 0.1, glow: r9 < 0.03,
    })
  }
  const sparkles = []
  for (let i = 0; i < 6; i++) {
    sparkles.push({
      x: seededRand(i + 900), y: seededRand(i + 1000) * 0.55,
      dur: 5 + seededRand(i + 1100) * 4, delay: seededRand(i + 1200) * 8,
    })
  }
  // 金尘粒子
  const ps = []
  function makeParticle(init) {
    return {
      x: Math.random() * w,
      y: init ? Math.random() * h : h + 10,
      r: Math.random() * 1.8 + 0.4,
      vy: -(Math.random() * 0.2 + 0.06),
      vx: (Math.random() - 0.5) * 0.12,
      op: Math.random() * 0.3 + 0.05,
      gold: Math.random() > 0.4,
      p: Math.random() * Math.PI * 2,
    }
  }
  function resize() {
    w = window.innerWidth
    h = window.innerHeight
    cv.width = w * dpr
    cv.height = h * dpr
    cv.style.width = w + 'px'
    cv.style.height = h + 'px'
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    ps.length = 0
    for (let i = 0; i < particleCount; i++) ps.push(makeParticle(true))
  }
  resize()
  bgResizeHandler = resize
  window.addEventListener('resize', resize)
  let start = performance.now()
  // 标签不可见时暂停 rAF（切标签页/最小化时停止烧 CPU/GPU）
  bgVisibilityHandler = () => {
    bgPaused = document.hidden
    if (!bgPaused && bgRaf === null) {
      start = performance.now()  // 重置时间基准避免跳变
      bgRaf = requestAnimationFrame(loop)
    }
  }
  document.addEventListener('visibilitychange', bgVisibilityHandler)
  let lastDraw = 0
  // 热节流自适应：监测真实 rAF 间隔，设备降频时主动降帧率减负降温，恢复后升回。
  // 仅在热节流时生效，冷状态全速 30fps。升级阈值 28/40ms，恢复阈值 22ms（带迟滞防抖动）。
  let degradeLevel = 0
  let lastRaf = 0
  const rafSamples = []
  const SAMPLE_WIN = 30
  const throttleMs = () => (degradeLevel === 0 ? 33 : degradeLevel === 1 ? 50 : 66)
  function loop(t) {
    if (bgPaused || bgIdle) { bgRaf = null; return }
    // 真实 rAF 间隔反映设备刷新能力（不受下方节流干扰）；跳过 >200ms 异常间隔（休眠/切标签唤醒）
    if (lastRaf > 0) {
      const rafDt = t - lastRaf
      if (rafDt < 200) {
        rafSamples.push(rafDt)
        if (rafSamples.length > SAMPLE_WIN) rafSamples.shift()
        if (rafSamples.length >= SAMPLE_WIN) {
          let avg = 0
          for (let i = 0; i < rafSamples.length; i++) avg += rafSamples[i]
          avg /= rafSamples.length
          if (avg > 28 && degradeLevel === 0) { degradeLevel = 1; rafSamples.length = 0 }
          else if (avg > 40 && degradeLevel === 1) { degradeLevel = 2; rafSamples.length = 0 }
          else if (avg < 22 && degradeLevel > 0) { degradeLevel--; rafSamples.length = 0 }
        }
      }
    }
    lastRaf = t
    // 帧率节流：正常 ~30fps(33ms)；热节流降级时降到 20fps(50ms)/15fps(66ms) 减负降温
    if (t - lastDraw < throttleMs()) { bgRaf = requestAnimationFrame(loop); return }
    lastDraw = t
    const time = (t - start) / 1000
    ctx.clearRect(0, 0, w, h)
    // 1. 金尘粒子（最底层）—— 移除 glow halo（原 gold&&r>1.2 时多一次 arc+fill，
    //    占金尘绘制量 50%+，opacity 0.08 几乎不可见，删除无损视觉）
    for (let i = 0; i < ps.length; i++) {
      const p = ps[i]
      p.x += p.vx + Math.sin(p.p) * 0.08
      p.y += p.vy
      p.p += 0.015
      const o = p.op * (0.6 + 0.4 * Math.sin(p.p))
      ctx.globalAlpha = p.gold ? o : o * 0.3
      ctx.fillStyle = p.gold ? '#f8e39a' : '#f5f2eb'
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fill()
      if (p.y < -15 || p.x < -15 || p.x > w + 15) ps[i] = makeParticle(false)
    }
    // 2. 闪烁星 —— 跳过透明度极低的星（<0.05 几乎不可见，省掉 arc+fill）
    for (let i = 0; i < stars.length; i++) {
      const s = stars[i]
      const phase = ((time + s.delay) % s.dur) / s.dur
      const k = (1 - Math.cos(phase * Math.PI * 2)) / 2
      const op = s.minOp + (s.maxOp - s.minOp) * k
      if (op < 0.05) continue
      const r = (s.sz * (1 + 0.3 * k)) / 2
      const x = s.x * w, y = s.y * h
      ctx.globalAlpha = op
      ctx.fillStyle = s.gold ? '#f8e39a' : '#f0ede6'
      ctx.beginPath()
      ctx.arc(x, y, r, 0, Math.PI * 2)
      ctx.fill()
      if (s.glow) {
        ctx.globalAlpha = 0.2
        ctx.fillStyle = '#f8e39a'
        ctx.beginPath()
        ctx.arc(x, y, s.sz * 3, 0, Math.PI * 2)
        ctx.fill()
      }
    }
    // 3. 十字闪光
    for (let i = 0; i < sparkles.length; i++) {
      const sp = sparkles[i]
      const phase = ((time + sp.delay) % sp.dur) / sp.dur
      if (phase < 0.4 || phase > 0.6) continue
      const local = (phase - 0.4) / 0.2
      const op = Math.sin(local * Math.PI) * 0.6
      const scale = 0.3 + 0.7 * Math.sin(local * Math.PI)
      ctx.save()
      ctx.translate(sp.x * w, sp.y * h)
      ctx.rotate(local * 90 * Math.PI / 180)
      ctx.globalAlpha = op
      ctx.fillStyle = '#f8e39a'
      ctx.fillRect(-0.5, -0.5, 1, 1)
      ctx.fillRect(-9 * scale, -0.5, 18 * scale, 1)
      ctx.fillRect(-0.5, -9 * scale, 1, 18 * scale)
      ctx.restore()
    }
    // 4. 爆发粒子（最顶层）
    for (let i = burstParticles.length - 1; i >= 0; i--) {
      const p = burstParticles[i]
      p.x += p.vx
      p.y += p.vy
      p.vy += 0.04
      p.life -= 0.02
      if (p.life <= 0) { burstParticles.splice(i, 1); continue }
      const op = p.life * (p.isGold ? 0.7 : 0.3)
      ctx.globalAlpha = op
      ctx.fillStyle = p.isGold ? '#f8e39a' : '#f5f2eb'
      ctx.beginPath()
      ctx.arc(p.x, p.y, 2 * p.life, 0, Math.PI * 2)
      ctx.fill()
      if (p.isGold) {
        ctx.globalAlpha = op * 0.5
        ctx.fillStyle = '#edce6e'
        ctx.beginPath()
        ctx.arc(p.x, p.y, 5 * p.life, 0, Math.PI * 2)
        ctx.fill()
      }
    }
    bgRaf = requestAnimationFrame(loop)
  }
  bgLoopFn = loop  // 暴露给滚动暂停后重启
  bgRaf = requestAnimationFrame(loop)

  /* 静止休眠：3秒无交互后完全停止 canvas rAF，GPU 占用归零。
     鼠标移动/触摸/滚动时唤醒重启。这是 GPU 100% 的核心解法：
     页面静止时背景星点/粒子虽好看但不需要持续重绘。 */
  function scheduleIdle() {
    clearTimeout(idleTimer)
    bgIdle = false
    idleTimer = setTimeout(() => {
      bgIdle = true
      // rAF 循环会在下一帧检测 bgIdle 并自动停止
    }, 3000)
  }
  function wakeFromIdle() {
    if (bgIdle) {
      bgIdle = false
      start = performance.now()  // 重置时间基准避免跳变
      if (bgRaf === null) bgRaf = requestAnimationFrame(loop)
    }
    scheduleIdle()
  }
  scheduleIdle()  // 初始启动休眠计时
  // 鼠标移动/触摸唤醒（桌面端鼠标移动频繁，用 rAF 节流避免过度唤醒）
  let wakeRaf = null
  function onUserActivity() {
    if (wakeRaf === null) {
      wakeRaf = requestAnimationFrame(() => { wakeRaf = null; wakeFromIdle() })
    }
  }
  document.addEventListener('mousemove', onUserActivity, { passive: true })
  document.addEventListener('touchstart', onUserActivity, { passive: true })
  document.addEventListener('keydown', onUserActivity, { passive: true })
}

/* ===== 背景动效 2：流星 ===== */
let shootTimer = null
function shoot() {
  if (prm) return
  const c = shootingStarsRef.value
  if (!c) return
  const s = document.createElement('div')
  s.className = 'shooting-star'
  const cw = c.clientWidth || window.innerWidth
  const ch = c.clientHeight || window.innerHeight * 0.5
  const sx = Math.random() * cw * 0.5 + cw * 0.3
  const sy = Math.random() * ch * 0.4 + 20
  const ang = -20 - Math.random() * 25
  const dur = 1.8 + Math.random() * 2.2
  const dist = 400 + Math.random() * 350
  s.style.cssText = `left:${sx}px;top:${sy}px;--sdur:${dur}s;--ang:${ang}deg;--sx:${-dist}px;--sy:${dist * 0.55}px;--tail:${100 + Math.random() * 100}px;`
  c.appendChild(s)
  setTimeout(() => s.remove(), dur * 1000 + 200)
}
function schedShoot() {
  if (prm) return
  shootTimer = setTimeout(() => {
    shoot()
    if (Math.random() < 0.5) setTimeout(shoot, 200 + Math.random() * 400)
    if (Math.random() < 0.25) setTimeout(shoot, 500 + Math.random() * 500)
    schedShoot()
  }, 2500 + Math.random() * 3000)
}

/* ===== 背景动效 4：光标金色光晕（lerp 跟随，静止时自动休眠） ===== */
let cursorRaf = null
let cursorMoveHandler = null
let cursorLeaveHandler = null
function initCursor() {
  if (prm || touch) return
  const g = cursorGlowRef.value
  if (!g) return
  let mx = window.innerWidth / 2, my = window.innerHeight / 2, cx = mx, cy = my
  let idle = 0
  cursorMoveHandler = (e) => {
    mx = e.clientX; my = e.clientY; g.classList.add('on')
    if (cursorRaf === null) { idle = 0; loop() } // 休眠中被鼠标唤醒
  }
  cursorLeaveHandler = () => g.classList.remove('on')
  document.addEventListener('mousemove', cursorMoveHandler)
  document.addEventListener('mouseleave', cursorLeaveHandler)
  function loop() {
    const dx = mx - cx, dy = my - cy
    cx += dx * 0.07
    cy += dy * 0.07
    g.style.transform = `translate3d(${cx}px,${cy}px,0) translate(-50%,-50%)`
    if (Math.abs(dx) < 0.1 && Math.abs(dy) < 0.1) {
      // 已收敛到目标点：再跑 30 帧（约 0.5s）确认静止后休眠，视觉无差
      if (++idle > 30) { cursorRaf = null; return }
    } else {
      idle = 0
    }
    cursorRaf = requestAnimationFrame(loop)
  }
  loop()
}

/* ===== 背景动效 5：点击爆发粒子（push 到 burstParticles，由 bgRaf 消费） ===== */
let burstClickHandler = null
function burstAt(x, y, count = 10, goldRatio = 0.7) {
  if (prm) return
  for (let i = 0; i < count; i++) {
    const ang = (Math.PI * 2 / count) * i + Math.random() * 0.4
    const spd = 1.5 + Math.random() * 2.5
    const isGold = Math.random() < goldRatio
    burstParticles.push({
      x, y,
      vx: Math.cos(ang) * spd,
      vy: Math.sin(ang) * spd - 1,
      life: 1,
      isGold,
    })
  }
}
function initBurst() {
  if (prm || touch) return
  burstClickHandler = (e) => {
    if (e.target.closest('button,textarea,.mood-tag,.river-star,.memory-card,.input-card,.scroll-hint,.light-btn')) return
    burstAt(e.clientX, e.clientY, 10, 0.7)
  }
  document.addEventListener('click', burstClickHandler)
}

/* ===== 视差滚动（rAF 节流）+ 滚动时冻结背景动画 =====
   滚动卡顿根因：主线程同时处理视差 update + canvas rAF(每帧 redraw 218 对象)
   + aurora/moon-glow 的 blur 合成。滚动时冻结 canvas + 暂停 CSS 动画，
   主线程专注滚动，滚停 150ms 后恢复。视觉上滚动时背景静止反而更自然。 */
let parallaxScrollHandler = null
let parallaxTicking = false
function initParallax() {
  if (prm) return
  function update() {
    const y = window.scrollY, vh = window.innerHeight
    if (y < vh) {
      const p = y / vh
      if (heroEmblemRef.value) heroEmblemRef.value.style.transform = `translate3d(0,${y * 0.35}px,0) rotate(${y * 0.025}deg) scale(${1 - p * 0.15})`
      if (moonGlowRef.value) moonGlowRef.value.style.transform = `translate(-50%,${y * 0.2}px) scale(${1 - p * 0.12})`
      if (heroBadgeRef.value) heroBadgeRef.value.style.transform = `translate3d(0,${y * 0.18}px,0)`
      if (heroTitleRef.value) heroTitleRef.value.style.transform = `translate3d(0,${y * 0.12}px,0)`
      if (heroSubRef.value) heroSubRef.value.style.transform = `translate3d(0,${y * 0.08}px,0)`
    }
    parallaxTicking = false
  }
  parallaxScrollHandler = () => {
    if (!parallaxTicking) { requestAnimationFrame(update); parallaxTicking = true }
    // 滚动时冻结 canvas rAF（主线程性能收益主要来源）。
    // 不再操作 classList 暂停 CSS 动画 —— paused→running 恢复时 opacity 突跳会产生高亮闪烁。
    // CSS 动画在 contain+will-change 合成层上运行，不阻塞滚动。
    if (!bgPaused) {
      bgPaused = true
    }
    // 滚动也唤醒 idle 休眠（用户在活动）
    if (bgIdle) {
      bgIdle = false
      clearTimeout(idleTimer)
    }
    clearTimeout(scrollStopTimer)
    scrollStopTimer = setTimeout(() => {
      if (bgPaused) {  // 只在确实处于滚动态时才恢复
        bgPaused = false
        // 滚停后如果不在 idle 休眠，恢复 canvas；idle 计时由 onUserActivity 管理
        if (!bgIdle && bgRaf === null && bgLoopFn) bgRaf = requestAnimationFrame(bgLoopFn)
      }
    }, 150)
  }
  window.addEventListener('scroll', parallaxScrollHandler, { passive: true })
}

/* ===== 滚动揭示（IntersectionObserver） ===== */
let revealObserver = null
function initReveal() {
  revealObserver = new IntersectionObserver((entries) => {
    entries.forEach((e) => {
      if (e.isIntersecting) {
        e.target.classList.add('visible')
        revealObserver.unobserve(e.target)
        // 入场过渡结束后清掉 transition-delay：它本是入场交错用的，
        // 不清除的话后续 hover 动画也要等 0.1~0.55s 才启动（可感知的"卡顿延迟"）
        const el = e.target
        const clear = () => {
          el.style.transitionDelay = '0s'
          el.removeEventListener('transitionend', clear)
        }
        el.addEventListener('transitionend', clear)
      }
    })
  }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' })
}

/* ===== 生命周期 ===== */
onMounted(async () => {
  // 首屏分阶段初始化：先启动可见的背景效果，再延后非关键功能
  // 避免首帧同时创建 18 个 CSS animation 合成层 + canvas 初始化 + 数据请求
  initBackgroundCanvas()
  initParallax()
  initReveal()
  // 延后 2 帧再启动 cursor/burst/shoot（非首屏关键路径）
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      initCursor()
      initBurst()
      schedShoot()
      if (!prm) { setTimeout(shoot, 1500); setTimeout(shoot, 3500) }
    })
  })

  try {
    await refreshAll()
    // 数据加载后，下一帧再观察滚动揭示元素（确保子组件已渲染）
    await nextTick()
    if (revealObserver) {
      document.querySelectorAll('.input-card,.stat-badge,.river-header,.memories-header,.memory-card').forEach((el) => revealObserver.observe(el))
    }
  } catch (e) {
    showError('加载失败，请检查后端是否启动')
  }
})

onUnmounted(() => {
  if (shootTimer) clearTimeout(shootTimer)
  if (scrollStopTimer) clearTimeout(scrollStopTimer)
  if (bgRaf) cancelAnimationFrame(bgRaf)
  if (cursorRaf) cancelAnimationFrame(cursorRaf)
  if (bgResizeHandler) window.removeEventListener('resize', bgResizeHandler)
  if (bgVisibilityHandler) document.removeEventListener('visibilitychange', bgVisibilityHandler)
  if (cursorMoveHandler) document.removeEventListener('mousemove', cursorMoveHandler)
  if (cursorLeaveHandler) document.removeEventListener('mouseleave', cursorLeaveHandler)
  if (burstClickHandler) document.removeEventListener('click', burstClickHandler)
  if (parallaxScrollHandler) window.removeEventListener('scroll', parallaxScrollHandler)
  if (revealObserver) revealObserver.disconnect()
})
</script>

<template>
  <!-- 背景层 -->
  <div class="cursor-glow" ref="cursorGlowRef"></div>
  <div class="bg-gradient"></div>
  <div class="bg-aurora">
    <div class="aurora-band aurora-1"></div>
    <div class="aurora-band aurora-2"></div>
    <div class="aurora-band aurora-3"></div>
  </div>
  <canvas id="particleCanvas" ref="particleCanvasRef"></canvas>
  <div class="shooting-stars" ref="shootingStarsRef"></div>

  <Transition name="error">
    <div v-if="errorMsg" class="error-bar">{{ errorMsg }}</div>
  </Transition>

  <div class="page">
    <div class="page-inner">
      <!-- Hero -->
      <section class="hero">
        <div class="hero-emblem" ref="heroEmblemRef">
          <svg viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <radialGradient id="mg" cx="0.38" cy="0.32" r="0.68">
                <stop offset="0%" stop-color="#fff8e0"/>
                <stop offset="40%" stop-color="#f8e39a"/>
                <stop offset="100%" stop-color="#c99a35"/>
              </radialGradient>
              <filter id="mgl"><feGaussianBlur stdDeviation="3" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
            </defs>
            <circle cx="60" cy="60" r="52" fill="none" stroke="rgba(248,227,154,0.08)" stroke-width="0.5"/>
            <circle cx="60" cy="60" r="44" fill="none" stroke="rgba(248,227,154,0.05)" stroke-width="0.5"/>
            <circle cx="60" cy="60" r="28" fill="url(#mg)" filter="url(#mgl)"/>
            <circle cx="72" cy="53" r="24" fill="#16162e"/>
            <g transform="translate(94,28)" opacity="0.7"><path d="M0,-4 L1.2,0 L0,4 L-1.2,0 Z" fill="#f8e39a"/></g>
            <g transform="translate(24,86)" opacity="0.4"><path d="M0,-2.5 L0.7,0 L0,2.5 L-0.7,0 Z" fill="#f8e39a"/></g>
          </svg>
        </div>
        <div class="moon-glow" ref="moonGlowRef"></div>
        <svg class="constellation" style="top:10%;left:4%;width:130px;height:80px" viewBox="0 0 130 80">
          <line x1="10" y1="20" x2="40" y2="35"/><line x1="40" y1="35" x2="58" y2="12"/><line x1="40" y1="35" x2="80" y2="52"/><line x1="80" y1="52" x2="110" y2="28"/><line x1="80" y1="52" x2="120" y2="65"/>
          <circle cx="10" cy="20" r="1.3"/><circle cx="40" cy="35" r="2"/><circle cx="58" cy="12" r="0.9"/><circle cx="80" cy="52" r="1.6"/><circle cx="110" cy="28" r="0.9"/><circle cx="120" cy="65" r="1.1"/>
        </svg>
        <svg class="constellation" style="bottom:20%;left:2%;width:100px;height:60px" viewBox="0 0 100 60">
          <line x1="8" y1="45" x2="32" y2="30"/><line x1="32" y1="30" x2="58" y2="42"/><line x1="58" y1="42" x2="85" y2="18"/><line x1="58" y1="42" x2="95" y2="50"/>
          <circle cx="8" cy="45" r="0.9"/><circle cx="32" cy="30" r="1.4"/><circle cx="58" cy="42" r="1.8"/><circle cx="85" cy="18" r="0.9"/><circle cx="95" cy="50" r="1.1"/>
        </svg>

        <div class="hero-badge" ref="heroBadgeRef">
          <span class="line"></span>TINY LIGHT · {{ year }}<span class="line"></span>
        </div>
        <h1 class="hero-title" ref="heroTitleRef">今日<span class="gold">微</span>光</h1>
        <div class="hero-ornament">
          <svg width="90" height="10" viewBox="0 0 90 10">
            <line x1="0" y1="5" x2="34" y2="5" stroke="rgba(237,206,110,0.25)" stroke-width="0.5"/>
            <circle cx="45" cy="5" r="2" fill="#edce6e"/>
            <circle cx="45" cy="5" r="5" fill="none" stroke="rgba(237,206,110,0.15)" stroke-width="0.5"/>
            <line x1="56" y1="5" x2="90" y2="5" stroke="rgba(237,206,110,0.25)" stroke-width="0.5"/>
          </svg>
        </div>
        <p class="hero-sub" ref="heroSubRef">每一天，都有值得被记住的一瞬。<br>把它点亮，让星河为你留存。</p>
        <div class="hero-date">
          <svg width="10" height="10" viewBox="0 0 16 16" fill="currentColor"><path d="M8 0l1.8 5.5L16 8l-6.2 2.5L8 16l-1.8-5.5L0 8l6.2-2.5z"/></svg>
          {{ heroDate }}
        </div>
        <div class="scroll-hint" @click="scrollToInput">
          <span class="scroll-hint-text">星河在等你</span>
          <div class="scroll-indicator"><div class="scroll-dot"></div></div>
        </div>
      </section>

      <!-- 输入区 -->
      <section class="section-input" id="inputSec" ref="inputSecRef">
        <LightInput
          :today-light="todayLight"
          :date-label="inputDateLabel"
          @submit="handleSubmit"
          @update="handleUpdate"
          @delete="handleDelete"
        />
      </section>

      <!-- 统计徽章 -->
      <section class="section-stats">
        <Stats :stats="stats" />
      </section>

      <!-- 星光河 -->
      <section class="section-river">
        <StarRiver :lights="riverLights" :year="year" @select="handleSelect" />
      </section>

      <!-- 往年今日 -->
      <section class="section-memories">
        <OnThisDay :lights="onThisDayLights" @select="handleSelect" />
      </section>

      <footer class="footer">
        <div class="footer-line"></div>
        <p class="footer-text">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg>
          愿你每天都有微光可循
        </p>
        <p class="footer-sub">TINY LIGHT · 今日微光</p>
      </footer>
    </div>
  </div>

  <Transition name="modal" appear>
    <LightDetail v-if="selectedLight" :light="selectedLight" @close="handleClose" />
  </Transition>
</template>

<style scoped>
.error-bar {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--glass-dark);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid rgba(196, 69, 69, 0.4);
  color: #f5f2eb;
  padding: 10px 18px;
  border-radius: 14px;
  font-size: 13px;
  z-index: 200;
  box-shadow: var(--glass-inner-shadow), 0 8px 24px rgba(0, 0, 0, 0.32);
  letter-spacing: 0.04em;
}
.error-enter-from, .error-leave-to {
  opacity: 0;
  transform: translate(-50%, -8px);
}
.error-enter-active, .error-leave-active {
  transition: opacity 200ms ease-out, transform 200ms cubic-bezier(0.16, 1, 0.3, 1);
}

.page { position: relative; z-index: 10; padding: 0 var(--pad); }
.page-inner { max-width: 1100px; margin: 0 auto; position: relative; }

/* Hero */
.hero {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  padding: 80px 0 60px;
  position: relative;
}
.hero-emblem {
  position: absolute;
  top: clamp(40px, 8vh, 90px);
  right: clamp(10px, 6vw, 80px);
  width: clamp(60px, 9vw, 110px);
  height: clamp(60px, 9vw, 110px);
  animation: floatEmblem 10s ease-in-out infinite;
}
@keyframes floatEmblem {
  0%, 100% { transform: translate3d(0, 0, 0) rotate(-3deg); }
  50% { transform: translate3d(0, -12px, 0) rotate(2deg); }
}
.moon-glow {
  position: absolute;
  top: 40%;
  left: 30%;
  transform: translate(-50%, -50%);
  width: 380px;
  height: 380px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(248,227,154,0.1) 0%, rgba(237,206,110,0.03) 40%, transparent 70%);
  animation: moonBreath 8s ease-in-out infinite;
  pointer-events: none;
}
@keyframes moonBreath {
  0%, 100% { opacity: 0.6; transform: translate(-50%, -50%) scale(1); }
  50% { opacity: 1; transform: translate(-50%, -50%) scale(1.15); }
}
.constellation { position: absolute; pointer-events: none; opacity: 0.2; }
.constellation line { stroke: rgba(245,242,235,0.1); stroke-width: 0.5; }
.constellation circle { fill: var(--platinum-2); }

.hero-badge {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  font-weight: 400;
  letter-spacing: 0.35em;
  text-transform: uppercase;
  color: var(--gold-3);
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 10px;
  opacity: 0;
  animation: fadeUp 1s var(--ease-out) 0.3s forwards;
}
.hero-badge .line { width: 24px; height: 1px; background: var(--gold-2); opacity: 0.5; }
.hero-title {
  font-family: var(--font-sans);
  font-size: clamp(3rem, 9vw, 5.5rem);
  font-weight: 200;
  color: var(--platinum-1);
  line-height: 1;
  letter-spacing: 0.12em;
  margin-bottom: 20px;
  opacity: 0;
  animation: fadeUp 1.2s var(--ease-out) 0.5s forwards;
  text-shadow: 0 0 60px rgba(248, 227, 154, 0.08);
}
.hero-title .gold {
  font-weight: 300;
  background: linear-gradient(135deg, var(--gold-3), var(--gold-4), var(--gold-2));
  background-size: 200% 200%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: shimmer 6s ease-in-out infinite;
  filter: drop-shadow(0 0 20px var(--gold-glow-soft));
}
@keyframes shimmer { 0%, 100% { background-position: 0% 50%; } 50% { background-position: 100% 50%; } }
.hero-ornament { margin-bottom: 20px; opacity: 0; animation: fadeUp 1.2s var(--ease-out) 0.7s forwards; }
.hero-sub {
  font-family: var(--font-sans);
  font-size: clamp(0.9rem, 1.8vw, 1.15rem);
  font-weight: 300;
  color: var(--text-light-muted);
  max-width: 340px;
  line-height: 2;
  letter-spacing: 0.06em;
  opacity: 0;
  animation: fadeUp 1.2s var(--ease-out) 0.9s forwards;
}
.hero-date {
  margin-top: 32px;
  font-family: var(--font-mono);
  font-size: 0.72rem;
  font-weight: 300;
  color: var(--gold-3);
  letter-spacing: 0.25em;
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0;
  animation: fadeUp 1.2s var(--ease-out) 1.2s forwards;
}
.hero-date svg { opacity: 0.6; }

.scroll-hint {
  position: absolute;
  bottom: clamp(28px, 5vh, 50px);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  opacity: 0;
  animation: fadeUp 1.5s var(--ease-out) 2s forwards;
}
.scroll-hint-text {
  font-family: var(--font-sans);
  font-size: 0.68rem;
  font-weight: 300;
  color: var(--gold-3);
  letter-spacing: 0.35em;
  opacity: 0.6;
  animation: breathe 3s ease-in-out infinite;
}
@keyframes breathe { 0%, 100% { opacity: 0.3; } 50% { opacity: 0.75; } }
.scroll-indicator {
  width: 20px;
  height: 32px;
  border: 1.5px solid rgba(237, 206, 110, 0.3);
  border-radius: 10px;
  display: flex;
  justify-content: center;
  padding-top: 6px;
}
.scroll-dot {
  width: 3px;
  height: 3px;
  background: var(--gold-3);
  border-radius: 50%;
  box-shadow: 0 0 6px var(--gold-glow-mid);
  animation: dotDrop 2s ease-in-out infinite;
}
@keyframes dotDrop {
  0% { transform: translate3d(0, 0, 0); opacity: 1; }
  60% { transform: translate3d(0, 10px, 0); opacity: 0; }
  61% { transform: translate3d(0, 0, 0); opacity: 0; }
  100% { transform: translate3d(0, 0, 0); opacity: 1; }
}

.section-input { padding: clamp(36px, 6vh, 56px) 0 24px; position: relative; }
.section-stats { padding: 32px 0 clamp(48px, 7vh, 80px); position: relative; min-height: 180px; }
.section-river { padding: clamp(24px, 4vh, 40px) 0 36px; position: relative; }
.section-memories { padding: clamp(32px, 5vh, 48px) 0 24px; position: relative; }

/* 页脚 */
.footer { padding: clamp(36px, 5vh, 52px) 0 clamp(24px, 3vh, 32px); text-align: center; position: relative; }
.footer-line { width: 60px; height: 1px; background: linear-gradient(to right, transparent, var(--gold-1), transparent); margin: 0 auto 24px; }
.footer-text {
  font-family: var(--font-sans);
  font-size: clamp(0.95rem, 1.8vw, 1.1rem);
  font-weight: 300;
  color: var(--text-dark-muted);
  letter-spacing: 0.18em;
  line-height: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.footer-text svg { color: var(--gold-1); opacity: 0.6; }
.footer-sub { margin-top: 14px; font-family: var(--font-mono); font-size: 0.62rem; color: var(--platinum-5); letter-spacing: 0.28em; }

/* 响应式 */
@media (max-width: 768px) {
  .hero { padding-top: 50px; }
  .hero-emblem { top: 25px; right: 10px; }
  .section-stats { min-height: auto; display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; padding: 20px 0 40px; }
}
@media (max-width: 480px) {
  .hero-title { letter-spacing: 0.06em; }
  .hero-emblem { width: 50px; height: 50px; top: 20px; right: 12px; }
}
</style>
