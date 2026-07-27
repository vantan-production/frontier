# フロンティア社員情報管理システム API設計仕様書

- チーム名: チームA
- 作成者: 浅野（API設計担当）

共通事項: ベースURLは `https://api.frontier-ems.example.co.jp/v1`。認証が必要なAPIは `Authorization: Bearer {access_token}` ヘッダーを付与する。

---

## 共通エラーコード

| コード | ステータス | 意味 | 対処法 |
| --- | --- | --- | --- |
| VALIDATION_ERROR | 422 | 入力値が不正 | エラー内の`details`を見て該当項目を修正し再送信 |
| DUPLICATE_EMAIL | 409 | メールアドレス重複 | 別のメールアドレスを使用 |
| DUPLICATE_EMPLOYEE_CODE | 409 | 社員番号重複 | 社員番号を確認し修正 |
| UNAUTHORIZED | 401 | 未認証、またはトークン失効 | 再ログイン、または`/auth/refresh`でトークン再取得 |
| FORBIDDEN | 403 | 権限不足 | ADMIN権限が必要な操作でないか確認。本人以外のデータ編集は不可 |
| NOT_FOUND | 404 | 指定IDのリソースが存在しない | IDを確認 |
| IMPORT_FILE_INVALID | 400 | インポートCSVの形式不正 | 文字コード・カラム構成を確認して再アップロード |
| RATE_LIMITED | 429 | 同時アクセス過多 | 時間をおいて再実行 |

---

## 1. 認証

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 1.1 ログイン | `POST /auth/login` | `{ "employee_code":"EMP00123", "password":"********" }` | 200 `{ "access_token":"...", "refresh_token":"...", "expires_in":3600, "employee":{"employee_id":123,"name":"山田 太郎","role":"EMPLOYEE"} }` | VALIDATION_ERROR(422) 未入力<br>UNAUTHORIZED(401) 認証情報誤り |
| 1.2 トークン再発行 | `POST /auth/refresh` | `{ "refresh_token":"..." }` | 200 `{ "access_token":"...", "expires_in":3600 }` | UNAUTHORIZED(401) リフレッシュトークン無効・失効 |
| 1.3 ログアウト | `POST /auth/logout` | ヘッダーのみ（`Authorization: Bearer`） | 204 No Content | UNAUTHORIZED(401) 未ログイン状態 |
| 1.4 アカウント作成 | `POST /accounts` | `{ "employee_code":"EMP00456", "email":"sato@example.co.jp", "password":"********" }` | 201 `{ "employee_id":456 }` | DUPLICATE_EMAIL(409)<br>VALIDATION_ERROR(422) 社員コード未登録 |

---

## 2. 社員検索・一覧表示

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 2.1 社員検索・一覧 | `GET /employees` | クエリ: `keyword`,`department_id`,`site_id`,`skill_id`(複数可),`skill_match`(and/or),`certification_id`,`employment_type_id`,`status`,`page`,`per_page`,`sort` | 200 `{ "data":[{"employee_id":123,"name":"山田 太郎","department":{...},"skills":[...]}], "meta":{"page":1,"per_page":20,"total_count":42} }`（`email`/`tell`は含めない） | VALIDATION_ERROR(422) パラメータ型不正<br>UNAUTHORIZED(401) |

---

## 3. 社員詳細プロフィール閲覧

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 3.1 詳細取得 | `GET /employees/{employee_id}` | パスパラメータ `employee_id` | 200 `{ "data":{"employee_id":123,"name":"...","email":"...","tell":"...","hobby":"...","self_pr":"...","department":{...},"seat":{...},"skills":[...],"certifications":[...]} }`（`email`/`tell`は本人またはADMINのみ） | NOT_FOUND(404) 該当社員なし<br>FORBIDDEN(403) 機密項目への不正アクセス |

---

