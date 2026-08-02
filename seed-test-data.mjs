// 生成测试数据 SQL，用 INSERT IGNORE 跳过已有日期
// 用法：
//   node seed-test-data.mjs              → 为数据库中所有已有用户插入数据
//   node seed-test-data.mjs user-xxxx    → 为指定 userId 插入数据
import { execSync } from 'child_process'

const MYSQL_BIN = process.env.MYSQL_PATH || 'C:\\Program Files\\MySQL\\MySQL Server 9.7\\bin\\mysql.exe'
const MYSQL_CMD = `"${MYSQL_BIN}" --default-character-set=utf8mb4 -u root -p123456 tiny_light`

// 确定目标用户列表
function resolveUserIds() {
  const arg = process.argv[2]
  if (arg) {
    console.log(`使用指定用户: ${arg}`)
    return [arg]
  }
  // 查询数据库中已有的所有用户
  try {
    const out = execSync(
      `${MYSQL_CMD} -N -e "SELECT DISTINCT user_id FROM tiny_light;" 2>nul`
    ).toString().trim()
    if (out) {
      const ids = out.split('\n').map(s => s.trim()).filter(Boolean)
      console.log(`检测到数据库中已有 ${ids.length} 个用户: ${ids.join(', ')}`)
      return ids
    }
  } catch { /* 表可能为空或不存在，忽略 */ }
  console.log('数据库中暂无用户数据。请先在浏览器中打开前端页面（会自动生成 userId），然后再运行本脚本。')
  console.log('或者手动指定: node seed-test-data.mjs <your-user-id>')
  process.exit(1)
}

const userIds = resolveUserIds()
const moods = ['开心', '平静', '感恩', '疲惫', '感动', '思念', '期待']

const contents = [
  '清晨第一缕阳光洒在窗台上，猫咪蜷在脚边打呼噜',
  '朋友发来一条温暖的消息，正好我也想起她',
  '读到一句触动内心的话，停下来抄在笔记本上',
  '午后的咖啡格外香浓，配一本翻了一半的书',
  '散步时遇到一只撒娇的橘猫，蹭了我的裤脚',
  '完成了一件拖延很久的事，浑身轻松',
  '听到一首久违的老歌，跟着哼了一路',
  '家人做了我爱吃的菜，满满一碗端上来',
  '雨停了，空气里全是泥土和青草的味道',
  '看到了很美的晚霞，整片天空都是橘红色',
  '和好友聊到深夜，原来我们都在各自努力',
  '今天的风很温柔，吹得人想在外面多待一会',
  '学会了一道新菜，虽然卖相一般但味道对了',
  '收到了意外的礼物，被惦记的感觉真好',
  '一个人安静看了很久的云，什么都没想',
  '工作上得到了认可，之前的辛苦都值了',
  '路边的花全开了，粉粉白白一片',
  '看到一对牵手散步的老人，希望老了也这样',
  '帮了陌生人一个小忙，对方一个笑容就够了',
  '泡了个舒服的热水澡，把一天的疲惫都融化了',
  '下班路上的耳机里刚好放到最爱的那首',
  '阳台上的植物发了新芽，原来它一直在长',
  '吃到了心心念念的那家小店，味道没变',
  '清晨被鸟叫醒，才发现窗户开了一整夜',
  '整理旧照片时看到三年前的自己，笑了笑',
]

// 生成 2026 年的点亮日期：1-7 月每 2-3 天点一颗，7/28-8/2 连续（streak）
function gen2026Dates() {
  const dates = []
  // 1月1日 - 7月27日：每 2-3 天一颗
  for (let month = 0; month < 7; month++) {
    const daysInMonth = new Date(2026, month + 1, 0).getDate()
    for (let day = 1; day <= daysInMonth; day += 2 + (month + day) % 2) {
      dates.push(new Date(2026, month, day))
    }
  }
  // 7月28日 - 8月2日：连续 6 天（保证 currentStreak = 6）
  const streakStart = [27, 28, 29, 30, 31] // 7月28-31
  streakStart.forEach(d => dates.push(new Date(2026, 6, d)))
  dates.push(new Date(2026, 7, 1)) // 8月1日
  dates.push(new Date(2026, 7, 2)) // 8月2日（今天）
  return dates
}

// 往年今日：8月2日
const pastDates = [
  new Date(2025, 7, 2),
  new Date(2024, 7, 2),
  new Date(2023, 7, 2),
]

function fmtDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
function fmtDateTime(d) {
  return `${fmtDate(d)} 20:00:00`
}

const allDates = [...gen2026Dates(), ...pastDates]

// 构建 INSERT IGNORE 语句
const values = []
userIds.forEach((uid, uidIdx) => {
  allDates.forEach((d, i) => {
    const content = contents[(i + uidIdx * 3) % contents.length]
    const mood = moods[(i + uidIdx) % moods.length]
    const dateStr = fmtDate(d)
    const createdStr = fmtDateTime(d)
    values.push(`('${uid}', '${content.replace(/'/g, "''")}', '${mood}', '${dateStr}', '${createdStr}')`)
  })
})

const sql = `SET NAMES utf8mb4;\nDELETE FROM tiny_light WHERE user_id IN (${userIds.map(u => `'${u}'`).join(',')});\nINSERT IGNORE INTO tiny_light (user_id, content, mood, light_date, created_at) VALUES\n${values.join(',\n')};`

// 执行
console.log(`生成 ${values.length} 条记录（${userIds.length} 个用户 × ${allDates.length} 天）`)
execSync(MYSQL_CMD, { input: sql, stdio: ['pipe', 'inherit', 'inherit'] })
console.log('插入完成')

// 验证
const result = execSync(`${MYSQL_CMD} -e "SELECT user_id, COUNT(*) as cnt, MIN(light_date) as earliest, MAX(light_date) as latest FROM tiny_light GROUP BY user_id;" 2>nul`).toString()
console.log('\n当前各用户数据：')
console.log(result)
