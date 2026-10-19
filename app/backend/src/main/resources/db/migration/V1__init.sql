-- ============================================================================
-- フロンティア社員情報管理システム 初期スキーマ
--
-- 対応: DB設計書.md
-- DBMS: PostgreSQL 16
--
-- 作成順は外部キーの依存関係に従う:
--   拠点系マスタ -> 独立マスタ -> employees -> 中間テーブル -> 業務テーブル -> 権限
--
-- 方針（DB設計書.md「設計方針」より）:
--   - 命名は snake_case。テーブル名は複数形、主キーは <単数形>_id
--   - 日時は TIMESTAMPTZ、日付のみの項目は DATE
--   - 社員のみ論理削除（status = 0）。マスタは物理削除だが参照中は FK で拒否される
--   - 外部キーは原則 ON DELETE RESTRICT（auth_tokens のみ CASCADE）
--   - 氏名の並び順のため name / name_kana にカラム単位で COLLATE "ja-x-icu"（決定事項5）
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 1. 拠点系マスタ（sites -> floors -> seats の順に依存）
-- ----------------------------------------------------------------------------

CREATE TABLE sites (
    site_id    BIGSERIAL    PRIMARY KEY,
    site_name  VARCHAR(100) NOT NULL,
    address    VARCHAR(255),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_sites_site_name UNIQUE (site_name)
);

COMMENT ON TABLE sites IS '拠点（本社、技術センターなど）';


CREATE TABLE floors (
    floor_id      BIGSERIAL   PRIMARY KEY,
    site_id       BIGINT      NOT NULL,
    floor_name    VARCHAR(50) NOT NULL,
    floor_number  SMALLINT    NOT NULL,
    map_image_url VARCHAR(500),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_floors_site     FOREIGN KEY (site_id) REFERENCES sites (site_id) ON DELETE RESTRICT,
    -- 同一拠点に同じ階は作れない
    CONSTRAINT uq_floors_site_num UNIQUE (site_id, floor_number)
);

COMMENT ON TABLE floors IS 'フロア。floor_number は並べ替え用で、地下は負数';


CREATE TABLE seats (
    seat_id     BIGSERIAL   PRIMARY KEY,
    floor_id    BIGINT      NOT NULL,
    seat_number VARCHAR(50) NOT NULL,
    position_x  INTEGER     NOT NULL,
    position_y  INTEGER     NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_seats_floor      FOREIGN KEY (floor_id) REFERENCES floors (floor_id) ON DELETE RESTRICT,
    CONSTRAINT uq_seats_floor_num  UNIQUE (floor_id, seat_number)
);

COMMENT ON TABLE seats IS '座席。position_x / position_y はマップ画像上の座標（px）';
COMMENT ON COLUMN seats.seat_id IS '空席判定は employees.seat_id を参照する。seats 側に在席フラグは持たない（二重管理を避けるため）';


-- ----------------------------------------------------------------------------
-- 2. 独立マスタ（他テーブルに依存しない）
-- ----------------------------------------------------------------------------

CREATE TABLE departments (
    department_id   BIGSERIAL    PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_departments_name UNIQUE (department_name)
);

COMMENT ON TABLE departments IS '部署';


CREATE TABLE positions (
    position_id   BIGSERIAL    PRIMARY KEY,
    position_name VARCHAR(100) NOT NULL,
    rank          SMALLINT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_positions_name UNIQUE (position_name)
);

COMMENT ON TABLE positions IS '役職。rank は序列（小さいほど上位）で並べ替えに使う';


CREATE TABLE employment_types (
    employment_type_id   BIGSERIAL   PRIMARY KEY,
    employment_type_name VARCHAR(50) NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_employment_types_name UNIQUE (employment_type_name)
);

COMMENT ON TABLE employment_types IS '雇用形態（正社員、契約社員、派遣など）';


