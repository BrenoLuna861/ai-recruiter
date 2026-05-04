-- ============================================================
-- AI Recruiter — MySQL Init Script
-- Executado automaticamente na primeira inicialização do container
-- ============================================================

CREATE DATABASE IF NOT EXISTS ai_recruiter
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ai_recruiter;

-- ============================================================
-- Tabela: users
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(191) NOT NULL,
    password_hash VARCHAR(72)  NOT NULL,        -- BCrypt max 72 bytes
    role          ENUM('CANDIDATE','RECRUITER','ADMIN') NOT NULL DEFAULT 'CANDIDATE',
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    INDEX idx_users_role (role),
    INDEX idx_users_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Tabela: resumes
-- LONGTEXT para suportar currículos extensos (>65KB)
-- ============================================================
CREATE TABLE IF NOT EXISTS resumes (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    title            VARCHAR(255) NOT NULL,
    file_path        VARCHAR(500),
    file_type        VARCHAR(20),
    content          LONGTEXT,                   -- FIX: era TEXT, truncava em ~65KB
    overall_score    TINYINT UNSIGNED,            -- 0-100
    skills_score     TINYINT UNSIGNED,
    experience_score TINYINT UNSIGNED,
    format_score     TINYINT UNSIGNED,
    ats_score        TINYINT UNSIGNED,
    analysis_mongo_id VARCHAR(24),               -- ObjectId do MongoDB
    status           ENUM('PENDING','ANALYZING','DONE','ERROR') NOT NULL DEFAULT 'PENDING',
    is_active        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_resumes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_resumes_user (user_id),
    INDEX idx_resumes_status (status),
    INDEX idx_resumes_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Tabela: jobs
-- ============================================================
CREATE TABLE IF NOT EXISTS jobs (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    recruiter_id    BIGINT       NOT NULL,
    title           VARCHAR(255) NOT NULL,
    company         VARCHAR(191) NOT NULL,
    description     LONGTEXT     NOT NULL,
    requirements    TEXT,
    location        VARCHAR(191),
    salary_range    VARCHAR(80),
    job_type        ENUM('FULL_TIME','PART_TIME','CONTRACT','INTERNSHIP','REMOTE') DEFAULT 'FULL_TIME',
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_jobs_recruiter FOREIGN KEY (recruiter_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_jobs_recruiter (recruiter_id),
    INDEX idx_jobs_active (is_active),
    INDEX idx_jobs_type (job_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Tabela: applications
-- ============================================================
CREATE TABLE IF NOT EXISTS applications (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    job_id       BIGINT   NOT NULL,
    candidate_id BIGINT   NOT NULL,
    resume_id    BIGINT   NOT NULL,
    match_score  TINYINT UNSIGNED,
    ai_feedback  TEXT,
    status       ENUM('PENDING','REVIEWED','SHORTLISTED','REJECTED','HIRED') NOT NULL DEFAULT 'PENDING',
    applied_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_application (job_id, candidate_id),
    CONSTRAINT fk_app_job       FOREIGN KEY (job_id)       REFERENCES jobs(id)    ON DELETE CASCADE,
    CONSTRAINT fk_app_candidate FOREIGN KEY (candidate_id) REFERENCES users(id)   ON DELETE CASCADE,
    CONSTRAINT fk_app_resume    FOREIGN KEY (resume_id)    REFERENCES resumes(id) ON DELETE CASCADE,
    INDEX idx_app_job (job_id),
    INDEX idx_app_candidate (candidate_id),
    INDEX idx_app_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Permissões: usuário da aplicação com acesso mínimo necessário
-- (root só para DML, não DDL em produção)
-- ============================================================
-- Ajuste o usuário conforme MYSQL_USER no .env
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ai_recruiter.* TO 'airecruiter'@'%';
-- FLUSH PRIVILEGES;

-- ============================================================
-- Dados demo (removidos em produção — comente ou delete)
-- ============================================================
-- Senhas: demo123 → BCrypt hash (fator 12)
INSERT IGNORE INTO users (name, email, password_hash, role) VALUES
  ('Candidato Demo',   'candidato@demo.com',   '$2a$12$KIx7V.4rq3Bk1VvJOGE0peK1ZULaFcJD7LbFhTAC5.9LPTNmLkMuC', 'CANDIDATE'),
  ('Recrutador Demo',  'recrutador@demo.com',  '$2a$12$KIx7V.4rq3Bk1VvJOGE0peK1ZULaFcJD7LbFhTAC5.9LPTNmLkMuC', 'RECRUITER');
