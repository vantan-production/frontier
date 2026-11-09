package jp.co.frontier.ems;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import jp.co.frontier.ems.entity.AuthToken;
import jp.co.frontier.ems.entity.Employee;
import jp.co.frontier.ems.entity.EmployeeStatus;
import jp.co.frontier.ems.entity.TokenType;
import jp.co.frontier.ems.repository.AuthTokenRepository;
import jp.co.frontier.ems.service.TokenService;
import jp.co.frontier.ems.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

/** 認証（API 1.1 ログイン / 1.2 再発行 / 1.3 ログアウト）のテスト。 */
class AuthApiTest extends ApiTestBase {

    private static final String ADMIN_EMAIL = "yamamoto@frontier.example.co.jp";
    private static final String EMPLOYEE_EMAIL = "watanabe@frontier.example.co.jp";

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Nested
    @DisplayName("ログイン")
    class ログイン {

        @Test
        @DisplayName("正しい資格情報ならトークンと社員情報が返る")
        void 正常系() throws Exception {
            JsonNode body = loginJson(ADMIN_EMAIL, SEED_PASSWORD);

            assertThat(body.get("access_token").asString()).isNotBlank();
            assertThat(body.get("refresh_token").asString()).isNotBlank();
            // 仕様書: expires_in は 3600 秒
            assertThat(body.get("expires_in").asInt()).isEqualTo(3600);
            assertThat(body.get("employee").get("name").asString()).isEqualTo("山本 由紀");
            assertThat(body.get("employee").get("role").asString()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("アクセストークンとリフレッシュトークンは別の値である")
        void トークンは別物() throws Exception {
            JsonNode body = loginJson(ADMIN_EMAIL, SEED_PASSWORD);
            assertThat(body.get("access_token").asString())
                    .isNotEqualTo(body.get("refresh_token").asString());
        }

        /**
         * 失敗理由を区別しないことの確認。
         *
         * <p>どれで失敗したか分かると、在籍している社員のメールアドレスを
         * 総当たりで特定できてしまうため、意図的に同一メッセージにしている。
         */
        @Test
        @DisplayName("失敗理由が異なっても401と同一メッセージを返す")
        void 失敗理由を区別しない() throws Exception {
            String 存在しないメール = message(login("nobody@frontier.example.co.jp", SEED_PASSWORD));
            String パスワード違い = message(login(ADMIN_EMAIL, "WrongPassword1"));
            String 退職者 = message(login("tanaka@frontier.example.co.jp", SEED_PASSWORD));
            String アカウント未作成 = message(login("sato@frontier.example.co.jp", SEED_PASSWORD));

            assertThat(パスワード違い).isEqualTo(存在しないメール);
            assertThat(退職者).isEqualTo(存在しないメール);
            assertThat(アカウント未作成).isEqualTo(存在しないメール);
        }

        @Test
        @DisplayName("未入力は422で、項目名はJSONのキー名で返る")
        void 未入力() throws Exception {
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"password":"x"}
                                    """))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.error.details[0].field").value("email"));
        }

        private String login(String email, String password) throws Exception {
            return mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","password":"%s"}
                                    """.formatted(email, password)))
                    .andExpect(status().isUnauthorized())
                    .andReturn().getResponse().getContentAsString();
        }

