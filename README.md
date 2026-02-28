# PotLog

**[English](#english)** | **[中文](#中文)**

---

<a id="english"></a>

# PotLog - Texas Hold'em Settlement Tool

A lightweight poker settlement tool for tracking buy-ins, generating optimal transfer schemes, and settling home games.

## Project Structure

```
potlog/
├── backend/              # Kotlin/Ktor Backend
│   ├── src/
│   │   ├── main/kotlin/com/potlog/
│   │   │   ├── models/       # Data Models
│   │   │   ├── services/     # Business Logic
│   │   │   ├── routes/       # API Routes
│   │   │   ├── database/     # MongoDB Config
│   │   │   └── Application.kt
│   │   └── test/             # Unit Tests
│   └── build.gradle.kts
├── frontend/             # React Frontend
│   ├── src/
│   │   ├── pages/        # Page Components
│   │   ├── components/   # UI Components
│   │   ├── api/          # API Client
│   │   ├── store/        # Zustand State Management
│   │   └── utils/        # Utility Functions
│   └── package.json
├── docker/               # Docker Init Scripts
└── docker-compose.yml    # Docker Compose Config
```

## Tech Stack

- **Backend**: Kotlin + Ktor + MongoDB
- **Frontend**: React + TypeScript + Vite + Tailwind CSS + Zustand
- **Data**: All amounts in Long (unit: cents)

## Quick Start

### Option 1: Docker Compose (Recommended)

```bash
# Start all services (MongoDB + Backend + Frontend)
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

Visit http://localhost:3000 to use the app.

### Option 2: Local Development

#### 1. Start MongoDB

```bash
# Start MongoDB with Docker
docker run -d --name potlog-mongo -p 27017:27017 mongo:7.0
```

#### 2. Start Backend

```bash
cd backend

# Run backend (Windows)
.\gradlew.bat run

# Run backend (Linux/Mac)
./gradlew run
```

Backend will start at http://localhost:8080.

#### 3. Start Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start dev server
npm run dev
```

Frontend will start at http://localhost:3000.

## Core Features

### 6-Digit Numeric ID System
- Each session gets a unique 6-digit numeric ID (e.g., `102938`)
- Access path: `domain.com/{numericId}`
- Automatic MongoDB unique index conflict handling under high concurrency

### Settlement Logic
1. **Balance Check**: Calculate `Diff = TotalBuyIn - TotalCashOut`
2. **Auto Balance** (if Diff ≠ 0):
   - **Max Winner**: Assign Diff to the player with highest net
   - **Proportional**: Distribute Diff by profit ratio (using Largest Remainder Method)
3. **Debt Minimization**: Greedy algorithm generates minimal transfer list

### Direct Transfers
- Record peer-to-peer transfers during active sessions
- Transfers are automatically deducted from settlement calculations
- Marking debts as settled automatically creates transfer records

### Re-settlement
- Reopen settled sessions for re-calculation
- All recorded transfers are preserved and factored into new settlement

## API Endpoints

### Session Management

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/sessions` | Create new session |
| GET | `/api/sessions/{numericId}` | Get session |
| POST | `/api/sessions/{numericId}/players` | Add player |
| POST | `/api/sessions/{numericId}/rebuy` | Player rebuy |
| POST | `/api/sessions/{numericId}/settle` | Execute settlement |
| POST | `/api/sessions/{numericId}/reopen` | Reopen settled session |
| POST | `/api/sessions/{numericId}/debts/settle` | Mark debt as settled |
| POST | `/api/sessions/{numericId}/transfers` | Add direct transfer |
| DELETE | `/api/sessions/{numericId}/transfers/{id}` | Remove transfer |
| POST | `/api/sessions/{numericId}/preview` | Preview settlement |
| POST | `/api/sessions/{numericId}/diff` | Calculate balance diff |

### User Statistics

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users/{userId}/stats` | Get user history stats |

## Frontend Pages

### Home (`/`)
- Create new session (enter blind level)
- Join existing session (enter 6-digit ID)
- Recent sessions list (LocalStorage, last 10)

### Session Page (`/{numericId}`)
- Display session info and total pot
- Player list with add player and rebuy
- Direct transfers management
- Settlement results with transfer scheme

### Settlement Page (`/{numericId}/settle`)
- Enter each player's cash out amount
- Real-time balance difference display
- Choose balance mode (Max Winner / Proportional)

## Running Tests

```bash
cd backend
./gradlew test
```

## Data Models

### PokerSession
```kotlin
data class PokerSession(
    val numericId: String,      // 6-digit primary key
    var status: SessionStatus,  // ACTIVE, SETTLED
    val stakes: String,         // e.g., "1/2"
    val players: List<Player>,
    val logs: List<TransactionLog>,
    val transfers: List<DirectTransfer>,
    val debts: List<Debt>
)
```

### Player
```kotlin
data class Player(
    val id: String,
    val name: String,
    var buyIn: Long = 0,      // Total buy-in (cents)
    var cashOut: Long = 0,    // Final chip count (cents)
    var net: Long = 0,        // Profit/loss (cents)
    val userId: String? = null
)
```

## Example Requests

### Create Session
```bash
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -d '{"stakes": "1/2"}'
```

### Add Player
```bash
curl -X POST http://localhost:8080/api/sessions/123456/players \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice", "initialBuyIn": 20000}'
```

### Execute Settlement
```bash
curl -X POST http://localhost:8080/api/sessions/123456/settle \
  -H "Content-Type: application/json" \
  -d '{
    "cashOuts": {
      "player1Id": 30000,
      "player2Id": 10000
    },
    "balanceMode": "PROPORTIONAL"
  }'
```

## Environment Variables

### Backend
| Variable | Default | Description |
|----------|---------|-------------|
| `MONGODB_URI` | `mongodb://localhost:27017` | MongoDB connection string |
| `MONGODB_DATABASE` | `potlog` | Database name |
| `PORT` | `8080` | Server port |

### Frontend
| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_URL` | `/api` | API base path |

## Roadmap

- [x] Backend data models
- [x] 6-digit ID generation (with conflict handling)
- [x] MongoDB connection and indexes
- [x] Core settlement logic (Largest Remainder Method)
- [x] REST API
- [x] Unit tests
- [x] Docker configuration
- [x] Frontend home page
- [x] Frontend session page
- [x] Frontend settlement page
- [x] Direct transfers
- [x] Re-settlement feature
- [ ] User authentication
- [ ] Production deployment

---

<a id="中文"></a>

# PotLog - 德州扑克线下私局记账工具

轻量级德州扑克结算工具，支持多人买入记录、自动结算和最简转账方案生成。

## 项目结构

```
potlog/
├── backend/              # Kotlin/Ktor 后端
│   ├── src/
│   │   ├── main/kotlin/com/potlog/
│   │   │   ├── models/       # 数据模型
│   │   │   ├── services/     # 业务逻辑
│   │   │   ├── routes/       # API 路由
│   │   │   ├── database/     # MongoDB 配置
│   │   │   └── Application.kt
│   │   └── test/             # 单元测试
│   └── build.gradle.kts
├── frontend/             # React 前端
│   ├── src/
│   │   ├── pages/        # 页面组件
│   │   ├── components/   # UI 组件
│   │   ├── api/          # API 客户端
│   │   ├── store/        # Zustand 状态管理
│   │   └── utils/        # 工具函数
│   └── package.json
├── docker/               # Docker 初始化脚本
└── docker-compose.yml    # Docker Compose 配置
```

## 技术栈

- **后端**: Kotlin + Ktor + MongoDB
- **前端**: React + TypeScript + Vite + Tailwind CSS + Zustand
- **数据**: 金额使用 Long (单位: 分/Cents)

## 快速开始

### 方式一：使用 Docker Compose（推荐）

```bash
# 启动所有服务（MongoDB + 后端 + 前端）
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

访问 http://localhost:3000 使用应用。

### 方式二：本地开发

#### 1. 启动 MongoDB

```bash
# 使用 Docker 启动 MongoDB
docker run -d --name potlog-mongo -p 27017:27017 mongo:7.0
```

#### 2. 启动后端

```bash
cd backend

# 运行后端（Windows）
.\gradlew.bat run

# 运行后端（Linux/Mac）
./gradlew run
```

后端将在 http://localhost:8080 启动。

#### 3. 启动前端

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端将在 http://localhost:3000 启动。

## 核心功能

### 6 位数字 ID 系统
- 每局生成唯一的 6 位纯数字 ID (如 `102938`)
- 访问路径: `domain.com/{numericId}`
- 高并发下自动处理 MongoDB 唯一索引冲突

### 结算逻辑
1. **账目校验**: 计算 `Diff = TotalBuyIn - TotalCashOut`
2. **自动平账** (若 Diff ≠ 0):
   - **最大赢家承担**: 将 Diff 补偿给 net 最高的玩家
   - **比例分摊**: 按盈利比例分摊 Diff (使用最大余数法)
3. **债务最小化**: 贪心算法生成最少次数转账列表

### 定向转账
- 在进行中的账本里记录玩家之间的直接转账
- 转账金额会在结算时自动从债务中扣除
- 标记债务已结清时会自动创建转账记录

### 重新结算
- 已结算的账本可以重新打开进行重新结算
- 所有已记录的转账会被保留并计入新的结算

## API 接口

### Session 管理

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/sessions` | 创建新 Session |
| GET | `/api/sessions/{numericId}` | 获取 Session |
| POST | `/api/sessions/{numericId}/players` | 添加玩家 |
| POST | `/api/sessions/{numericId}/rebuy` | 玩家加买 |
| POST | `/api/sessions/{numericId}/settle` | 执行结算 |
| POST | `/api/sessions/{numericId}/reopen` | 重新打开已结算账本 |
| POST | `/api/sessions/{numericId}/debts/settle` | 标记债务已结清 |
| POST | `/api/sessions/{numericId}/transfers` | 添加定向转账 |
| DELETE | `/api/sessions/{numericId}/transfers/{id}` | 删除转账 |
| POST | `/api/sessions/{numericId}/preview` | 预览结算结果 |
| POST | `/api/sessions/{numericId}/diff` | 计算账目差额 |

### 用户统计

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/users/{userId}/stats` | 获取用户历史统计 |

## 前端页面

### 首页 (`/`)
- 创建新局（输入盲注级别）
- 加入现有局（输入 6 位数字 ID）
- 历史账本列表（LocalStorage 存储最近 10 条）

### 账本页 (`/{numericId}`)
- 显示账本信息和总买入
- 玩家列表，支持添加玩家和加买
- 定向转账管理
- 结算结果和转账方案显示

### 结算页 (`/{numericId}/settle`)
- 输入每个玩家的带出金额
- 实时显示账目差额
- 选择平账方式（最大赢家承担/按比例分摊）

## 运行测试

```bash
cd backend
./gradlew test
```

## 数据模型

### PokerSession
```kotlin
data class PokerSession(
    val numericId: String,      // 6位数字主键
    var status: SessionStatus,  // ACTIVE, SETTLED
    val stakes: String,         // 例如 "1/2"
    val players: List<Player>,
    val logs: List<TransactionLog>,
    val transfers: List<DirectTransfer>,
    val debts: List<Debt>
)
```

### Player
```kotlin
data class Player(
    val id: String,
    val name: String,
    var buyIn: Long = 0,      // 总买入 (分)
    var cashOut: Long = 0,    // 最终筹码量 (分)
    var net: Long = 0,        // 盈亏 (分)
    val userId: String? = null
)
```

## 示例请求

### 创建 Session
```bash
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -d '{"stakes": "1/2"}'
```

### 添加玩家
```bash
curl -X POST http://localhost:8080/api/sessions/123456/players \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice", "initialBuyIn": 20000}'
```

### 执行结算
```bash
curl -X POST http://localhost:8080/api/sessions/123456/settle \
  -H "Content-Type: application/json" \
  -d '{
    "cashOuts": {
      "player1Id": 30000,
      "player2Id": 10000
    },
    "balanceMode": "PROPORTIONAL"
  }'
```

## 环境变量

### 后端
| 变量 | 默认值 | 描述 |
|------|--------|------|
| `MONGODB_URI` | `mongodb://localhost:27017` | MongoDB 连接字符串 |
| `MONGODB_DATABASE` | `potlog` | 数据库名称 |
| `PORT` | `8080` | 服务端口 |

### 前端
| 变量 | 默认值 | 描述 |
|------|--------|------|
| `VITE_API_URL` | `/api` | API 基础路径 |

## 开发计划

- [x] 后端数据模型
- [x] 6 位数字 ID 生成 (含冲突处理)
- [x] MongoDB 连接和索引
- [x] 核心结算逻辑 (最大余数法)
- [x] REST API
- [x] 单元测试
- [x] Docker 配置
- [x] 前端首页
- [x] 前端录入页
- [x] 前端结算页
- [x] 定向转账功能
- [x] 重新结算功能
- [ ] 用户认证
- [ ] 生产环境部署
