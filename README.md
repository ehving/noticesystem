# Notice System（多库公告通知与同步管理系统）

一个面向课程数据库实践的“多源同步公告管理平台”，同时支持 **MySQL / PostgreSQL / SQL Server** 三种数据库平台，提供 **PC 管理端 + 普通用户端 + 移动端统计视图**，实现公告/用户/角色/部门等业务管理、跨库实时同步与周期纠偏、同步日志追踪、冲突检测与邮件告警、冲突修复闭环与可视化报表。

---

## 1. 项目特性

- **三库同构**：MySQL / PostgreSQL / SQL Server 表结构一致（≥5 张，实际 9 张）
- **跨库同步**：
  - 实时同步：业务写入成功后触发 `submitSync`，同步到其余两库
  - 周期纠偏：定时全量同步/校验，修复短时失败带来的分歧
- **可追踪可审计**：
  - 同步日志 `sync_log` 记录每次“源库→目标库”的同步结果（SUCCESS/FAILED/CONFLICT/ERROR）
  - 聚合统计接口支撑 PC/移动端报表展示
- **冲突治理闭环**：
  - 冲突工单 `sync_conflict` + 三库快照明细 `sync_conflict_item`
  - 邮件告警（冷却/限频）
  - PC 管理端选择来源库修复（覆盖写入其余库）、忽略/重开、修复后重检
- **权限与安全**：
  - JWT 登录鉴权
  - 角色/权限区分（管理端能力需管理员权限）
- **前端体验**：
  - Vue3 + TypeScript + Element Plus
  - ECharts 报表（PC/移动端视图）

---

## 2. 技术栈

### 后端
- Java 17
- Spring Boot 3.x
- MyBatis-Plus
- dynamic-datasource-spring-boot3-starter（多数据源）
- MySQL / PostgreSQL / SQL Server
- JWT
- 邮件：JavaMailSender（SMTP）

### 前端
- Vue 3 + Composition API
- TypeScript
- Element Plus
- Pinia
- Axios
- ECharts(core) + dayjs

---

## 3. 目录结构（示意）

> 实际目录以仓库为准，以下为典型结构说明。

### 后端（Spring Boot）
- `src/main/java/**/controller`：普通端/管理端接口
- `src/main/java/**/service`：业务服务、同步服务、冲突服务、报表服务
- `src/main/java/**/mapper`：按库拆分 Mapper（mysql/pg/sqlserver）
- `src/main/java/**/entity`：实体映射
- `src/main/java/**/vo`：前后端交互 VO/DTO
- `src/main/resources`：配置、SQL 资源等

### 前端（Vue3）
- `src/api`：Axios 封装与模块化 API
- `src/router`：路由与权限守卫
- `src/stores`：Pinia（Token/用户信息/权限）
- `src/views`：页面（用户端/管理端/移动端）
- `src/components`：通用组件
- `src/utils`：工具方法（时间/提示/校验等）

### SQL 脚本与备份
- `sql/mysql/notice_system_mysql.sql`
- `sql/pg/notice_system_pg.sql`
- `sql/sqlserver/notice_system_sqlserver.sql`
- `backup/mysql/*`
- `backup/pg/*`
- `backup/sqlserver/*`

---

## 4. 数据库设计

### 4.1 表对象（9 张，同构）
- `role`
- `dept`
- `users`
- `notice`
- `notice_target_dept`
- `notice_read`
- `sync_log`
- `sync_conflict`
- `sync_conflict_item`

### 4.2 触发器
- `trg_notice_read_after_insert`：`notice_read` 插入后自动维护 `notice.view_count`（浏览量 +1）

### 4.3 存储过程
- `sp_clean_sync_log(p_retain_days, p_max_count)`：按保留天数与最大条数清理 `sync_log`

---

## 5. 核心机制说明

### 5.1 多数据源与切库
- 使用 `dynamic-datasource-spring-boot3-starter` 配置三套数据源，默认主库为 MySQL（primary）
- 通过 `DatabaseType { MYSQL, PG, SQLSERVER }` 作为统一标识
- 管理端接口通常支持 `db` 参数选择落库；未指定时使用默认库
- Mapper 按数据库拆分并通过 `@DS("mysql"|"pg"|"sqlserver")` 显式绑定数据源，避免 ThreadLocal 隐式切库带来的污染风险

