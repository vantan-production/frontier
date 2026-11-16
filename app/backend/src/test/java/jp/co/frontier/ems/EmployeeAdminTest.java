package jp.co.frontier.ems;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jp.co.frontier.ems.entity.EmployeeStatus;
import jp.co.frontier.ems.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 社員情報管理（API 6.1〜6.3）と紐付け（6.4〜6.9）のテスト。
 *
 * <p>いずれも ADMIN 専用なので、権限の確認を最優先に置いている。
 */
class EmployeeAdminTest extends ApiTestBase {

    private static final String ADMIN_EMAIL = "yamamoto@frontier.example.co.jp";
    private static final String 一般社員のメール = "watanabe@frontier.example.co.jp";

    /**
     * ADMIN 専用エンドポイントの権限確認。
     *
     * <p>{@code @PreAuthorize} の例外を共通エラーハンドラで受けないと
     * 500 になってしまうため、ステータスまで固定する。
     */
    @Nested
    @DisplayName("権限")
    class 権限 {

        @Test
        @DisplayName("一般社員が社員登録すると403")
        void 一般社員は登録できない() throws Exception {
            String token = loginAndGetAccessToken(一般社員のメール);

            mockMvc.perform(post("/v1/employees")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(新規社員のJSON("EMP90001", "新人 太郎", "newbie@frontier.example.co.jp")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("一般社員が社員を論理削除すると403")
        void 一般社員は削除できない() throws Exception {
            String token = loginAndGetAccessToken(一般社員のメール);
            Long id = employee("EMP00005").getEmployeeId();

            mockMvc.perform(delete("/v1/employees/" + id)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("一般社員がスキルを紐付けると403")
        void 一般社員は紐付けできない() throws Exception {
            String token = loginAndGetAccessToken(一般社員のメール);
            Long id = employee("EMP00005").getEmployeeId();

            mockMvc.perform(post("/v1/employees/" + id + "/skills")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"skill_id":%d,"proficiency":2}
                                    """.formatted(skillId("Python"))))
                    .andExpect(status().isForbidden());
        }

        /** 未認証でも403。ログインすれば通るのかを区別して返さないため。 */
        @Test
        @DisplayName("未認証でも403（401ではない）")
        void 未認証は403() throws Exception {
            mockMvc.perform(post("/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(新規社員のJSON("EMP90002", "新人 次郎", "n2@frontier.example.co.jp")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("社員の登録")
    class 登録 {

        @Test
        @DisplayName("登録できてLocationが返り、パスワードは未設定のまま")
        void 正常系() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            String body = mockMvc.perform(post("/v1/employees")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(新規社員のJSON("EMP90001", "新人 太郎", "newbie@frontier.example.co.jp")))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.data.name").value("新人 太郎"))
                    // ADMIN の操作なので機密項目を含む
                    .andExpect(jsonPath("$.data.email").value("newbie@frontier.example.co.jp"))
                    .andExpect(jsonPath("$.data.status").value(1))
                    .andReturn().getResponse().getContentAsString();

            Long id = json(body).get("data").get("employee_id").asLong();
            // パスワードは本人がアカウント作成（API 1.4）で設定するまで未設定
            assertThat(employeeRepository.findById(id).orElseThrow().getPasswordHash()).isNull();
        }

        @Test
        @DisplayName("社員番号が重複すると409")
        void 社員番号の重複() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(post("/v1/employees")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(新規社員のJSON("EMP00005", "別人", "other@frontier.example.co.jp")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMPLOYEE_CODE"));
        }

        @Test
        @DisplayName("メールアドレスが重複すると409")
        void メールの重複() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(post("/v1/employees")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(新規社員のJSON("EMP90003", "別人", "ito@frontier.example.co.jp")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMAIL"));
        }

        /** 座席のUNIQUE制約違反をそのまま投げると500になるため、事前に確認して409にしている。 */
        @Test
        @DisplayName("他社員が使用中の座席を指定すると409")
        void 座席の二重割当() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            Long 使用中の座席 = employee("EMP00005").getSeat().getSeatId();

            mockMvc.perform(post("/v1/employees")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"employee_code":"EMP90004","name":"新人","email":"n4@frontier.example.co.jp",
                                     "hire_date":"2026-04-01","department_id":%d,"employment_type_id":%d,
                                     "seat_id":%d}
                                    """.formatted(開発部(), 正社員(), 使用中の座席)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("CONFLICT"));
        }

        @Test
        @DisplayName("必須項目が無いと422で、項目名はsnake_case")
        void 必須項目なし() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(post("/v1/employees")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"名前だけ"}
                                    """))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.error.details[*].field",
                            org.hamcrest.Matchers.hasItem("employee_code")));
        }

        @Test
        @DisplayName("存在しない部署を指定すると422")
        void 存在しない部署() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(post("/v1/employees")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"employee_code":"EMP90005","name":"新人","email":"n5@frontier.example.co.jp",
                                     "hire_date":"2026-04-01","department_id":999999,"employment_type_id":%d}
                                    """.formatted(正社員())))
                    .andExpect(status().isUnprocessableContent());
        }
    }