CREATE TABLE skills (
    skill_id   BIGSERIAL    PRIMARY KEY,
    skill_name VARCHAR(100) NOT NULL,
    category   VARCHAR(50),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_skills_name UNIQUE (skill_name)
);

COMMENT ON TABLE skills IS 'スキルマスタ。category は分類（言語、フレームワーク、DBなど）';


CREATE TABLE certifications (
    certification_id   BIGSERIAL    PRIMARY KEY,
    certification_name VARCHAR(150) NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_certifications_name UNIQUE (certification_name)
);

COMMENT ON TABLE certifications IS '資格マスタ';


CREATE TABLE projects (
    project_id   BIGSERIAL    PRIMARY KEY,
    project_name VARCHAR(200) NOT NULL,
    start_date   DATE,
    end_date     DATE,
    description  TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_projects_period CHECK (end_date IS NULL OR start_date IS NULL OR start_date <= end_date)
);

COMMENT ON TABLE projects IS 'プロジェクトマスタ。end_date が NULL なら継続中';


-- ----------------------------------------------------------------------------
-- 3. employees（システムの中心。上記すべてのマスタを参照する）
-- ----------------------------------------------------------------------------

CREATE TABLE employees (
    employee_id        BIGSERIAL    PRIMARY KEY,
    employee_code      VARCHAR(20)  NOT NULL,
    name               VARCHAR(100) COLLATE "ja-x-icu" NOT NULL,
    name_kana          VARCHAR(100) COLLATE "ja-x-icu",
    email              VARCHAR(255) NOT NULL,
    tel                VARCHAR(20),
    password_hash      VARCHAR(255),
    image_url          VARCHAR(500),
    hobby              VARCHAR(255),
    self_pr            TEXT,
    hire_date          DATE         NOT NULL,
    department_id      BIGINT       NOT NULL,
    position_id        BIGINT,
    employment_type_id BIGINT       NOT NULL,
    seat_id            BIGINT,
    status             SMALLINT     NOT NULL DEFAULT 1,
    role               VARCHAR(20)  NOT NULL DEFAULT 'EMPLOYEE',
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_employees_code  UNIQUE (employee_code),
    CONSTRAINT uq_employees_email UNIQUE (email),
    -- 1座席に1人まで。座席の二重割当を DB 制約で防ぐ（API 6.1 / 6.2 の CONFLICT(409)）
    -- NULL は重複と見なされないため、未着席の社員は何人でも持てる
    CONSTRAINT uq_employees_seat  UNIQUE (seat_id),

    CONSTRAINT ck_employees_status CHECK (status IN (0, 1)),
    CONSTRAINT ck_employees_role   CHECK (role IN ('EMPLOYEE', 'ADMIN')),

    CONSTRAINT fk_employees_department      FOREIGN KEY (department_id)      REFERENCES departments (department_id)           ON DELETE RESTRICT,
    CONSTRAINT fk_employees_position        FOREIGN KEY (position_id)        REFERENCES positions (position_id)               ON DELETE RESTRICT,
    CONSTRAINT fk_employees_employment_type FOREIGN KEY (employment_type_id) REFERENCES employment_types (employment_type_id) ON DELETE RESTRICT,
    CONSTRAINT fk_employees_seat            FOREIGN KEY (seat_id)            REFERENCES seats (seat_id)                       ON DELETE RESTRICT
);

COMMENT ON TABLE  employees            IS '社員。論理削除のみ（status = 0）。退職時は seat_id も NULL にして座席を空ける';
COMMENT ON COLUMN employees.email      IS 'ログインID。ADMIN が社員登録時に設定する（決定事項1, 2）。機密項目';
COMMENT ON COLUMN employees.tel        IS '機密項目。本人または ADMIN のみ閲覧可';
COMMENT ON COLUMN employees.role       IS 'EMPLOYEE / ADMIN。人事部の全員と社長に ADMIN を付与する（決定事項3, 6）';
COMMENT ON COLUMN employees.status     IS '0:退職・無効（論理削除） / 1:在籍';

