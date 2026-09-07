# フロンティア社員情報管理システム API設計仕様書

- チーム名: チームA
- 作成者: 浅野（API設計担当）

---

## 共通事項

| 項目 | 内容 |
| --- | --- |
| ベースURL | `https://api.frontier-ems.example.co.jp/v1` |
| 認証方式 | Bearer トークン方式。`Authorization: Bearer {access_token}` ヘッダーを付与する（詳細は下記「認証トークンの仕様」） |
| 認証不要API | `POST /auth/login`、`POST /auth/refresh`、`POST /accounts` の3つのみ。それ以外は全て認証必須 |
| リクエスト形式 | `Content-Type: application/json`（ファイルアップロードのみ `multipart/form-data`） |
| 文字コード | UTF-8 |
| 日付形式 | 日付のみは `YYYY-MM-DD`（例 `2026-04-01`）、日時は ISO8601 の JST（例 `2026-07-27T10:15:00+09:00`） |
| 成功レスポンス | 単一・一覧ともに `data` キーで包む。一覧は `meta` にページ情報を持つ |
| `Location` ヘッダー | ベースURLからの絶対パスで返す（例 `/v1/employees/456`） |

### 認証トークンの仕様

本システムのトークンは **opaque token（不透明トークン）** 方式を採用する。
JWT のようにトークン自体へ情報を埋め込むのではなく、**意味を持たないランダム文字列**を発行し、
サーバ側のトークンテーブルに保存して照合する。ログアウト時に即座に失効させられることを重視した判断。

| 項目 | 内容 |
| --- | --- |
| トークン形式 | 暗号論的に安全な乱数を Base64URL エンコードした文字列（32バイト以上） |
| 保存場所 | サーバ側のトークンテーブル（トークンのハッシュ値・社員ID・有効期限・種別を保持） |
| 照合方法 | リクエストのたびにトークンをハッシュ化してテーブルを検索し、有効期限と失効フラグを確認する |
| アクセストークン有効期限 | 1時間（`expires_in`: 3600 秒） |
| リフレッシュトークン有効期限 | 14日間。ローテーションはせず、期限まで同一のものを使い続ける |
| 失効 | ログアウト時・退職（`status=0`）時に該当レコードを失効させる。失効済みトークンでのアクセスは UNAUTHORIZED(401) |
| クライアント保存先 | ブラウザのメモリまたは `sessionStorage`。トークンは平文でDBに保存しない（ハッシュ値のみ保存） |

**JWT は採用しない。** JWT は署名が有効な限りサーバ側で取り消せず、1.3 ログアウトの「トークンを即時無効化する」要件を満たせないため。
この方式ではトークン用の秘密鍵（`JWT_SECRET` 等）は不要。

### ページネーション共通仕様

一覧取得APIは以下のクエリパラメータを共通で受け付ける。

| パラメータ | 既定値 | 上限 | 説明 |
| --- | --- | --- | --- |
| `page` | 1 | - | 1始まりのページ番号 |
| `per_page` | 20 | 100 | 1ページあたりの件数。上限超過は VALIDATION_ERROR(422) |

レスポンスの `meta` は `{ "page":1, "per_page":20, "total_count":42, "total_pages":3 }` の形式。

---

## 共通エラーコード