    @Nested
    @DisplayName("社員の更新")
    class 更新 {

        @Test
        @DisplayName("PUTは全項目の置換で、送らなかった任意項目はnullになる")
        void 全項目置換() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            var 対象 = employee("EMP00005");
            Long id = 対象.getEmployeeId();

            // 元は役職「部長」で座席あり。どちらも送らない
            mockMvc.perform(put("/v1/employees/" + id)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"employee_code":"EMP00005","name":"伊藤 大輔","email":"ito@frontier.example.co.jp",
                                     "hire_date":"2008-04-01","department_id":%d,"employment_type_id":%d}
                                    """.formatted(開発部(), 正社員())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.position").value(org.hamcrest.Matchers.nullValue()))
                    .andExpect(jsonPath("$.data.seat").value(org.hamcrest.Matchers.nullValue()));
        }

        @Test
        @DisplayName("自分自身の社員番号は重複扱いにならない")
        void 自分自身は重複としない() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            Long id = employee("EMP00005").getEmployeeId();

            mockMvc.perform(put("/v1/employees/" + id)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"employee_code":"EMP00005","name":"伊藤 大輔(改)","email":"ito@frontier.example.co.jp",
                                     "hire_date":"2008-04-01","department_id":%d,"employment_type_id":%d}
                                    """.formatted(開発部(), 正社員())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("伊藤 大輔(改)"));
        }

        @Test
        @DisplayName("存在しない社員の更新は404")
        void 存在しない社員() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(put("/v1/employees/999999")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(新規社員のJSON("EMP90006", "誰か", "x@frontier.example.co.jp")))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("社員の論理削除")
    class 論理削除 {

        @Test
        @DisplayName("statusが0になり座席が空き、レコードは残る")
        void 正常系() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            var 対象 = employee("EMP00005");
            Long id = 対象.getEmployeeId();
            Long 元の座席 = 対象.getSeat().getSeatId();

