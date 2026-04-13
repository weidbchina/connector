# Database Monitor Tool (V1.0)

## 概述
本工具是一套轻量级、可配置的 Oracle 19c 数据库监控与交互平台。包含数据监测告警与 Web 端 SQL 云客户端两大核心功能。

## 技术栈
- **后端**: Java 17, Spring Boot 3, Quartz, H2 Database
- **前端**: React, TypeScript, Vite, Ant Design, Monaco Editor

## 目录结构
- `backend/`: Spring Boot 后端源码
- `frontend/`: React 前端源码

## 快速开始

### 前置要求
- Java 17+
- Maven 3.8+
- Node.js 18+

### 1. 启动后端
后端依赖 Maven 进行构建和运行。

```bash
cd backend
mvn spring-boot:run
```
服务默认运行在 `http://localhost:8080`。
H2 控制台: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/dbmonitor`)

### 2. 启动前端
```bash
cd frontend
npm install
npm run dev
```
前端服务默认运行在 `http://localhost:5173`。

## 功能特性
1. **多数据源管理**: 支持配置多个 Oracle 19c 连接，密码加密存储。
2. **SQL 云客户端**: 
   - 支持语法高亮、智能补全（表名、字段名）。
   - 仅允许 SELECT 查询，自动拦截 DML/DDL。
   - 结果集导出 CSV。
3. **监测告警**:
   - 自定义 SQL 监测任务。
   - 支持数值阈值、正则匹配等校验规则。
   - 模拟短信告警（日志输出）。
