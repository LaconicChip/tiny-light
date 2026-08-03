# 今日微光 · Tiny Light

> 每天只记一句"今天最亮的一刻"，把 365 天串成一条可回看的星光河。

一个疗愈系每日微光记录应用。每天记录一件让自己开心/感动/温暖的小事（一句话 + 心情），这些记录在页面上呈现为一条蜿蜒流动的星河--点亮的日子是金色发光的星，未点亮的是暗淡的小星。低谷时回头望去，被自己过去的光治愈。

![style](https://img.shields.io/badge/style-%E7%99%BD%E9%87%91%E6%98%9F%E5%85%89%E6%B2%B3-c99a35)

---

## ✨ 功能

- **记录今日微光**：一句话 + 7 种心情（开心/平静/感恩/疲惫/感动/思念/期待），一人一天一颗
- **年度星光河**：SVG 贝塞尔曲线沿路径散布 365 颗星，金色脉冲 = 已点亮，今日星带双层光环
- **那年今日**：近 5 年同月同日的微光，便签卡散落布局（无数据也保留占位，布局稳定）
- **统计徽章**：连续点亮 / 最长连续 / 今年点亮 / 累计天数 + 心情分布
- **编辑 / 删除**：仅今天的微光可改可删
- **微光详情**：点击亮星触发涟漪 + 打开玻璃态弹窗
- **20+ 动效**：闪烁星、金色微尘、流星、极光、光标光晕、视差、3D 卡片倾斜、按钮磁吸……（今日星金色光环、按压反馈、过渡显式属性）
- **无障碍兜底**：`prefers-reduced-motion` 禁用动画，触屏禁用桌面专属交互

---

## 🛠 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3.5 + Vite 8 + axios，Geist 字体，内联 SVG 图标 |
| 后端 | Spring Boot 4.1.0 + Java 17 + Bean Validation |
| 持久层 | 原生 MyBatis（mybatis-spring-boot-starter 4.1.0）+ MySQL |
| 构建 | Maven（后端）/ npm（前端） |

> ⚠️ 不用 MyBatis-Plus（3.5.17 与 Spring Boot 4.1 不兼容）；不降级 Spring Boot；不引入 GSAP 等动画库。

---

## 📁 项目结构

```
tiny-light/
├── src/main/java/com/tinylight/
│   ├── controller/        # TinyLightController（10 个接口）+ HealthController
│   ├── service/           # TinyLightService（业务逻辑 + 连续天数计算）
│   ├── mapper/            # TinyLightMapper（原生 MyBatis 注解 SQL）
│   ├── dto/               # LightRequest/Response, StatsResponse, PageResponse
│   ├── entity/            # TinyLight 实体
│   └── common/            # GlobalExceptionHandler（统一异常 -> HTTP 状态码）
├── src/main/resources/
│   ├── application.yml    # MySQL 配置（password 123456，私有库可入 git）
│   └── schema.sql         # 建表语句（启动自动执行）
├── tiny-light-frontend/   # Vue 3 前端
│   └── src/
│       ├── App.vue        # Hero + 6 套背景动效 + 视差 + 滚动揭示
│       ├── components/    # LightInput / StarRiver / Stats / OnThisDay / LightDetail
│       ├── api/lights.js  # axios 封装，localStorage userId
│       └── style.css      # 白金/夜空/金色 CSS 变量体系
├── seed-test-data.mjs     # 测试数据种子脚本
├── 需求分析.md             # 完整需求分析文档
└── pom.xml
```

---

## 🚀 快速开始

### 前置要求

- **Java 17+**
- **Maven**（或用项目自带的 `./mvnw`）
- **MySQL 8**（本地运行，root/123456，或自行改 `application.yml`）
- **Node.js 18+**

### 1. 启动后端

```bash
./mvnw spring-boot:run
```

后端跑在 `http://localhost:8080`，启动时自动建 `tiny_light` 表。验证：

```bash
curl http://localhost:8080/api/health
# {"status":"UP","timestamp":"..."}
```

### 2. 启动前端

```bash
cd tiny-light-frontend
npm install
npm run dev
```

前端跑在 `http://localhost:5173`，Vite 自动代理 `/api` -> `8080`。打开浏览器访问即可。

### 3.（可选）灌入测试数据

```bash
node seed-test-data.mjs
```

为所有已有测试用户灌入数据：今年 1 月到昨天的点亮记录 + 过去 5 年全年记录（往年今日用）。日期由脚本按运行当天动态生成，**保留用户当天真实点亮的微光**（DELETE 排除今天）。往年记录铺满 5 年，任意日期访问"那年今日"都有数据，不用每天跑脚本。脚本会先清今天之前的数据再插入，谨慎在生产环境使用。

---

## 📡 API 概览

基址 `http://localhost:8080/api/lights`（`userId` 由前端 localStorage 生成）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/` | 记录今日微光（重复 409，mood 非白名单 400） |
| GET | `/` | 分页历史列表（`page`, `size`） |
| GET | `/today` | 今日状态 |
| GET | `/river?year=` | 年度星光河 |
| GET | `/on-this-day` | 那年今日（近 5 年） |
| GET | `/stats` | 统计 + 心情分布 |
| GET | `/{id}` | 详情（需 userId 校验归属，越权 403） |
| PUT | `/{id}` | 编辑今天（非今天 403） |
| DELETE | `/{id}` | 删除今天（非今天 403） |
| GET | `/api/health` | 健康检查 |

完整接口字段与响应结构见 [需求分析.md](需求分析.md#6-接口需求api规格)。

---

## 🔒 安全

- **越权防护**：详情/编辑/删除均需 `userId` 参数，后端校验归属，非本人返回 403
- **XSS 防护**：`mood` 后端 `@Pattern` 白名单（存储层）+ 前端 `textContent` 渲染（渲染层）双重保证
- **重复防护**：数据库 `UNIQUE(user_id, light_date)` + 业务层捕获冲突 -> 409

---

## 📝 相关文档

- [需求分析.md](需求分析.md) - 完整需求、数据设计、接口规格、验收标准
- [PERF_PLAN.md](PERF_PLAN.md) - 流畅度优化经验、踩坑记录与设计思路
- [CLAUDE.md](CLAUDE.md) - 项目行为准则与编码规范

---

## 📄 许可

个人学习项目，暂未开源授权。
