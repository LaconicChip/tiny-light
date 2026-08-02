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
