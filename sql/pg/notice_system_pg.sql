-- =========================
-- 1) 建库（psql 环境）
-- =========================
-- 在 psql 里先执行这里的建库语句或者手动建库
 CREATE DATABASE notice_system_pg
   WITH ENCODING='UTF8'
        LC_COLLATE='C'
        LC_CTYPE='C'
        TEMPLATE=template0;

-- 再连接到此目标库（psql 命令）
-- \connect notice_system_pg

-- 后再执行下面的建表语句
/* =========================
   2) 清理旧表（CASCADE）
   ========================= */
DROP TABLE IF EXISTS sync_conflict_item CASCADE;
DROP TABLE IF EXISTS sync_conflict CASCADE;

DROP TABLE IF EXISTS notice_read CASCADE;
DROP TABLE IF EXISTS notice_target_dept CASCADE;
DROP TABLE IF EXISTS notice CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS dept CASCADE;
DROP TABLE IF EXISTS role CASCADE;
DROP TABLE IF EXISTS sync_log CASCADE;



-- =========================
-- 3) role
-- =========================
CREATE TABLE role (
    id          CHAR(32)      NOT NULL,
    name        VARCHAR(50)   NOT NULL,
    create_time TIMESTAMP     NOT NULL,
    update_time TIMESTAMP     NOT NULL,
    CONSTRAINT pk_role PRIMARY KEY (id),
    CONSTRAINT uk_role_name UNIQUE (name)
);

-- =========================
-- 4) dept
-- =========================
CREATE TABLE dept (
    id          CHAR(32)       NOT NULL,
    name        VARCHAR(100)   NOT NULL,
    parent_id   CHAR(32),
    description VARCHAR(255),
    sort_order  INTEGER,
    status      SMALLINT       NOT NULL DEFAULT 1,
    create_time TIMESTAMP      NOT NULL,
    update_time TIMESTAMP      NOT NULL,
    CONSTRAINT pk_dept PRIMARY KEY (id),
    CONSTRAINT fk_dept_parent FOREIGN KEY (parent_id) REFERENCES dept(id)
);
CREATE INDEX idx_dept_parent ON dept(parent_id);

-- =========================
-- 5) users
-- =========================
CREATE TABLE users (
    id               CHAR(32)      NOT NULL,
    username         VARCHAR(50)   NOT NULL,
    password         VARCHAR(100)  NOT NULL,
    role_id          CHAR(32)      NOT NULL,
    dept_id          CHAR(32),
    nickname         VARCHAR(50),
    email            VARCHAR(100),
    phone            VARCHAR(20),
    avatar           VARCHAR(255),
    status           SMALLINT      NOT NULL DEFAULT 1,
    last_login_time  TIMESTAMP,
    create_time      TIMESTAMP     NOT NULL,
    update_time      TIMESTAMP     NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_user_username UNIQUE (username),
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT uk_user_phone UNIQUE (phone),
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT fk_user_dept FOREIGN KEY (dept_id) REFERENCES dept(id)
);
CREATE INDEX idx_user_role ON users(role_id);
CREATE INDEX idx_user_dept ON users(dept_id);

-- =========================
-- 6) notice
-- =========================
CREATE TABLE notice (
    id            CHAR(32)       NOT NULL,
    title         VARCHAR(200)   NOT NULL,
    content       TEXT           NOT NULL,
    publisher_id  CHAR(32)       NOT NULL,
    level         VARCHAR(20)    NOT NULL DEFAULT 'NORMAL',
    publish_time  TIMESTAMP,
    expire_time   TIMESTAMP,
    status        VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    view_count    BIGINT         NOT NULL DEFAULT 0,
    create_time   TIMESTAMP      NOT NULL,
    update_time   TIMESTAMP      NOT NULL,
    CONSTRAINT pk_notice PRIMARY KEY (id),
    CONSTRAINT fk_notice_publisher FOREIGN KEY (publisher_id) REFERENCES users(id)
);
CREATE INDEX idx_notice_status_publish ON notice(status, publish_time);
CREATE INDEX idx_notice_publisher ON notice(publisher_id);

