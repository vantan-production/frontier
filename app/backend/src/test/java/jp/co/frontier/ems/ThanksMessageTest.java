package jp.co.frontier.ems;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * サンクスメッセージ（API 8.1〜8.3）のテスト。
 *
 * <p>シードには5件入っている（V3__dev_employees.sql）。
 */
class ThanksMessageTest extends ApiTestBase {

    private static final String 送信者のメール = "watanabe@frontier.example.co.jp";

    @Nested
    @DisplayName("送信")
    class 送信 {

        @Test
        @DisplayName("送信できて201とLocationが返り、送信者はトークンから決まる")
        void 正常系() throws Exception {
            String token = loginAndGetAccessToken(送信者のメール);
            Long 宛先 = employee("EMP00005").getEmployeeId();

            mockMvc.perform(send(token, """
                            {"to_employee_id":%d,"body":"レビューありがとうございました！"}
                            """.formatted(宛先)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.data.body").value("レビューありがとうございました！"))
                    // 送信者はリクエストに含めていないがトークンから決まる
                    .andExpect(jsonPath("$.data.from_employee.name").value("渡辺 翔"))
                    .andExpect(jsonPath("$.data.to_employee.name").value("伊藤 大輔"))
                    .andExpect(jsonPath("$.data.sent_at").exists());
        }

        /** タイムラインに出すものなので機密項目は含めない。 */
        @Test
        @DisplayName("送信者・受信者にemail/telは含まれない")
        void 機密項目は含まれない() throws Exception {
            String token = loginAndGetAccessToken(送信者のメール);
            Long 宛先 = employee("EMP00005").getEmployeeId();

            mockMvc.perform(send(token, """
                            {"to_employee_id":%d,"body":"ありがとう"}
                            """.formatted(宛先)))
                    .andExpect(jsonPath("$.data.from_employee.email").doesNotExist())
                    .andExpect(jsonPath("$.data.to_employee.tel").doesNotExist());
        }

        /** DBの CHECK 制約でも防がれるが、500にしないため事前に弾いている。 */
        @Test
        @DisplayName("自分宛ては422")
        void 自分宛て() throws Exception {
            String token = loginAndGetAccessToken(送信者のメール);
            Long 自分 = employee("EMP00006").getEmployeeId();

            mockMvc.perform(send(token, """
                            {"to_employee_id":%d,"body":"自分にありがとう"}
                            """.formatted(自分)))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.error.details[0].field").value("to_employee_id"));
        }

        @Test
        @DisplayName("退職者宛ては404")
        void 退職者宛て() throws Exception {
            String token = loginAndGetAccessToken(送信者のメール);
            Long 退職者 = employee("EMP00010").getEmployeeId();

            mockMvc.perform(send(token, """
                            {"to_employee_id":%d,"body":"ありがとう"}
                            """.formatted(退職者)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("存在しない宛先は404")
        void 存在しない宛先() throws Exception {
            String token = loginAndGetAccessToken(送信者のメール);

            mockMvc.perform(send(token, """
                            {"to_employee_id":999999,"body":"ありがとう"}
                            """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("本文が空だと422")
        void 本文が空() throws Exception {
            String token = loginAndGetAccessToken(送信者のメール);
            Long 宛先 = employee("EMP00005").getEmployeeId();

            mockMvc.perform(send(token, """
                            {"to_employee_id":%d,"body":""}
                            """.formatted(宛先)))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.error.details[0].field").value("body"));
        }

        @Test
        @DisplayName("本文が300文字を超えると422")
        void 本文が長すぎる() throws Exception {
            String token = loginAndGetAccessToken(送信者のメール);
            Long 宛先 = employee("EMP00005").getEmployeeId();

            mockMvc.perform(send(token, """
                            {"to_employee_id":%d,"body":"%s"}
                            """.formatted(宛先, "あ".repeat(301))))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("トークン無しは401")
        void 未認証() throws Exception {
            mockMvc.perform(post("/v1/thanks-messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"to_employee_id":1,"body":"ありがとう"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        private MockHttpServletRequestBuilder send(String token, String body) {
            return post("/v1/thanks-messages")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body);
        }
    }

    @Nested
    @DisplayName("タイムライン")
    class タイムライン {

        @Test
        @DisplayName("新着順で返る")
        void 新着順() throws Exception {
            mockMvc.perform(get("/v1/thanks-messages"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(5)))
                    // シードで最も新しいのは財前→中村（10/17）
                    .andExpect(jsonPath("$.data[0].from_employee.name").value("財前 修"))
                    .andExpect(jsonPath("$.data[0].to_employee.name").value("中村 彩"))
                    .andExpect(jsonPath("$.meta.total_count").value(5));
        }

        @Test
        @DisplayName("送信した分が先頭に来る")
        void 送信後は先頭() throws Exception {
            String token = loginAndGetAccessToken(送信者のメール);
            Long 宛先 = employee("EMP00005").getEmployeeId();

            mockMvc.perform(post("/v1/thanks-messages")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"to_employee_id":%d,"body":"最新のメッセージ"}
                                    """.formatted(宛先)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/v1/thanks-messages"))
                    .andExpect(jsonPath("$.data", Matchers.hasSize(6)))
                    .andExpect(jsonPath("$.data[0].body").value("最新のメッセージ"));
        }

        @Test
        @DisplayName("ページングできる")
        void ページング() throws Exception {
            mockMvc.perform(get("/v1/thanks-messages")
                            .param("per_page", "2")
                            .param("page", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(2)))
                    .andExpect(jsonPath("$.meta.page").value(2))
                    .andExpect(jsonPath("$.meta.total_pages").value(3));
        }

        @Test
        @DisplayName("per_pageの上限超過は422")
        void per_pageの上限() throws Exception {
            mockMvc.perform(get("/v1/thanks-messages").param("per_page", "101"))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.error.details[0].field").value("per_page"));
        }
    }

    @Nested
    @DisplayName("社員別の送受信履歴")
    class 社員別履歴 {

        @Test
        @DisplayName("既定は送信・受信の両方が返る")
        void 既定は両方() throws Exception {
            Long 中村 = employee("EMP00003").getEmployeeId();

            // 中村は山本へ1件送信、財前から1件受信
            mockMvc.perform(get("/v1/employees/" + 中村 + "/thanks-messages"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(2)));
        }

        @Test
        @DisplayName("type=sentで送信分だけ返る")
        void 送信分のみ() throws Exception {
            Long 中村 = employee("EMP00003").getEmployeeId();

            mockMvc.perform(get("/v1/employees/" + 中村 + "/thanks-messages")
                            .param("type", "sent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.data[0].from_employee.name").value("中村 彩"));
        }

        @Test
        @DisplayName("type=receivedで受信分だけ返る")
        void 受信分のみ() throws Exception {
            Long 中村 = employee("EMP00003").getEmployeeId();

            mockMvc.perform(get("/v1/employees/" + 中村 + "/thanks-messages")
                            .param("type", "received"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.data[0].to_employee.name").value("中村 彩"));
        }

        @Test
        @DisplayName("やりとりが無い社員は空で返る")
        void やりとりなし() throws Exception {
            Long 佐藤 = employee("EMP00009").getEmployeeId();

            mockMvc.perform(get("/v1/employees/" + 佐藤 + "/thanks-messages"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(0)))
                    .andExpect(jsonPath("$.meta.total_count").value(0));
        }

        @Test
        @DisplayName("存在しない社員は404")
        void 存在しない社員() throws Exception {
            mockMvc.perform(get("/v1/employees/999999/thanks-messages"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("typeの値が不正だと422")
        void typeが不正() throws Exception {
            Long 中村 = employee("EMP00003").getEmployeeId();

            mockMvc.perform(get("/v1/employees/" + 中村 + "/thanks-messages")
                            .param("type", "bogus"))
                    .andExpect(status().isUnprocessableContent());
        }
    }
}
