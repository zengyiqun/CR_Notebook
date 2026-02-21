# CR Notebook — 项目开发计划

> 本项目使用 **Cursor IDE** + **Claude (Anthropic)** 大语言模型，通过 AI 辅助编程方式从零构建。
> 以下为完整的功能规划、技术选型、迭代计划和实现记录。

---

## 一、项目定位

构建一款受 [Craft](https://craft.do/) 启发的**全功能笔记应用**，核心目标：

- 流畅的富文本编辑体验
- 双向链接与知识图谱构建个人知识网络
- 多租户（个人 + 组织）数据隔离
- 覆盖日常场景：笔记、任务、日历、每日记录、白板

---

## 二、技术选型

### 前端

| 技术 | 版本 | 选型理由 |
|------|------|----------|
| Vue 3 | 3.5+ | Composition API 更好的逻辑复用与 TypeScript 集成 |
| Vite | 7.x | 极速 HMR 开发体验 |
| Pinia | 3.x | 轻量、类型友好的状态管理 |
| Vue Router | 4.x | 官方路由方案 |
| Tailwind CSS | 4.x | Utility-first 快速构建 UI |
| TipTap | 3.x | 基于 ProseMirror 的可扩展富文本编辑器 |
| D3.js | 7.x | 力导向图可视化知识图谱 |
| TypeScript | 5.x | 类型安全 |

### 后端

| 技术 | 版本 | 选型理由 |
|------|------|----------|
| Spring Boot | 3.2.5 | 企业级 Java Web 框架 |
| Java | 17 | LTS 版本，支持 Records、Sealed Classes 等现代特性 |
| MyBatis-Plus | 3.5.15 | 简化 CRUD，Lambda 查询，代码生成 |
| MySQL | 8.0+ | 成熟可靠的关系型数据库 |
| Flyway | 9.x | 数据库版本化迁移管理 |
| Spring Security + JJWT | 0.12.5 | JWT 无状态认证 |

---

## 三、功能模块规划

### Phase 1 — 基础架构 ✅

- [x] 项目脚手架搭建（Vue 3 + Vite 前端、Spring Boot 后端）
- [x] MySQL 数据库设计与 Flyway V1 初始化迁移
- [x] JWT 认证体系（注册 / 登录 / 令牌验证）
- [x] 多租户架构（TenantContext + TenantInterceptor）
- [x] 前端路由、布局框架（AppSidebar + 主内容区）
- [x] 基础 CORS 配置 & Vite API 代理

### Phase 2 — 核心笔记功能 ✅

- [x] TipTap 富文本编辑器集成
  - Markdown 快捷输入（标题、列表、代码块、引用、分割线）
  - 斜杠命令菜单（`/` 触发）
  - 任务列表（复选框）
- [x] 笔记 CRUD（创建、读取、更新、删除）
- [x] 文件夹管理（多层级组织、拖拽笔记）
- [x] 笔记搜索（标题 + 摘要全文搜索）
- [x] 笔记置顶
- [x] 标签系统（添加 / 删除标签，侧边栏按标签过滤）
- [x] 自动保存（编辑器内容变更后延时自动保存）
- [x] Ctrl+S 手动保存
- [x] 目录导航（H1-H3 标题提取与滚动定位）
- [x] PDF / DOCX 导出
- [x] Markdown 文件拖拽导入

### Phase 3 — 双向链接 ✅

- [x] 自定义 TipTap NoteLink 扩展节点
  - `[[` 触发自动补全弹窗
  - 选择目标笔记后插入 `[[noteId|noteTitle]]` 格式节点
  - 渲染为可点击的链接标签
- [x] 后端 `[[id|title]]` 正则解析
- [x] 反向链接 API（`/api/notes/:id/backlinks`）
- [x] 反向引用面板（编辑器侧边展示引用当前笔记的所有笔记）

### Phase 4 — 任务管理 ✅

- [x] 独立任务视图
- [x] 任务 CRUD + 完成状态切换
- [x] 优先级（高 / 中 / 低）
- [x] 截止日期
- [x] 侧边栏未完成任务计数

### Phase 5 — 每日笔记 ✅

- [x] 按日期自动创建日记
- [x] 日历视图快速切换日期
- [x] 有记录日期高亮标记
- [x] 天气自动获取（基于地理位置，中文地名显示）
- [x] 心情记录（8 种心情选择）
- [x] 编辑 / 只读模式切换

### Phase 6 — 日历事件 ✅

- [x] 月视图日历
- [x] 全天事件 / 定时事件
- [x] 跨天事件支持（V5 迁移：`end_date` + `end_time`）
- [x] 双击空白新增事件 / 双击事件编辑
- [x] 颜色标记

### Phase 7 — 白板 ✅

- [x] 自由绘图画布
- [x] 多白板管理

### Phase 8 — 统计分析 ✅

- [x] 个人 / 组织用量仪表盘
- [x] 可选时间范围（7 / 30 / 90 天）
- [x] 可视化图表

### Phase 9 — 组织协作 ✅

- [x] 创建 / 管理组织
- [x] 组织成员邀请（按用户名搜索）
- [x] 角色管理（OWNER / ADMIN / MEMBER）
- [x] 个人空间 ↔ 组织空间一键切换
- [x] 数据完全租户隔离

### Phase 10 — 知识图谱 ✅

- [x] 后端知识图谱 API（`/api/notes/graph`）
  - 遍历所有笔记解析 `[[id|title]]` 链接
  - 返回 `{ nodes, edges }` 结构
  - 自动过滤自引用、无效链接，边去重
- [x] 前端 D3.js 力导向图可视化
  - 节点大小按链接数缩放
  - 颜色按文件夹分组
  - 拖拽交互
  - 画布缩放 / 平移
  - 搜索定位节点
  - 缩放控制按钮
  - 悬停高亮关联节点与边
  - 点击节点显示详情面板（标题、标签、链接列表、跳转按钮）
- [x] 侧边栏新增「知识图谱」菜单项

### Phase 11 — UI / UX 打磨 ✅

- [x] 明暗主题切换
- [x] 6 种主题色可选
- [x] 响应式布局（移动端适配）
- [x] 侧边栏文档计数开关
- [x] 编辑按钮与失焦保存冲突修复 → 改为区域外双击退出编辑模式
- [x] 每日笔记「完成」按钮手动退出编辑
- [x] PDF 导出优化 — 修复文字显示不全
  - 修复容器 `overflow:hidden` 截断内容问题
  - 统一容器宽度与 html2canvas `windowWidth` 为 700px
  - 添加 `overflow-wrap: break-word` 防止长文本溢出
  - 代码块 `pre` 使用 `white-space: pre-wrap` 自动换行
  - 启用自动分页控制 `pagebreak: { mode: ['avoid-all', 'css', 'legacy'] }`
  - 增大页面边距至 15mm、字体缩至 14px 以适配 A4
- [x] Word 导出优化 — 修复排版混乱
  - 新增 `sanitizeHtmlForWord()` 预处理 TipTap HTML
  - 任务列表 `<ul data-type="taskList">` 转为 ☐/☑ 纯文本段落
  - 代码块 `<pre><code>` 简化为带内联样式的 `<pre>`
  - 笔记链接 `.note-link-chip` 转为 `[[标题]]` 纯文本
  - 添加 `@page` A4 页面设置与标准页边距
  - 补充 `mso-style-name` 使标题在 Word 导航窗格正确显示
  - 完善中英文字体回退链（Microsoft YaHei / PingFang SC / Calibri）

### Phase 12 — 测试与文档 ✅

- [x] 单元测试（JUnit 5 + Mockito）
  - Controller 层：Note、Folder、Task、Auth、Stats、Organization、User
  - Service 层：Note（含 backlinks、graph）、Folder、Task、Auth、DailyNote、CalendarEvent、Whiteboard、Stats、Organization
  - 集成测试：完整 CRUD、多租户隔离、认证流程
- [x] Maven Surefire 配置（Java 17 + Mockito `--add-opens` 兼容）
- [x] README.md 完整文档
- [x] PLAN.md 开发计划文档

---

## 四、数据库迁移版本

| 版本 | 文件名 | 说明 |
|------|--------|------|
| V1 | `V1__init.sql` | 初始化全部核心表：用户、组织、组织成员、文件夹、笔记、标签、标签关联、任务、每日笔记、日历事件、白板 |
| V2 | `V2__add_avatar.sql` | 用户 & 组织新增头像字段 |
| V3 | `V3__enlarge_avatar_url.sql` | 头像字段扩容为 MEDIUMTEXT（支持 Base64 Data URL）|
| V4 | `V4__add_note_tags.sql` | 笔记新增 JSON 格式标签字段 |
| V5 | `V5__calendar_event_end_date.sql` | 日历事件支持跨天（`end_date` + `end_time`）|
| V6 | `V6__daily_note_weather_mood.sql` | 每日笔记新增天气 & 心情字段 |

---

## 五、关键架构决策

### 1. 多租户隔离方案

采用**共享数据库、共享表**模式，通过 `tenant_id` + `tenant_type` 字段实现行级隔离：
- `TenantContext`：ThreadLocal 存储当前请求的租户信息
- `TenantInterceptor`：从请求头 `X-Tenant-Id` / `X-Tenant-Type` 提取并设置上下文
- 所有 Service 层查询自动附加租户过滤条件

### 2. 双向链接实现

- **存储格式**：`[[noteId|noteTitle]]` 内嵌在笔记 HTML/Markdown 内容中
- **前端**：自定义 TipTap `NoteLink` 扩展节点，渲染为可交互的链接标签
- **后端**：通过正则 `\[\[(\d+)\|([^\]]+)\]\]` 解析内容中的链接关系
- **优势**：无需额外关系表，链接关系从内容动态解析，保持数据一致性

### 3. 编辑模式交互

- 初始实现：焦点离开自动保存 → **问题**：与编辑按钮的 click 事件冲突，导致无法进入编辑模式
- 最终方案：移除 focusout 事件，改为**区域外双击**退出编辑模式
  - 双击是明确的有意识操作，不会与按钮单击冲突
  - 同时保留手动「保存」「完成」按钮

### 4. 知识图谱渲染

- 采用 D3.js `forceSimulation` 力导向图
- 节点大小 = `Math.sqrt(linkCount) * 3 + 基础半径`，链接数越多节点越大
- 颜色按 `folderId` 分组映射到预设色板
- 使用 SVG `<marker>` 实现有向箭头

### 5. 文档导出方案

**PDF 导出**：使用 `html2pdf.js`（html2canvas + jsPDF）
- 离屏创建 DOM 容器（`position:fixed; left:-9999px`），不影响页面且不截断内容
- html2canvas `windowWidth` 与容器宽度严格一致（700px）避免缩放偏差
- 所有文本启用 `word-break` 防止长代码/URL 溢出

**Word 导出**：使用 MSO HTML 格式（`.doc`）
- 利用 Word 对 HTML+CSS 的解析能力，通过 `xmlns:w` / `xmlns:o` 命名空间注入 Word 专用指令
- 导出前通过 `sanitizeHtmlForWord()` 清理 TipTap 特有的 HTML 结构（任务列表、链接节点、嵌套代码块）
- 使用 `mso-style-name` 映射 Word 内建样式，确保标题层级在导航窗格中正确显示

---

## 六、开发工具与流程

| 工具 | 用途 |
|------|------|
| **Cursor IDE** | 开发环境，集成 AI 编程助手 |
| **Claude (Anthropic)** | AI 大语言模型，驱动代码生成、架构设计、Bug 修复、文档编写 |
| **GitHub** | 代码托管 |
| **GitHub CLI (`gh`)** | 命令行创建仓库 & 推送 |
| **Maven Wrapper (`mvnw`)** | 后端构建（无需全局安装 Maven）|
| **npm** | 前端依赖管理 |
| **Vite** | 前端开发服务器 & 构建 |
| **Flyway** | 数据库版本迁移 |
| **Docker Compose** | 可选的 MySQL 容器化部署 |

### 开发流程

```
需求描述（自然语言 Prompt）
    ↓
Cursor + Claude 生成代码
    ↓
运行 & 验证（dev server / unit test）
    ↓
发现问题 → 反馈 Prompt → AI 修复
    ↓
功能完成 → 下一轮迭代
```

所有代码（前端 Vue/TS、后端 Java、数据库迁移、单元测试、项目文档）均通过上述人机协作循环完成，**未手动编写代码**。

---

## 七、仓库信息

- **GitHub 地址**：https://github.com/zengyiqun/CR_Notebook
- **分支**：`main`
- **许可证**：MIT（开源）
