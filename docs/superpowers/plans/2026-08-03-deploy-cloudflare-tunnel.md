# Cloudflare Tunnel 部署实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 tiny-light 项目以零成本部署到公网，家用电脑 + Cloudflare Tunnel (trycloudflare 免费临时域名) + MySQL，供 2-5 个朋友使用。

**Architecture:** 前端构建产物嵌入 Spring Boot 的 static 目录，一个 JAR 同时服务 API 和静态文件。一个 `start.bat` 启动 JAR + Cloudflare Tunnel，叉掉窗口即停止全部。一个 `update.bat` 拉代码 + 重新构建。

**Tech Stack:** Spring Boot 4.1 (Java 17), Vue 3 + Vite, MySQL 8, cloudflared (trycloudflare 模式), Windows

---

## 文件结构

| 文件 | 操作 | 职责 |
|---|---|---|
| `deploy/build.bat` | 创建 | 一键构建：前端 build → 复制到 static → Maven 打包 JAR |
| `deploy/start.bat` | 创建 | 启动 JAR + cloudflared 隧道，叉掉窗口停止全部 |
| `deploy/update.bat` | 创建 | git pull + 前端依赖 + 重新构建 JAR |
| `deploy/README.md` | 创建 | 部署操作指南 |
| `.gitignore` | 修改 | 追加排除 `src/main/resources/static/` |
| `src/main/resources/application.yml` | 修改 | 关闭 SQL 日志 |

---

### Task 1: 更新 .gitignore

**Files:**
- Modify: `.gitignore`

- [ ] **Step 1: 追加排除规则**

在 `.gitignore` 末尾追加：

```gitignore

### 构建产物：前端嵌入后端的静态文件 ###
src/main/resources/static/
```

- [ ] **Step 2: Commit**

```bash
git add .gitignore
git commit -m "chore: gitignore 排除前端构建产物嵌入目录"
```

---

### Task 2: 创建 deploy/build.bat 构建脚本

**Files:**
- Create: `deploy/build.bat`

- [ ] **Step 1: 创建构建脚本**

创建 `deploy/build.bat`：

```bat
@echo off
chcp 65001 >nul
setlocal

echo ========================================
echo  tiny-light 一键构建
echo ========================================
echo.

set PROJECT_ROOT=%~dp0..
set FRONTEND_DIR=%PROJECT_ROOT%\tiny-light-frontend
set STATIC_DIR=%PROJECT_ROOT%\src\main\resources\static

echo [1/4] 清理旧的静态文件...
if exist "%STATIC_DIR%" rmdir /s /q "%STATIC_DIR%"
mkdir "%STATIC_DIR%"
echo     done
echo.

echo [2/4] 构建前端...
cd /d "%FRONTEND_DIR%"
call npm run build
if errorlevel 1 (
    echo [ERROR] 前端构建失败
    pause
    exit /b 1
)
echo.

echo [3/4] 复制前端产物到 Spring Boot static 目录...
xcopy /E /I /Y "%FRONTEND_DIR%\dist\*" "%STATIC_DIR%\"
echo     done
echo.

echo [4/4] 打包 Spring Boot JAR...
cd /d "%PROJECT_ROOT%"
call mvnw clean package -DskipTests
if errorlevel 1 (
    echo [ERROR] Maven 打包失败
    pause
    exit /b 1
)
echo.
echo ========================================
echo  构建完成！
echo  JAR: target\tiny-light-0.0.1-SNAPSHOT.jar
echo ========================================

endlocal
```

- [ ] **Step 2: Commit**

```bash
git add deploy/build.bat
git commit -m "feat: 添加一键构建脚本 deploy/build.bat"
```

---

### Task 3: 创建 deploy/start.bat 启动脚本

**Files:**
- Create: `deploy/start.bat`

**设计要点：**
- 先检查 MySQL 是否在跑
- 先检查 JAR 是否存在（不存在提示先 build）
- 后台启动 JAR，前台启动 cloudflared
- cloudflared 前台运行 = 叉掉窗口就停隧道
- 用 trap 机制在脚本退出时 kill 掉 JAR 进程

- [ ] **Step 1: 创建启动脚本**

创建 `deploy/start.bat`：

