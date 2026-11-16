package jp.co.frontier.ems;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jp.co.frontier.ems.support.ApiTestBase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 社員検索（API 2.1）のテスト。
 *
 * <p>シードは在籍9名・退職1名。スキルの保有状況は
 * 伊藤(EMP00005): Java / Spring Boot / PostgreSQL、
 * 渡辺(EMP00006): TypeScript / React / JavaScript / Figma、
 * 斎藤(EMP00007): Docker / AWS / Python、
 * 中村(EMP00003): SQL。
 */
class EmployeeSearchTest extends ApiTestBase {

    private static final String ADMIN_EMAIL = "yamamoto@frontier.example.co.jp";
    private static final String 一般社員のメール = "watanabe@frontier.example.co.jp";

    @Nested
    @DisplayName("既定の挙動")
    class 既定 {

        @Test
        @DisplayName("条件なしでは在籍者のみが社員番号順で返る")
        void 条件なし() throws Exception {
            mockMvc.perform(get("/v1/employees"))
                    .andExpect(status().isOk())
                    // 在籍9名（退職者の田中は含まれない）
                    .andExpect(jsonPath("$.data", Matchers.hasSize(9)))
                    .andExpect(jsonPath("$.data[0].name").value("高橋 誠"))
                    .andExpect(jsonPath("$.meta.total_count").value(9))
                    .andExpect(jsonPath("$.meta.page").value(1))
                    .andExpect(jsonPath("$.meta.per_page").value(20))
                    .andExpect(jsonPath("$.meta.total_pages").value(1));
        }

        /** 仕様書 2.1: 一覧には email / tel を含めない（ADMINでも）。 */
        @Test
        @DisplayName("ADMINでも一覧にemail/telは含まれない")
        void 一覧に機密項目は含まれない() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(get("/v1/employees").header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.data[0].email").doesNotExist())
                    .andExpect(jsonPath("$.data[0].tel").doesNotExist())
                    // 代わりに部署・役職・スキルが入る
                    .andExpect(jsonPath("$.data[0].department.department_name").exists());
        }
    }

    @Nested
    @DisplayName("絞り込み")
    class 絞り込み {

        @Test
        @DisplayName("部署で絞り込める")
        void 部署() throws Exception {
            Long 開発部 = masterId("/v1/departments", "department_name", "開発部", "department_id");

            mockMvc.perform(get("/v1/employees").param("department_id", 開発部.toString()))
                    .andExpect(status().isOk())
                    // 開発部は伊藤・渡辺・斎藤の3名（退職者の田中は除外）
                    .andExpect(jsonPath("$.data", Matchers.hasSize(3)));
        }

        @Test
        @DisplayName("複数の部署をカンマ区切りで指定できる")
        void 複数部署() throws Exception {
            Long 開発部 = masterId("/v1/departments", "department_name", "開発部", "department_id");
            Long 人事部 = masterId("/v1/departments", "department_name", "人事部", "department_id");

            mockMvc.perform(get("/v1/employees")
                            .param("department_id", 開発部 + "," + 人事部))
                    .andExpect(status().isOk())
                    // 開発部3名 + 人事部3名
                    .andExpect(jsonPath("$.data", Matchers.hasSize(6)));
        }

