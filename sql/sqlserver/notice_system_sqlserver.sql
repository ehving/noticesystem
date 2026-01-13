/* =========================
   0) 建库 + 切库
   ========================= */
IF DB_ID(N'notice_system_sqlserver') IS NULL
BEGIN
    CREATE DATABASE notice_system_sqlserver;
END
GO

USE notice_system_sqlserver;
GO

/* =========================
   1) 清理旧对象（注意外键顺序）
   ========================= */

-- 先删触发器
IF OBJECT_ID(N'dbo.trg_notice_read_after_insert', N'TR') IS NOT NULL
    DROP TRIGGER dbo.trg_notice_read_after_insert;
GO

-- 先删过程
IF OBJECT_ID(N'dbo.sp_clean_sync_log', N'P') IS NOT NULL
    DROP PROCEDURE dbo.sp_clean_sync_log;
GO

-- 子表优先
IF OBJECT_ID(N'dbo.sync_conflict_item', N'U') IS NOT NULL DROP TABLE dbo.sync_conflict_item;
IF OBJECT_ID(N'dbo.sync_conflict', N'U') IS NOT NULL DROP TABLE dbo.sync_conflict;

IF OBJECT_ID(N'dbo.notice_read', N'U') IS NOT NULL DROP TABLE dbo.notice_read;
IF OBJECT_ID(N'dbo.notice_target_dept', N'U') IS NOT NULL DROP TABLE dbo.notice_target_dept;
IF OBJECT_ID(N'dbo.notice', N'U') IS NOT NULL DROP TABLE dbo.notice;
IF OBJECT_ID(N'dbo.users', N'U') IS NOT NULL DROP TABLE dbo.users;
IF OBJECT_ID(N'dbo.dept', N'U') IS NOT NULL DROP TABLE dbo.dept;
IF OBJECT_ID(N'dbo.role', N'U') IS NOT NULL DROP TABLE dbo.role;
IF OBJECT_ID(N'dbo.sync_log', N'U') IS NOT NULL DROP TABLE dbo.sync_log;
GO

/* =========================
   2) 角色表 role
   ========================= */
CREATE TABLE dbo.role (
    id          CHAR(32)      NOT NULL,
    name        NVARCHAR(50)  NOT NULL,
    create_time DATETIME2     NOT NULL,
    update_time DATETIME2     NOT NULL,
    CONSTRAINT pk_role PRIMARY KEY (id),
    CONSTRAINT uk_role_name UNIQUE (name)
);
GO

/* =========================
   3) 部门表 dept
   ========================= */
CREATE TABLE dbo.dept (
    id          CHAR(32)       NOT NULL,
    name        NVARCHAR(100)  NOT NULL,
    parent_id   CHAR(32)       NULL,
    description NVARCHAR(255)  NULL,
    sort_order  INT            NULL,
    status      TINYINT        NOT NULL CONSTRAINT df_dept_status DEFAULT (1),
    create_time DATETIME2      NOT NULL,
    update_time DATETIME2      NOT NULL,
    CONSTRAINT pk_dept PRIMARY KEY (id),
    CONSTRAINT fk_dept_parent FOREIGN KEY (parent_id) REFERENCES dbo.dept(id)
);
GO

CREATE INDEX idx_dept_parent ON dbo.dept(parent_id);
GO

/* =========================
   4) 用户表 users
   - 关键：email/phone 的唯一性用 filtered unique index，避免 NULL 冲突
   ========================= */
CREATE TABLE dbo.users (
    id              CHAR(32)       NOT NULL,
    username        NVARCHAR(50)   NOT NULL,
    [password]      NVARCHAR(100)  NOT NULL,
    role_id         CHAR(32)       NOT NULL,
    dept_id         CHAR(32)       NULL,
    nickname        NVARCHAR(50)   NULL,
    email           NVARCHAR(100)  NULL,
    phone           NVARCHAR(20)   NULL,
    avatar          NVARCHAR(255)  NULL,
    status          TINYINT        NOT NULL CONSTRAINT df_users_status DEFAULT (1),
    last_login_time DATETIME2      NULL,
    create_time     DATETIME2      NOT NULL,
    update_time     DATETIME2      NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_user_username UNIQUE (username),
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES dbo.role(id),
    CONSTRAINT fk_user_dept FOREIGN KEY (dept_id) REFERENCES dbo.dept(id)
);
GO

CREATE INDEX idx_user_role ON dbo.users(role_id);
CREATE INDEX idx_user_dept ON dbo.users(dept_id);
GO