-- 部署・役職での絞り込み（API 2.1）
CREATE INDEX idx_employees_department ON employees (department_id);
CREATE INDEX idx_employees_position   ON employees (position_id);
-- 在籍者のみの検索が既定のため、ほぼ全クエリで使う
CREATE INDEX idx_employees_status     ON employees (status);
-- キーワード検索。部分一致が重くなったら pg_trgm の GIN インデックスを検討（未確定事項3）
CREATE INDEX idx_employees_name       ON employees (name);
CREATE INDEX idx_employees_name_kana  ON employees (name_kana);


-- ----------------------------------------------------------------------------
-- 4. 中間テーブル（複合主キーで重複登録を DB 側で防ぐ）
-- ----------------------------------------------------------------------------

CREATE TABLE employee_skills (
    employee_id      BIGINT       NOT NULL,
    skill_id         BIGINT       NOT NULL,
    proficiency      SMALLINT     NOT NULL,
    experience_years NUMERIC(4,1),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- 同一スキルの重複登録を防ぐ（API 6.4 の CONFLICT(409)）
    CONSTRAINT pk_employee_skills PRIMARY KEY (employee_id, skill_id),
    CONSTRAINT ck_employee_skills_proficiency CHECK (proficiency BETWEEN 1 AND 3),
    CONSTRAINT ck_employee_skills_years       CHECK (experience_years IS NULL OR experience_years >= 0),
    CONSTRAINT fk_employee_skills_employee FOREIGN KEY (employee_id) REFERENCES employees (employee_id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_skills_skill    FOREIGN KEY (skill_id)    REFERENCES skills (skill_id)       ON DELETE RESTRICT
);

COMMENT ON TABLE  employee_skills             IS '社員とスキルの紐付け';
COMMENT ON COLUMN employee_skills.proficiency IS '1:初級 / 2:中級 / 3:上級';

-- スキルからの逆引き検索（API 2.1 の skill_id 検索）
CREATE INDEX idx_employee_skills_skill ON employee_skills (skill_id);


CREATE TABLE employee_certifications (
    employee_id      BIGINT      NOT NULL,
    certification_id BIGINT      NOT NULL,
    acquire_date     DATE        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_employee_certifications PRIMARY KEY (employee_id, certification_id),
    CONSTRAINT fk_employee_certifications_employee      FOREIGN KEY (employee_id)      REFERENCES employees (employee_id)           ON DELETE RESTRICT,
    CONSTRAINT fk_employee_certifications_certification FOREIGN KEY (certification_id) REFERENCES certifications (certification_id) ON DELETE RESTRICT
);

COMMENT ON TABLE employee_certifications IS '社員と資格の紐付け';

CREATE INDEX idx_employee_certifications_certification ON employee_certifications (certification_id);


CREATE TABLE employee_projects (
    employee_id     BIGINT       NOT NULL,
    project_id      BIGINT       NOT NULL,
    role_in_project VARCHAR(100),
    start_date      DATE,
    end_date        DATE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_employee_projects PRIMARY KEY (employee_id, project_id),
    CONSTRAINT ck_employee_projects_period CHECK (end_date IS NULL OR start_date IS NULL OR start_date <= end_date),
    CONSTRAINT fk_employee_projects_employee FOREIGN KEY (employee_id) REFERENCES employees (employee_id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_projects_project  FOREIGN KEY (project_id)  REFERENCES projects (project_id)   ON DELETE RESTRICT
);

COMMENT ON TABLE employee_projects IS '社員と担当プロジェクトの紐付け。end_date が NULL なら継続中';

CREATE INDEX idx_employee_projects_project ON employee_projects (project_id);


-- ----------------------------------------------------------------------------
-- 5. 業務テーブル
-- ----------------------------------------------------------------------------

CREATE TABLE auth_tokens (
    token_id        BIGSERIAL   PRIMARY KEY,
    token_hash      VARCHAR(64) NOT NULL,
    employee_id     BIGINT      NOT NULL,
    token_type      VARCHAR(10) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    parent_token_id BIGINT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_auth_tokens_hash UNIQUE (token_hash),
    CONSTRAINT ck_auth_tokens_type CHECK (token_type IN ('ACCESS', 'REFRESH')),
    -- 社員が物理削除された場合はトークンも消す
    CONSTRAINT fk_auth_tokens_employee FOREIGN KEY (employee_id)     REFERENCES employees (employee_id)    ON DELETE CASCADE,
    -- REFRESH を削除したら、そこから発行された ACCESS も消える
    CONSTRAINT fk_auth_tokens_parent   FOREIGN KEY (parent_token_id) REFERENCES auth_tokens (token_id)     ON DELETE CASCADE
);

COMMENT ON TABLE  auth_tokens                 IS 'opaque token 方式のため発行済みトークンを管理する。有効判定は revoked_at IS NULL AND expires_at > now()';
COMMENT ON COLUMN auth_tokens.token_hash      IS 'トークンの SHA-256 ハッシュ（16進64文字）。平文は保存しない（DB が漏れてもなりすまされないようにするため）';
COMMENT ON COLUMN auth_tokens.parent_token_id IS 'ACCESS の場合、発行元の REFRESH トークン。ログアウト時の連鎖失効に使う';

-- 毎リクエストで検索するため必須
CREATE INDEX idx_auth_tokens_hash     ON auth_tokens (token_hash);
CREATE INDEX idx_auth_tokens_employee ON auth_tokens (employee_id);
-- 期限切れレコードの定期削除で使う
CREATE INDEX idx_auth_tokens_expires  ON auth_tokens (expires_at);


CREATE TABLE thanks_messages (
    message_id        BIGSERIAL    PRIMARY KEY,
    from_employee_id  BIGINT       NOT NULL,
    to_employee_id    BIGINT       NOT NULL,
    body              VARCHAR(300) NOT NULL,
    sent_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- 自分宛て送信を防ぐ（API 8.1 の VALIDATION_ERROR(422)）
    CONSTRAINT ck_thanks_messages_not_self CHECK (from_employee_id <> to_employee_id),
    CONSTRAINT ck_thanks_messages_body     CHECK (length(body) BETWEEN 1 AND 300),
    CONSTRAINT fk_thanks_messages_from FOREIGN KEY (from_employee_id) REFERENCES employees (employee_id) ON DELETE RESTRICT,
    CONSTRAINT fk_thanks_messages_to   FOREIGN KEY (to_employee_id)   REFERENCES employees (employee_id) ON DELETE RESTRICT
);

COMMENT ON TABLE thanks_messages IS 'サンクスメッセージ。編集・削除しない想定のため updated_at を持たない';

-- タイムラインは新着順固定
CREATE INDEX idx_thanks_messages_sent_at ON thanks_messages (sent_at DESC);
CREATE INDEX idx_thanks_messages_from    ON thanks_messages (from_employee_id);
CREATE INDEX idx_thanks_messages_to      ON thanks_messages (to_employee_id);


CREATE TABLE change_requests (
    request_id         BIGSERIAL    PRIMARY KEY,
    target_employee_id BIGINT       NOT NULL,
    target_field       VARCHAR(50)  NOT NULL,
    before_value       VARCHAR(255),
    after_value        VARCHAR(255) NOT NULL,
    reason             VARCHAR(500),
    status             SMALLINT     NOT NULL DEFAULT 0,
    requested_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reviewed_by        BIGINT,
    reviewed_at        TIMESTAMPTZ,
    review_comment     VARCHAR(500),

    CONSTRAINT ck_change_requests_status CHECK (status IN (0, 1, 2)),
    CONSTRAINT fk_change_requests_target   FOREIGN KEY (target_employee_id) REFERENCES employees (employee_id) ON DELETE RESTRICT,
    CONSTRAINT fk_change_requests_reviewer FOREIGN KEY (reviewed_by)        REFERENCES employees (employee_id) ON DELETE RESTRICT
);

COMMENT ON TABLE  change_requests              IS '基本情報の変更申請。申請対象はログイン中の本人に固定';
COMMENT ON COLUMN change_requests.before_value IS '申請時にサーバが現在値を取得して記録する（改ざん防止）';
COMMENT ON COLUMN change_requests.status       IS '0:申請中 / 1:承認 / 2:却下';

-- 同じ社員・同じ項目で申請中のものは1件まで（API 9.1 の CONFLICT(409)）
-- 処理済み（status 1 / 2）は何件でも残せるため部分 UNIQUE にする
CREATE UNIQUE INDEX uq_change_requests_pending
    ON change_requests (target_employee_id, target_field)
    WHERE status = 0;

CREATE INDEX idx_change_requests_status ON change_requests (status);
CREATE INDEX idx_change_requests_target ON change_requests (target_employee_id);


CREATE TABLE import_jobs (
    job_id       VARCHAR(50)  PRIMARY KEY,
    executed_by  BIGINT       NOT NULL,
    file_name    VARCHAR(255),
    status       VARCHAR(20)  NOT NULL,
    total_rows   INTEGER,
    success_rows INTEGER,
    error_rows   INTEGER,
    errors       JSONB,
    started_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    finished_at  TIMESTAMPTZ,

    CONSTRAINT ck_import_jobs_status CHECK (status IN ('processing', 'completed', 'failed')),
    CONSTRAINT fk_import_jobs_executed_by FOREIGN KEY (executed_by) REFERENCES employees (employee_id) ON DELETE RESTRICT
);

COMMENT ON TABLE  import_jobs        IS 'CSV 一括インポートのジョブ。job_id は imp_20260727_001 形式';
COMMENT ON COLUMN import_jobs.errors IS 'エラー詳細 [{"row":34,"reason":"..."}]。件数・構造が可変なので JSONB（未確定事項2）';


-- ----------------------------------------------------------------------------
-- 6. 権限テーブル
--
-- 前期スコープでは使用しない（DB設計書.md 決定事項3）。
-- 権限制御は employees.role（EMPLOYEE / ADMIN）の2値のみで行い、
-- 人事部の全員と社長に ADMIN を付与する。
--
-- テーブルだけ先に作っておくのは、後から使い始めるときに
-- マイグレーションを追加しなくて済むようにするため。初期データは投入しない。
-- ----------------------------------------------------------------------------

CREATE TABLE permissions (
    permission_id   BIGSERIAL    PRIMARY KEY,
    permission_code VARCHAR(50)  NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_permissions_code UNIQUE (permission_code)
);

COMMENT ON TABLE permissions IS '権限マスタ。前期では未使用（空のまま）。使う場合のコード案は DB設計書.md 8.1 を参照';


CREATE TABLE permission_assignments (
    assignment_id BIGSERIAL   PRIMARY KEY,
    permission_id BIGINT      NOT NULL,
    target_type   VARCHAR(20) NOT NULL,
    target_id     BIGINT      NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_permission_assignments UNIQUE (permission_id, target_type, target_id),
    CONSTRAINT ck_permission_assignments_target_type CHECK (target_type IN ('DEPT', 'POSITION', 'EMPLOYEE')),
    CONSTRAINT fk_permission_assignments_permission  FOREIGN KEY (permission_id) REFERENCES permissions (permission_id) ON DELETE RESTRICT
);

COMMENT ON TABLE  permission_assignments           IS '権限の付与先。前期では未使用（空のまま）';
COMMENT ON COLUMN permission_assignments.target_id IS 'target_type により参照先テーブルが変わるため外部キーを張れない（ポリモーフィック関連）。整合性はアプリ側で担保する。決定事項4';
