# フロンティア社員情報管理システム DB設計書

- チーム名: チームA
- 作成者: 浅野（API設計・DB設計担当）
- DBMS: PostgreSQL 16

本書は [API設計仕様書.md](API設計仕様書.md) に対応するテーブル設計を定義する。

---

## 設計方針

| 項目 | 方針 |
| --- | --- |
| 命名規則 | すべて snake_case。テーブル名は複数形（`employees`）、主キーは `<単数形>_id`（`employee_id`） |
| 主キー | `BIGSERIAL`（自動採番）。中間テーブルのみ複合主キー |
| 文字コード | UTF-8（`UTF8`）。照合順序は DB 全体ではなく `employees.name` / `employees.name_kana` にカラム単位で `COLLATE "ja-x-icu"` を指定する（決定事項5） |
| 日時型 | `TIMESTAMPTZ`（タイムゾーン付き）。アプリ側では JST として扱う |
| 日付型 | `DATE`（時刻を持たない項目） |
| 共通カラム | 全テーブルに `created_at` / `updated_at` を持たせる（中間テーブル・ログ系を除く） |
| 削除方針 | **社員のみ論理削除**（`status = 0`）。マスタは物理削除だが、参照中は外部キー制約で拒否される |
| 外部キー | 原則 `ON DELETE RESTRICT`。参照が残っている親は消せない（API の CONFLICT(409) に対応） |
| 真偽値 | `BOOLEAN` を使う。区分値は `SMALLINT`（API設計仕様書の「共通の値定義」に準拠） |

---

## ER図

```mermaid
erDiagram
    departments      ||--o{ employees : "所属"
    positions        ||--o{ employees : "役職"
    employment_types ||--o{ employees : "雇用形態"
    seats            ||--o| employees : "着席(1対1)"

    sites  ||--o{ floors : "拠点内"
    floors ||--o{ seats  : "フロア内"

    employees ||--o{ employee_skills         : ""
    skills    ||--o{ employee_skills         : ""
    employees ||--o{ employee_certifications : ""
    certifications ||--o{ employee_certifications : ""
    employees ||--o{ employee_projects       : ""
    projects  ||--o{ employee_projects       : ""

    employees ||--o{ auth_tokens : "トークン発行"

    employees ||--o{ thanks_messages : "送信"
    employees ||--o{ thanks_messages : "受信"

    employees ||--o{ change_requests : "申請対象"
    employees ||--o{ change_requests : "承認者"

    employees ||--o{ import_jobs : "実行者"

    permissions ||--o{ permission_assignments : "付与"
```

**テーブル数: 19**

| 分類 | テーブル |
| --- | --- |
| 社員（1） | `employees` |
| マスタ（9） | `departments` `positions` `sites` `floors` `seats` `employment_types` `skills` `certifications` `projects` |
| 中間（3） | `employee_skills` `employee_certifications` `employee_projects` |
| 認証（1） | `auth_tokens` |
| 機能（4） | `thanks_messages` `change_requests` `import_jobs` |
| 権限（2） | `permissions` `permission_assignments` |

---

## 1. employees（社員）