        private String message(String body) {
            return json(body).get("error").get("message").asString();
        }
    }

    @Nested
    @DisplayName("トークン再発行")
    class 再発行 {

        @Test
        @DisplayName("有効なリフレッシュトークンで新しいアクセストークンが返る")
        void 正常系() throws Exception {
            String refresh = loginJson(ADMIN_EMAIL, SEED_PASSWORD).get("refresh_token").asString();

            String body = mockMvc.perform(post("/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"refresh_token":"%s"}
                                    """.formatted(refresh)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(json(body).get("access_token").asString()).isNotBlank();
            assertThat(json(body).get("expires_in").asInt()).isEqualTo(3600);
            // ローテーションしないので refresh_token は返さない
            assertThat(json(body).has("refresh_token")).isFalse();
        }

        /** アクセストークンを refresh に渡しても通ってはいけない（種別の検証）。 */
        @Test
        @DisplayName("アクセストークンをリフレッシュとして使うと401")
        void 種別が違うトークンは拒否() throws Exception {
            String access = loginJson(ADMIN_EMAIL, SEED_PASSWORD).get("access_token").asString();

            mockMvc.perform(post("/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"refresh_token":"%s"}
                                    """.formatted(access)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("存在しないトークンは401")
        void 不正なトークン() throws Exception {
            mockMvc.perform(post("/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"refresh_token":"bogus"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("ログアウト")
    class ログアウト {

        @Test
        @DisplayName("トークン無しは401")
        void 未認証() throws Exception {
            mockMvc.perform(post("/v1/auth/logout"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("ログアウト後は同じアクセストークンが使えない")
        void アクセストークンが失効する() throws Exception {
            String access = loginAndGetAccessToken(EMPLOYEE_EMAIL);

            mockMvc.perform(get("/v1/me").header("Authorization", "Bearer " + access))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/v1/auth/logout").header("Authorization", "Bearer " + access))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/v1/me").header("Authorization", "Bearer " + access))
                    .andExpect(status().isUnauthorized());
        }

        /** 仕様書 1.3: アクセスとそれに紐づくリフレッシュの両方を失効させる。 */
        @Test
        @DisplayName("ログアウトでリフレッシュトークンも連鎖して失効する")
        void リフレッシュも連鎖失効する() throws Exception {
            JsonNode login = loginJson(EMPLOYEE_EMAIL, SEED_PASSWORD);
            String access = login.get("access_token").asString();
            String refresh = login.get("refresh_token").asString();

            mockMvc.perform(post("/v1/auth/logout").header("Authorization", "Bearer " + access))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"refresh_token":"%s"}
                                    """.formatted(refresh)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("トークンの有効性（手動では確認できない時間絡みの挙動）")
    class トークンの有効性 {

        @Test
        @DisplayName("期限切れのアクセストークンは401")
        void 期限切れ() throws Exception {
            Employee 社員 = employee("EMP00006");
            TokenService.IssuedToken refresh = tokenService.issueRefreshToken(社員);
            TokenService.IssuedToken access = tokenService.issueAccessToken(社員, refresh.entity());

            // 1時間待てないので期限を過去にずらす
            access.entity().setExpiresAt(OffsetDateTime.now().minusMinutes(1));
            authTokenRepository.saveAndFlush(access.entity());

            mockMvc.perform(get("/v1/me")
                            .header("Authorization", "Bearer " + access.rawToken()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("期限が1秒でも残っていれば通る（境界）")
        void 期限内なら通る() throws Exception {
            Employee 社員 = employee("EMP00006");
            TokenService.IssuedToken refresh = tokenService.issueRefreshToken(社員);
            TokenService.IssuedToken access = tokenService.issueAccessToken(社員, refresh.entity());

            access.entity().setExpiresAt(OffsetDateTime.now().plusSeconds(30));
            authTokenRepository.saveAndFlush(access.entity());

            mockMvc.perform(get("/v1/me")
                            .header("Authorization", "Bearer " + access.rawToken()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("失効済みのトークンは401")
        void 失効済み() throws Exception {
            Employee 社員 = employee("EMP00006");
            TokenService.IssuedToken refresh = tokenService.issueRefreshToken(社員);
            TokenService.IssuedToken access = tokenService.issueAccessToken(社員, refresh.entity());

            access.entity().setRevokedAt(OffsetDateTime.now());
            authTokenRepository.saveAndFlush(access.entity());

            mockMvc.perform(get("/v1/me")
                            .header("Authorization", "Bearer " + access.rawToken()))
                    .andExpect(status().isUnauthorized());
        }

        /**
         * 退職させた社員のトークンは、レコードが有効なままでも通らない。
         * 個人情報が見えてしまうため、ここは必ず守られていないといけない。
         */
        @Test
        @DisplayName("在籍中に発行したトークンは退職させると使えなくなる")
        void 退職したら使えない() throws Exception {
            Employee 社員 = employee("EMP00006");
            String access = loginAndGetAccessToken(EMPLOYEE_EMAIL);

            mockMvc.perform(get("/v1/me").header("Authorization", "Bearer " + access))
                    .andExpect(status().isOk());

            社員.setStatus(EmployeeStatus.RETIRED);
            employeeRepository.saveAndFlush(社員);

            mockMvc.perform(get("/v1/me").header("Authorization", "Bearer " + access))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("平文のトークンはDBに保存されない（ハッシュのみ）")
        void 平文は保存されない() throws Exception {
            String access = loginAndGetAccessToken(EMPLOYEE_EMAIL);

            boolean 平文が存在する = authTokenRepository.findAll().stream()
                    .map(AuthToken::getTokenHash)
                    .anyMatch(hash -> hash.equals(access));

            assertThat(平文が存在する).isFalse();
            // ハッシュは16進64文字
            assertThat(authTokenRepository.findAll())
                    .allSatisfy(t -> assertThat(t.getTokenHash()).matches("[0-9a-f]{64}"));
        }

        @Test
        @DisplayName("ログインでアクセスとリフレッシュが親子で紐づく")
        void 親子関係() throws Exception {
            Employee 社員 = employee("EMP00006");
            TokenService.IssuedToken refresh = tokenService.issueRefreshToken(社員);
            TokenService.IssuedToken access = tokenService.issueAccessToken(社員, refresh.entity());

            assertThat(access.entity().getTokenType()).isEqualTo(TokenType.ACCESS);
            assertThat(refresh.entity().getTokenType()).isEqualTo(TokenType.REFRESH);
            assertThat(access.entity().getParentToken().getTokenId())
                    .isEqualTo(refresh.entity().getTokenId());
            assertThat(refresh.entity().getParentToken()).isNull();
        }
    }
}