        @Test
        @DisplayName("キーワードは氏名・趣味・自己PRに部分一致する")
        void キーワード() throws Exception {
            mockMvc.perform(get("/v1/employees").param("keyword", "渡辺"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name").value("渡辺 翔"));

            // 趣味にも当たる（高橋 誠の趣味はゴルフ）
            mockMvc.perform(get("/v1/employees").param("keyword", "ゴルフ"))
                    .andExpect(jsonPath("$.data", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name").value("高橋 誠"));
        }

        @Test
        @DisplayName("キーワードは氏名カナにも当たる")
        void キーワードはカナにも当たる() throws Exception {
            mockMvc.perform(get("/v1/employees").param("keyword", "わたなべ"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name").value("渡辺 翔"));
        }

        @Test
        @DisplayName("勤務拠点で絞り込める（座席経由）")
        void 拠点() throws Exception {
            Long 本社 = masterId("/v1/sites", "site_name", "本社", "site_id");

            mockMvc.perform(get("/v1/employees").param("site_id", 本社.toString()))
                    .andExpect(status().isOk())
                    // 本社に着席しているのは3F×4名 + 2F×3名 = 7名
                    // （財前は未着席、佐藤は技術センター）
                    .andExpect(jsonPath("$.data", Matchers.hasSize(7)));
        }

        @Test
        @DisplayName("雇用形態で絞り込める")
        void 雇用形態() throws Exception {
            Long アルバイト = masterId(
                    "/v1/employment-types", "employment_type_name", "アルバイト",
                    "employment_type_id");

            mockMvc.perform(get("/v1/employees").param("employment_type_id", アルバイト.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name").value("小林 健太"));
        }
    }

    /**
     * スキルの AND / OR はこのAPIで一番壊れやすいところ。
     *
     * <p>中間テーブルを JOIN + IN で書くと AND が「いずれかを持つ」の意味に
     * なってしまうため、実装は EXISTS の副問い合わせで組んでいる。
     */
    @Nested
    @DisplayName("スキルの AND / OR")
    class スキル検索 {

        @Test
        @DisplayName("単一スキルで絞り込める")
        void 単一() throws Exception {
            mockMvc.perform(search("skill_id", skillId("Java")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name").value("伊藤 大輔"));
        }

        @Test
        @DisplayName("既定はANDで、両方を持つ社員だけが返る")
        void 既定はAND() throws Exception {
            // Java は伊藤のみ、React は渡辺のみ。両方持つ社員はいない
            mockMvc.perform(search("skill_id", skillId("Java") + "," + skillId("React")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(0)))
                    .andExpect(jsonPath("$.meta.total_count").value(0));
        }

        @Test
        @DisplayName("ANDで両方を持つ社員は返る")
        void ANDで該当あり() throws Exception {
            // 伊藤は Java と Spring Boot の両方を持つ
            mockMvc.perform(search("skill_id", skillId("Java") + "," + skillId("Spring Boot")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name").value("伊藤 大輔"));
        }

        @Test
        @DisplayName("ORではいずれかを持つ社員が返る")
        void OR() throws Exception {
            mockMvc.perform(get("/v1/employees")
                            .param("skill_id", skillId("Java") + "," + skillId("React"))
                            .param("skill_match", "or"))
                    .andExpect(status().isOk())
                    // 伊藤（Java）と渡辺（React）
                    .andExpect(jsonPath("$.data", Matchers.hasSize(2)));
        }

        /** AND を JOIN で書くと同じ社員が複数行に増えて件数が狂う。その回帰防止。 */
        @Test
        @DisplayName("ORで複数スキルを持つ社員が重複して返らない")
        void ORでも重複しない() throws Exception {
            // 伊藤は Java / Spring Boot / PostgreSQL の3つを持つ
            mockMvc.perform(get("/v1/employees")
                            .param("skill_id", skillId("Java") + "," + skillId("Spring Boot")
                                    + "," + skillId("PostgreSQL"))
                            .param("skill_match", "or"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.meta.total_count").value(1));
        }

        @Test
        @DisplayName("一覧行にスキルが入る")
        void 一覧にスキルが入る() throws Exception {
            mockMvc.perform(search("skill_id", skillId("Java")))
                    .andExpect(jsonPath("$.data[0].skills", Matchers.hasSize(3)))
                    .andExpect(jsonPath("$.data[0].skills[0].skill_name").exists())
                    .andExpect(jsonPath("$.data[0].skills[0].proficiency")
                            .value(Matchers.isA(Integer.class)));
        }

        private MockHttpServletRequestBuilder search(String name, Object value) {
            return get("/v1/employees").param(name, String.valueOf(value));
        }

        private Long skillId(String skillName) throws Exception {
            return masterId("/v1/skills", "skill_name", skillName, "skill_id");
        }
    }

    @Nested
    @DisplayName("退職者の扱い")
    class 退職者 {

        @Test
        @DisplayName("既定では退職者が含まれない")
        void 既定では除外() throws Exception {
            mockMvc.perform(get("/v1/employees").param("keyword", "田中"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(0)));
        }

        @Test
        @DisplayName("ADMINはstatus=0で退職者を検索できる")
        void ADMINは検索できる() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(get("/v1/employees")
                            .param("status", "0")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name").value("田中 隆"));
        }

        @Test
        @DisplayName("一般社員がstatus=0を指定すると403")
        void 一般社員は403() throws Exception {
            String token = loginAndGetAccessToken(一般社員のメール);

            mockMvc.perform(get("/v1/employees")
                            .param("status", "0")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("未認証がstatus=0を指定すると403")
        void 未認証は403() throws Exception {
            mockMvc.perform(get("/v1/employees").param("status", "0"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("並び順とページネーション")
    class 並び順とページング {

        @Test
        @DisplayName("sortで昇順・降順を切り替えられる")
        void 並び順() throws Exception {
            mockMvc.perform(get("/v1/employees").param("sort", "hire_date"))
                    .andExpect(status().isOk())
                    // 入社が最も古いのは高橋 誠（2005-04-01）
                    .andExpect(jsonPath("$.data[0].name").value("高橋 誠"));

            mockMvc.perform(get("/v1/employees").param("sort", "-hire_date"))
                    .andExpect(status().isOk())
                    // 最も新しいのは佐藤 望（2026-04-01）
                    .andExpect(jsonPath("$.data[0].name").value("佐藤 望"));
        }

        /** 許可した項目以外を渡すと、属性が見つからず500になってしまうので422で弾く。 */
        @Test
        @DisplayName("許可していないsortは422")
        void 不正なsort() throws Exception {
            mockMvc.perform(get("/v1/employees").param("sort", "password_hash"))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.error.details[0].field").value("sort"));
        }

        @Test
        @DisplayName("ページ番号は1始まりで、metaに総件数が入る")
        void ページング() throws Exception {
            mockMvc.perform(get("/v1/employees")
                            .param("per_page", "4")
                            .param("page", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(4)))
                    .andExpect(jsonPath("$.meta.page").value(1))
                    .andExpect(jsonPath("$.meta.total_count").value(9))
                    .andExpect(jsonPath("$.meta.total_pages").value(3));

            mockMvc.perform(get("/v1/employees")
                            .param("per_page", "4")
                            .param("page", "3"))
                    .andExpect(status().isOk())
                    // 9件を4件ずつなので3ページ目は1件
                    .andExpect(jsonPath("$.data", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.meta.page").value(3));
        }

        @Test
        @DisplayName("per_pageが上限100を超えると422")
        void per_pageの上限() throws Exception {
            mockMvc.perform(get("/v1/employees").param("per_page", "101"))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.error.details[0].field").value("per_page"));
        }

        @Test
        @DisplayName("pageが0以下だと422")
        void pageの下限() throws Exception {
            mockMvc.perform(get("/v1/employees").param("page", "0"))
                    .andExpect(status().isUnprocessableContent());
        }
    }

    /** マスタ名から ID を引く。シードの採番に依存しないようにするため。 */
    private Long masterId(String path, String nameKey, String nameValue, String idKey)
            throws Exception {
        String body = mockMvc.perform(get(path)).andReturn().getResponse().getContentAsString();
        for (var node : json(body).get("data")) {
            if (nameValue.equals(node.get(nameKey).asString())) {
                return node.get(idKey).asLong();
            }
        }
        throw new IllegalStateException("シードに存在しない: " + nameValue);
    }
}