```bat
@echo off
chcp 65001 >nul
setlocal

set PROJECT_ROOT=%~dp0..
set JAR_FILE=%PROJECT_ROOT%\target\tiny-light-0.0.1-SNAPSHOT.jar
set CF_EXE=%PROJECT_ROOT%\deploy\cloudflared.exe

echo ========================================
echo  tiny-light 启动
echo ========================================
echo.

REM --- 检查 JAR ---
if not exist "%JAR_FILE%" (
    echo [ERROR] JAR 不存在，请先运行 deploy\build.bat
    pause
    exit /b 1
)

REM --- 检查 cloudflared ---
if not exist "%CF_EXE%" (
    echo [ERROR] cloudflared.exe 不存在
    echo 请下载: https://github.com/cloudflare/cloudflared/releases/latest
    echo 下载 cloudflared-windows-amd64.exe，重命名为 cloudflared.exe
    echo 放到 deploy\ 目录下
    pause
    exit /b 1
)

REM --- 检查 MySQL ---
echo [1/3] 检查 MySQL...
sc query MySQL >nul 2>&1
if errorlevel 1 (
    sc query MySQL80 >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] MySQL 服务未找到，请确认 MySQL 已安装
        pause
        exit /b 1
    )
)
echo     MySQL OK
echo.

REM --- 启动 JAR（后台）---
echo [2/3] 启动 Spring Boot...
cd /d "%PROJECT_ROOT%"
start "tiny-light-server" /min java -jar "%JAR_FILE%"
echo     JAR 已后台启动 (端口 8080)
echo.

REM --- 等待 Spring Boot 启动 ---
echo     等待服务就绪...
:WAIT_LOOP
timeout /t 2 /nobreak >nul
curl -s http://localhost:8080/api/health >nul 2>&1
if errorlevel 1 (
    goto WAIT_LOOP
)
echo     服务已就绪
echo.

REM --- 启动 cloudflared 隧道（前台）---
echo [3/3] 启动 Cloudflare Tunnel...
echo.
echo ========================================
echo  服务已启动！
echo.
echo  本地访问: http://localhost:8080
echo  公网地址: 见下方 cloudflared 输出的 URL
echo  (类似 https://xxx-xxx-xxx.trycloudflare.com)
echo.
echo  把公网地址发给朋友即可访问
echo  关闭本窗口 = 停止全部服务
echo ========================================
echo.

"%CF_EXE%" tunnel --url http://localhost:8080

REM --- cloudflared 退出后（用户叉掉窗口），清理 JAR 进程 ---
echo.
echo 正在停止 Spring Boot...
taskkill /FI "WINDOWTITLE eq tiny-light-server*" /F >nul 2>&1
echo 已停止

endlocal
```

- [ ] **Step 2: Commit**

```bash
git add deploy/start.bat
git commit -m "feat: 添加启动脚本 deploy/start.bat (JAR+隧道一键启动)"
```

---

### Task 4: 创建 deploy/update.bat 更新脚本

**Files:**
- Create: `deploy/update.bat`

- [ ] **Step 1: 创建更新脚本**

创建 `deploy/update.bat`：

```bat
@echo off
chcp 65001 >nul
setlocal

set PROJECT_ROOT=%~dp0..

echo ========================================
echo  tiny-light 更新
echo ========================================
echo.

echo [1/4] 拉取最新代码...
cd /d "%PROJECT_ROOT%"
git pull origin main
if errorlevel 1 (
    echo [ERROR] git pull 失败，请检查是否有未提交的改动
    pause
    exit /b 1
)
echo.

echo [2/4] 更新前端依赖...
cd /d "%PROJECT_ROOT%\tiny-light-frontend"
call npm install
if errorlevel 1 (
    echo [ERROR] npm install 失败
    pause
    exit /b 1
)
echo.

echo [3/4] 重新构建...
call "%PROJECT_ROOT%\deploy\build.bat"
if errorlevel 1 (
    echo [ERROR] 构建失败
    pause
    exit /b 1
)
echo.

echo [4/4] 更新完成！
echo.
echo ========================================
echo  更新完成，请重新运行 deploy\start.bat
echo ========================================

endlocal
```

