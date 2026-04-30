-- Users table
CREATE TABLE users (
                       id         BIGSERIAL    PRIMARY KEY,
                       username   VARCHAR(50)  NOT NULL UNIQUE,
                       email      VARCHAR(150) NOT NULL UNIQUE,
                       password   VARCHAR(255) NOT NULL,
                       role       VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
                       created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Companies (one row per domain)
CREATE TABLE companies (
                           id              BIGSERIAL    PRIMARY KEY,
                           domain          VARCHAR(255) NOT NULL UNIQUE,
                           company_name    VARCHAR(255),
                           trust_score     INTEGER      NOT NULL DEFAULT 50,
                           verdict         VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN',
                           domain_age_days INTEGER,
                           is_safe_browse  BOOLEAN      DEFAULT TRUE,
                           analysis_count  INTEGER      NOT NULL DEFAULT 0,
                           last_analyzed   TIMESTAMP    NOT NULL DEFAULT NOW(),
                           created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Each analysis run saved here
CREATE TABLE analyses (
                          id              BIGSERIAL PRIMARY KEY,
                          user_id         BIGINT    REFERENCES users(id)     ON DELETE SET NULL,
                          company_id      BIGINT    REFERENCES companies(id) ON DELETE CASCADE,
                          job_url         TEXT,
                          job_text        TEXT,
                          company_name    VARCHAR(255),
                          trust_score     INTEGER   NOT NULL,
                          verdict         VARCHAR(20) NOT NULL,
                          flags           JSONB     NOT NULL DEFAULT '[]',
                          score_breakdown JSONB     NOT NULL DEFAULT '{}',
                          created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Community scam reports
CREATE TABLE scam_reports (
                              id              BIGSERIAL    PRIMARY KEY,
                              user_id         BIGINT       REFERENCES users(id) ON DELETE SET NULL,
                              company_name    VARCHAR(255) NOT NULL,
                              domain          VARCHAR(255),
                              description     TEXT         NOT NULL,
                              stipend_offered VARCHAR(100),
                              experience      TEXT,
                              upvotes         INTEGER      NOT NULL DEFAULT 0,
                              verified        BOOLEAN      NOT NULL DEFAULT FALSE,
                              created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
                              updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Prevents double-upvoting
CREATE TABLE report_upvotes (
                                user_id   BIGINT NOT NULL REFERENCES users(id)        ON DELETE CASCADE,
                                report_id BIGINT NOT NULL REFERENCES scam_reports(id) ON DELETE CASCADE,
                                PRIMARY KEY (user_id, report_id)
);

-- Indexes for faster queries
CREATE INDEX idx_companies_domain    ON companies(domain);
CREATE INDEX idx_analyses_user       ON analyses(user_id);
CREATE INDEX idx_scam_reports_domain ON scam_reports(domain);
CREATE INDEX idx_scam_reports_votes  ON scam_reports(upvotes DESC);