### 5.2 实时同步（submitSync）
- 对需要同步的业务表，写操作统一封装在 `MultiDbSyncServiceImpl` 的 `saveInDb/updateByIdInDb/removeByIdInDb` 中
- 源库写入成功后触发 `syncService.submitSync(entityType, id, action, sourceDb)`
- `submitSync` 遍历除源库外的目标库，逐库执行同步写入，并为每一次“源→目标”写入 `sync_log`
- 同步完成后（非 DELETE 且存在成功目标库）触发 post-check，对三库快照校验并在必要时生成冲突工单

### 5.3 周期纠偏（定时全量同步）
- 定时任务 `SyncFullTask` 支持全量纠偏：从指定源库全量读取各实体记录，逐条 `submitSync(UPDATE)` 复用实时同步链路
- 典型配置项：
  - `notice.sync.full.cron`（默认每日 3 点）
  - `notice.sync.full.source-db`（默认 MYSQL）

### 5.4 冲突检测与修复闭环
- 冲突类型：
  - `MISSING`：部分库缺失
  - `MISMATCH`：三库存在但关键字段不一致（`row_hash` 不一致）
- 冲突状态：
  - `OPEN / RESOLVED / IGNORED`
- 冲突由两类触发路径发现：
  - 实时 post-check：同步提交后立即校验三库快照
  - 周期兜底：对近期实体集合进行一致性复核
- 冲突修复：
  - 管理端选择来源库（可信数据源）
  - 覆盖写入其余库（缺失 INSERT，不一致 UPDATE）
  - 更新冲突为 `RESOLVED`，并在修复后重检一致性

---

## 6. 快速开始

### 6.1 准备环境
- MySQL / PostgreSQL / SQL Server 三库可用（本地或远程均可）
- JDK 17、Maven
- Node.js（建议 16+ 或 18+），npm/pnpm

### 6.2 初始化数据库
分别在三库中执行对应脚本完成建库建表：

- MySQL：`sql/mysql/notice_system_mysql.sql`
- PostgreSQL：`sql/pg/notice_system_pg.sql`
- SQL Server：`sql/sqlserver/notice_system_sqlserver.sql`

如需快速还原演示环境，可使用 `backup/` 目录下备份文件恢复。

### 6.3 后端启动
进入后端工程目录：

```bash
mvn clean package
java -jar target/*.jar
```

或在 IDE 中运行启动类。

启动前请确认 `application.yml`（或等价配置文件）已配置：

- 三套数据源连接信息（mysql/pg/sqlserver）
- 默认主库（primary）
- JWT 配置
- 邮件 SMTP（可选）
- 定时任务 cron（可选）

### 6.4 前端启动
进入前端工程目录：

```bash
npm install
npm run dev
```

或使用 pnpm：

```bash
pnpm install
pnpm dev
```

确保前端已配置后端 API 基地址（如通过环境变量或配置文件）。

---

## 7. 使用说明（典型流程）

1. **管理员登录 PC 管理端**
   - 维护角色、部门、用户
   - 创建公告、设置状态与定向部门、发布/撤回

2. **普通用户访问公告**
   - 查看公告列表与详情
   - 进入详情自动写入阅读记录并更新浏览量

3. **同步与治理**
   - 业务写入成功后自动实时同步到其余两库
   - 同步日志可按状态/实体/时间筛选分页查看，并支持聚合统计
   - 发生冲突时生成冲突工单并邮件提醒
   - 管理员在冲突详情页选择来源库一键修复

4. **报表**
   - PC/移动端可查看同步状态统计、冲突分布统计与每日概览

---

## 8. 配置项说明（要点）

- 多数据源：mysql/pg/sqlserver 连接信息与 primary 默认库
- JWT：密钥、过期时间等
- 邮件 SMTP：host/port/username/password 等（用于冲突告警）
- 定时任务：全量同步/校验 cron、源库选择
- 默认落库：接口未指定 `db` 参数时使用默认库作为源库

---

## 9. 常见问题

**Q：为什么不使用 ThreadLocal 动态切库？**  
A：本项目采用“按库拆分 Mapper + @DS 显式绑定”的方式，切库行为更可控，且同步场景需要在同一方法内同时访问源库与目标库。

**Q：跨库同步如何保证一致性？**  
A：采用最终一致性策略：实时同步尽快传播变更，失败可重试；周期纠偏兜底；冲突检测 + 人工修复闭环保证最终一致。

**Q：冲突是如何判定的？**  
A：按实体主键对齐三库记录，计算关键字段指纹 `row_hash`。存在缺失则 `MISSING`，均存在但 hash 不一致则 `MISMATCH`。

---

## 10. License
课程实践项目用途

