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
const starsBgRef = ref(null)
const shootingStarsRef = ref(null)
const particleCanvasRef = ref(null)
const cursorGlowRef = ref(null)
const burstContainerRef = ref(null)
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

/* ===== 背景动效 1：150 颗闪烁星 + 8 颗十字闪光 ===== */
function createStars() {
  const c = starsBgRef.value
  if (!c) return
  const f = document.createDocumentFragment()
  for (let i = 0; i < 150; i++) {
    const s = document.createElement('div')
    s.className = 'star-twinkle'
    const sz = Math.random() * 2.2 + 0.4
    s.style.cssText = `left:${Math.random() * 100}%;top:${Math.random() * 100}%;width:${sz}px;height:${sz}px;--dur:${2 + Math.random() * 4}s;--delay:${Math.random() * 5}s;--min:${0.1 + Math.random() * 0.15};--max:${0.3 + Math.random() * 0.5};`
    if (Math.random() < 0.1) s.style.background = 'var(--gold-4)'
    if (Math.random() < 0.03) s.style.boxShadow = `0 0 ${sz * 3}px rgba(248,227,154,0.2)`
    f.appendChild(s)
  }
  for (let i = 0; i < 8; i++) {
    const sp = document.createElement('div')
    sp.className = 'sparkle-cross'
    sp.style.cssText = `left:${Math.random() * 100}%;top:${Math.random() * 55}%;--sd:${Math.random() * 8}s;animation-duration:${5 + Math.random() * 4}s;`
    f.appendChild(sp)
  }
  c.appendChild(f)
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

/* ===== 背景动效 3：Canvas 金色微尘 60 颗 ===== */
let particleRaf = null
let particleResizeHandler = null
function initParticles() {
  if (prm) return
  const cv = particleCanvasRef.value
  if (!cv) return
  const ctx = cv.getContext('2d')
  let w, h, ps = [], N = 60
  function resize() { w = cv.width = window.innerWidth; h = cv.height = window.innerHeight }
  resize()
  particleResizeHandler = resize
  window.addEventListener('resize', resize)
  function P(init) {
    this.x = Math.random() * w
    this.y = init ? Math.random() * h : h + 10
    this.r = Math.random() * 1.8 + 0.4
    this.vy = -(Math.random() * 0.2 + 0.06)
    this.vx = (Math.random() - 0.5) * 0.12
    this.op = Math.random() * 0.3 + 0.05
    this.gold = Math.random() > 0.4
    this.p = Math.random() * Math.PI * 2
  }
  for (let i = 0; i < N; i++) ps.push(new P(true))
  function loop() {
    ctx.clearRect(0, 0, w, h)
    for (let i = 0; i < ps.length; i++) {
      const p = ps[i]
      p.x += p.vx + Math.sin(p.p) * 0.08
      p.y += p.vy
      p.p += 0.015
      const o = p.op * (0.6 + 0.4 * Math.sin(p.p))
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = p.gold ? `rgba(248,227,154,${o})` : `rgba(245,242,235,${o * 0.3})`
      ctx.fill()
      if (p.gold && p.r > 1.2) {
        ctx.beginPath()
        ctx.arc(p.x, p.y, p.r * 2.5, 0, Math.PI * 2)
        ctx.fillStyle = `rgba(248,227,154,${o * 0.08})`
        ctx.fill()
      }
      if (p.y < -15 || p.x < -15 || p.x > w + 15) ps[i] = new P(false)
    }
    particleRaf = requestAnimationFrame(loop)
  }
  loop()
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

/* ===== 背景动效 5：点击爆发粒子 ===== */
let burstClickHandler = null
function burstAt(x, y, count = 10, goldRatio = 0.7) {
  if (prm) return
  const container = burstContainerRef.value
  if (!container) return
  for (let i = 0; i < count; i++) {
    const p = document.createElement('div')
    p.className = 'burst-particle'
    const ang = (Math.PI * 2 / count) * i + Math.random() * 0.4
    const spd = 1.5 + Math.random() * 2.5
    const isGold = Math.random() < goldRatio
    p.style.cssText = `left:${x}px;top:${y}px;background:${isGold ? '#f8e39a' : '#f5f2eb'};box-shadow:0 0 6px ${isGold ? 'rgba(237,206,110,0.4)' : 'transparent'};opacity:${isGold ? 0.8 : 0.4};`
    container.appendChild(p)
    let dx = 0, dy = 0, vx = Math.cos(ang) * spd, vy = Math.sin(ang) * spd - 1, life = 1
    function anim() {
      dx += vx; dy += vy; vy += 0.04; life -= 0.02
      if (life <= 0) { p.remove(); return }
      p.style.transform = `translate3d(${dx}px,${dy}px,0) scale(${life})`
      p.style.opacity = life * (isGold ? 0.7 : 0.3)
      requestAnimationFrame(anim)
    }
    requestAnimationFrame(anim)
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

/* ===== 视差滚动（rAF 节流） ===== */
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
  createStars()
  initParticles()
  initCursor()
  initBurst()
  initParallax()
  initReveal()
  schedShoot()
  if (!prm) { setTimeout(shoot, 1500); setTimeout(shoot, 3500) }

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
  if (particleRaf) cancelAnimationFrame(particleRaf)
  if (cursorRaf) cancelAnimationFrame(cursorRaf)
  if (particleResizeHandler) window.removeEventListener('resize', particleResizeHandler)
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
  <div class="click-burst-container" ref="burstContainerRef"></div>
  <div class="bg-gradient"></div>
  <div class="bg-aurora">
    <div class="aurora-band aurora-1"></div>
    <div class="aurora-band aurora-2"></div>
    <div class="aurora-band aurora-3"></div>
  </div>
  <canvas id="particleCanvas" ref="particleCanvasRef"></canvas>
  <div class="stars-bg" ref="starsBgRef"></div>
  <div class="shooting-stars" ref="shootingStarsRef"></div>
  <div class="noise"></div>

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
  filter: blur(30px);
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
.section-stats { padding: 32px 0 clamp(48px, 7vh, 80px); position: relative; min-height: 150px; }
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
