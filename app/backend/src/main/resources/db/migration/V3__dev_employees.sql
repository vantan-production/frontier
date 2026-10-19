-- ============================================================================
-- 開発・動作確認用の社員データ
--
-- **実在しない人のダミーデータ。** 本番相当の環境に入れたくない場合はこのファイルを
-- 外す（Flyway は適用済みのマイグレーションが消えると検証エラーになるため、
-- 外すなら最初の適用前に行うこと）。
--
-- ログイン用パスワードは全員 Password1! （BCrypt cost 10）
-- 佐藤 望 のみ password_hash = NULL（アカウント未作成の状態。API 1.4 の確認用）
--
-- ID は採番に任せ、参照はすべて名前の JOIN で解決している。
-- 明示的に ID を指定すると BIGSERIAL のシーケンスが進まず、
-- 次の INSERT で主キー衝突が起きるため。
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 社員
--
-- role = 'ADMIN' は人事部の全員と社長（決定事項3, 6）。
-- 小林 健太（人事部・アルバイト）も ADMIN になる点に注意。
-- 「人事部の全員に ADMIN」という決定の帰結がそのまま出ている例。
-- ----------------------------------------------------------------------------

INSERT INTO employees (
    employee_code, name, name_kana, email, tel, password_hash, hobby, self_pr,
    hire_date, department_id, position_id, employment_type_id, seat_id, status, role
)
SELECT
    v.code, v.name, v.kana, v.email, v.tel, v.pw, v.hobby, v.self_pr,
    v.hire_date::date,
    d.department_id,
    p.position_id,
    et.employment_type_id,
    s.seat_id,
    v.status::smallint,
    v.role
FROM (VALUES
    -- 社長。閲覧だけでなく編集も可（決定事項3）
    ('EMP00001', '高橋 誠',   'たかはし まこと', 'takahashi@frontier.example.co.jp', '03-1234-0001',
     '$2y$10$3Uf8KRUm7owSEepsPeZq9OYngk4Ts3JJfbv2K9rw1c2O/khOMrNRW', 'ゴルフ',
     '創業以来の方針である「人を育てる会社」を守り続けたいと考えています。',
     '2005-04-01', '経営企画部', '社長', '正社員', '3F-A01', 1, 'ADMIN'),

    -- 人事部（全員 ADMIN）
    ('EMP00002', '山本 由紀', 'やまもと ゆき',   'yamamoto@frontier.example.co.jp', '03-1234-0002',
     '$2y$10$3Uf8KRUm7owSEepsPeZq9OYngk4Ts3JJfbv2K9rw1c2O/khOMrNRW', '読書',
     '採用と育成を担当しています。社員のキャリア相談はいつでも歓迎です。',
     '2010-04-01', '人事部', '部長', '正社員', '3F-A02', 1, 'ADMIN'),
    ('EMP00003', '中村 彩',   'なかむら あや',   'nakamura@frontier.example.co.jp', '03-1234-0003',
     '$2y$10$3Uf8KRUm7owSEepsPeZq9OYngk4Ts3JJfbv2K9rw1c2O/khOMrNRW', 'ヨガ',
     '労務・評価制度の運用を担当。社会保険まわりの相談に対応できます。',
     '2018-04-01', '人事部', '主任', '正社員', '3F-A03', 1, 'ADMIN'),
    ('EMP00004', '小林 健太', 'こばやし けんた', 'kobayashi@frontier.example.co.jp', '03-1234-0004',
     '$2y$10$3Uf8KRUm7owSEepsPeZq9OYngk4Ts3JJfbv2K9rw1c2O/khOMrNRW', 'バスケットボール',
     '人事部で勤怠データの集計を担当しています。',
     '2025-04-01', '人事部', '一般', 'アルバイト', '3F-A04', 1, 'ADMIN'),

    -- 開発部
    ('EMP00005', '伊藤 大輔', 'いとう だいすけ', 'ito@frontier.example.co.jp', '03-1234-0005',
     '$2y$10$3Uf8KRUm7owSEepsPeZq9OYngk4Ts3JJfbv2K9rw1c2O/khOMrNRW', 'キャンプ',
     'バックエンドを中心に15年。設計レビューはいつでも声をかけてください。',
     '2008-04-01', '開発部', '部長', '正社員', '2F-B01', 1, 'EMPLOYEE'),
    ('EMP00006', '渡辺 翔',   'わたなべ しょう', 'watanabe@frontier.example.co.jp', '03-1234-0006',
     '$2y$10$3Uf8KRUm7owSEepsPeZq9OYngk4Ts3JJfbv2K9rw1c2O/khOMrNRW', 'トレイルラン',
     'React と TypeScript が得意です。社内ポータル刷新でフロントを担当しました。',
     '2017-04-01', '開発部', '主任', '正社員', '2F-B02', 1, 'EMPLOYEE'),
    ('EMP00007', '斎藤 美咲', 'さいとう みさき', 'saito@frontier.example.co.jp', '03-1234-0007',
     '$2y$10$3Uf8KRUm7owSEepsPeZq9OYngk4Ts3JJfbv2K9rw1c2O/khOMrNRW', '映画鑑賞',
     'インフラ寄りの開発を担当。Docker と AWS をよく触っています。',
     '2022-04-01', '開発部', '一般', '契約社員', '2F-B03', 1, 'EMPLOYEE'),

    -- 未着席（座席が未割当。フロアマップで空席として見える）
    ('EMP00008', '財前 修',   'ざいぜん おさむ', 'zaizen@frontier.example.co.jp', '03-1234-0008',
     '$2y$10$3Uf8KRUm7owSEepsPeZq9OYngk4Ts3JJfbv2K9rw1c2O/khOMrNRW', '将棋',
     '営業部で既存顧客を担当しています。',
     '2021-04-01', '営業部', '一般', '正社員', NULL, 1, 'EMPLOYEE'),

    -- アカウント未作成（password_hash が NULL。API 1.4 の動作確認用）
    ('EMP00009', '佐藤 望',   'さとう のぞみ',   'sato@frontier.example.co.jp', '03-1234-0009',
     NULL, NULL, NULL,
     '2026-04-01', '総務部', '一般', '正社員', '1F-C01', 1, 'EMPLOYEE'),

    -- 退職者（論理削除済み。既定の検索では出てこない）
    ('EMP00010', '田中 隆',   'たなか たかし',   'tanaka@frontier.example.co.jp', NULL,
     NULL, NULL, NULL,
     '2015-04-01', '開発部', '一般', '正社員', NULL, 0, 'EMPLOYEE')
) AS v(code, name, kana, email, tel, pw, hobby, self_pr,
       hire_date, dept, pos, emp_type, seat, status, role)
