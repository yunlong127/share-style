# 生活分享网站系统

## 设计原则说明

本项目实现了以下设计原则：

### 1. 单一职责原则 (SRP)
- 服务层按职责拆分为查询和命令接口
- 每个类只负责一个明确的功能

### 2. 开闭原则 (OCP)
- 使用策略模式处理不同内容类型（文章/图片/视频）
- 新增内容类型只需添加新策略类，无需修改现有代码

### 3. 依赖倒置原则 (DIP)
- Controller依赖Service接口而非实现
- 通过构造函数注入依赖

### 4. 接口隔离原则 (ISP)
- Service接口按功能细分为查询和命令接口
- 客户端只依赖需要的接口

## 项目结构

```
backend/     - Spring Boot 后端
frontend/    - Vue3 前端
sql/         - 数据库初始化脚本
```

## 运行方式

### 方式一：使用IDE（推荐）

1. **后端**
   - 使用 IntelliJ IDEA 或 Eclipse 打开 `backend` 文件夹
   - 等待Maven下载依赖
   - 运行 `LifeShareApplication.java`
   - 后端服务将启动在 http://localhost:8080

2. **前端**
   - 打开终端，进入 `frontend` 目录
   - 运行 `npm install`
   - 运行 `npm run dev`
   - 前端服务将启动在 http://localhost:3000

### 方式二：命令行

需要安装：
- Java 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+（或使用内置H2数据库）

**后端启动：**
```bash
cd backend
mvn spring-boot:run
```

**前端启动：**
```bash
cd frontend
npm install
npm run dev
```

## 默认账号

- 管理员：admin / admin123
- 用户1：zhangsan / 123456
- 用户2：lisi / 123456

## 功能特性

- 用户注册/登录
- 发布文章/图片/视频
- 浏览和筛选内容
- 点赞和评论
- 个人资料管理