システムの中心となるテーブル。API設計仕様書の 2〜6章に対応。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| `employee_id` | BIGSERIAL | × | 自動採番 | 主キー |
| `employee_code` | VARCHAR(20) | × | | 社員番号（例 `EMP00123`）。アカウント作成時の本人確認に使う。UNIQUE |
| `name` | VARCHAR(100) | × | | 氏名。`COLLATE "ja-x-icu"`（決定事項5） |
| `name_kana` | VARCHAR(100) | ○ | | 氏名カナ。かな検索用。`COLLATE "ja-x-icu"`（決定事項5） |
| `email` | VARCHAR(255) | × | | メールアドレス。**ログインID**。UNIQUE。ADMINが社員登録時に設定する。**機密項目**（本人・ADMINのみ閲覧可） |
| `tel` | VARCHAR(20) | ○ | | 電話番号。**機密項目** |
| `password_hash` | VARCHAR(255) | ○ | | BCrypt ハッシュ。アカウント未作成の間は NULL |
| `image_url` | VARCHAR(500) | ○ | | 顔写真URL。API 5.2 のアップロードでサーバが払い出す |
| `hobby` | VARCHAR(255) | ○ | | 趣味。本人が編集可 |
| `self_pr` | TEXT | ○ | | 自己PR。本人が編集可 |
| `hire_date` | DATE | × | | 入社日 |
| `department_id` | BIGINT | × | | → `departments` |
| `position_id` | BIGINT | ○ | | → `positions` |
| `employment_type_id` | BIGINT | × | | → `employment_types` |
| `seat_id` | BIGINT | ○ | | → `seats`。**UNIQUE**（1座席に1人まで） |
| `status` | SMALLINT | × | 1 | 0:退職・無効（論理削除） / 1:在籍 |
| `role` | VARCHAR(20) | × | 'EMPLOYEE' | `EMPLOYEE` / `ADMIN`。**人事部および社長に `ADMIN` を付与する**（決定事項3） |
| `created_at` | TIMESTAMPTZ | × | now() | |
| `updated_at` | TIMESTAMPTZ | × | now() | |

**制約**

- UNIQUE: `employee_code`, `email`, `seat_id`
- CHECK: `status IN (0, 1)` / `role IN ('EMPLOYEE', 'ADMIN')`
- FK: `department_id`, `position_id`, `employment_type_id`, `seat_id`（すべて `ON DELETE RESTRICT`）

**インデックス**

| 対象 | 理由 |
| --- | --- |
| `department_id` | 部署での絞り込み（API 2.1） |
| `position_id` | 役職での絞り込み |
| `status` | 在籍者のみの検索が既定のため、ほぼ全クエリで使う |
| `name`, `name_kana` | キーワード検索。部分一致には `pg_trgm` の GIN インデックスを検討 |

**補足**

- `seat_id` を employees 側に持たせ UNIQUE を張ることで、**座席の二重割当をDB制約で防ぐ**（API 6.1 / 6.2 の CONFLICT(409)）。
- 論理削除（API 6.3）は `status = 0` に更新し、あわせて `seat_id` を NULL にする。座席を空けるため。

---

## 2. マスタテーブル

### 2.1 departments（部署）

| カラム | 型 | NULL | 説明 |
| --- | --- | --- | --- |
| `department_id` | BIGSERIAL | × | 主キー |
| `department_name` | VARCHAR(100) | × | 部署名。UNIQUE |
| `created_at` / `updated_at` | TIMESTAMPTZ | × | |

### 2.2 positions（役職）

| カラム | 型 | NULL | 説明 |
| --- | --- | --- | --- |
| `position_id` | BIGSERIAL | × | 主キー |
| `position_name` | VARCHAR(100) | × | 役職名（部長、課長、主任 など）。UNIQUE |
| `rank` | SMALLINT | ○ | 序列。並べ替え用（小さいほど上位） |
| `created_at` / `updated_at` | TIMESTAMPTZ | × | |

### 2.3 sites（拠点）

| カラム | 型 | NULL | 説明 |
| --- | --- | --- | --- |
| `site_id` | BIGSERIAL | × | 主キー |
| `site_name` | VARCHAR(100) | × | 拠点名（本社、技術センター など）。UNIQUE |
| `address` | VARCHAR(255) | ○ | 住所 |
| `created_at` / `updated_at` | TIMESTAMPTZ | × | |

### 2.4 floors（フロア）

| カラム | 型 | NULL | 説明 |
| --- | --- | --- | --- |
| `floor_id` | BIGSERIAL | × | 主キー |
| `site_id` | BIGINT | × | → `sites` |
| `floor_name` | VARCHAR(50) | × | 表示名（例 `3階`） |
| `floor_number` | SMALLINT | × | 階数。並べ替え用。地下は負数 |
| `map_image_url` | VARCHAR(500) | ○ | フロアマップ画像のURL |
| `created_at` / `updated_at` | TIMESTAMPTZ | × | |