JOIN departments       d  ON d.department_name       = v.dept
JOIN positions         p  ON p.position_name         = v.pos
JOIN employment_types  et ON et.employment_type_name = v.emp_type
LEFT JOIN seats        s  ON s.seat_number           = v.seat;


-- ----------------------------------------------------------------------------
-- スキルの紐付け（API 2.1 の skill_id 検索の確認用）
-- ----------------------------------------------------------------------------

INSERT INTO employee_skills (employee_id, skill_id, proficiency, experience_years)
SELECT e.employee_id, sk.skill_id, v.proficiency::smallint, v.years::numeric(4,1)
FROM (VALUES
    ('EMP00005', 'Java',        3, 15.0),
    ('EMP00005', 'Spring Boot', 3, 8.0),
    ('EMP00005', 'PostgreSQL',  3, 12.0),
    ('EMP00006', 'TypeScript',  3, 6.0),
    ('EMP00006', 'React',       3, 5.5),
    ('EMP00006', 'JavaScript',  2, 8.0),
    ('EMP00007', 'Docker',      2, 3.0),
    ('EMP00007', 'AWS',         2, 2.5),
    ('EMP00007', 'Python',      2, 4.0),
    ('EMP00003', 'SQL',         1, 1.5),
    ('EMP00006', 'Figma',       1, 2.0)
) AS v(code, skill_name, proficiency, years)
JOIN employees e  ON e.employee_code = v.code
JOIN skills    sk ON sk.skill_name   = v.skill_name;


-- ----------------------------------------------------------------------------
-- 資格の紐付け
-- ----------------------------------------------------------------------------

INSERT INTO employee_certifications (employee_id, certification_id, acquire_date)
SELECT e.employee_id, c.certification_id, v.acquire_date::date
FROM (VALUES
    ('EMP00005', '応用情報技術者',                                     '2012-06-15'),
    ('EMP00006', '基本情報技術者',                                     '2018-11-20'),
    ('EMP00007', 'AWS認定ソリューションアーキテクト - アソシエイト',   '2024-02-10'),
    ('EMP00002', '衛生管理者（第一種）',                               '2015-08-01'),
    ('EMP00003', '日商簿記2級',                                        '2020-11-15'),
    ('EMP00008', 'TOEIC 800点以上',                                    '2023-05-28')
) AS v(code, cert_name, acquire_date)
JOIN employees      e ON e.employee_code      = v.code
JOIN certifications c ON c.certification_name = v.cert_name;


