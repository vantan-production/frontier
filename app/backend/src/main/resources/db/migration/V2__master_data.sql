-- ============================================================================
-- マスタの初期データ
--
-- 業務上の参照データなので、どの環境でも投入される前提で versioned migration に置く。
-- 動作確認用の社員データは V3__dev_employees.sql に分けてある
-- （実在しない人のデータなので、本番相当の環境に入れたくない場合は V3 を外す）。
--
-- エンドポイント設計書.md「7. 実装フェーズ」でマスタの書込系API（7.3〜7.9）を
-- フェーズ3に後回しできるのは、ここで初期データを流すため。
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 部署・役職・雇用形態
-- ----------------------------------------------------------------------------

-- 人事部は role = 'ADMIN' の付与先なので必須（決定事項3, 6）
INSERT INTO departments (department_name) VALUES
    ('経営企画部'),
    ('人事部'),
    ('総務部'),
    ('開発部'),
    ('営業部');

-- rank は小さいほど上位。社長は role = 'ADMIN' の付与先なので必須
INSERT INTO positions (position_name, rank) VALUES
    ('社長', 1),
    ('部長', 2),
    ('課長', 3),
    ('主任', 4),
    ('一般', 5);

INSERT INTO employment_types (employment_type_name) VALUES
    ('正社員'),
    ('契約社員'),
    ('派遣社員'),
    ('アルバイト');


-- ----------------------------------------------------------------------------
-- 拠点・フロア・座席（API 4. フロアマップ用）
-- ----------------------------------------------------------------------------

INSERT INTO sites (site_name, address) VALUES
    ('本社',         '東京都渋谷区○○1-2-3'),
    ('技術センター', '神奈川県横浜市○○4-5-6');

INSERT INTO floors (site_id, floor_name, floor_number, map_image_url)
SELECT s.site_id, v.floor_name, v.floor_number, v.map_image_url
FROM (VALUES
    ('本社',         '1階', 1, '/maps/hq_1f.png'),
    ('本社',         '2階', 2, '/maps/hq_2f.png'),
    ('本社',         '3階', 3, '/maps/hq_3f.png'),
    ('技術センター', '1階', 1, '/maps/tc_1f.png'),
    ('技術センター', '2階', 2, '/maps/tc_2f.png')
) AS v(site_name, floor_name, floor_number, map_image_url)
JOIN sites s ON s.site_name = v.site_name;

-- position_x / position_y はマップ画像上の座標（px）
INSERT INTO seats (floor_id, seat_number, position_x, position_y)
SELECT f.floor_id, v.seat_number, v.position_x, v.position_y
FROM (VALUES
    ('本社',         3, '3F-A01', 120, 100),
    ('本社',         3, '3F-A02', 220, 100),
    ('本社',         3, '3F-A03', 320, 100),
    ('本社',         3, '3F-A04', 120, 220),
    ('本社',         3, '3F-A05', 220, 220),
    ('本社',         3, '3F-A06', 320, 220),
    ('本社',         2, '2F-B01', 120, 100),
    ('本社',         2, '2F-B02', 220, 100),
    ('本社',         2, '2F-B03', 320, 100),
    ('本社',         2, '2F-B04', 120, 220),
    ('技術センター', 1, '1F-C01', 150, 150),
    ('技術センター', 1, '1F-C02', 260, 150),
    ('技術センター', 1, '1F-C03', 370, 150)
) AS v(site_name, floor_number, seat_number, position_x, position_y)
JOIN sites  s ON s.site_name = v.site_name
JOIN floors f ON f.site_id = s.site_id AND f.floor_number = v.floor_number;


-- ----------------------------------------------------------------------------
-- スキル・資格・プロジェクト（API 2.1 の絞り込み検索用）
-- ----------------------------------------------------------------------------

INSERT INTO skills (skill_name, category) VALUES
    ('Java',        '言語'),
    ('Python',      '言語'),
    ('TypeScript',  '言語'),
    ('JavaScript',  '言語'),
    ('SQL',         '言語'),
    ('Spring Boot', 'フレームワーク'),
    ('React',       'フレームワーク'),
    ('Vue.js',      'フレームワーク'),
    ('PostgreSQL',  'データベース'),
    ('MySQL',       'データベース'),
    ('Docker',      'インフラ'),
    ('AWS',         'インフラ'),
    ('Git',         'ツール'),
    ('Figma',       'ツール');

INSERT INTO certifications (certification_name) VALUES
    ('基本情報技術者'),
    ('応用情報技術者'),
    ('AWS認定ソリューションアーキテクト - アソシエイト'),
    ('TOEIC 800点以上'),
    ('日商簿記2級'),
    ('衛生管理者（第一種）');

INSERT INTO projects (project_name, start_date, end_date, description) VALUES
    ('社内ポータル刷新',   '2024-04-01', '2025-03-31', '社内ポータルを React + Spring Boot で再構築'),
    ('人事システム更新',   '2025-04-01', NULL,         '人事評価システムのリプレース。継続中'),
    ('営業支援ツール開発', '2023-04-01', '2024-03-31', '営業部向けの案件管理ツールを新規開発');
