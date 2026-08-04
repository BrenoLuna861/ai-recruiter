-- ============================================================
-- Migracao: token de reset em claro -> SHA-256
-- Rodar no MySQL do Railway ANTES de subir o deploy.
-- ============================================================
--
-- POR QUE UM DROP E NAO UM ALTER:
-- A tabela antiga tem `token VARCHAR NOT NULL`. Com ddl-auto: update o
-- Hibernate ADICIONA as colunas novas (token_hash, used_at, request_ip,
-- created_at) mas NAO remove a antiga nem afrouxa o NOT NULL dela. Resultado:
-- todo INSERT novo falharia, porque ninguem preenche mais `token`.
--
-- Perder o conteudo da tabela e inofensivo: sao links de recuperacao
-- temporarios. Quem estiver com um link aberto no e-mail so precisa pedir
-- outro. Nenhum dado de usuario e afetado.

DROP TABLE IF EXISTS password_reset_tokens;

-- O Hibernate recria a tabela no proximo boot, ja no formato novo:
--
--   id          BIGINT AUTO_INCREMENT PRIMARY KEY
--   token_hash  VARCHAR(64)  NOT NULL UNIQUE   -- SHA-256 hex do token
--   email       VARCHAR(191) NOT NULL
--   expires_at  DATETIME     NOT NULL
--   used_at     DATETIME     NULL              -- null = ainda nao usado
--   request_ip  VARCHAR(45)  NULL              -- auditoria
--   created_at  DATETIME     NOT NULL
--
-- + indices em (email) e (expires_at)

-- ------------------------------------------------------------
-- Conferencia depois do primeiro boot da versao nova:
-- ------------------------------------------------------------
-- SHOW CREATE TABLE password_reset_tokens;
--
-- Esperado: existir token_hash e NAO existir mais a coluna token.
