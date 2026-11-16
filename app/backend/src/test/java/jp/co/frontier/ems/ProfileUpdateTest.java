package jp.co.frontier.ems;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jp.co.frontier.ems.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 自己プロフィール更新（API 5.1）のテスト。 */
class ProfileUpdateTest extends ApiTestBase {

    private static final String 対象のメール = "watanabe@frontier.example.co.jp";

    @Test
    @DisplayName("趣味と自己PRを更新できる")
    void 両方更新() throws Exception {
        String token = loginAndGetAccessToken(対象のメール);

        mockMvc.perform(update(token, """
                        {"hobby":"登山、写真撮影","self_pr":"フロントエンド開発が得意です。"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hobby").value("登山、写真撮影"))
                .andExpect(jsonPath("$.data.self_pr").value("フロントエンド開発が得意です。"));
    }

    /** PATCH なので送っていない項目は変わらない。 */
    @Test
    @DisplayName("片方だけ送ると、もう片方は変わらない")
    void 片方のみ更新() throws Exception {
        String token = loginAndGetAccessToken(対象のメール);
        String 元の自己PR = employee("EMP00006").getSelfPr();

        mockMvc.perform(update(token, """
                        {"hobby":"陶芸"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hobby").value("陶芸"))
                .andExpect(jsonPath("$.data.self_pr").value(元の自己PR));
    }

    /**
     * JSON に項目が無い場合と null を明示した場合は Jackson 上で区別できないため、
     * どちらも「更新しない」として扱う。値を消すには空文字を送る。
     */
    @Test
    @DisplayName("nullを明示しても更新されない（消すには空文字を送る）")
    void nullは更新しない() throws Exception {
        String token = loginAndGetAccessToken(対象のメール);
        String 元の趣味 = employee("EMP00006").getHobby();

        mockMvc.perform(update(token, """
                        {"hobby":null}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hobby").value(元の趣味));
    }

    @Test
    @DisplayName("空文字を送ると値を消せる")
    void 空文字で消せる() throws Exception {
        String token = loginAndGetAccessToken(対象のメール);

        mockMvc.perform(update(token, """
                        {"hobby":""}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hobby").value(""));
    }

    @Test
    @DisplayName("更新後の社員情報は3.1と同形式で、本人なので機密項目を含む")
    void レスポンスは社員詳細と同形式() throws Exception {
        String token = loginAndGetAccessToken(対象のメール);

        mockMvc.perform(update(token, """
                        {"hobby":"将棋"}
                        """))
                .andExpect(jsonPath("$.data.email").value(対象のメール))
                .andExpect(jsonPath("$.data.department.department_name").value("開発部"))
                .andExpect(jsonPath("$.data.skills").isArray());
    }

    @Test
    @DisplayName("趣味が255文字を超えると422")
    void 趣味の文字数超過() throws Exception {
        String token = loginAndGetAccessToken(対象のメール);

        mockMvc.perform(update(token, """
                        {"hobby":"%s"}
                        """.formatted("あ".repeat(256))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.details[0].field").value("hobby"));
    }

    @Test
    @DisplayName("自己PRが2000文字を超えると422")
    void 自己PRの文字数超過() throws Exception {
        String token = loginAndGetAccessToken(対象のメール);

        mockMvc.perform(update(token, """
                        {"self_pr":"%s"}
                        """.formatted("あ".repeat(2001))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.details[0].field").value("self_pr"));
    }

    @Test
    @DisplayName("トークン無しは401")
    void 未認証() throws Exception {
        mockMvc.perform(patch("/v1/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hobby":"陶芸"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    /** 他人のプロフィールを触る経路が無いことの確認（/me 配下のみ）。 */
    @Test
    @DisplayName("他人の趣味は変わらない")
    void 他人には影響しない() throws Exception {
        String token = loginAndGetAccessToken(対象のメール);
        String 他人の趣味 = employee("EMP00005").getHobby();

        mockMvc.perform(update(token, """
                        {"hobby":"陶芸"}
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/employees/" + employee("EMP00005").getEmployeeId()))
                .andExpect(jsonPath("$.data.hobby").value(他人の趣味));
    }

    private MockHttpServletRequestBuilder update(String token, String body) {
        return patch("/v1/me/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