- [ ] **Step 2: Commit**

```bash
git add deploy/update.bat
git commit -m "feat: 添加更新脚本 deploy/update.bat"
```

---

### Task 5: 关闭生产环境 SQL 日志

**Files:**
- Modify: `src/main/resources/application.yml:13`

- [ ] **Step 1: 注释掉 SQL 日志配置**

将第 13 行：
```yaml
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 控制台打印 SQL
```
改为：
```yaml
    # log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 生产环境关闭 SQL 日志
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/application.yml
git commit -m "chore: 关闭生产环境 SQL 控制台日志"
```

---

### Task 6: 创建 deploy/README.md 部署指南

**Files:**
- Create: `deploy/README.md`

- [ ] **Step 1: 创建部署指南**

创建 `deploy/README.md`：

````markdown
# tiny-light 部署指南

## 前置条件

1. **Java 17** 已安装（`java -version`）
2. **MySQL 8** 已安装并运行，root密码 123456
3. **Node.js 18+** 已安装（用于构建前端）
4. **Git** 已安装

## 首次部署

### 1. 下载 cloudflared

下载地址：https://github.com/cloudflare/cloudflared/releases/latest

下载 `cloudflared-windows-amd64.exe`，重命名为 `cloudflared.exe`，放到 `deploy\` 目录下。

### 2. 构建项目

双击运行 `deploy\build.bat`

等待构建完成，产物为 `target\tiny-light-0.0.1-SNAPSHOT.jar`

### 3. 启动服务

双击运行 `deploy\start.bat`

窗口会显示：
- 本地地址：`http://localhost:8080`
- 公网地址：`https://xxx-xxx-xxx.trycloudflare.com`（每次启动不同）

把公网地址发给朋友即可。

**关闭服务：** 直接叉掉 start.bat 窗口，JAR 和隧道一起停止。

## 日常更新

当前端或后端代码有更新时：

双击运行 `deploy\update.bat`

脚本会自动：拉取代码 → 更新依赖 → 重新构建 JAR

更新完成后，重新运行 `deploy\start.bat` 即可。

> **注意：** 更新前请先关闭正在运行的 start.bat 窗口。

## trycloudflare 说明

- 免费临时域名，无需注册账号，无需域名
- 每次启动 URL 不同，需重新发给朋友
- 如需固定域名，购买一个域名（.top 约¥10/年）挂到 Cloudflare，改用命名隧道

## 故障排查

| 问题 | 解决方案 |
|---|---|
| 启动报 JAR 不存在 | 先运行 `deploy\build.bat` |
| 启动报 cloudflared 不存在 | 下载 cloudflared.exe 放到 deploy\ 目录 |
| API 报 500 | MySQL 未启动，检查 Windows 服务 |
| 页面白屏 | 重新运行 `deploy\build.bat` |
| 公网地址打不开 | 检查 start.bat 窗口是否还在运行 |
````

- [ ] **Step 2: Commit**

```bash
git add deploy/README.md
git commit -m "docs: 添加部署操作指南"
```

---

### Task 7: 测试完整构建流程

**Files:**
- 无文件改动

- [ ] **Step 1: 运行构建脚本**

Run: `deploy\build.bat`
Expected: 4 步全部完成，显示"构建完成"

- [ ] **Step 2: 验证 JAR 包含前端**

Run: `jar tf target\tiny-light-0.0.1-SNAPSHOT.jar | findstr "static/index.html"`
Expected: `BOOT-INF/classes/static/index.html`

- [ ] **Step 3: 启动并验证（无 cloudflared 也能测本地）**

Run: `deploy\start.bat`（如果没有 cloudflared.exe 会报错，但 JAR 会先启动）

另开终端验证：
Run: `curl http://localhost:8080/api/health`
Expected: `{"status":"UP",...}`

浏览器访问 `http://localhost:8080`，确认页面正常。

- [ ] **Step 4: 停止服务**

叉掉 start.bat 窗口

---

### Task 8: 推送

**Files:**
- 无文件改动

- [ ] **Step 1: Push**

```bash
git push origin main
```

- [ ] **Step 2: 确认**

Run: `git log --oneline -6`
Expected: 看到 deploy 相关的 commit
