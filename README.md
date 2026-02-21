# Rabbit Notebook (CR Notebook)

一款受 [Craft](https://craft.do/) 启发的全栈笔记应用，支持多租户数据隔离，基于 Vue 3 + Spring Boot + MySQL 构建。

> **🤖 AI 生成声明**
>
> 本项目完全由 **[Cursor IDE](https://cursor.com/)** + **Claude (Anthropic)** 大语言模型通过 AI 辅助编程生成。
> 从架构设计、代码实现、单元测试到项目文档，均通过人机协作的 Prompt 驱动方式完成。
> 详细的开发计划与迭代过程见 [PLAN.md](./PLAN.md)。

## 功能特性

### 核心编辑

- **富文本编辑器** — 基于 TipTap (ProseMirror)，支持 Markdown 快捷输入
- **斜杠命令菜单** — 输入 `/` 快速插入标题、列表、代码块、任务列表等
- **目录导航** — 自动提取 H1-H3 标题，支持点击滚动定位
- **PDF / DOCX 导出** — 一键导出当前笔记为 PDF 或 Word 文档
- **Ctrl+S 快捷保存** — 全局支持键盘快捷键保存

### 双链笔记 & 知识图谱

- **双向链接** — 在编辑器中输入 `[[` 触发自动补全，选择目标笔记创建 `[[noteId|noteTitle]]` 格式链接
- **反向引用面板** — 查看哪些笔记引用了当前笔记
- **知识图谱可视化** — 基于 D3.js 力导向图，展示笔记间的链接关系网络
  - 节点大小按链接数动态缩放
  - 颜色按文件夹分组
  - 支持拖拽、缩放、搜索定位、点击查看详情

### 笔记管理

- **文件夹管理** — 支持多层级文件夹组织，拖拽笔记到文件夹
- **标签系统** — 为笔记添加标签，侧边栏按标签快速过滤
- **笔记搜索** — 全文搜索标题和摘要
- **笔记置顶** — 重要笔记置顶显示
- **Markdown 导入** — 拖拽 `.md` 文件直接导入

### 任务管理

- 独立任务视图，支持优先级（高/中/低）和截止日期
- 任务完成状态切换
- 侧边栏显示未完成任务数

### 每日笔记

- 按日期自动创建日记，日历视图快速切换日期
- 有记录的日期标记高亮
- 自动获取当前位置天气（中文地名）
- 心情记录（8种心情选择）
- 双击编辑、失焦自动保存

### 日历事件

- 月视图日历，支持跨天事件
- 全天事件 / 定时事件
- 双击空白新增事件，双击事件编辑
- 颜色标记

### 白板

- 自由绘图画布
- 支持多个白板管理

### 用量统计

- 个人 / 组织用量仪表盘
- 可选时间范围（7天 / 30天 / 90天）

### 多租户 & 认证

- JWT 认证（注册 / 登录）
- 个人空间 + 组织空间切换
- 所有数据表通过 `tenant_id` + `tenant_type` 实现租户隔离
- 组织成员管理（OWNER / ADMIN / MEMBER 角色）

### UI / UX

- 明暗主题切换
- 6 种主题色可选
- 响应式布局，移动端适配
- 侧边栏文档计数开关

---

## 技术栈

| 层级 | 技术 |
|------|------|
| **前端** | Vue 3.5 + Composition API + TypeScript |
| **构建** | Vite 7 |
| **状态管理** | Pinia 3 |
| **路由** | Vue Router 4 |
| **样式** | Tailwind CSS 4 |
| **富文本编辑器** | TipTap 3 (ProseMirror) + tiptap-markdown |
| **图谱可视化** | D3.js 7 |
| **后端** | Spring Boot 3.2.5 (Java 17) |
| **ORM** | MyBatis-Plus 3.5.15 |
| **数据库** | MySQL 8.0+ |
| **数据库迁移** | Flyway 9.x |
| **认证** | Spring Security + JJWT 0.12.5 |

---

## 快速开始

### 环境要求

- **Java** 17+
- **Node.js** 18+
- **MySQL** 8.0+（或通过 Docker 启动）
- **Maven** 3.8+（或使用项目内置 `mvnw`）

### 1. 创建数据库

连接 MySQL 后执行：

```sql
CREATE DATABASE IF NOT EXISTS cr_notebook
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

> Flyway 会在后端首次启动时自动执行所有迁移脚本，无需手动建表。

### 2. 启动后端

```bash
cd backend

# 按需修改数据库连接（默认 root/123@abc）
# 配置文件：src/main/resources/application.yml

./mvnw spring-boot:run
```

后端启动在 `http://localhost:8080`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动在 `http://localhost:5173`，通过 Vite Proxy 自动转发 `/api` 请求到后端。

### 4. 生产构建

```bash
# 前端构建
cd frontend
npm run build    # 产物输出到 dist/

# 后端构建
cd backend
./mvnw clean package -DskipTests    # 产物 target/notebook-0.1.0-SNAPSHOT.jar
java -jar target/notebook-0.1.0-SNAPSHOT.jar
```

---

## 数据库结构

Flyway 迁移脚本位于 `backend/src/main/resources/db/migration/`，以下为完整建表语句（V1 ~ V6）：

### V1 — 初始化核心表

```sql
-- 用户表
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 组织表
CREATE TABLE sys_organization (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 组织成员表
CREATE TABLE sys_org_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES sys_organization(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    UNIQUE KEY uk_org_user (organization_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 文件夹表
CREATE TABLE note_folder (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(10) DEFAULT '📁',
    parent_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id, tenant_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 笔记表
CREATE TABLE note (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    folder_id BIGINT,
    title VARCHAR(255) NOT NULL DEFAULT '',
    content LONGTEXT,
    excerpt VARCHAR(500) DEFAULT '',
    is_pinned TINYINT(1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id, tenant_type),
    INDEX idx_folder (folder_id),
    FULLTEXT INDEX ft_search (title, excerpt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 标签表
CREATE TABLE note_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id, tenant_type),
    UNIQUE KEY uk_tenant_tag (tenant_id, tenant_type, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 笔记-标签关联表
CREATE TABLE note_tag_relation (
    note_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (note_id, tag_id),
    FOREIGN KEY (note_id) REFERENCES note(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES note_tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 任务表
CREATE TABLE task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    note_id BIGINT,
    content VARCHAR(500) NOT NULL,
    completed TINYINT(1) DEFAULT 0,
    priority VARCHAR(10) DEFAULT 'MEDIUM',
    due_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id, tenant_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 每日笔记表
CREATE TABLE daily_note (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    note_date DATE NOT NULL,
    content LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_date (tenant_id, tenant_type, note_date),
    INDEX idx_tenant (tenant_id, tenant_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 日历事件表
CREATE TABLE calendar_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    title VARCHAR(200) NOT NULL,
    event_date DATE NOT NULL,
    event_time TIME,
    description VARCHAR(500),
    color VARCHAR(20) DEFAULT '#6366f1',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id, tenant_type),
    INDEX idx_date (event_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 白板表
CREATE TABLE whiteboard (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    title VARCHAR(200) NOT NULL DEFAULT '新白板',
    data LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id, tenant_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### V2 — 用户 & 组织头像

```sql
ALTER TABLE sys_user ADD COLUMN avatar_url VARCHAR(500) DEFAULT NULL;
ALTER TABLE sys_organization ADD COLUMN avatar_url VARCHAR(500) DEFAULT NULL;
```

### V3 — 头像字段扩容（支持 Base64 Data URL）

```sql
ALTER TABLE sys_user MODIFY COLUMN avatar_url MEDIUMTEXT DEFAULT NULL;
ALTER TABLE sys_organization MODIFY COLUMN avatar_url MEDIUMTEXT DEFAULT NULL;
```

### V4 — 笔记标签字段（JSON 数组）

```sql
ALTER TABLE note ADD COLUMN tags TEXT DEFAULT NULL;
```

### V5 — 日历事件跨天支持

```sql
ALTER TABLE calendar_event ADD COLUMN end_date DATE NULL AFTER event_time;
ALTER TABLE calendar_event ADD COLUMN end_time TIME NULL AFTER end_date;
```

### V6 — 每日笔记天气 & 心情

```sql
ALTER TABLE daily_note ADD COLUMN weather VARCHAR(255) NULL;
ALTER TABLE daily_note ADD COLUMN mood VARCHAR(50) NULL;
```

---

## 项目结构

```
CR_Notebook_DEMO/
├── frontend/                      # Vue 3 前端
│   └── src/
│       ├── api/                   # HTTP 客户端 & API 模块
│       ├── components/
│       │   ├── editor/            # TipTap 编辑器相关
│       │   │   ├── TipTapEditor.vue        # 核心编辑器组件
│       │   │   ├── SlashCommandMenu.vue    # 斜杠命令菜单
│       │   │   ├── NoteLinkSuggestion.vue  # 双链补全弹窗
│       │   │   └── extensions/
│       │   │       └── NoteLink.ts         # 双链 TipTap 扩展
│       │   ├── layout/            # 布局组件（侧边栏、笔记列表等）
│       │   └── ui/                # 通用 UI 组件（对话框、头像选择等）
│       ├── views/                 # 页面视图
│       │   ├── NotesView.vue              # 笔记主视图
│       │   ├── DailyNoteView.vue          # 每日笔记
│       │   ├── TasksView.vue              # 任务管理
│       │   ├── CalendarView.vue           # 日历
│       │   ├── WhiteboardView.vue         # 白板
│       │   ├── KnowledgeGraphView.vue     # 知识图谱
│       │   ├── StatsView.vue              # 统计
│       │   └── LoginView.vue              # 登录
│       ├── stores/                # Pinia 状态管理
│       ├── types/                 # TypeScript 类型定义
│       ├── styles/                # 全局样式
│       └── router/                # 路由配置
├── backend/                       # Spring Boot 后端
│   └── src/
│       ├── main/java/com/cr/notebook/
│       │   ├── config/            # 安全、CORS、异常处理配置
│       │   ├── security/          # JWT 生成/验证、认证过滤器
│       │   ├── tenant/            # 多租户上下文 & 拦截器
│       │   ├── entity/            # 数据库实体（MyBatis-Plus）
│       │   ├── mapper/            # MyBatis-Plus Mapper 接口
│       │   ├── dto/               # 数据传输对象
│       │   ├── service/           # 业务逻辑层
│       │   └── controller/        # REST 控制器
│       └── main/resources/
│           ├── application.yml    # 应用配置
│           └── db/migration/      # Flyway 迁移脚本（V1~V6）
└── README.md
```

---

## API 端点

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录 |

### 笔记

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/notes` | 列出笔记（可选 `?folderId=`） |
| GET | `/api/notes/:id` | 获取笔记详情 |
| POST | `/api/notes` | 创建笔记 |
| PUT | `/api/notes/:id` | 更新笔记 |
| DELETE | `/api/notes/:id` | 删除笔记 |
| GET | `/api/notes/search?q=` | 搜索笔记 |
| GET | `/api/notes/:id/backlinks` | 获取反向链接（双链） |
| GET | `/api/notes/graph` | 获取知识图谱数据 |

### 文件夹

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/folders` | 列出文件夹 |
| POST | `/api/folders` | 创建文件夹 |
| PUT | `/api/folders/:id` | 更新文件夹 |
| DELETE | `/api/folders/:id` | 删除文件夹 |

### 任务

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/tasks` | 列出任务 |
| POST | `/api/tasks` | 创建任务 |
| PUT | `/api/tasks/:id` | 更新任务 |
| DELETE | `/api/tasks/:id` | 删除任务 |

### 每日笔记

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/daily-notes/:date` | 获取/创建指定日期笔记 |
| PUT | `/api/daily-notes/:date` | 更新每日笔记 |
| GET | `/api/daily-notes/dates?from=&to=` | 查询有记录的日期列表 |

### 日历事件

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/calendar-events?start=&end=` | 按日期范围查询事件 |
| POST | `/api/calendar-events` | 创建事件 |
| PUT | `/api/calendar-events/:id` | 更新事件 |
| DELETE | `/api/calendar-events/:id` | 删除事件 |

### 白板

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/whiteboards` | 列出白板 |
| POST | `/api/whiteboards` | 创建白板 |
| PUT | `/api/whiteboards/:id` | 更新白板 |
| DELETE | `/api/whiteboards/:id` | 删除白板 |

### 统计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/stats/personal?days=7` | 个人用量统计 |
| GET | `/api/stats/org/:orgId?days=7` | 组织用量统计 |

### 用户

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/users/me` | 获取当前用户信息 |
| PUT | `/api/users/me/avatar` | 更新头像 |
| PUT | `/api/users/me/password` | 修改密码 |
| GET | `/api/users/search?q=` | 搜索用户（组织邀请） |

### 组织

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/organizations` | 列出我的组织 |
| POST | `/api/organizations` | 创建组织 |
| GET | `/api/organizations/:id` | 获取组织详情 |
| PUT | `/api/organizations/:id` | 更新组织 |
| DELETE | `/api/organizations/:id` | 删除组织 |
| GET | `/api/organizations/:id/members` | 列出成员 |
| POST | `/api/organizations/:id/members` | 添加成员 |
| PUT | `/api/organizations/:id/members/:userId` | 修改成员角色 |
| DELETE | `/api/organizations/:id/members/:userId` | 移除成员 |

---

## 多租户架构

- **个人空间**：`tenant_id = user_id`，`tenant_type = PERSONAL`
- **组织空间**：`tenant_id = org_id`，`tenant_type = ORGANIZATION`
- 通过请求头 `X-Tenant-Id` + `X-Tenant-Type` 切换租户上下文
- 所有业务数据表均包含 `tenant_id` 和 `tenant_type` 字段，在 Service 层自动过滤

---

## 运行测试

```bash
cd backend
./mvnw test
```

测试覆盖：
- **Controller 测试** — NoteController、FolderController、TaskController、AuthController、StatsController、OrganizationController、UserController
- **Service 测试** — NoteService（含 backlinks、graph 图谱测试）、FolderService、TaskService、AuthService、DailyNoteService、CalendarEventService、WhiteboardService、StatsService、OrganizationService
- **集成测试** — 完整 CRUD 流程、多租户数据隔离、认证流程

---

## 配置说明

后端配置文件 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cr_notebook?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
    username: root
    password: 123@abc       # 按需修改

app:
  jwt:
    secret: cr-notebook-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256
    expiration-ms: 86400000 # JWT 过期时间：24小时

  cors:
    allowed-origins: http://localhost:5173,http://localhost:3000
```

前端 API 代理配置 `frontend/vite.config.ts`：

```typescript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```