-- =========================
-- 7) notice_target_dept
-- =========================
CREATE TABLE notice_target_dept (
    id          CHAR(32)    NOT NULL,
    notice_id   CHAR(32)    NOT NULL,
    dept_id     CHAR(32)    NOT NULL,
    create_time TIMESTAMP   NOT NULL,
    update_time TIMESTAMP   NOT NULL,
    CONSTRAINT pk_notice_target_dept PRIMARY KEY (id),
    CONSTRAINT fk_ntd_notice FOREIGN KEY (notice_id) REFERENCES notice(id),
    CONSTRAINT fk_ntd_dept FOREIGN KEY (dept_id) REFERENCES dept(id)
);
CREATE INDEX idx_ntd_notice ON notice_target_dept(notice_id);
CREATE INDEX idx_ntd_dept ON notice_target_dept(dept_id);

-- =========================
-- 8) notice_read
-- =========================
CREATE TABLE notice_read (
    id          CHAR(32)     NOT NULL,
    notice_id   CHAR(32)     NOT NULL,
    user_id     CHAR(32)     NOT NULL,
    read_time   TIMESTAMP    NOT NULL,
    device_type VARCHAR(20),
    create_time TIMESTAMP    NOT NULL,
    update_time TIMESTAMP    NOT NULL,
    CONSTRAINT pk_notice_read PRIMARY KEY (id),
    CONSTRAINT fk_nr_notice FOREIGN KEY (notice_id) REFERENCES notice(id),
    CONSTRAINT fk_nr_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_nr_notice_user ON notice_read(notice_id, user_id);
CREATE INDEX idx_nr_user ON notice_read(user_id);

-- =========================
-- 9) sync_log
-- =========================
CREATE TABLE sync_log (
    id          CHAR(32)      NOT NULL,
    entity_type VARCHAR(50)   NOT NULL,
    entity_id   VARCHAR(64)   NOT NULL,
    action      VARCHAR(20)   NOT NULL,
    source_db   VARCHAR(20)   NOT NULL,
    target_db   VARCHAR(20)   NOT NULL,
    status      VARCHAR(20)   NOT NULL,
    error_msg   TEXT,
    retry_count INTEGER       NOT NULL DEFAULT 0,
    create_time TIMESTAMP     NOT NULL,
    update_time TIMESTAMP     NOT NULL,
    CONSTRAINT pk_sync_log PRIMARY KEY (id),
    CONSTRAINT ck_sync_log_action CHECK (action IN ('CREATE','UPDATE','DELETE')),
    CONSTRAINT ck_sync_log_source_db CHECK (source_db IN ('MYSQL','PG','SQLSERVER')),
    CONSTRAINT ck_sync_log_target_db CHECK (target_db IN ('MYSQL','PG','SQLSERVER')),
    CONSTRAINT ck_sync_log_status CHECK (status IN ('SUCCESS','FAILED','CONFLICT','ERROR'))
);
CREATE INDEX idx_sync_log_status_create ON sync_log(status, create_time);
CREATE INDEX idx_sync_log_entity ON sync_log(entity_type, entity_id);
CREATE INDEX idx_sync_log_src_tgt ON sync_log(source_db, target_db);