## 4. フロアマップ・座席図表示

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 4.1 拠点一覧 | `GET /sites` | なし | 200 `{ "data":[{"site_id":1,"site_name":"本社","address":"..."}] }` | UNAUTHORIZED(401) |
| 4.2 拠点内フロア一覧 | `GET /sites/{site_id}/floors` | パスパラメータ `site_id` | 200 `{ "data":[{"floor_id":5,"floor_name":"3階","floor_number":3}] }` | NOT_FOUND(404) 拠点なし |
| 4.3 フロア内座席一覧 | `GET /floors/{floor_id}/seats` | パスパラメータ `floor_id` | 200 `{ "data":{"floor":{...},"seats":[{"seat_id":88,"seat_number":"3F-A12","position_x":120,"position_y":340,"employee":{...}}]} }` | NOT_FOUND(404) フロアなし |
| 4.4 社員の座席取得 | `GET /employees/{employee_id}/seat` | パスパラメータ `employee_id` | 200 `{ "data":{"seat_id":88,"seat_number":"3F-A12","floor":{...},"site":{...}} }` | NOT_FOUND(404) 社員または座席未登録 |

---

## 5. 自己PR編集

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 5.1 自己プロフィール更新 | `PATCH /employees/{employee_id}/self-profile` | `{ "hobby":"登山、写真撮影", "self_pr":"フロントエンド開発が得意です。", "image_url":"https://.../123.jpg" }`（任意項目） | 200 更新後の社員情報（3章と同形式） | FORBIDDEN(403) 本人以外が編集<br>VALIDATION_ERROR(422) 文字数超過等 |

---

## 6. 社員情報一元管理（CRUD・ADMIN専用）

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 6.1 新規登録 | `POST /employees` | `{ "employee_code":"EMP00456","name":"佐藤 花子","email":"...","hire_date":"2026-04-01","department_id":4,"employment_type_id":1,"seat_id":null }` | 201 作成された社員情報（`Location: /employees/456`） | DUPLICATE_EMAIL(409)<br>DUPLICATE_EMPLOYEE_CODE(409)<br>VALIDATION_ERROR(422) |
| 6.2 更新 | `PUT /employees/{employee_id}` | 6.1と同様の全項目 | 200 更新後の社員情報 | NOT_FOUND(404)<br>VALIDATION_ERROR(422)<br>FORBIDDEN(403) ADMIN以外 |
| 6.3 削除（論理削除） | `DELETE /employees/{employee_id}` | パスパラメータ `employee_id`（`status`を0に更新するのみ） | 204 No Content | NOT_FOUND(404)<br>FORBIDDEN(403) |
| 6.4 スキル紐付け追加/解除 | `POST /employees/{employee_id}/skills`<br>`DELETE /employees/{employee_id}/skills/{skill_id}` | `{ "skill_id":12,"proficiency":3,"experience_years":5.5 }` | 201（追加）/ 204（解除） | NOT_FOUND(404) 社員またはスキルなし<br>VALIDATION_ERROR(422) proficiencyが1〜3の範囲外 |
| 6.5 資格紐付け追加/解除 | `POST /employees/{employee_id}/certifications`<br>`DELETE /employees/{employee_id}/certifications/{certification_id}` | `{ "certification_id":3,"acquire_date":"2018-10-01" }` | 201（追加）/ 204（解除） | NOT_FOUND(404)<br>VALIDATION_ERROR(422) 日付形式不正 |

---