**制約**: UNIQUE(`site_id`, `floor_number`) — 同一拠点に同じ階は作れない

### 2.5 seats（座席）

| カラム | 型 | NULL | 説明 |
| --- | --- | --- | --- |
| `seat_id` | BIGSERIAL | × | 主キー |
| `floor_id` | BIGINT | × | → `floors` |
| `seat_number` | VARCHAR(50) | × | 座席番号（例 `3F-A12`） |
| `position_x` | INTEGER | × | マップ画像上のX座標（px） |
| `position_y` | INTEGER | × | マップ画像上のY座標（px） |
| `created_at` / `updated_at` | TIMESTAMPTZ | × | |

**制約**: UNIQUE(`floor_id`, `seat_number`)

> 空席かどうかは `employees.seat_id` を参照して判定する（seats 側に在席フラグは持たない）。二重管理を避けるため。

### 2.6 employment_types（雇用形態）

| カラム | 型 | NULL | 説明 |
| --- | --- | --- | --- |
| `employment_type_id` | BIGSERIAL | × | 主キー |
| `employment_type_name` | VARCHAR(50) | × | 正社員、契約社員、派遣 など。UNIQUE |
| `created_at` / `updated_at` | TIMESTAMPTZ | × | |

### 2.7 skills（スキルマスタ）

| カラム | 型 | NULL | 説明 |
| --- | --- | --- | --- |
| `skill_id` | BIGSERIAL | × | 主キー |
| `skill_name` | VARCHAR(100) | × | スキル名（Java、React など）。UNIQUE |
| `category` | VARCHAR(50) | ○ | 分類（言語、フレームワーク、DB など） |
| `created_at` / `updated_at` | TIMESTAMPTZ | × | |

### 2.8 certifications（資格マスタ）

| カラム | 型 | NULL | 説明 |
| --- | --- | --- | --- |
| `certification_id` | BIGSERIAL | × | 主キー |
| `certification_name` | VARCHAR(150) | × | 資格名。UNIQUE |
| `created_at` / `updated_at` | TIMESTAMPTZ | × | |

### 2.9 projects（プロジェクトマスタ）

| カラム | 型 | NULL | 説明 |
| --- | --- | --- | --- |
| `project_id` | BIGSERIAL | × | 主キー |
| `project_name` | VARCHAR(200) | × | プロジェクト名 |
| `start_date` | DATE | ○ | 開始日 |
| `end_date` | DATE | ○ | 終了日。継続中は NULL |
| `description` | TEXT | ○ | 概要 |
| `created_at` / `updated_at` | TIMESTAMPTZ | × | |

**制約**: CHECK(`end_date` IS NULL OR `start_date` <= `end_date`)

---

## 3. 中間テーブル

### 3.1 employee_skills（社員↔スキル）

| カラム | 型 | NULL | 説明 |
| --- | --- | --- | --- |
| `employee_id` | BIGINT | × | → `employees`。複合主キー |
| `skill_id` | BIGINT | × | → `skills`。複合主キー |
| `proficiency` | SMALLINT | × | 1:初級 / 2:中級 / 3:上級 |
| `experience_years` | NUMERIC(4,1) | ○ | 経験年数（例 `5.5`）。最大 999.9 |
| `created_at` / `updated_at` | TIMESTAMPTZ | × | |

**制約**
- PK(`employee_id`, `skill_id`) — **同一スキルの重複登録をDBで防ぐ**（API 6.4 の CONFLICT(409)）
- CHECK: `proficiency BETWEEN 1 AND 3`
- CHECK: `experience_years >= 0`
- インデックス: `skill_id`（スキルからの逆引き検索。API 2.1 の `skill_id` 検索で使う）

### 3.2 employee_certifications（社員↔資格）