-- =========================
-- 10) sync_conflict
-- =========================
CREATE TABLE sync_conflict (
    id                   CHAR(32)      NOT NULL,
    entity_type           VARCHAR(50)   NOT NULL,
    entity_id             VARCHAR(64)   NOT NULL,
    status               VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    conflict_type         VARCHAR(20)   NULL,
    first_seen_at         TIMESTAMP     NOT NULL,
    last_seen_at          TIMESTAMP     NOT NULL,
    last_checked_at       TIMESTAMP     NOT NULL,
    last_notified_at      TIMESTAMP     NULL,
    notify_count          INTEGER       NOT NULL DEFAULT 0,
    resolution_source_db  VARCHAR(20)   NULL,
    resolution_note       VARCHAR(512)  NULL,
    resolved_at           TIMESTAMP     NULL,
    create_time           TIMESTAMP     NOT NULL,
    update_time           TIMESTAMP     NOT NULL,
    CONSTRAINT pk_sync_conflict PRIMARY KEY (id),
    CONSTRAINT uk_sync_conflict_entity UNIQUE (entity_type, entity_id),
    CONSTRAINT ck_sync_conflict_status CHECK (status IN ('OPEN','RESOLVED','IGNORED')),
    CONSTRAINT ck_sync_conflict_type CHECK (conflict_type IS NULL OR conflict_type IN ('MISSING','MISMATCH')),
    CONSTRAINT ck_sync_conflict_resolution_db CHECK (resolution_source_db IS NULL OR resolution_source_db IN ('MYSQL','PG','SQLSERVER'))
);
CREATE INDEX idx_conflict_status_seen ON sync_conflict(status, last_seen_at);
CREATE INDEX idx_conflict_checked ON sync_conflict(last_checked_at);

-- =========================
-- 11) sync_conflict_item
-- =========================
CREATE TABLE sync_conflict_item (
    id              CHAR(32)     NOT NULL,
    conflict_id     CHAR(32)     NOT NULL,
    db_type         VARCHAR(20)  NOT NULL,
    exists_flag     SMALLINT     NOT NULL DEFAULT 0,
    row_hash        VARCHAR(64),
    row_version     VARCHAR(64),
    row_update_time TIMESTAMP,
    last_checked_at TIMESTAMP    NOT NULL,
    create_time     TIMESTAMP    NOT NULL,
    update_time     TIMESTAMP    NOT NULL,
    CONSTRAINT pk_sync_conflict_item PRIMARY KEY (id),
    CONSTRAINT uk_conflict_item UNIQUE (conflict_id, db_type),
    CONSTRAINT fk_conflict_item_conflict
        FOREIGN KEY (conflict_id) REFERENCES sync_conflict(id) ON DELETE CASCADE,
    CONSTRAINT ck_item_db_type CHECK (db_type IN ('MYSQL','PG','SQLSERVER')),
    CONSTRAINT ck_exists_flag CHECK (exists_flag IN (0,1))
);
CREATE INDEX idx_item_conflict ON sync_conflict_item(conflict_id);
CREATE INDEX idx_item_db_exists ON sync_conflict_item(db_type, exists_flag);

-- =========================
-- 12) notice_read view_count 触发器（保留）
-- =========================
CREATE OR REPLACE FUNCTION fn_notice_read_after_insert()
RETURNS trigger AS
$$
BEGIN
    UPDATE notice
    SET view_count = COALESCE(view_count, 0) + 1
    WHERE id = NEW.notice_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_notice_read_after_insert ON notice_read;

CREATE TRIGGER trg_notice_read_after_insert
AFTER INSERT ON notice_read
FOR EACH ROW
EXECUTE FUNCTION fn_notice_read_after_insert();

-- =========================
-- 13) 清理日志过程（保留）
-- =========================
DROP PROCEDURE IF EXISTS sp_clean_sync_log(INT, BIGINT);
CREATE OR REPLACE PROCEDURE sp_clean_sync_log(
    IN p_retain_days INT,
    IN p_max_count   BIGINT
)
LANGUAGE plpgsql
AS
$$
DECLARE
    v_retain_days INT    := COALESCE(p_retain_days, 90);
    v_max_count   BIGINT := COALESCE(p_max_count,   100000);
    v_threshold   TIMESTAMP;
    v_total       BIGINT;
    v_need_delete BIGINT;
BEGIN
    v_threshold := NOW() - (v_retain_days || ' days')::interval;

    DELETE FROM sync_log
    WHERE create_time < v_threshold;

    SELECT COUNT(*) INTO v_total FROM sync_log;

    IF v_total > v_max_count THEN
        v_need_delete := v_total - v_max_count;

        DELETE FROM sync_log
        WHERE id IN (
            SELECT id
            FROM sync_log
            ORDER BY create_time ASC
            LIMIT v_need_delete
        );
    END IF;
END;
$$;
