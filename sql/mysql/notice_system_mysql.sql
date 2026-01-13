/* =========================
   0) 建库 + 切库
   ========================= */
CREATE DATABASE IF NOT EXISTS notice_system_mysql
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE notice_system_mysql;

-- =========================
-- 清理旧表（先子表后父表）
-- =========================
DROP TABLE IF EXISTS sync_conflict_item;
DROP TABLE IF EXISTS sync_conflict;

DROP TABLE IF EXISTS notice_read;
DROP TABLE IF EXISTS notice_target_dept;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS dept;
DROP TABLE IF EXISTS role;
DROP TABLE IF EXISTS sync_log;

-- =========================
-- role
-- =========================
CREATE TABLE role (
    id          CHAR(32)     NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    create_time DATETIME     NOT NULL,
    update_time DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- dept
-- =========================
CREATE TABLE dept (
    id          CHAR(32)      NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    parent_id   CHAR(32)      NULL,
    description VARCHAR(255)  NULL,
    sort_order  INT           NULL,
    status      TINYINT       NOT NULL DEFAULT 1,
    create_time DATETIME      NOT NULL,
    update_time DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_dept_parent (parent_id),
    CONSTRAINT fk_dept_parent
        FOREIGN KEY (parent_id) REFERENCES dept(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- users
-- =========================
CREATE TABLE users (
    id               CHAR(32)      NOT NULL,
    username         VARCHAR(50)   NOT NULL,
    `password`       VARCHAR(100)  NOT NULL,
    role_id          CHAR(32)      NOT NULL,
    dept_id          CHAR(32)      NULL,
    nickname         VARCHAR(50)   NULL,
    email            VARCHAR(100)  NULL,
    phone            VARCHAR(20)   NULL,
    avatar           VARCHAR(255)  NULL,
    status           TINYINT       NOT NULL DEFAULT 1,
    last_login_time  DATETIME      NULL,
    create_time      DATETIME      NOT NULL,
    update_time      DATETIME      NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_email (email),
    UNIQUE KEY uk_user_phone (phone),
    KEY idx_user_role (role_id),
    KEY idx_user_dept (dept_id),
    CONSTRAINT fk_user_role
        FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT fk_user_dept
        FOREIGN KEY (dept_id) REFERENCES dept(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- notice
-- =========================
CREATE TABLE notice (
    id            CHAR(32)      NOT NULL,
    title         VARCHAR(200)  NOT NULL,
    content       TEXT          NOT NULL,
    publisher_id  CHAR(32)      NOT NULL,
    `level`       VARCHAR(20)   NOT NULL DEFAULT 'NORMAL',
    publish_time  DATETIME      NULL,
    expire_time   DATETIME      NULL,
    `status`      VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    view_count    BIGINT        NOT NULL DEFAULT 0,
    create_time   DATETIME      NOT NULL,
    update_time   DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notice_status_publish (`status`, publish_time),
    KEY idx_notice_publisher (publisher_id),
    CONSTRAINT fk_notice_publisher
        FOREIGN KEY (publisher_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- notice_target_dept
-- =========================
CREATE TABLE notice_target_dept (
    id          CHAR(32)    NOT NULL,
    notice_id   CHAR(32)    NOT NULL,
    dept_id     CHAR(32)    NOT NULL,
    create_time DATETIME    NOT NULL,
    update_time DATETIME    NOT NULL,
    PRIMARY KEY (id),
    KEY idx_ntd_notice (notice_id),
    KEY idx_ntd_dept (dept_id),
    CONSTRAINT fk_ntd_notice
        FOREIGN KEY (notice_id) REFERENCES notice(id),
    CONSTRAINT fk_ntd_dept
        FOREIGN KEY (dept_id) REFERENCES dept(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- notice_read
-- =========================
CREATE TABLE notice_read (
    id          CHAR(32)     NOT NULL,
    notice_id   CHAR(32)     NOT NULL,
    user_id     CHAR(32)     NOT NULL,
    read_time   DATETIME     NOT NULL,
    device_type VARCHAR(20)  NULL,
    create_time DATETIME     NOT NULL,
    update_time DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_nr_notice_user (notice_id, user_id),
    KEY idx_nr_user (user_id),
    CONSTRAINT fk_nr_notice
        FOREIGN KEY (notice_id) REFERENCES notice(id),
    CONSTRAINT fk_nr_user
        FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- sync_log
-- =========================
CREATE TABLE sync_log (
    id          CHAR(32)     NOT NULL,
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   VARCHAR(64)  NOT NULL,
    action      VARCHAR(20)  NOT NULL,
    source_db   VARCHAR(20)  NOT NULL,
    target_db   VARCHAR(20)  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    error_msg   TEXT         NULL,
    retry_count INT          NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL,
    update_time DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_sync_log_status_create (status, create_time),
    KEY idx_sync_log_entity (entity_type, entity_id),
    KEY idx_sync_log_src_tgt (source_db, target_db),
    CONSTRAINT ck_sync_log_action CHECK (action IN ('CREATE','UPDATE','DELETE')),
    CONSTRAINT ck_sync_log_db_source CHECK (source_db IN ('MYSQL','PG','SQLSERVER')),
    CONSTRAINT ck_sync_log_db_target CHECK (target_db IN ('MYSQL','PG','SQLSERVER')),
    CONSTRAINT ck_sync_log_status CHECK (status IN ('SUCCESS','FAILED','CONFLICT','ERROR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- sync_conflict
-- =========================
CREATE TABLE sync_conflict (
    id                   CHAR(32)     NOT NULL,
    entity_type           VARCHAR(50)  NOT NULL,
    entity_id             VARCHAR(64)  NOT NULL,
    status               VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    conflict_type         VARCHAR(20)  NULL,
    first_seen_at         DATETIME     NOT NULL,
    last_seen_at          DATETIME     NOT NULL,
    last_checked_at       DATETIME     NOT NULL,
    last_notified_at      DATETIME     NULL,
    notify_count          INT          NOT NULL DEFAULT 0,
    resolution_source_db  VARCHAR(20)  NULL,
    resolution_note       VARCHAR(512) NULL,
    resolved_at           DATETIME     NULL,
    create_time           DATETIME     NOT NULL,
    update_time           DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_conflict_entity (entity_type, entity_id),
    KEY idx_conflict_status_seen (status, last_seen_at),
    KEY idx_conflict_checked (last_checked_at),
    CONSTRAINT ck_sync_conflict_status CHECK (status IN ('OPEN','RESOLVED','IGNORED')),
    CONSTRAINT ck_sync_conflict_type CHECK (conflict_type IS NULL OR conflict_type IN ('MISSING','MISMATCH')),
    CONSTRAINT ck_sync_conflict_resolution_db CHECK (resolution_source_db IS NULL OR resolution_source_db IN ('MYSQL','PG','SQLSERVER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- sync_conflict_item
-- =========================
CREATE TABLE sync_conflict_item (
    id              CHAR(32)     NOT NULL,
    conflict_id     CHAR(32)     NOT NULL,
    db_type         VARCHAR(20)  NOT NULL,
    exists_flag     TINYINT      NOT NULL DEFAULT 0,
    row_hash        VARCHAR(64)  NULL,
    row_version     VARCHAR(64)  NULL,
    row_update_time DATETIME     NULL,
    last_checked_at DATETIME     NOT NULL,
    create_time     DATETIME     NOT NULL,
    update_time     DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_conflict_db (conflict_id, db_type),
    KEY idx_item_conflict (conflict_id),
    KEY idx_item_db_exists (db_type, exists_flag),
    CONSTRAINT fk_conflict_item_conflict
        FOREIGN KEY (conflict_id) REFERENCES sync_conflict(id) ON DELETE CASCADE,
    CONSTRAINT ck_item_db_type CHECK (db_type IN ('MYSQL','PG','SQLSERVER')),
    CONSTRAINT ck_item_exists CHECK (exists_flag IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- trigger: notice_read AFTER INSERT
-- =========================
DROP TRIGGER IF EXISTS trg_notice_read_after_insert;
DELIMITER $$

CREATE TRIGGER trg_notice_read_after_insert
AFTER INSERT ON notice_read
FOR EACH ROW
BEGIN
    UPDATE notice
    SET view_count = IFNULL(view_count, 0) + 1
    WHERE id = NEW.notice_id;
END$$

DELIMITER ;

-- =========================
-- procedure: sp_clean_sync_log
-- =========================
DROP PROCEDURE IF EXISTS sp_clean_sync_log;
DELIMITER $$

CREATE PROCEDURE sp_clean_sync_log(
    IN p_retain_days INT,
    IN p_max_count   BIGINT
)
BEGIN
    DECLARE v_retain_days INT;
    DECLARE v_max_count   BIGINT;
    DECLARE v_threshold   DATETIME;
    DECLARE v_total       BIGINT;
    DECLARE v_need_delete BIGINT;

    SET v_retain_days = IFNULL(p_retain_days, 90);
    SET v_max_count   = IFNULL(p_max_count,   100000);

    SET v_threshold = DATE_SUB(NOW(), INTERVAL v_retain_days DAY);

    DELETE FROM sync_log
    WHERE create_time < v_threshold;

    SELECT COUNT(*) INTO v_total FROM sync_log;

    IF v_total > v_max_count THEN
        SET v_need_delete = v_total - v_max_count;

        DELETE FROM sync_log
        ORDER BY create_time ASC
        LIMIT v_need_delete;
    END IF;
END$$

DELIMITER ;
