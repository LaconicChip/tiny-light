# 部署方案：家用电脑 + Cloudflare Tunnel + MySQL

## 目标

将 tiny-light 项目以零成本部署到公网，供 2-5 个朋友使用。不购买云服务器，不开放路由器端口，不依赖外部 PaaS 平台。

## 架构

```
朋友浏览器 (手机/电脑)
    ↓ HTTPS
Cloudflare CDN (免费)
    ↓ 隧道（电脑主动建立的出站连接）
家用电脑 localhost:8080
    ├── Spring Boot JAR
    │     ├── /api/*  → 后端接口
    │     └── /*      → 前端静态文件 (Vue build 产物)
    └── MySQL localhost:3306
```

核心：Cloudflare Tunnel 是反向隧道，电脑主动往外拨号，不需要公网 IP、不开放入站端口、不配置路由器。

## 为什么不用 Nginx

Spring Boot 内置静态资源服务，默认从 `classpath:/static/` 提供文件。把 Vue 构建产物放进 `src/main/resources/static/`，一个 JAR 包同时服务 API 和前端，8080 端口搞定一切。

## 组件设计

### 1. 前端嵌入后端 (构建脚本)

**问题**：当前前端是独立 dev server，生产环境需要嵌入 Spring Boot。

**方案**：写一个 `deploy/build.bat` 构建脚本：
1. `cd tiny-light-frontend && npm run build` → 产物在 `dist/`
2. 复制 `dist/*` 到 `src/main/resources/static/`
3. `mvnw clean package -DskipTests` → 打包含前端资源的 JAR

**不修改 vite.config.js 的 outDir**：保持前端独立性，用复制步骤衔接。`npm run dev` 仍正常工作。

**.gitignore**：排除 `src/main/resources/static/`，避免构建产物进 git。

### 2. Spring Boot 静态服务 (零配置)

Spring Boot WebMVC 默认从 `classpath:/static/` 提供静态文件：
- `GET /` → `static/index.html`（默认行为）
- `GET /assets/*.js` → `static/assets/*.js`
- `GET /api/*` → Controller 接口

无需额外配置。前端 axios `baseURL: '/api'` 是相对路径，同源访问直接生效。

### 3. Cloudflare Tunnel 配置

**前置条件**：一个挂在 Cloudflare DNS 上的域名。

**步骤**：
1. 下载安装 `cloudflared`（Windows 单 exe）
2. `cloudflared tunnel login` → 浏览器授权
3. `cloudflared tunnel create tiny-light` → 创建隧道，生成 UUID
4. 编写 `~/.cloudflared/config.yml`：
   ```yaml
   tunnel: <UUID>
   credentials-file: C:\Users\<user>\.cloudflared\<UUID>.json
   ingress:
     - hostname: tiny-light.yourdomain.com
       service: http://localhost:8080
     - service: http_status:404
   ```
5. `cloudflared tunnel route dns tiny-light tiny-light.yourdomain.com` → 绑定域名
6. `cloudflared tunnel run tiny-light` → 启动隧道

### 4. 开机自启

两个 Windows 服务，开机自动运行：

**a) cloudflared 服务**：
`cloudflared service install` → 注册为 Windows 服务，开机自启

**b) JAR 启动脚本** (`deploy/start.bat`)：
```bat
@echo off
cd /d <项目根目录>
java -jar target/tiny-light-0.0.1-SNAPSHOT.jar
```
通过 Windows 任务计划程序设置"开机时运行"。

**c) MySQL**：已是 Windows 服务，开机自启（现有状态不变）。

### 5. 域名方案

| 方式 | 费用 | 备注 |
|---|---|---|
| 自有域名挂 Cloudflare | ¥10-70/年 | 最佳，固定地址 |
| trycloudflare 随机域名 | 免费 | 每次重启 URL 变化，不适合分享 |
| 免费二级域名 | 免费 | 需申请审核 |

推荐自有域名（.top 约 ¥10/年），接近零成本。

## 需要创建的文件

| 文件 | 用途 |
|---|---|
| `deploy/build.bat` | 一键构建：前端→复制→打包JAR |
| `deploy/start.bat` | 启动 Spring Boot JAR |
| `.gitignore` 追加 | 排除 `src/main/resources/static/` |

## 不需要改动的部分

- `application.yml`：数据库配置不变（localhost:3306）
- `vite.config.js`：dev proxy 保留，生产构建不受影响
- `src/api/lights.js`：`baseURL: '/api'` 相对路径，同源访问直接生效
- 后端 Java 代码：零改动
- MySQL：现有服务和数据不变

## 风险与限制

| 风险 | 影响 | 缓解 |
|---|---|---|
| 电脑关机/休眠 | 服务中断 | 设置电源选项不休眠；接受偶尔中断 |
| 家用宽带断网 | 服务中断 | 无额外措施，网络恢复即自愈 |
| Cloudflare 政策变更 | 隧道不可用 | 低概率；可迁移到 frp/ngrok |
| 域名费用 | 约 ¥10/年 | 接近零成本，可接受 |

## 实施步骤概览

1. 创建 `deploy/build.bat` 构建脚本
2. 创建 `deploy/start.bat` 启动脚本
3. 更新 `.gitignore`
4. 测试构建：前端嵌入 → JAR 打包 → 本地验证
5. 配置 Cloudflare Tunnel（用户手动操作，文档指导）
6. 配置开机自启
7. 验证公网访问