| カラム | 型 | NULL | 説明 |
| --- | --- | --- | --- |
| `employee_id` | BIGINT | × | → `employees`。複合主キー |
| `certification_id` | BIGINT | × | → `certifications`。複合主キー |
| `acquire_date` | DATE | × | 取得日 |
| `created_at` / `updated_at` | TIMESTAMPTZ | × | |

**制約**: PK(`employee_id`, `certification_id`)、インデックス `certification_id`

### 3.3 employee_projects（社員↔プロジェクト）

| カラム | 型 | NULL | 説明 |
| --- | --- | --- | --- |
| `employee_id` | BIGINT | × | → `employees`。複合主キー |
| `project_id` | BIGINT | × | → `projects`。複合主キー |
| `role_in_project` | VARCHAR(100) | ○ | 担当役割（フロントエンド担当 など） |
| `start_date` | DATE | ○ | 参画日 |
| `end_date` | DATE | ○ | 離任日。継続中は NULL |
| `created_at` / `updated_at` | TIMESTAMPTZ | × | |

**制約**: PK(`employee_id`, `project_id`)、CHECK(`end_date` IS NULL OR `start_date` <= `end_date`)、インデックス `project_id`

---

## 4. auth_tokens（認証トークン）

API設計仕様書の「認証トークンの仕様」に対応。**opaque token 方式**のため、発行したトークンをここで管理する。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| `token_id` | BIGSERIAL | × | 自動採番 | 主キー |
| `token_hash` | VARCHAR(64) | × | | トークンの SHA-256 ハッシュ（16進64文字）。UNIQUE |
| `employee_id` | BIGINT | × | | → `employees` |
| `token_type` | VARCHAR(10) | × | | `ACCESS` / `REFRESH` |
| `expires_at` | TIMESTAMPTZ | × | | 有効期限（ACCESS:1時間後 / REFRESH:14日後） |
| `revoked_at` | TIMESTAMPTZ | ○ | NULL | 失効日時。NULL なら有効 |
| `parent_token_id` | BIGINT | ○ | | ACCESS の場合、発行元の REFRESH トークン。ログアウト時の連鎖失効に使う |
| `created_at` | TIMESTAMPTZ | × | now() | |

**制約**
- UNIQUE: `token_hash`
- CHECK: `token_type IN ('ACCESS', 'REFRESH')`
- FK: `employee_id`（`ON DELETE CASCADE` — 社員が物理削除された場合はトークンも消す）
- インデックス: `token_hash`（**毎リクエストで検索するため必須**）、`employee_id`、`expires_at`

**運用**

- **平文のトークンは保存しない。** ハッシュ値のみを保存し、照合時はリクエストのトークンをハッシュ化して検索する。DBが漏れてもなりすまされないようにするため
- 期限切れレコードは定期的に物理削除する（バッチ、または起動時のクリーンアップ）
- 有効性の判定条件: `revoked_at IS NULL AND expires_at > now()`

---

## 5. thanks_messages（サンクスメッセージ）

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| `message_id` | BIGSERIAL | × | 自動採番 | 主キー |
| `from_employee_id` | BIGINT | × | | 送信者 → `employees` |
| `to_employee_id` | BIGINT | × | | 受信者 → `employees` |
| `body` | VARCHAR(300) | × | | 本文（1〜300文字） |
| `sent_at` | TIMESTAMPTZ | × | now() | 送信日時 |

**制約**
- CHECK: `from_employee_id <> to_employee_id` — **自分宛て送信をDBで防ぐ**（API 8.1）
- CHECK: `length(body) BETWEEN 1 AND 300`
- インデックス: `sent_at DESC`（タイムラインは新着順固定のため）、`from_employee_id`、`to_employee_id`

> メッセージは編集・削除しない想定のため `updated_at` を持たない。

---