すべてのエラーレスポンスは以下の共通フォーマットで返す。

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "入力値に誤りがあります",
    "details": [
      { "field": "email", "reason": "メールアドレスの形式が正しくありません" }
    ]
  }
}
```

`details` は VALIDATION_ERROR のときのみ必須。それ以外は空配列または省略。

| コード | ステータス | 意味 | 対処法 |
| --- | --- | --- | --- |
| VALIDATION_ERROR | 422 | 入力値が不正 | エラー内の`details`を見て該当項目を修正し再送信 |
| DUPLICATE_EMAIL | 409 | メールアドレス重複 | 別のメールアドレスを使用 |
| DUPLICATE_EMPLOYEE_CODE | 409 | 社員番号重複 | 社員番号を確認し修正 |
| CONFLICT | 409 | 状態の競合（参照中マスタの削除、スキル等の重複紐付け、座席の二重割当 など） | `message`の内容を確認し、参照を解除するか別の値を指定 |
| UNAUTHORIZED | 401 | 未認証、またはトークン失効 | 再ログイン、または`/auth/refresh`でトークン再取得 |
| FORBIDDEN | 403 | 権限不足 | ADMIN権限が必要な操作でないか確認。本人以外のデータ編集は不可 |
| NOT_FOUND | 404 | 指定IDのリソースが存在しない | IDを確認 |
| IMPORT_FILE_INVALID | 400 | インポートCSVの形式不正 | 文字コード・カラム構成を確認して再アップロード |
| RATE_LIMITED | 429 | 一定時間内のリクエスト数が上限を超過 | 時間をおいて再実行 |
| INTERNAL_SERVER_ERROR | 500 | サーバ内部エラー | 時間をおいて再実行。復旧しない場合は管理者へ連絡 |

---

## 共通の値定義

同じ `status` という名前でも対象によって意味が異なるため、以下に定義する。

| 対象 | 値 | 意味 |
| --- | --- | --- |
| 社員 `status` | 0 | 退職・無効（論理削除済み） |
| 社員 `status` | 1 | 在籍 |
| 変更申請 `status` | 0 | 申請中 |
| 変更申請 `status` | 1 | 承認済み |
| 変更申請 `status` | 2 | 却下 |
| スキル `proficiency` | 1 / 2 / 3 | 初級 / 中級 / 上級 |
| 権限 `role` | `EMPLOYEE` / `ADMIN` | 一般社員 / 人事・管理者 |

- `GET /employees` の `status` は未指定時 `1`（在籍のみ）を既定とする。退職者を含めたい場合のみ明示的に指定する。
- `status=0` の社員を検索・閲覧できるのは ADMIN のみ。

---

## 1. 認証

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 1.1 ログイン | `POST /auth/login`（認証不要） | `{ "employee_code":"EMP00123", "password":"********" }` | 200 `{ "access_token":"...", "refresh_token":"...", "expires_in":3600, "employee":{"employee_id":123,"name":"山田 太郎","role":"EMPLOYEE"} }` | VALIDATION_ERROR(422) 未入力<br>UNAUTHORIZED(401) 認証情報誤り |
| 1.2 トークン再発行 | `POST /auth/refresh`（認証不要） | `{ "refresh_token":"..." }` | 200 `{ "access_token":"...", "expires_in":3600 }`<br>※リフレッシュトークンはローテーションせず、有効期限（14日）まで同一のものを使い続ける。期限切れ後は再ログインが必要 | UNAUTHORIZED(401) リフレッシュトークン無効・失効 |
| 1.3 ログアウト | `POST /auth/logout` | ヘッダーのみ（`Authorization: Bearer`）<br>※サーバ側のトークンテーブルから、アクセストークンとそれに紐づくリフレッシュトークンの両方を失効させる | 204 No Content | UNAUTHORIZED(401) 未ログイン状態 |
| 1.4 アカウント作成 | `POST /accounts`（認証不要） | `{ "employee_code":"EMP00456", "email":"sato@example.co.jp", "password":"********" }`<br>※ログインIDは`employee_code`。`email`は連絡先・通知用で、ログインには使用しない | 201 `{ "data":{"employee_id":456} }`（`Location: /v1/employees/456`） | DUPLICATE_EMAIL(409)<br>DUPLICATE_EMPLOYEE_CODE(409) 既にアカウント作成済み<br>NOT_FOUND(404) 社員マスタに該当社員コードなし<br>VALIDATION_ERROR(422) パスワード強度不足等 |
| 1.5 ログイン中の自分の情報取得 | `GET /me` | ヘッダーのみ | 200 `{ "data":{...} }`（3.1と同形式。`email`/`tel`を含む） | UNAUTHORIZED(401) |

---

## 2. 社員検索・一覧表示

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 2.1 社員検索・一覧 | `GET /employees` | 下表のクエリパラメータ | 200 `{ "data":[{"employee_id":123,"name":"山田 太郎","image_url":"...","department":{...},"position":{...},"skills":[...]}], "meta":{"page":1,"per_page":20,"total_count":42,"total_pages":3} }`（`email`/`tel`は含めない） | VALIDATION_ERROR(422) パラメータ型不正・`per_page`上限超過<br>UNAUTHORIZED(401) |

### 2.1 クエリパラメータ

| パラメータ | 複数指定 | 説明 |
| --- | --- | --- |
| `keyword` | 不可 | 氏名・趣味・自己PRに対する部分一致 |
| `department_id` | 可 | 所属部署 |
| `position_id` | 可 | 役職 |
| `site_id` | 可 | 勤務拠点 |
| `floor_id` | 可 | 勤務フロア |
| `skill_id` | 可 | 保有スキル |
| `skill_match` | 不可 | `and`（既定） / `or`。`skill_id`を複数指定したときの結合方法 |
| `certification_id` | 可 | 保有資格 |
| `certification_match` | 不可 | `and`（既定） / `or` |
| `project_id` | 可 | 過去の担当プロジェクト |
| `employment_type_id` | 可 | 雇用形態 |
| `status` | 不可 | 既定 `1`（在籍のみ）。`0`指定はADMINのみ |
| `page` / `per_page` | 不可 | 共通事項のページネーション仕様に準ずる |
| `sort` | 不可 | `employee_code`(既定) / `name` / `hire_date` / `department_id`。先頭に`-`を付けると降順（例 `-hire_date`） |

※複数指定はカンマ区切り（例 `skill_id=12,15,20`）。

---

## 3. 社員詳細プロフィール閲覧

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 3.1 詳細取得 | `GET /employees/{employee_id}` | パスパラメータ `employee_id` | 200 `{ "data":{"employee_id":123,"employee_code":"EMP00123","name":"...","image_url":"...","email":"...","tel":"...","hobby":"...","self_pr":"...","hire_date":"2020-04-01","status":1,"department":{...},"position":{...},"employment_type":{...},"seat":{...},"skills":[...],"certifications":[...],"projects":[...]} }` | NOT_FOUND(404) 該当社員なし<br>UNAUTHORIZED(401) |

**機密項目の扱い**：`email` / `tel` は**本人またはADMINのみレスポンスに含める**。それ以外の社員が取得した場合はキーごと省略して 200 を返す（403は返さない）。詳細取得そのものは全社員に許可されているため。

---

## 4. フロアマップ・座席図表示

※拠点・フロア・座席の**参照系はこの章**、**登録・更新・削除は7章**に定義する。

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 4.1 拠点一覧 | `GET /sites` | なし | 200 `{ "data":[{"site_id":1,"site_name":"本社","address":"..."}] }` | UNAUTHORIZED(401) |
| 4.2 拠点内フロア一覧 | `GET /sites/{site_id}/floors` | パスパラメータ `site_id` | 200 `{ "data":[{"floor_id":5,"floor_name":"3階","floor_number":3,"map_image_url":"..."}] }` | NOT_FOUND(404) 拠点なし |
| 4.3 フロア内座席一覧 | `GET /floors/{floor_id}/seats` | パスパラメータ `floor_id` | 200 `{ "data":{"floor":{...},"seats":[{"seat_id":88,"seat_number":"3F-A12","position_x":120,"position_y":340,"employee":{...}}]} }`<br>※空席は `"employee": null` | NOT_FOUND(404) フロアなし |
| 4.4 社員の座席取得 | `GET /employees/{employee_id}/seat` | パスパラメータ `employee_id` | 200 `{ "data":{"seat_id":88,"seat_number":"3F-A12","floor":{...},"site":{...}} }`<br>※**座席未登録の社員は 200 で `"data": null`**（社員が存在しない場合のみ404） | NOT_FOUND(404) 社員なし |

---

## 5. 自己PR編集

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 5.1 自己プロフィール更新 | `PATCH /me/profile` | `{ "hobby":"登山、写真撮影", "self_pr":"フロントエンド開発が得意です。" }`（すべて任意項目・指定した項目のみ更新） | 200 更新後の社員情報（3章と同形式） | VALIDATION_ERROR(422) 文字数超過等<br>UNAUTHORIZED(401) |
| 5.2 顔写真アップロード | `POST /me/image`（`multipart/form-data`） | 画像ファイル（ファイルキー`file`）<br>形式: JPEG / PNG、最大5MB | 200 `{ "data":{"image_url":"https://.../123.jpg"} }` | VALIDATION_ERROR(422) 形式・サイズ不正<br>UNAUTHORIZED(401) |
| 5.3 顔写真削除 | `DELETE /me/image` | なし（既定画像に戻る） | 204 No Content | UNAUTHORIZED(401) |

- 編集対象は常に**ログイン中の本人**であるため、パスに `employee_id` は含めない（他人のIDを指定しうる形にしない）。
- `image_url` はクライアントから直接指定できない。必ず 5.2 のアップロードAPI経由でサーバが払い出す。
- 編集可能なのは `hobby` / `self_pr` / 顔写真のみ。それ以外の基本情報の変更は 9章の変更申請を使う。

---

## 6. 社員情報一元管理（CRUD・ADMIN専用）

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 6.1 新規登録 | `POST /employees` | `{ "employee_code":"EMP00456","name":"佐藤 花子","email":"...","tel":"...","hire_date":"2026-04-01","department_id":4,"position_id":2,"employment_type_id":1,"seat_id":null }` | 201 作成された社員情報（`Location: /v1/employees/456`） | DUPLICATE_EMAIL(409)<br>DUPLICATE_EMPLOYEE_CODE(409)<br>CONFLICT(409) 指定座席が他社員に割当済み<br>VALIDATION_ERROR(422)<br>FORBIDDEN(403) ADMIN以外 |
| 6.2 更新 | `PUT /employees/{employee_id}` | 6.1と同様の全項目 | 200 更新後の社員情報 | NOT_FOUND(404)<br>DUPLICATE_EMAIL(409)<br>DUPLICATE_EMPLOYEE_CODE(409)<br>CONFLICT(409) 座席の二重割当<br>VALIDATION_ERROR(422)<br>FORBIDDEN(403) ADMIN以外 |
| 6.3 削除（論理削除） | `DELETE /employees/{employee_id}` | パスパラメータ `employee_id`（`status`を0に更新し、座席の割当を解除するのみ。レコードは削除しない） | 204 No Content | NOT_FOUND(404) 未登録、または既に`status=0`<br>FORBIDDEN(403) ADMIN以外 |
| 6.4 スキル紐付け追加 | `POST /employees/{employee_id}/skills` | `{ "skill_id":12,"proficiency":3,"experience_years":5.5 }` | 201 `{ "data":{"skill_id":12,"skill_name":"Java","proficiency":3,"experience_years":5.5} }` | NOT_FOUND(404) 社員またはスキルなし<br>CONFLICT(409) 同一スキルが登録済み<br>VALIDATION_ERROR(422) `proficiency`が1〜3の範囲外 |
| 6.5 スキル紐付け更新/解除 | `PATCH /employees/{employee_id}/skills/{skill_id}`<br>`DELETE /employees/{employee_id}/skills/{skill_id}` | 更新: `{ "proficiency":2,"experience_years":6.0 }` | 200（更新）/ 204（解除） | NOT_FOUND(404) 紐付けなし<br>VALIDATION_ERROR(422) |
| 6.6 資格紐付け追加 | `POST /employees/{employee_id}/certifications` | `{ "certification_id":3,"acquire_date":"2018-10-01" }` | 201 `{ "data":{"certification_id":3,"certification_name":"基本情報技術者","acquire_date":"2018-10-01"} }` | NOT_FOUND(404)<br>CONFLICT(409) 同一資格が登録済み<br>VALIDATION_ERROR(422) 日付形式不正・未来日 |
| 6.7 資格紐付け解除 | `DELETE /employees/{employee_id}/certifications/{certification_id}` | パスパラメータ | 204 No Content | NOT_FOUND(404) 紐付けなし |
| 6.8 担当プロジェクト紐付け追加 | `POST /employees/{employee_id}/projects` | `{ "project_id":7,"role_in_project":"フロントエンド担当","start_date":"2024-04-01","end_date":"2025-03-31" }`（`end_date`は継続中なら`null`） | 201 `{ "data":{...} }` | NOT_FOUND(404) 社員またはプロジェクトなし<br>CONFLICT(409) 同一プロジェクトが登録済み<br>VALIDATION_ERROR(422) 開始日 > 終了日 |
| 6.9 担当プロジェクト紐付け解除 | `DELETE /employees/{employee_id}/projects/{project_id}` | パスパラメータ | 204 No Content | NOT_FOUND(404) 紐付けなし |

※6.4〜6.9 はすべて ADMIN のみ実行可。ADMIN以外は FORBIDDEN(403)。

---

## 7. マスタ管理（ADMIN専用・書込系）

**参照系（GET）は全社員が利用可、登録・更新・削除は ADMIN のみ。**
共通のエラーは以下のとおりで、各行では省略する。

- VALIDATION_ERROR(422) 入力値不正
- FORBIDDEN(403) ADMIN以外の書込
- NOT_FOUND(404) 対象IDなし
- CONFLICT(409) 参照中のマスタを削除しようとした（例: 所属社員がいる部署、着席中の座席）

| No | 参照 | 登録 | 更新 / 削除 | リクエストボディ |
| --- | --- | --- | --- | --- |
| 7.1 部署 | `GET /departments` | `POST /departments` | `PUT` / `DELETE /departments/{department_id}` | `{ "department_name":"開発部" }` |
| 7.2 役職 | `GET /positions` | `POST /positions` | `PUT` / `DELETE /positions/{position_id}` | `{ "position_name":"主任","rank":3 }` |
| 7.3 拠点 | `GET /sites`（4.1） | `POST /sites` | `PUT` / `DELETE /sites/{site_id}` | `{ "site_name":"本社","address":"..." }` |
| 7.4 フロア | `GET /sites/{site_id}/floors`（4.2） | `POST /floors` | `PUT` / `DELETE /floors/{floor_id}` | `{ "site_id":1,"floor_name":"3階","floor_number":3,"map_image_url":"..." }` |
| 7.5 座席 | `GET /floors/{floor_id}/seats`（4.3） | `POST /seats` | `PUT` / `DELETE /seats/{seat_id}` | `{ "floor_id":5,"seat_number":"3F-A12","position_x":120,"position_y":340 }` |
| 7.6 雇用形態 | `GET /employment-types` | `POST /employment-types` | `PUT` / `DELETE /employment-types/{employment_type_id}` | `{ "employment_type_name":"契約社員" }` |
| 7.7 スキル | `GET /skills` | `POST /skills` | `PUT` / `DELETE /skills/{skill_id}` | `{ "skill_name":"Java","category":"言語" }` |
| 7.8 資格 | `GET /certifications` | `POST /certifications` | `PUT` / `DELETE /certifications/{certification_id}` | `{ "certification_name":"基本情報技術者" }` |
| 7.9 プロジェクト | `GET /projects` | `POST /projects` | `PUT` / `DELETE /projects/{project_id}` | `{ "project_name":"社内ポータル刷新","start_date":"2024-04-01","end_date":"2025-03-31","description":"..." }` |
| 7.10 権限 | `GET /permissions`（ADMINのみ） | `POST /permissions` | `PUT` / `DELETE /permissions/{permission_id}` | `{ "permission_code":"VIEW","permission_name":"閲覧可能" }` |
| 7.11 権限付与 | `GET /permission-assignments`（ADMINのみ） | `POST /permission-assignments` | `DELETE /permission-assignments/{assignment_id}` | `{ "permission_id":2,"target_type":"DEPT","target_id":4 }` |

**レスポンス**：`GET` 200 `{ "data":[...] }` / `POST` 201 `{ "data":{...} }`（`Location`ヘッダー付き） / `PUT` 200 `{ "data":{...} }` / `DELETE` 204 No Content

---

## 8. サンクスメッセージ送信・タイムライン閲覧

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 8.1 送信 | `POST /thanks-messages` | `{ "to_employee_id":456,"body":"先日はありがとうございました！" }`<br>※`from_employee_id`はトークンから判定するため送信不要。本文は1〜300文字 | 201 `{ "data":{"message_id":999,"from_employee":{...},"to_employee":{...},"body":"...","sent_at":"2026-07-27T10:15:00+09:00"} }` | NOT_FOUND(404) 宛先社員なし・退職者<br>VALIDATION_ERROR(422) 本文が空欄／文字数超過／宛先が自分自身 |
| 8.2 タイムライン取得 | `GET /thanks-messages` | クエリ `page`,`per_page`（新着順固定） | 200 `{ "data":[{...}], "meta":{...} }` | UNAUTHORIZED(401) |
| 8.3 社員別送受信履歴 | `GET /employees/{employee_id}/thanks-messages` | パスパラメータ `employee_id`<br>クエリ `type`（`sent` / `received` / `all`（既定））,`page`,`per_page` | 200 `{ "data":[{...}], "meta":{...} }` | NOT_FOUND(404) 社員なし<br>VALIDATION_ERROR(422) `type`の値不正 |

---

## 9. 基本情報変更申請

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 9.1 申請作成 | `POST /change-requests` | `{ "target_field":"department_id","after_value":"7","reason":"組織改編のため" }`<br>※申請対象は**ログイン中の本人**に固定。`before_value`は改ざん防止のためサーバ側で現在値を取得して記録する | 201 `{ "data":{"request_id":55,"target_employee":{...},"target_field":"department_id","before_value":"4","after_value":"7","status":0,"requested_at":"..."} }` | VALIDATION_ERROR(422) 編集不可項目（`self_pr`/`hobby`/顔写真は5章で直接編集）<br>CONFLICT(409) 同一項目の申請が処理中 |
| 9.2 一覧・詳細取得 | `GET /change-requests`（クエリ `status`,`page`,`per_page`）<br>`GET /change-requests/{request_id}` | ADMINは全件、EMPLOYEEは自分の申請のみ取得できる | 200 申請一覧／詳細 | FORBIDDEN(403) 他人の申請をEMPLOYEEが閲覧<br>NOT_FOUND(404)<br>VALIDATION_ERROR(422) `status`の値不正 |
| 9.3 承認・却下 | `PATCH /change-requests/{request_id}` | `{ "status":1,"comment":"承認します" }`（1:承認 2:却下） | 200 更新後の申請情報（承認時は対象社員情報も同時に更新する） | NOT_FOUND(404) 申請なし<br>FORBIDDEN(403) ADMIN以外<br>VALIDATION_ERROR(422) 処理済み申請、または`status`が1/2以外 |
| 9.4 申請取り下げ | `DELETE /change-requests/{request_id}` | パスパラメータ（申請者本人のみ、`status=0`のときのみ可） | 204 No Content | NOT_FOUND(404)<br>FORBIDDEN(403) 申請者本人以外<br>VALIDATION_ERROR(422) 処理済み申請 |

---

## 10. 一括データインポート/エクスポート

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 10.1 インポート | `POST /employees/import`（`multipart/form-data`） | CSVファイル（ファイルキー`file`）<br>文字コード: UTF-8（BOM有無どちらも可）／改行: CRLF・LF<br>1行目はヘッダー行必須、最大10,000行 | 202 `{ "data":{"job_id":"imp_20260727_001","status":"processing","total_rows":812} }`<br>※取り込みは非同期。結果は10.2で取得する | IMPORT_FILE_INVALID(400) 文字コード・カラム構成不正<br>VALIDATION_ERROR(422) 行数上限超過<br>FORBIDDEN(403) ADMIN以外 |
| 10.2 インポート結果取得 | `GET /employees/import/{job_id}` | パスパラメータ `job_id` | 200 `{ "data":{"status":"completed","total_rows":812,"success_rows":805,"error_rows":7,"errors":[{"row":34,"reason":"department_idが存在しません"}]} }`<br>`status`: `processing` / `completed` / `failed` | NOT_FOUND(404) job_idなし<br>FORBIDDEN(403) ADMIN以外 |
| 10.3 エクスポート | `GET /employees/export` | クエリは2.1と同じ検索条件を指定可（`department_id`/`site_id`/`status` 等）。`page`/`per_page`は無効で全件出力 | 200 CSVファイル<br>`Content-Type: text/csv; charset=UTF-8`<br>`Content-Disposition: attachment; filename="employees_20260727.csv"`<br>※Excel対応のためUTF-8 BOM付きで出力 | VALIDATION_ERROR(422) パラメータ型不正<br>FORBIDDEN(403) ADMIN以外 |