-- SQL Server：NULL 也会参与 UNIQUE，必须用 filtered unique index
CREATE UNIQUE INDEX uk_user_email_notnull ON dbo.users(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX uk_user_phone_notnull ON dbo.users(phone) WHERE phone IS NOT NULL;
GO

/* =========================
   5) 公告表 notice
   ========================= */
CREATE TABLE dbo.notice (
    id           CHAR(32)       NOT NULL,
    title        NVARCHAR(200)  NOT NULL,
    content      NVARCHAR(MAX)  NOT NULL,
    publisher_id CHAR(32)       NOT NULL,
    [level]      NVARCHAR(20)   NOT NULL CONSTRAINT df_notice_level DEFAULT (N'NORMAL'),
    publish_time DATETIME2      NULL,
    expire_time  DATETIME2      NULL,
    [status]     NVARCHAR(20)   NOT NULL CONSTRAINT df_notice_status DEFAULT (N'DRAFT'),
    view_count   BIGINT         NOT NULL CONSTRAINT df_notice_view DEFAULT (0),
    create_time  DATETIME2      NOT NULL,
    update_time  DATETIME2      NOT NULL,
    CONSTRAINT pk_notice PRIMARY KEY (id),
    CONSTRAINT fk_notice_publisher FOREIGN KEY (publisher_id) REFERENCES dbo.users(id),
    CONSTRAINT ck_notice_level CHECK ([level] IN (N'NORMAL', N'IMPORTANT', N'URGENT')),
    CONSTRAINT ck_notice_status CHECK ([status] IN (N'DRAFT', N'PUBLISHED', N'RECALLED'))
);
GO

CREATE INDEX idx_notice_status_publish ON dbo.notice([status], publish_time);
CREATE INDEX idx_notice_publisher ON dbo.notice(publisher_id);
GO

/* =========================
   6) 公告-部门关联表 notice_target_dept
   ========================= */
CREATE TABLE dbo.notice_target_dept (
    id          CHAR(32)    NOT NULL,
    notice_id   CHAR(32)    NOT NULL,
    dept_id     CHAR(32)    NOT NULL,
    create_time DATETIME2   NOT NULL,
    update_time DATETIME2   NOT NULL,
    CONSTRAINT pk_notice_target_dept PRIMARY KEY (id),
    CONSTRAINT fk_ntd_notice FOREIGN KEY (notice_id) REFERENCES dbo.notice(id),
    CONSTRAINT fk_ntd_dept FOREIGN KEY (dept_id) REFERENCES dbo.dept(id)
);
GO

CREATE INDEX idx_ntd_notice ON dbo.notice_target_dept(notice_id);
CREATE INDEX idx_ntd_dept ON dbo.notice_target_dept(dept_id);
GO

/* =========================
   7) 公告阅读记录表 notice_read
   ========================= */
CREATE TABLE dbo.notice_read (
    id          CHAR(32)     NOT NULL,
    notice_id   CHAR(32)     NOT NULL,
    user_id     CHAR(32)     NOT NULL,
    read_time   DATETIME2    NOT NULL,
    device_type NVARCHAR(20) NULL,
    create_time DATETIME2    NOT NULL,
    update_time DATETIME2    NOT NULL,
    CONSTRAINT pk_notice_read PRIMARY KEY (id),
    CONSTRAINT fk_nr_notice FOREIGN KEY (notice_id) REFERENCES dbo.notice(id),
    CONSTRAINT fk_nr_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT ck_notice_read_device CHECK (device_type IS NULL OR device_type IN (N'PC', N'MOBILE'))
);
GO

CREATE INDEX idx_nr_notice_user ON dbo.notice_read(notice_id, user_id);
CREATE INDEX idx_nr_user ON dbo.notice_read(user_id);
GO

/* =========================
   8) 同步日志表 sync_log
   ========================= */
CREATE TABLE dbo.sync_log (
    id          CHAR(32)      NOT NULL,
    entity_type NVARCHAR(50)  NOT NULL,
    entity_id   NVARCHAR(64)  NOT NULL,
    action      NVARCHAR(20)  NOT NULL,
    source_db   NVARCHAR(20)  NOT NULL,
    target_db   NVARCHAR(20)  NOT NULL,
    status      NVARCHAR(20)  NOT NULL,
    error_msg   NVARCHAR(MAX) NULL,
    retry_count INT           NOT NULL CONSTRAINT df_sync_log_retry DEFAULT (0),
    create_time DATETIME2     NOT NULL,
    update_time DATETIME2     NOT NULL,
    CONSTRAINT pk_sync_log PRIMARY KEY (id),
    CONSTRAINT ck_sync_log_action CHECK (action IN (N'CREATE', N'UPDATE', N'DELETE')),
    CONSTRAINT ck_sync_log_source_db CHECK (source_db IN (N'MYSQL', N'PG', N'SQLSERVER')),
    CONSTRAINT ck_sync_log_target_db CHECK (target_db IN (N'MYSQL', N'PG', N'SQLSERVER')),
    CONSTRAINT ck_sync_log_status CHECK (status IN (N'SUCCESS', N'FAILED', N'CONFLICT', N'ERROR'))
);
GO

CREATE INDEX idx_sync_log_status_create ON dbo.sync_log(status, create_time);
CREATE INDEX idx_sync_log_entity ON dbo.sync_log(entity_type, entity_id);
CREATE INDEX idx_sync_log_src_tgt ON dbo.sync_log(source_db, target_db);
GO

/* =========================
   9) 冲突工单表 sync_conflict
   ========================= */
CREATE TABLE dbo.sync_conflict (
    id                   CHAR(32)       NOT NULL,
    entity_type           NVARCHAR(50)   NOT NULL,
    entity_id             NVARCHAR(64)   NOT NULL,
    status               NVARCHAR(20)   NOT NULL CONSTRAINT df_conflict_status DEFAULT (N'OPEN'),
    conflict_type         NVARCHAR(20)   NULL,
    first_seen_at         DATETIME2      NOT NULL,
    last_seen_at          DATETIME2      NOT NULL,
    last_checked_at       DATETIME2      NOT NULL,
    last_notified_at      DATETIME2      NULL,
    notify_count          INT            NOT NULL CONSTRAINT df_conflict_notify DEFAULT (0),
    resolution_source_db  NVARCHAR(20)   NULL,
    resolution_note       NVARCHAR(512)  NULL,
    resolved_at           DATETIME2      NULL,
    create_time           DATETIME2      NOT NULL,
    update_time           DATETIME2      NOT NULL,
    CONSTRAINT pk_sync_conflict PRIMARY KEY (id),
    CONSTRAINT uk_sync_conflict_entity UNIQUE (entity_type, entity_id),
    CONSTRAINT ck_sync_conflict_status CHECK (status IN (N'OPEN', N'RESOLVED', N'IGNORED')),
    CONSTRAINT ck_sync_conflict_type CHECK (conflict_type IS NULL OR conflict_type IN (N'MISSING', N'MISMATCH')),
    CONSTRAINT ck_sync_conflict_resolution_db CHECK (
        resolution_source_db IS NULL OR resolution_source_db IN (N'MYSQL', N'PG', N'SQLSERVER')
    )
);
GO

CREATE INDEX idx_conflict_status_seen ON dbo.sync_conflict(status, last_seen_at);
CREATE INDEX idx_conflict_checked ON dbo.sync_conflict(last_checked_at);
GO

/* =========================
   10) 冲突明细表 sync_conflict_item
   ========================= */
CREATE TABLE dbo.sync_conflict_item (
    id              CHAR(32)       NOT NULL,
    conflict_id     CHAR(32)       NOT NULL,
    db_type         NVARCHAR(20)   NOT NULL,
    exists_flag     TINYINT        NOT NULL CONSTRAINT df_item_exists DEFAULT (0),
    row_hash        NVARCHAR(64)   NULL,
    row_version     NVARCHAR(64)   NULL,
    row_update_time DATETIME2      NULL,
    last_checked_at DATETIME2      NOT NULL,
    create_time     DATETIME2      NOT NULL,
    update_time     DATETIME2      NOT NULL,
    CONSTRAINT pk_sync_conflict_item PRIMARY KEY (id),
    CONSTRAINT uk_sync_conflict_item UNIQUE (conflict_id, db_type),
    CONSTRAINT fk_sync_conflict_item_conflict
        FOREIGN KEY (conflict_id) REFERENCES dbo.sync_conflict(id) ON DELETE CASCADE,
    CONSTRAINT ck_item_db_type CHECK (db_type IN (N'MYSQL', N'PG', N'SQLSERVER')),
    CONSTRAINT ck_item_exists CHECK (exists_flag IN (0,1))
);
GO

CREATE INDEX idx_item_conflict ON dbo.sync_conflict_item(conflict_id);
CREATE INDEX idx_item_db_exists ON dbo.sync_conflict_item(db_type, exists_flag);
GO

/* =========================
   11) 触发器：notice_read 插入后 view_count +1（支持批量）
   ========================= */
CREATE TRIGGER dbo.trg_notice_read_after_insert
ON dbo.notice_read
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE n
    SET n.view_count = ISNULL(n.view_count, 0) + x.cnt
    FROM dbo.notice n
    INNER JOIN (
        SELECT notice_id, COUNT(*) AS cnt
        FROM inserted
        GROUP BY notice_id
    ) x ON x.notice_id = n.id;
END
GO

/* =========================
   12) 清理日志过程 sp_clean_sync_log
   ========================= */
CREATE PROCEDURE dbo.sp_clean_sync_log
    @p_retain_days INT = 90,
    @p_max_count   BIGINT = 100000
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @retain_days INT = ISNULL(@p_retain_days, 90);
    DECLARE @max_count   BIGINT = ISNULL(@p_max_count, 100000);

    DECLARE @threshold DATETIME2 = DATEADD(DAY, -@retain_days, SYSDATETIME());

    -- 1) 按时间删
    DELETE FROM dbo.sync_log
    WHERE create_time < @threshold;

    -- 2) 按数量限流（删最早的）
    DECLARE @total BIGINT;
    SELECT @total = COUNT(*) FROM dbo.sync_log;

    IF @total > @max_count
    BEGIN
        DECLARE @need_delete BIGINT = @total - @max_count;

        ;WITH cte AS (
            SELECT TOP (@need_delete) id
            FROM dbo.sync_log
            ORDER BY create_time ASC
        )
        DELETE FROM dbo.sync_log
        WHERE id IN (SELECT id FROM cte);
    END
END
GO