## 6. change_requests（基本情報変更申請）

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| `request_id` | BIGSERIAL | × | 自動採番 | 主キー |
| `target_employee_id` | BIGINT | × | | 申請対象（＝申請者本人） → `employees` |
| `target_field` | VARCHAR(50) | × | | 変更対象のカラム名（例 `department_id`） |
| `before_value` | VARCHAR(255) | ○ | | 変更前の値。**申請時にサーバが現在値を取得して記録** |
| `after_value` | VARCHAR(255) | × | | 変更後の希望値 |
| `reason` | VARCHAR(500) | ○ | | 申請理由 |
| `status` | SMALLINT | × | 0 | 0:申請中 / 1:承認 / 2:却下 |
| `requested_at` | TIMESTAMPTZ | × | now() | 申請日時 |
| `reviewed_by` | BIGINT | ○ | | 承認・却下したADMIN → `employees` |
| `reviewed_at` | TIMESTAMPTZ | ○ | | 処理日時 |
| `review_comment` | VARCHAR(500) | ○ | | 承認・却下のコメント |

**制約**
- CHECK: `status IN (0, 1, 2)`
- 部分UNIQUE: `UNIQUE (target_employee_id, target_field) WHERE status = 0`
  同じ社員・同じ項目で**申請中のものは1件まで**（API 9.1 の CONFLICT(409)）。処理済みは何件でも残せる
- インデックス: `status`、`target_employee_id`

---

## 7. import_jobs（一括インポートジョブ）

| カラム | 型 | NULL | 説明 |
| --- | --- | --- | --- |
| `job_id` | VARCHAR(50) | × | 主キー。`imp_20260727_001` 形式 |
| `executed_by` | BIGINT | × | 実行したADMIN → `employees` |
| `file_name` | VARCHAR(255) | ○ | アップロードされたファイル名 |
| `status` | VARCHAR(20) | × | `processing` / `completed` / `failed` |
| `total_rows` | INTEGER | ○ | 総行数 |
| `success_rows` | INTEGER | ○ | 成功行数 |
| `error_rows` | INTEGER | ○ | エラー行数 |
| `errors` | JSONB | ○ | エラー詳細。`[{"row":34,"reason":"..."}]` |
| `started_at` | TIMESTAMPTZ | × | 開始日時 |
| `finished_at` | TIMESTAMPTZ | ○ | 完了日時 |

**制約**: CHECK `status IN ('processing','completed','failed')`

> エラー詳細は件数・構造が可変なので `JSONB` を採用。専用テーブルに分ける必要が出たら分離する。

---

## 8. 権限テーブル

> **前期スコープでは使用しない。** 権限制御は `employees.role`（`EMPLOYEE` / `ADMIN`）の2値で行う（決定事項3）。
> テーブル自体は `V1__init.sql` で作成するが、初期データは投入せず空のままにする。後から使い始めるときにマイグレーションを追加しなくて済むようにしておくため。
>
> 以下 8.1 / 8.2 の内容は、**「閲覧はできるが編集はできない利用者」が必要になった場合の設計案**として残す。2026-10-19 時点では社長にも編集権限を与える判断になったため、該当者がいない。

### 8.1 permissions（権限マスタ）

| カラム | 型 | NULL | 説明 |
| --- | --- | --- | --- |
| `permission_id` | BIGSERIAL | × | 主キー |
| `permission_code` | VARCHAR(50) | × | 権限コード。UNIQUE |
| `permission_name` | VARCHAR(100) | × | 表示名 |
| `created_at` / `updated_at` | TIMESTAMPTZ | × | |

**権限コード一覧（設計案・前期では未投入）**

API設計仕様書で「ADMIN専用」としている箇所を整理した結果、権限を使う場合は以下の4つで足りる。

