package jp.co.frontier.ems;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jp.co.frontier.ems.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

/** アカウント作成（API 1.4）とパスワード変更（API 1.6）のテスト。 */
class AccountApiTest extends ApiTestBase {

    /** シードでパスワード未設定の社員（佐藤 望） */
    private static final String 未作成の社員番号 = "EMP00009";
    private static final String 未作成のメール = "sato@frontier.example.co.jp";

    /** シードでパスワード設定済みの社員（小林 健太） */
    private static final String 作成済みの社員番号 = "EMP00004";
    private static final String 作成済みのメール = "kobayashi@frontier.example.co.jp";

    private static final String 新パスワード = "Shiny2026pass";

    @Nested
    @DisplayName("アカウント作成")
    class アカウント作成 {

        @Test
        @DisplayName("社員番号とメールが一致すれば作成でき、Locationが返る")
        void 正常系() throws Exception {
            mockMvc.perform(create(未作成の社員番号, 未作成のメール, 新パスワード))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location",
                            "/v1/employees/" + employee(未作成の社員番号).getEmployeeId()))
                    .andExpect(jsonPath("$.data.employee_id")
                            .value(employee(未作成の社員番号).getEmployeeId().intValue()));
        }

        @Test
        @DisplayName("作成したパスワードでログインできる")
        void 作成後にログインできる() throws Exception {
            mockMvc.perform(create(未作成の社員番号, 未作成のメール, 新パスワード))
                    .andExpect(status().isCreated());

            JsonNode body = loginJson(未作成のメール, 新パスワード);
            assertThat(body.get("access_token").asString()).isNotBlank();
        }

        @Test
        @DisplayName("すでに作成済みなら409")
        void 重複() throws Exception {
            mockMvc.perform(create(作成済みの社員番号, 作成済みのメール, 新パスワード))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMPLOYEE_CODE"));
        }

        /**
         * 本人確認（メール照合）を「作成済み」判定より先に行っていることの確認。
         *
         * <p>逆順だと、メールアドレスを知らなくても社員番号だけで
         * アカウントの有無を調べられてしまう。
         */
        @Test
        @DisplayName("作成済みの社員でもメール不一致なら422（アカウントの有無を伏せる）")
        void 本人確認が先に行われる() throws Exception {
            mockMvc.perform(create(作成済みの社員番号, "wrong@frontier.example.co.jp", 新パスワード))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("存在しない社員番号は404")
        void 存在しない社員番号() throws Exception {
            mockMvc.perform(create("EMP99999", "x@frontier.example.co.jp", 新パスワード))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        /** 退職者は404。社員番号の存在有無を伏せるため403ではない。 */
        @Test
        @DisplayName("退職者は404")
        void 退職者() throws Exception {
            mockMvc.perform(create("EMP00010", "tanaka@frontier.example.co.jp", 新パスワード))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("パスワードに数字が無いと422で、項目名はsnake_case")
        void 強度不足_数字なし() throws Exception {
            mockMvc.perform(create(未作成の社員番号, 未作成のメール, "onlyletters"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.details[0].field").value("password"));
        }

        @Test
        @DisplayName("パスワードが8文字未満だと422")
        void 強度不足_短い() throws Exception {
            mockMvc.perform(create(未作成の社員番号, 未作成のメール, "Ab1"))
                    .andExpect(status().isUnprocessableEntity());
        }

        private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder create(
                String code, String email, String password) {
            return post("/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"employee_code":"%s","email":"%s","password":"%s"}
                            """.formatted(code, email, password));
        }
    }

    @Nested
    @DisplayName("パスワード変更")
    class パスワード変更 {

        private static final String 対象のメール = "watanabe@frontier.example.co.jp";

        @Test
        @DisplayName("正しい現在のパスワードなら変更でき、新パスワードでログインできる")
        void 正常系() throws Exception {
            String token = loginAndGetAccessToken(対象のメール);

            mockMvc.perform(change(token, SEED_PASSWORD, 新パスワード))
                    .andExpect(status().isNoContent());

            assertThat(loginJson(対象のメール, 新パスワード).get("access_token").asString())
                    .isNotBlank();
            // 旧パスワードでは入れない
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","password":"%s"}
                                    """.formatted(対象のメール, SEED_PASSWORD)))
                    .andExpect(status().isUnauthorized());
        }

        /**
         * 変更後は<b>変更に使った端末も含めて</b>全セッションを失効させる。
         * パスワードを変える理由が「漏れたかもしれない」である可能性を考えると、
         * 現在の端末だけ残す作りは安全でない。
         */
        @Test
        @DisplayName("変更すると全端末のセッションが失効する")
        void 全セッションが失効する() throws Exception {
            String 端末1 = loginAndGetAccessToken(対象のメール);
            String 端末2 = loginAndGetAccessToken(対象のメール);

            mockMvc.perform(get("/v1/me").header("Authorization", "Bearer " + 端末1))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/v1/me").header("Authorization", "Bearer " + 端末2))
                    .andExpect(status().isOk());

            mockMvc.perform(change(端末1, SEED_PASSWORD, 新パスワード))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/v1/me").header("Authorization", "Bearer " + 端末1))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/v1/me").header("Authorization", "Bearer " + 端末2))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("現在のパスワードが違うと422で、項目名はcurrent_password")
        void 現在のパスワードが違う() throws Exception {
            String token = loginAndGetAccessToken(対象のメール);

            mockMvc.perform(change(token, "WrongPassword1", 新パスワード))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.details[0].field").value("current_password"));
        }

        @Test
        @DisplayName("現在と同じパスワードは422で、項目名はnew_password")
        void 現在と同じ() throws Exception {
            String token = loginAndGetAccessToken(対象のメール);

            mockMvc.perform(change(token, SEED_PASSWORD, SEED_PASSWORD))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.details[0].field").value("new_password"));
        }

        @Test
        @DisplayName("新パスワードの強度不足は422で、項目名はnew_password")
        void 強度不足() throws Exception {
            String token = loginAndGetAccessToken(対象のメール);

            mockMvc.perform(change(token, SEED_PASSWORD, "onlyletters"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.details[0].field").value("new_password"));
        }

        @Test
        @DisplayName("トークン無しは401")
        void 未認証() throws Exception {
            mockMvc.perform(patch("/v1/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"current_password":"%s","new_password":"%s"}
                                    """.formatted(SEED_PASSWORD, 新パスワード)))
                    .andExpect(status().isUnauthorized());
        }

        private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder change(
                String token, String current, String next) {
            return patch("/v1/me/password")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"current_password":"%s","new_password":"%s"}
                            """.formatted(current, next));
        }
    }
}
