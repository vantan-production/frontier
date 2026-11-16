package jp.co.frontier.ems;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jp.co.frontier.ems.support.ApiTestBase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * マスタの登録・更新・削除（API 7.1〜7.9 の書込系）のテスト。
 *
 * <p>マスタ9種で形が同じなので、全種類を同じ観点で確認するのではなく
 * 「代表1種で CRUD を通し、残りは登録できることと権限を確認する」形にしている。
 * 参照中削除の 409 と権限は、壊れると影響が大きいので重点的に見る。
 */
class MasterAdminTest extends ApiTestBase {

    private static final String ADMIN_EMAIL = "yamamoto@frontier.example.co.jp";
    private static final String 一般社員のメール = "watanabe@frontier.example.co.jp";

    @Nested
    @DisplayName("権限")
    class 権限 {

        @Test
        @DisplayName("一般社員はマスタを登録できない（403）")
        void 一般社員は登録できない() throws Exception {
            String token = loginAndGetAccessToken(一般社員のメール);

            mockMvc.perform(post("/v1/departments")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"department_name":"新部署"}
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("一般社員はマスタを削除できない（403）")
        void 一般社員は削除できない() throws Exception {
            String token = loginAndGetAccessToken(一般社員のメール);
            Long id = masterId("/v1/skills", "skill_name", "Figma", "skill_id");

            mockMvc.perform(delete("/v1/skills/" + id)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        /** 参照（GET）は全社員が使えること。書込だけが ADMIN 限定。 */
        @Test
        @DisplayName("参照は一般社員でも使える")
        void 参照は一般社員でも使える() throws Exception {
            String token = loginAndGetAccessToken(一般社員のメール);

            mockMvc.perform(get("/v1/departments").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("部署（代表してCRUDを通す）")
    class 部署 {

        @Test
        @DisplayName("登録でき201とLocationが返る")
        void 登録() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(write(post("/v1/departments"), token, """
                            {"department_name":"情報システム部"}
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.data.department_name").value("情報システム部"))
                    .andExpect(jsonPath("$.data.department_id").exists());
        }

        @Test
        @DisplayName("更新できる")
        void 更新() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            Long id = masterId("/v1/departments", "department_name", "総務部", "department_id");

            mockMvc.perform(write(put("/v1/departments/" + id), token, """
                            {"department_name":"総務・法務部"}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.department_name").value("総務・法務部"));
        }

        @Test
        @DisplayName("所属社員がいない部署は削除できる")
        void 削除() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            String body = mockMvc.perform(write(post("/v1/departments"), token, """
                            {"department_name":"一時部署"}
                            """))
                    .andReturn().getResponse().getContentAsString();
            Long id = json(body).get("data").get("department_id").asLong();

            mockMvc.perform(delete("/v1/departments/" + id)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        }

        /**
         * 外部キー制約（RESTRICT）の違反をそのまま投げると500になる。
         * GlobalExceptionHandler が 409 に変換していることの確認。
         */
        @Test
        @DisplayName("所属社員がいる部署の削除は409（500にならない）")
        void 参照中は削除できない() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            Long 開発部 = masterId("/v1/departments", "department_name", "開発部", "department_id");

            mockMvc.perform(delete("/v1/departments/" + 開発部)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("CONFLICT"));
        }

        @Test
        @DisplayName("同名の部署は登録できない（409）")
        void 重複は登録できない() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(write(post("/v1/departments"), token, """
                            {"department_name":"開発部"}
                            """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("部署名が空だと422")
        void 必須項目なし() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(write(post("/v1/departments"), token, """
                            {"department_name":""}
                            """))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.error.details[0].field").value("department_name"));
        }

        @Test
        @DisplayName("存在しないIDの更新・削除は404")
        void 存在しないID() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(write(put("/v1/departments/999999"), token, """
                            {"department_name":"x"}
                            """))
                    .andExpect(status().isNotFound());

            mockMvc.perform(delete("/v1/departments/999999")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("他のマスタの登録")
    class 他のマスタ {

        @Test
        @DisplayName("役職・雇用形態・スキル・資格・プロジェクトを登録できる")
        void 登録できる() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(write(post("/v1/positions"), token, """
                            {"position_name":"課長補佐","rank":4}
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.rank").value(4));

            mockMvc.perform(write(post("/v1/employment-types"), token, """
                            {"employment_type_name":"業務委託"}
                            """))
                    .andExpect(status().isCreated());

            mockMvc.perform(write(post("/v1/skills"), token, """
                            {"skill_name":"Kotlin","category":"言語"}
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.category").value("言語"));

            mockMvc.perform(write(post("/v1/certifications"), token, """
                            {"certification_name":"情報処理安全確保支援士"}
                            """))
                    .andExpect(status().isCreated());

            mockMvc.perform(write(post("/v1/projects"), token, """
                            {"project_name":"勤怠システム刷新","start_date":"2026-04-01","description":"刷新"}
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.end_date").value(Matchers.nullValue()));
        }

        @Test
        @DisplayName("プロジェクトの開始日が終了日より後だと422")
        void プロジェクトの日付逆転() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(write(post("/v1/projects"), token, """
                            {"project_name":"逆転","start_date":"2026-04-01","end_date":"2025-03-31"}
                            """))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("使用中のスキルは削除できない（409）")
        void 使用中のスキル() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            Long java = masterId("/v1/skills", "skill_name", "Java", "skill_id");

            mockMvc.perform(delete("/v1/skills/" + java)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("拠点・フロア・座席")
    class 拠点系 {

        @Test
        @DisplayName("拠点→フロア→座席の順に登録できる")
        void 階層的に登録できる() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            String site = mockMvc.perform(write(post("/v1/sites"), token, """
                            {"site_name":"大阪支社","address":"大阪府大阪市..."}
                            """))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            Long siteId = json(site).get("data").get("site_id").asLong();

            String floor = mockMvc.perform(write(post("/v1/floors"), token, """
                            {"site_id":%d,"floor_name":"5階","floor_number":5,"map_image_url":"/maps/osaka_5f.png"}
                            """.formatted(siteId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.floor_number").value(5))
                    .andReturn().getResponse().getContentAsString();
            Long floorId = json(floor).get("data").get("floor_id").asLong();

            mockMvc.perform(write(post("/v1/seats"), token, """
                            {"floor_id":%d,"seat_number":"5F-A01","position_x":100,"position_y":200}
                            """.formatted(floorId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.seat_number").value("5F-A01"))
                    // 登録直後のレスポンスにも所在が入る
                    .andExpect(jsonPath("$.data.floor.floor_name").value("5階"))
                    .andExpect(jsonPath("$.data.site.site_name").value("大阪支社"));
        }

        @Test
        @DisplayName("地下は負の階数で登録できる")
        void 地下階() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            Long siteId = masterId("/v1/sites", "site_name", "本社", "site_id");

            mockMvc.perform(write(post("/v1/floors"), token, """
                            {"site_id":%d,"floor_name":"地下1階","floor_number":-1}
                            """.formatted(siteId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.floor_number").value(-1));
        }

        @Test
        @DisplayName("存在しない拠点にフロアを作ろうとすると422")
        void 存在しない拠点() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(write(post("/v1/floors"), token, """
                            {"site_id":999999,"floor_name":"1階","floor_number":1}
                            """))
                    .andExpect(status().isUnprocessableContent());
        }

        /** 着席中の座席は、誰が座っているかを伝えたいので事前確認している。 */
        @Test
        @DisplayName("着席中の座席の削除は409で、着席者の名前が返る")
        void 着席中の座席() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            Long 使用中 = employee("EMP00005").getSeat().getSeatId();

            mockMvc.perform(delete("/v1/seats/" + 使用中)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.message",
                            Matchers.containsString("伊藤 大輔")));
        }

        @Test
        @DisplayName("フロアに座席が残っていると削除できない（409）")
        void 座席が残るフロア() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            Long siteId = masterId("/v1/sites", "site_name", "本社", "site_id");
            String floors = mockMvc.perform(get("/v1/sites/" + siteId + "/floors"))
                    .andReturn().getResponse().getContentAsString();
            Long floorId = json(floors).get("data").get(2).get("floor_id").asLong();

            mockMvc.perform(delete("/v1/floors/" + floorId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isConflict());
        }
    }

    /** 権限マスタ（API 7.10 / 7.11）は前期スコープで使わない判断のため未実装。 */
    @Nested
    @DisplayName("権限マスタ（未実装）")
    class 権限マスタ {

        @Test
        @DisplayName("権限マスタのエンドポイントは存在しない（404）")
        void 未実装() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(get("/v1/permissions").header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }

    // ---------- 補助 ----------

    private MockHttpServletRequestBuilder write(
            MockHttpServletRequestBuilder builder, String token, String body) {

        return builder.header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

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
