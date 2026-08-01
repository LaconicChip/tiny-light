<script setup>
import { ref, watch } from 'vue'
import { PhSparkle, PhPencilSimple, PhTrash, PhCheck, PhX, PhSmiley, PhLeaf, PhMoon, PhSmileySad, PhHeart } from '@phosphor-icons/vue'

const props = defineProps({
  todayLight: Object,
})

const emit = defineEmits(['submit', 'update', 'delete'])

const content = ref('')
const mood = ref('')
const isEditing = ref(false)

const moods = [
  { key: '开心', icon: PhSmiley },
  { key: '平静', icon: PhLeaf },
  { key: '疲惫', icon: PhMoon },
  { key: '难过', icon: PhSmileySad },
  { key: '感恩', icon: PhHeart },
]

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
  if (confirm('确定删除今天的微光吗？删了可以重新点亮。')) {
    emit('delete')
  }
}
</script>

<template>
  <section class="light-input">
    <!-- 已点亮 + 查看态 -->
    <template v-if="todayLight && !isEditing">
      <p class="lit-tip">
        <PhSparkle :size="15" weight="fill" />
        <span>今天的微光已点亮</span>
      </p>
      <blockquote class="lit-content">{{ todayLight.content }}</blockquote>
      <p v-if="todayLight.mood" class="lit-mood">
        <component :is="moods.find(m => m.key === todayLight.mood)?.icon" :size="15" weight="regular" />
        <span>{{ todayLight.mood }}</span>
      </p>
      <div class="actions">
        <button class="text-btn" @click="startEdit">
          <PhPencilSimple :size="13" weight="regular" />
          <span>编辑</span>
        </button>
        <button class="text-btn danger" @click="confirmDelete">
          <PhTrash :size="13" weight="regular" />
          <span>删除</span>
        </button>
      </div>
    </template>

    <!-- 已点亮 + 编辑态 -->
    <template v-else-if="todayLight && isEditing">
      <p class="lit-tip editing">
        <PhPencilSimple :size="15" weight="regular" />
        <span>编辑今天的微光</span>
      </p>
      <textarea
        v-model="content"
        rows="3"
        maxlength="200"
        placeholder="今天有什么让你心头一暖的瞬间？"
      />
      <div class="moods">
        <button
          v-for="m in moods"
          :key="m.key"
          type="button"
          :class="['mood-btn', { active: mood === m.key }]"
          @click="mood = m.key"
        >
          <component :is="m.icon" :size="18" :weight="mood === m.key ? 'fill' : 'regular'" />
          <span>{{ m.key }}</span>
        </button>
      </div>
      <p v-if="content.trim() && !mood" class="hint">先选个心情吧</p>
      <div class="actions">
        <button class="text-btn" @click="cancelEdit">
          <PhX :size="13" weight="regular" />
          <span>取消</span>
        </button>
        <button class="submit-btn" :disabled="!content.trim() || !mood" @click="saveEdit">
          <PhCheck :size="15" weight="bold" />
          <span>保存</span>
        </button>
      </div>
    </template>

    <!-- 未点亮 -->
    <template v-else>
      <p class="form-tip">今天有什么让你心头一暖的瞬间？</p>
      <textarea
        v-model="content"
        rows="3"
        maxlength="200"
        placeholder="一句话就够，记下这束光。"
      />
      <div class="moods">
        <button
          v-for="m in moods"
          :key="m.key"
          type="button"
          :class="['mood-btn', { active: mood === m.key }]"
          @click="mood = m.key"
        >
          <component :is="m.icon" :size="18" :weight="mood === m.key ? 'fill' : 'regular'" />
          <span>{{ m.key }}</span>
        </button>
      </div>
      <p v-if="content.trim() && !mood" class="hint">先选个心情吧</p>
      <button class="submit-btn block" :disabled="!content.trim() || !mood" @click="submit">
        <PhSparkle :size="15" weight="fill" />
        <span>点亮今天</span>
      </button>
    </template>
  </section>
</template>

<style scoped>
.light-input {
  position: relative;
  background: var(--bg-paper);
  border-left: 2px solid var(--gold);
  border-radius: 0 8px 8px 0;
  padding: 26px 28px 22px;
}
.lit-tip,
.lit-tip.editing {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 14px;
  color: var(--gold);
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 0.06em;
}
.form-tip {
  margin: 0 0 14px;
  color: var(--text);
  font-family: var(--font-display);
  font-size: 17px;
  letter-spacing: 0.04em;
}
textarea {
  width: 100%;
  resize: none;
  border: none;
  border-bottom: 1px dashed var(--hairline);
  border-radius: 0;
  padding: 8px 0 12px;
  font-size: 16px;
  font-family: var(--font-display);
  line-height: 1.85;
  background: transparent;
  color: var(--text);
  box-sizing: border-box;
  overflow-y: auto;
  transition: border-color 150ms ease;
}
textarea::placeholder {
  color: var(--text-faint);
  font-family: var(--font-body);
  font-size: 14px;
}
textarea:focus {
  outline: none;
  border-bottom-color: var(--gold);
}
.moods {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  margin: 16px 0 12px;
}
.mood-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px 5px 10px;
  border: none;
  background: transparent;
  color: var(--text-soft);
  cursor: pointer;
  font-size: 14px;
  font-family: var(--font-display);
  letter-spacing: 0.04em;
  transition: color 150ms ease, transform 100ms ease;
}
.mood-btn:hover {
  color: var(--gold);
  transform: translateY(-1px);
}
.mood-btn.active {
  color: var(--gold);
  position: relative;
}
.mood-btn.active::after {
  content: '';
  position: absolute;
  left: 8px;
  right: 8px;
  bottom: 2px;
  height: 1px;
  background: var(--gold);
  opacity: 0.6;
}
.hint {
  margin: 0 0 8px;
  color: var(--text-soft);
  font-size: 13px;
  text-align: right;
  font-family: var(--font-body);
}
.actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
}
.text-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: var(--text-soft);
  cursor: pointer;
  font-size: 13px;
  font-family: var(--font-body);
  padding: 6px 10px;
  border-radius: 6px;
  transition: color 150ms ease, background 150ms ease;
}
.text-btn:hover {
  color: var(--text);
  background: rgba(201, 169, 97, 0.08);
}
.text-btn.danger:hover {
  color: #c44545;
  background: rgba(196, 69, 69, 0.06);
}
.submit-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  border: none;
  border-radius: 999px;
  background: var(--gold);
  color: #fff;
  font-size: 14px;
  font-family: var(--font-display);
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: background 150ms ease;
}
.submit-btn:hover:not(:disabled) {
  background: var(--gold-lit);
}
.submit-btn.block {
  margin-top: 8px;
  padding: 10px 26px;
}
.submit-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.lit-content {
  margin: 0 0 10px;
  padding: 0 0 0 4px;
  border: none;
  color: var(--text);
  font-size: 18px;
  line-height: 1.85;
  font-family: var(--font-display);
  letter-spacing: 0.02em;
}
.lit-mood {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 14px;
  color: var(--text-soft);
  font-size: 14px;
  font-family: var(--font-display);
  letter-spacing: 0.04em;
}
</style>