-- ----------------------------------------------------------------------------
-- 担当プロジェクトの紐付け
-- ----------------------------------------------------------------------------

INSERT INTO employee_projects (employee_id, project_id, role_in_project, start_date, end_date)
SELECT e.employee_id, pr.project_id, v.role_in_project, v.start_date::date, v.end_date::date
FROM (VALUES
    ('EMP00005', '社内ポータル刷新',   'プロジェクトマネージャー', '2024-04-01', '2025-03-31'),
    ('EMP00006', '社内ポータル刷新',   'フロントエンド担当',       '2024-04-01', '2025-03-31'),
    ('EMP00007', '社内ポータル刷新',   'インフラ担当',             '2024-06-01', '2025-03-31'),
    ('EMP00005', '人事システム更新',   'テックリード',             '2025-04-01', NULL),
    ('EMP00003', '人事システム更新',   '業務要件の取りまとめ',     '2025-04-01', NULL),
    ('EMP00008', '営業支援ツール開発', '業務要件の取りまとめ',     '2023-04-01', '2024-03-31')
) AS v(code, project_name, role_in_project, start_date, end_date)
JOIN employees e  ON e.employee_code = v.code
JOIN projects  pr ON pr.project_name = v.project_name;


-- ----------------------------------------------------------------------------
-- サンクスメッセージ（API 8.2 タイムラインの確認用。新着順で表示される）
-- ----------------------------------------------------------------------------

INSERT INTO thanks_messages (from_employee_id, to_employee_id, body, sent_at)
SELECT ef.employee_id, et.employee_id, v.body, v.sent_at::timestamptz
FROM (VALUES
    ('EMP00006', 'EMP00005', '設計レビューで的確な指摘をいただき、手戻りを防げました。ありがとうございました！', '2026-10-12 10:15:00+09'),
    ('EMP00007', 'EMP00006', 'ビルドが通らなくなったとき、すぐに原因を見つけてくれて助かりました。',             '2026-10-14 14:30:00+09'),
    ('EMP00003', 'EMP00002', '評価面談の準備を手伝っていただきありがとうございました。',                       '2026-10-15 09:00:00+09'),
    ('EMP00005', 'EMP00007', '夜間の障害対応、本当にお疲れさまでした。助かりました。',                         '2026-10-16 18:45:00+09'),
    ('EMP00008', 'EMP00003', '入社手続きの件、丁寧に案内してくださって安心できました。',                       '2026-10-17 11:20:00+09')
) AS v(from_code, to_code, body, sent_at)
JOIN employees ef ON ef.employee_code = v.from_code
JOIN employees et ON et.employee_code = v.to_code;


-- ----------------------------------------------------------------------------
-- 変更申請（API 9. の確認用）
--
-- 申請中は同一社員・同一項目で1件まで（部分UNIQUE制約）。
-- 処理済みは何件でも残せるため、同じ項目で承認済み1件＋申請中1件を入れてある。
-- ----------------------------------------------------------------------------

INSERT INTO change_requests (
    target_employee_id, target_field, before_value, after_value, reason,
    status, requested_at, reviewed_by, reviewed_at, review_comment
)
SELECT
    e.employee_id, v.target_field, v.before_value, v.after_value, v.reason,
    v.status::smallint, v.requested_at::timestamptz,
    r.employee_id, v.reviewed_at::timestamptz, v.review_comment
FROM (VALUES
    -- 申請中（status = 0）
    ('EMP00007', 'tel',           '03-1234-0007', '03-9999-0007', '携帯電話の番号が変わりました。',
     0, '2026-10-16 13:00:00+09', NULL,       NULL,                    NULL),
    -- 承認済み（status = 1）。同じ社員の tel は上で申請中なので、別項目にしてある
    ('EMP00006', 'hobby',         'ランニング',   'トレイルラン', '趣味の表記を具体的にしたいです。',
     1, '2026-09-20 10:00:00+09', 'EMP00002', '2026-09-21 09:30:00+09', '承認します。'),
    -- 却下（status = 2）
    ('EMP00008', 'department_id', '5',            '4',            '開発部への異動を希望します。',
     2, '2026-09-25 16:00:00+09', 'EMP00002', '2026-09-28 10:00:00+09', '来期の異動検討時に改めて相談しましょう。')
) AS v(code, target_field, before_value, after_value, reason,
       status, requested_at, reviewer_code, reviewed_at, review_comment)
JOIN employees e ON e.employee_code = v.code
LEFT JOIN employees r ON r.employee_code = v.reviewer_code;