## 7. マスタ管理（部署・拠点・フロア・座席・雇用形態・スキル・資格・権限）

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 7.1 部署 | `GET /departments`<br>`POST/PUT/DELETE /departments/{id}` | `{ "department_name":"開発部" }` | 200/201 マスタ情報 / 204 削除成功 | VALIDATION_ERROR(422)<br>FORBIDDEN(403) ADMIN以外の書込<br>NOT_FOUND(404) |
| 7.2 拠点 | `GET /sites`<br>`POST/PUT/DELETE /sites/{id}` | `{ "site_name":"本社","address":"..." }` | 同上 | 同上 |
| 7.3 フロア | `GET /sites/{id}/floors`<br>`POST/PUT/DELETE /floors/{id}` | `{ "site_id":1,"floor_name":"3階","floor_number":3,"map_image_url":"..." }` | 同上 | 同上 |
| 7.4 座席 | `GET /floors/{id}/seats`<br>`POST/PUT/DELETE /seats/{id}` | `{ "floor_id":5,"seat_number":"3F-A12","position_x":120,"position_y":340 }` | 同上 | 同上 |
| 7.5 雇用形態 | `GET /employment-types`<br>`POST/PUT/DELETE /employment-types/{id}` | `{ "employment_type_name":"契約社員" }` | 同上 | 同上 |
| 7.6 スキルマスタ | `GET /skills`<br>`POST/PUT/DELETE /skills/{id}` | `{ "skill_name":"Java","category":"言語" }` | 同上 | 同上 |
| 7.7 資格マスタ | `GET /certifications`<br>`POST/PUT/DELETE /certifications/{id}` | `{ "certification_name":"基本情報技術者" }` | 同上 | 同上 |
| 7.8 権限マスタ | `GET /permissions`（ADMINのみ）<br>`POST/PUT/DELETE /permissions/{id}` | `{ "permission_code":"VIEW","permission_name":"閲覧可能" }` | 同上 | 同上 |
| 7.9 権限付与 | `GET /permission-assignments`（ADMINのみ）<br>`POST/DELETE /permission-assignments/{id}` | `{ "permission_id":2,"target_type":"DEPT","target_id":4 }` | 同上 | 同上 |

---

## 8. サンクスメッセージ送信・タイムライン閲覧

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 8.1 送信 | `POST /thanks-messages` | `{ "to_employee_id":456,"body":"先日はありがとうございました！" }` | 201 `{ "data":{"message_id":999,"from_employee_id":123,"to_employee_id":456,"body":"...","sent_at":"2026-07-27T10:15:00+09:00"} }` | NOT_FOUND(404) 宛先社員なし<br>VALIDATION_ERROR(422) 本文が空欄 |
| 8.2 タイムライン取得 | `GET /thanks-messages` | クエリ `page`,`per_page`（新着順固定） | 200 `{ "data":[{...}], "meta":{...} }` | UNAUTHORIZED(401) |
| 8.3 社員別送受信履歴 | `GET /employees/{employee_id}/thanks-messages` | パスパラメータ `employee_id` | 200 送受信メッセージ一覧 | NOT_FOUND(404) 社員なし |

---

## 9. 基本情報変更申請

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 9.1 申請作成 | `POST /change-requests` | `{ "target_employee_id":123,"target_field":"department_id","before_value":"4","after_value":"7" }` | 201 `{ "data":{"request_id":55,"status":0,"requested_at":"..."} }` | VALIDATION_ERROR(422) 編集不可項目<br>NOT_FOUND(404) 対象社員なし |
| 9.2 一覧・詳細取得 | `GET /change-requests`（`status`でフィルタ可）<br>`GET /change-requests/{request_id}` | クエリ`status`、またはパスパラメータ`request_id` | 200 申請一覧／詳細 | FORBIDDEN(403) 他人の申請をEMPLOYEEが閲覧<br>NOT_FOUND(404) |
| 9.3 承認・却下 | `PATCH /change-requests/{request_id}` | `{ "status":1 }`（1:承認 2:却下） | 200 更新後の申請情報（承認時は対象社員情報も更新） | FORBIDDEN(403) ADMIN以外<br>VALIDATION_ERROR(422) 処理済み申請 |

---

## 10. 一括データインポート/エクスポート

| No | エンドポイント | リクエスト | レスポンス | エラー |
| --- | --- | --- | --- | --- |
| 10.1 インポート | `POST /employees/import`（`multipart/form-data`） | CSVファイル（ファイルキー`file`） | 202 `{ "data":{"job_id":"imp_20260727_001","status":"processing","accepted_rows":812} }` | IMPORT_FILE_INVALID(400)<br>FORBIDDEN(403) ADMIN以外 |
| 10.2 インポート結果取得 | `GET /employees/import/{job_id}` | パスパラメータ `job_id` | 200 `{ "data":{"status":"completed","total_rows":812,"success_rows":805,"error_rows":7,"errors":[{"row":34,"reason":"department_idが存在しません"}]} }` | NOT_FOUND(404) job_idなし |
| 10.3 エクスポート | `GET /employees/export` | クエリ `department_id`/`site_id` 等 | 200 CSVファイル（`Content-Type: text/csv`） | FORBIDDEN(403) ADMIN以外 |