| permission_id | `permission_code` | `permission_name` | 対象API |
| --- | --- | --- | --- |
| 1 | `VIEW_PRIVATE` | 機密情報の閲覧 | 機密項目 `email` / `tel` の閲覧（API 3章）<br>退職者（`status=0`）の検索・閲覧（API 2.1）<br>CSVエクスポート（API 10.3） |
| 2 | `MANAGE_EMPLOYEES` | 社員情報の管理 | 社員の登録・更新・削除（API 6.1〜6.3）<br>スキル・資格・プロジェクトの紐付け（API 6.4〜6.9）<br>CSVインポート（API 10.1, 10.2） |
| 3 | `MANAGE_MASTERS` | マスタの管理 | マスタの登録・更新・削除（API 7.1〜7.9） |
| 4 | `APPROVE_REQUESTS` | 変更申請の承認 | 変更申請の全件閲覧・承認・却下（API 9.2, 9.3） |

> **この4コードを使う場合の `employees.role` との関係**：`role = 'ADMIN'` を「管理系3つ（`MANAGE_EMPLOYEES` / `MANAGE_MASTERS` / `APPROVE_REQUESTS`）のいずれかを持つ人」と定義すれば、API設計仕様書の各所にある「FORBIDDEN(403) ADMIN以外」はそのまま成立する。書き換えが必要になるのは `VIEW_PRIVATE` 側の4箇所（機密項目・`status=0` の2箇所・エクスポート）と、用語を説明する注記の計5箇所のみ。
>
> **権限管理API（API 7.10 / 7.11）自体は権限テーブルで制御せず、`role = 'ADMIN'` で制御する。** 権限を配る権限を権限テーブルで守ると循環し、自分に権限を付与できてしまうため。
>
> **前期では上記を適用しない。** `role` の2値がそのまま権限の実体であり、API設計仕様書の「ADMIN以外は403」は**全箇所そのままで正しい**。仕様書の修正は不要。

### 8.2 permission_assignments（権限付与）

| カラム | 型 | NULL | 説明 |
| --- | --- | --- | --- |
| `assignment_id` | BIGSERIAL | × | 主キー |
| `permission_id` | BIGINT | × | → `permissions` |
| `target_type` | VARCHAR(20) | × | 付与先の種別。`DEPT` / `POSITION` / `EMPLOYEE` |
| `target_id` | BIGINT | × | 付与先のID。`target_type` によって参照先が変わる |
| `created_at` | TIMESTAMPTZ | × | |

**制約**
- UNIQUE(`permission_id`, `target_type`, `target_id`)
- CHECK: `target_type IN ('DEPT','POSITION','EMPLOYEE')`

> `target_id` は `target_type` によって参照先テーブルが変わるため、**外部キー制約を張れない**（ポリモーフィック関連）。整合性はアプリ側で担保する。
>
> **この形を維持すると決定した。** 前期スコープでは権限付与API（API 7.11）の管理画面を実装せず、下記の初期データ5行を固定で流すだけなので、手入力による不正なIDの混入が起きない。`UNIQUE` と `CHECK` が1行で書けるシンプルさを取った。
>
> 7.11 を実装することになったら、`target_type` / `target_id` を `department_id` / `position_id` / `employee_id` の3列に分割し（CHECK でちょうど1つだけ NOT NULL）、それぞれに外部キーを張る設計に変更する。手入力のミスをDB側で弾けるようになり、管理画面に付与先の名称を表示するのも楽になるため。

**付与の設計案（前期では未投入）**

部署・役職に付与することで、年2回（4月・10月）の人事異動に権限が自動で追従する（異動した本人の権限を付け外しする運用が発生しない）。これが権限テーブルを使う最大の利点だが、前期では `role` を手で設定する運用とする。

| permission | `target_type` | 付与先 | 備考 |
| --- | --- | --- | --- |
| 1 `VIEW_PRIVATE` | `DEPT` | 人事部 | |
| 2 `MANAGE_EMPLOYEES` | `DEPT` | 人事部 | |
| 3 `MANAGE_MASTERS` | `DEPT` | 人事部 | |
| 4 `APPROVE_REQUESTS` | `DEPT` | 人事部 | |
| 1 `VIEW_PRIVATE` | `POSITION` | 社長 | **閲覧のみ。** 社員情報の編集・マスタ管理・申請承認はできない |

`target_id` には、シードで採番された `departments.department_id`（人事部）と `positions.position_id`（社長）が入る。

