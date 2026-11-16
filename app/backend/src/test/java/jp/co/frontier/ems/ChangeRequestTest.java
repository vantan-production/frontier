package jp.co.frontier.ems;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * 基本情報変更申請（API 9.1〜9.4）のテスト。
 *
 * <p>シードには3件入っている（申請中1・承認1・却下1）。
 */
class ChangeRequestTest extends ApiTestBase {

    private static final String ADMIN_EMAIL = "yamamoto@frontier.example.co.jp";
    private static final String 申請者のメール = "watanabe@frontier.example.co.jp";
    private static final String 別の社員のメール = "ito@frontier.example.co.jp";

    @Nested
    @DisplayName("申請の作成")
    class 作成 {

        @Test
        @DisplayName("申請できて、変更前の値はサーバが記録する")
        void 正常系() throws Exception {
            String token = loginAndGetAccessToken(申請者のメール);
            String 元の電話番号 = employee("EMP00006").getTel();

            mockMvc.perform(create(token, """
                            {"target_field":"tel","after_value":"090-1111-2222","reason":"携帯に変更"}
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.target_field").value("tel"))
                    .andExpect(jsonPath("$.data.after_value").value("090-1111-2222"))
                    // クライアントは before_value を送っていない
                    .andExpect(jsonPath("$.data.before_value").value(元の電話番号))
                    .andExpect(jsonPath("$.data.status").value(0))
                    .andExpect(jsonPath("$.data.target_employee.name").value("渡辺 翔"));
        }

        /**
         * 任意のカラム名を受け付けると role や password_hash を書き換える申請を
         * 作れてしまうため、許可した項目だけを受け付ける。
         */
        @Test
        @DisplayName("許可していない項目は422")
        void 許可外の項目() throws Exception {
            String token = loginAndGetAccessToken(申請者のメール);

            mockMvc.perform(create(token, """
                            {"target_field":"role","after_value":"ADMIN"}
                            """))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.error.details[0].field").value("target_field"));

            mockMvc.perform(create(token, """
                            {"target_field":"password_hash","after_value":"x"}
                            """))
                    .andExpect(status().isUnprocessableContent());
        }

        /** 趣味・自己PRは本人が直接編集できるので申請の対象外（仕様書 9.1）。 */
        @Test
        @DisplayName("趣味・自己PRは申請の対象外で422")
        void 直接編集できる項目() throws Exception {
            String token = loginAndGetAccessToken(申請者のメール);

            mockMvc.perform(create(token, """
                            {"target_field":"hobby","after_value":"陶芸"}
                            """))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("同じ項目で申請中のものがあると409")
        void 同一項目の重複() throws Exception {
            String token = loginAndGetAccessToken(申請者のメール);

            mockMvc.perform(create(token, """
                            {"target_field":"tel","after_value":"090-1111-2222"}
                            """))
                    .andExpect(status().isCreated());

            mockMvc.perform(create(token, """
                            {"target_field":"tel","after_value":"090-3333-4444"}
                            """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("別の項目なら同時に申請できる")
        void 別項目は同時に申請できる() throws Exception {
            String token = loginAndGetAccessToken(申請者のメール);

            mockMvc.perform(create(token, """
                            {"target_field":"tel","after_value":"090-1111-2222"}
                            """))
                    .andExpect(status().isCreated());

            mockMvc.perform(create(token, """
                            {"target_field":"name_kana","after_value":"わたなべ しょう"}
                            """))
                    .andExpect(status().isCreated());
        }

        /** 承認時まで気づけないと「承認したのに反映できない」状態になるため申請時に検証する。 */
        @Test
        @DisplayName("存在しない部署への申請は422")
        void 存在しない部署() throws Exception {
            String token = loginAndGetAccessToken(申請者のメール);

            mockMvc.perform(create(token, """
                            {"target_field":"department_id","after_value":"999999"}
                            """))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("IDが数値でないと422")
        void IDが数値でない() throws Exception {
            String token = loginAndGetAccessToken(申請者のメール);

            mockMvc.perform(create(token, """
                            {"target_field":"department_id","after_value":"開発部"}
                            """))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("日付の形式が不正だと422")
        void 日付の形式不正() throws Exception {
            String token = loginAndGetAccessToken(申請者のメール);

            mockMvc.perform(create(token, """
                            {"target_field":"hire_date","after_value":"2026/04/01"}
                            """))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("使用中の座席への申請は409")
        void 使用中の座席() throws Exception {
            String token = loginAndGetAccessToken(申請者のメール);
            Long 使用中 = employee("EMP00005").getSeat().getSeatId();

            mockMvc.perform(create(token, """
                            {"target_field":"seat_id","after_value":"%d"}
                            """.formatted(使用中)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("トークン無しは401")
        void 未認証() throws Exception {
            mockMvc.perform(post("/v1/change-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"target_field":"tel","after_value":"090-0000-0000"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        private MockHttpServletRequestBuilder create(String token, String body) {
            return post("/v1/change-requests")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body);
        }
    }

    @Nested
    @DisplayName("一覧・詳細")
    class 一覧と詳細 {

        @Test
        @DisplayName("ADMINは全件見られる")
        void ADMINは全件() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(get("/v1/change-requests")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(3)))
                    .andExpect(jsonPath("$.meta.total_count").value(3));
        }

        @Test
        @DisplayName("一般社員は自分の申請だけ見られる")
        void 一般社員は自分の分だけ() throws Exception {
            String token = loginAndGetAccessToken(申請者のメール);

            // シードで渡辺の申請は hobby の承認済み1件
            mockMvc.perform(get("/v1/change-requests")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.data[0].target_employee.name").value("渡辺 翔"));
        }

        @Test
        @DisplayName("statusで絞り込める")
        void statusで絞り込み() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(get("/v1/change-requests")
                            .param("status", "0")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.data[0].status").value(0));
        }

        @Test
        @DisplayName("他人の申請詳細は403")
        void 他人の詳細は403() throws Exception {
            String adminToken = loginAndGetAccessToken(ADMIN_EMAIL);
            String 他人のトークン = loginAndGetAccessToken(別の社員のメール);

            String body = mockMvc.perform(get("/v1/change-requests")
                            .param("status", "0")
                            .header("Authorization", "Bearer " + adminToken))
                    .andReturn().getResponse().getContentAsString();
            Long requestId = json(body).get("data").get(0).get("request_id").asLong();

            mockMvc.perform(get("/v1/change-requests/" + requestId)
                            .header("Authorization", "Bearer " + 他人のトークン))
                    .andExpect(status().isForbidden());

            // ADMIN なら見られる
            mockMvc.perform(get("/v1/change-requests/" + requestId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("存在しない申請は404")
        void 存在しない申請() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(get("/v1/change-requests/999999")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("statusの値が不正だと422")
        void statusが不正() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(get("/v1/change-requests")
                            .param("status", "9")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnprocessableContent());
        }
    }

    @Nested
    @DisplayName("承認・却下")
    class 承認却下 {

        /** 承認すると対象社員の情報も同時に更新される（仕様書 9.3）。 */
        @Test
        @DisplayName("承認すると対象社員の情報が実際に更新される")
        void 承認で反映される() throws Exception {
            String 申請者トークン = loginAndGetAccessToken(申請者のメール);
            String adminToken = loginAndGetAccessToken(ADMIN_EMAIL);

            String created = mockMvc.perform(post("/v1/change-requests")
                            .header("Authorization", "Bearer " + 申請者トークン)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"target_field":"tel","after_value":"090-1111-2222","reason":"携帯に変更"}
                                    """))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            Long requestId = json(created).get("data").get("request_id").asLong();

            mockMvc.perform(review(adminToken, requestId, """
                            {"status":1,"comment":"承認します"}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value(1))
                    .andExpect(jsonPath("$.data.reviewed_by.name").value("山本 由紀"))
                    .andExpect(jsonPath("$.data.reviewed_at").exists());

            assertThat(employee("EMP00006").getTel()).isEqualTo("090-1111-2222");
        }

        @Test
        @DisplayName("却下では対象社員の情報は変わらない")
        void 却下では反映されない() throws Exception {
            String 申請者トークン = loginAndGetAccessToken(申請者のメール);
            String adminToken = loginAndGetAccessToken(ADMIN_EMAIL);
            String 元の電話番号 = employee("EMP00006").getTel();

            String created = mockMvc.perform(post("/v1/change-requests")
                            .header("Authorization", "Bearer " + 申請者トークン)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"target_field":"tel","after_value":"090-9999-9999"}
                                    """))
                    .andReturn().getResponse().getContentAsString();
            Long requestId = json(created).get("data").get("request_id").asLong();

            mockMvc.perform(review(adminToken, requestId, """
                            {"status":2,"comment":"今回は見送ります"}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value(2));

            assertThat(employee("EMP00006").getTel()).isEqualTo(元の電話番号);
        }

        @Test
        @DisplayName("一般社員は承認できない（403）")
        void 一般社員は承認できない() throws Exception {
            String adminToken = loginAndGetAccessToken(ADMIN_EMAIL);
            String 一般トークン = loginAndGetAccessToken(別の社員のメール);

            String body = mockMvc.perform(get("/v1/change-requests")
                            .param("status", "0")
                            .header("Authorization", "Bearer " + adminToken))
                    .andReturn().getResponse().getContentAsString();
            Long requestId = json(body).get("data").get(0).get("request_id").asLong();

            mockMvc.perform(review(一般トークン, requestId, """
                            {"status":1}
                            """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("処理済みの申請を再度処理すると422")
        void 処理済み() throws Exception {
            String adminToken = loginAndGetAccessToken(ADMIN_EMAIL);

            String body = mockMvc.perform(get("/v1/change-requests")
                            .param("status", "1")
                            .header("Authorization", "Bearer " + adminToken))
                    .andReturn().getResponse().getContentAsString();
            Long 承認済み = json(body).get("data").get(0).get("request_id").asLong();

            mockMvc.perform(review(adminToken, 承認済み, """
                            {"status":2}
                            """))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("statusに0（申請中）を指定すると422")
        void 申請中には戻せない() throws Exception {
            String adminToken = loginAndGetAccessToken(ADMIN_EMAIL);

            String body = mockMvc.perform(get("/v1/change-requests")
                            .param("status", "0")
                            .header("Authorization", "Bearer " + adminToken))
                    .andReturn().getResponse().getContentAsString();
            Long requestId = json(body).get("data").get(0).get("request_id").asLong();

            mockMvc.perform(review(adminToken, requestId, """
                            {"status":0}
                            """))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.error.details[0].field").value("status"));
        }

        private MockHttpServletRequestBuilder review(String token, Long requestId, String body) {
            return patch("/v1/change-requests/" + requestId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body);
        }
    }

    @Nested
    @DisplayName("取り下げ")
    class 取り下げ {

        @Test
        @DisplayName("申請者本人は申請中のものを取り下げられる")
        void 正常系() throws Exception {
            String token = loginAndGetAccessToken(申請者のメール);

            String created = mockMvc.perform(post("/v1/change-requests")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"target_field":"tel","after_value":"090-1111-2222"}
                                    """))
                    .andReturn().getResponse().getContentAsString();
            Long requestId = json(created).get("data").get("request_id").asLong();

            mockMvc.perform(delete("/v1/change-requests/" + requestId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/v1/change-requests/" + requestId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("他人の申請は取り下げられない（403）")
        void 他人の申請() throws Exception {
            String 申請者トークン = loginAndGetAccessToken(申請者のメール);
            String 他人のトークン = loginAndGetAccessToken(別の社員のメール);

            String created = mockMvc.perform(post("/v1/change-requests")
                            .header("Authorization", "Bearer " + 申請者トークン)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"target_field":"tel","after_value":"090-1111-2222"}
                                    """))
                    .andReturn().getResponse().getContentAsString();
            Long requestId = json(created).get("data").get("request_id").asLong();

            mockMvc.perform(delete("/v1/change-requests/" + requestId)
                            .header("Authorization", "Bearer " + 他人のトークン))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("処理済みの申請は取り下げられない（422）")
        void 処理済み() throws Exception {
            String adminToken = loginAndGetAccessToken(ADMIN_EMAIL);
            String 申請者トークン = loginAndGetAccessToken(申請者のメール);

            String body = mockMvc.perform(get("/v1/change-requests")
                            .param("status", "1")
                            .header("Authorization", "Bearer " + adminToken))
                    .andReturn().getResponse().getContentAsString();
            Long 承認済み = json(body).get("data").get(0).get("request_id").asLong();

            mockMvc.perform(delete("/v1/change-requests/" + 承認済み)
                            .header("Authorization", "Bearer " + 申請者トークン))
                    .andExpect(status().isUnprocessableContent());
        }
    }
}