            mockMvc.perform(delete("/v1/employees/" + id)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            var 削除後 = employeeRepository.findById(id).orElseThrow();
            assertThat(削除後.getStatus()).isEqualTo(EmployeeStatus.RETIRED);
            assertThat(削除後.getSeat()).isNull();

            // 空いた座席は他の社員に割り当てられる
            mockMvc.perform(post("/v1/employees")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"employee_code":"EMP90007","name":"新人","email":"n7@frontier.example.co.jp",
                                     "hire_date":"2026-04-01","department_id":%d,"employment_type_id":%d,
                                     "seat_id":%d}
                                    """.formatted(開発部(), 正社員(), 元の座席)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("既に退職済みなら404")
        void 既に退職済み() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            Long id = employee("EMP00010").getEmployeeId();

            mockMvc.perform(delete("/v1/employees/" + id)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }

        /** 退職させた社員のトークンは失効する（API 6.3 / DB設計書 4章）。 */
        @Test
        @DisplayName("退職させるとその社員のトークンが失効する")
        void トークンが失効する() throws Exception {
            String 対象のトークン = loginAndGetAccessToken(一般社員のメール);
            String adminToken = loginAndGetAccessToken(ADMIN_EMAIL);
            Long id = employee("EMP00006").getEmployeeId();

            mockMvc.perform(get("/v1/me").header("Authorization", "Bearer " + 対象のトークン))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/v1/employees/" + id)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/v1/me").header("Authorization", "Bearer " + 対象のトークン))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("紐付け")
    class 紐付け {

        @Test
        @DisplayName("スキルを追加・更新・解除できる")
        void スキル() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            Long id = employee("EMP00003").getEmployeeId();
            Long python = skillId("Python");

            mockMvc.perform(link(HttpMethod.POST, "/v1/employees/" + id + "/skills", token, """
                            {"skill_id":%d,"proficiency":2,"experience_years":3.5}
                            """.formatted(python)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.skill_name").value("Python"))
                    .andExpect(jsonPath("$.data.proficiency").value(2))
                    .andExpect(jsonPath("$.data.experience_years").value(3.5));

            mockMvc.perform(link(HttpMethod.PATCH,
                            "/v1/employees/" + id + "/skills/" + python, token, """
                            {"proficiency":3,"experience_years":4.0}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.proficiency").value(3));

            mockMvc.perform(delete("/v1/employees/" + id + "/skills/" + python)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            mockMvc.perform(delete("/v1/employees/" + id + "/skills/" + python)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("同じスキルの二重登録は409")
        void スキルの重複() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            Long id = employee("EMP00005").getEmployeeId();

            // 伊藤は既に Java を持っている
            mockMvc.perform(link(HttpMethod.POST, "/v1/employees/" + id + "/skills", token, """
                            {"skill_id":%d,"proficiency":3}
                            """.formatted(skillId("Java"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("CONFLICT"));
        }

        @Test
        @DisplayName("習熟度が範囲外だと422")
        void 習熟度の範囲外() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            Long id = employee("EMP00003").getEmployeeId();

            mockMvc.perform(link(HttpMethod.POST, "/v1/employees/" + id + "/skills", token, """
                            {"skill_id":%d,"proficiency":4}
                            """.formatted(skillId("Python"))))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.error.details[0].field").value("proficiency"));
        }

        @Test
        @DisplayName("資格を追加・解除できる")
        void 資格() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            Long id = employee("EMP00006").getEmployeeId();
            Long 資格 = masterId("/v1/certifications", "certification_name",
                    "応用情報技術者", "certification_id");

            mockMvc.perform(link(HttpMethod.POST,
                            "/v1/employees/" + id + "/certifications", token, """
                            {"certification_id":%d,"acquire_date":"2024-06-15"}
                            """.formatted(資格)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.certification_name").value("応用情報技術者"))
                    .andExpect(jsonPath("$.data.acquire_date").value("2024-06-15"));

            mockMvc.perform(delete("/v1/employees/" + id + "/certifications/" + 資格)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("資格の取得日に未来日を指定すると422")
        void 資格の未来日() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            Long id = employee("EMP00006").getEmployeeId();
            Long 資格 = masterId("/v1/certifications", "certification_name",
                    "応用情報技術者", "certification_id");

            mockMvc.perform(link(HttpMethod.POST,
                            "/v1/employees/" + id + "/certifications", token, """
                            {"certification_id":%d,"acquire_date":"2099-01-01"}
                            """.formatted(資格)))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.error.details[0].field").value("acquire_date"));
        }

        @Test
        @DisplayName("プロジェクトを追加・解除できる")
        void プロジェクト() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            Long id = employee("EMP00003").getEmployeeId();
            Long pj = masterId("/v1/projects", "project_name", "営業支援ツール開発", "project_id");

            mockMvc.perform(link(HttpMethod.POST, "/v1/employees/" + id + "/projects", token, """
                            {"project_id":%d,"role_in_project":"要件整理","start_date":"2023-04-01","end_date":"2024-03-31"}
                            """.formatted(pj)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.project_name").value("営業支援ツール開発"))
                    .andExpect(jsonPath("$.data.role_in_project").value("要件整理"));

            mockMvc.perform(delete("/v1/employees/" + id + "/projects/" + pj)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("参画日が離任日より後だと422")
        void プロジェクトの日付逆転() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            Long id = employee("EMP00003").getEmployeeId();
            Long pj = masterId("/v1/projects", "project_name", "営業支援ツール開発", "project_id");

            mockMvc.perform(link(HttpMethod.POST, "/v1/employees/" + id + "/projects", token, """
                            {"project_id":%d,"start_date":"2025-04-01","end_date":"2024-03-31"}
                            """.formatted(pj)))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("存在しない社員への紐付けは404")
        void 存在しない社員() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(link(HttpMethod.POST, "/v1/employees/999999/skills", token, """
                            {"skill_id":%d,"proficiency":2}
                            """.formatted(skillId("Python"))))
                    .andExpect(status().isNotFound());
        }

        private MockHttpServletRequestBuilder link(
                HttpMethod method, String path, String token, String body) {

            return org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .request(method, path)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body);
        }
    }

    // ---------- 補助 ----------

    private String 新規社員のJSON(String code, String name, String email) throws Exception {
        return """
                {"employee_code":"%s","name":"%s","email":"%s","hire_date":"2026-04-01",
                 "department_id":%d,"employment_type_id":%d}
                """.formatted(code, name, email, 開発部(), 正社員());
    }

    private Long 開発部() throws Exception {
        return masterId("/v1/departments", "department_name", "開発部", "department_id");
    }

    private Long 正社員() throws Exception {
        return masterId("/v1/employment-types", "employment_type_name", "正社員",
                "employment_type_id");
    }

    private Long skillId(String skillName) throws Exception {
        return masterId("/v1/skills", "skill_name", skillName, "skill_id");
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