> 社長を `POSITION` で付与するのは、代替わりしても新しい社長に権限が自動で移るため。個人（`EMPLOYEE`）に付けると、退任後も権限が残る。
>
> `DEPT` 付与は **人事部の全員**（アルバイト・新人を含む）が対象になる。「人事部の課長以上」のような絞り込みは今の設計では表現できないため、必要になった場合は該当者を `EMPLOYEE` で個別に付与して対応する。

---

## API設計仕様書との対応

| API | 主に使うテーブル |
| --- | --- |
| 1. 認証 | `employees`, `auth_tokens` |
| 2. 社員検索 | `employees` + 全マスタ + 中間テーブル3つ |
| 3. 社員詳細 | 同上 |
| 4. フロアマップ | `sites`, `floors`, `seats`, `employees` |
| 5. 自己PR編集 | `employees` |
| 6. 社員CRUD | `employees`, 中間テーブル3つ |
| 7. マスタ管理 | 各マスタ |
| 8. サンクスメッセージ | `thanks_messages`, `employees` |
| 9. 変更申請 | `change_requests`, `employees` |
| 10. インポート/エクスポート | `import_jobs`, `employees` |

---

## 決定事項

| No | 内容 | 決定 |
| --- | --- | --- |
| 1 | **ログインID** | **メールアドレス（`email`）**。`employee_code` はアカウント作成時の本人確認にのみ使う |
| 2 | **email の登録者** | **ADMIN** が社員登録（API 6.1）時に設定する。本人はアカウント作成（API 1.4）時に照合のため送信するだけで、新規に決めることはできない。そのため `employees.email` は **NOT NULL** |
| 3 | **権限制御の方式** | **`employees.role`（`EMPLOYEE` / `ADMIN`）の2値のみで行う。** `ADMIN` は**人事部と社長**に付与し、社長にも閲覧だけでなく編集を許可する（2026-10-19 チーム判断）。これにより「閲覧のみ」の利用者が存在しなくなるため、`permissions` / `permission_assignments` は前期では使用しない（テーブルは作成するが空。8章の注記を参照）。API設計仕様書の「FORBIDDEN(403) ADMIN以外」は全箇所そのまま有効で、**仕様書の修正は不要** |
| 4 | **`permission_assignments` の形** | **`target_type` / `target_id` のポリモーフィック設計を維持する。** 前期では使用しないため作り替える理由がない。実際に使い始める際は 8.2 の注記にある列分割案を再検討する |
| 5 | **照合順序の指定方法** | **カラム単位で `COLLATE "ja-x-icu"` を指定する**（対象は `employees.name` / `employees.name_kana`）。DB 全体に適用するには compose への `POSTGRES_INITDB_ARGS` 追加とボリュームの作り直しが必要で、チーム全員の環境に影響する。`ja-x-icu` はコンテナのデフォルト照合順序（`en_US.utf8`）のままでもカラム単位で利用できることを確認済みのため、氏名ソートが必要な2カラムのみに指定する |
| 6 | **`role = 'ADMIN'` の付与範囲** | **人事部の全員**（＋社長）に付与する（2026-10-19 チーム判断）。`role` は社員ごとの手設定のため、異動時は付け外しが必要になる。年2回の異動で運用が重くなった場合は、8章の権限テーブル（部署・役職への付与で自動追従する）への移行を検討する |

---

## 未確定事項（チームで要確認）

| No | 内容 | 論点 |
| --- | --- | --- |
| 1 | `name_kana` の追加 | かな検索のために追加した。API設計仕様書には未記載なので、使うなら仕様書側にも追記が必要 |
| 2 | `import_jobs.errors` の JSONB | 別テーブルに分けるかどうか。今の要件なら JSONB で十分 |
| 3 | 検索の部分一致 | `keyword` 検索を LIKE で済ませるか、`pg_trgm` 拡張を入れるか。800名程度なら LIKE で足りる見込み |
