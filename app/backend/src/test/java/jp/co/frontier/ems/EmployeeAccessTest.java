package jp.co.frontier.ems;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jp.co.frontier.ems.support.ApiTestBase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 社員情報の閲覧制御とエラー形式のテスト。
 *
 * <p>機密項目の出し分けは漏れると個人情報が露出するため、
 * 閲覧者の4パターンすべてを確認する。
 */
class EmployeeAccessTest extends ApiTestBase {

    private static final String ADMIN_EMAIL = "yamamoto@frontier.example.co.jp";
    private static final String 一般社員のメール = "watanabe@frontier.example.co.jp";
    private static final String 一般社員の社員番号 = "EMP00006";

    /** 他人（伊藤 大輔・開発部長） */
    private static final String 他人の社員番号 = "EMP00005";
    private static final String 他人のメール = "ito@frontier.example.co.jp";

    @Nested
    @DisplayName("機密項目（email / tel）の出し分け")
    class 機密項目 {

        @Test
        @DisplayName("未認証では他人のemail/telがキーごと省略される")
        void 未認証() throws Exception {
            mockMvc.perform(get(詳細(他人の社員番号)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("伊藤 大輔"))
                    .andExpect(jsonPath("$.data.email").doesNotExist())
                    .andExpect(jsonPath("$.data.tel").doesNotExist());
        }

        @Test
        @DisplayName("一般社員が他人を見てもemail/telは省略される")
        void 一般社員が他人を見る() throws Exception {
            String token = loginAndGetAccessToken(一般社員のメール);

            mockMvc.perform(get(詳細(他人の社員番号)).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").doesNotExist())
                    .andExpect(jsonPath("$.data.tel").doesNotExist());
        }

        @Test
        @DisplayName("ADMINは他人のemail/telを閲覧できる")
        void ADMINが他人を見る() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(get(詳細(他人の社員番号)).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value(他人のメール))
                    .andExpect(jsonPath("$.data.tel").exists());
        }

        @Test
        @DisplayName("本人は自分のemail/telを閲覧できる")
        void 本人が自分を見る() throws Exception {
            String token = loginAndGetAccessToken(一般社員のメール);

            mockMvc.perform(get(詳細(一般社員の社員番号)).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value(一般社員のメール));
        }

        @Test
        @DisplayName("GET /me には必ずemail/telが含まれる")
        void me() throws Exception {
            String token = loginAndGetAccessToken(一般社員のメール);

            mockMvc.perform(get("/v1/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value(一般社員のメール))
                    .andExpect(jsonPath("$.data.tel").exists());
        }
    }

    @Nested
    @DisplayName("退職者の閲覧")
    class 退職者 {

        /** 退職者（田中 隆・status=0） */
        private static final String 退職者の社員番号 = "EMP00010";

        /** 403ではなく404。退職者の社員IDを総当たりで特定できないようにするため。 */
        @Test
        @DisplayName("一般社員には404（存在自体を伏せる）")
        void 一般社員() throws Exception {
            String token = loginAndGetAccessToken(一般社員のメール);

            mockMvc.perform(get(詳細(退職者の社員番号)).header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("未認証にも404")
        void 未認証() throws Exception {
            mockMvc.perform(get(詳細(退職者の社員番号)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("ADMINは閲覧でき、statusは0で返る")
        void ADMIN() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(get(詳細(退職者の社員番号)).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("田中 隆"))
                    .andExpect(jsonPath("$.data.status").value(0))
                    // 退職時に座席を空けている。
                    // 機密項目以外は値が null でもキーを残す設計なので、
                    // キーの不在ではなく null であることを確認する
                    .andExpect(jsonPath("$.data.seat").value(Matchers.nullValue()));
        }
    }

    @Nested
    @DisplayName("レスポンス構造")
    class レスポンス構造 {

        @Test
        @DisplayName("区分値は数値で返る（statusとproficiency）")
        void 区分値は数値() throws Exception {
            mockMvc.perform(get(詳細(他人の社員番号)))
                    .andExpect(jsonPath("$.data.status").value(1))
                    .andExpect(jsonPath("$.data.skills[0].proficiency")
                            .value(Matchers.isA(Integer.class)));
        }

        @Test
        @DisplayName("座席には拠点名とフロア名が含まれる")
        void 座席の入れ子() throws Exception {
            mockMvc.perform(get(詳細(他人の社員番号)))
                    .andExpect(jsonPath("$.data.seat.seat_number").value("2F-B01"))
                    .andExpect(jsonPath("$.data.seat.floor_name").value("2階"))
                    .andExpect(jsonPath("$.data.seat.site_name").value("本社"));
        }

        @Test
        @DisplayName("未着席の社員はseatがnullで200")
        void 未着席() throws Exception {
            // 財前 修（EMP00008）はシードで座席未割当
            mockMvc.perform(get(詳細("EMP00008")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.seat").value(Matchers.nullValue()))
                    // キー自体は存在する（省略されるのは email / tel のみ）
                    .andExpect(jsonPath("$.data.department").exists());
        }

        @Test
        @DisplayName("一覧は data で包まれる")
        void dataで包まれる() throws Exception {
            mockMvc.perform(get("/v1/departments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].department_name").exists());
        }
    }

    /**
     * エラー形式のテスト。
     *
     * <p>ここは実装中に2回壊した箇所（存在しないURLと許可されないメソッドが
     * 500で返っていた）なので、回帰しないよう固定する。
     */
    @Nested
    @DisplayName("エラー形式")
    class エラー形式 {

        @Test
        @DisplayName("存在しないURLは404 NOT_FOUND（500にならない）")
        void 存在しないURL() throws Exception {
            mockMvc.perform(get("/v1/nonexistent"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("許可されないメソッドは405（500にならない）")
        void 許可されないメソッド() throws Exception {
            mockMvc.perform(delete("/v1/departments"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
        }

        @Test
        @DisplayName("存在しない社員IDは404")
        void 存在しない社員ID() throws Exception {
            mockMvc.perform(get("/v1/employees/999999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("エラーは常に error.code / error.message の形で返る")
        void エラーの形() throws Exception {
            mockMvc.perform(get("/v1/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").exists())
                    .andExpect(jsonPath("$.error.message").exists())
                    // VALIDATION_ERROR 以外では details を省略する
                    .andExpect(jsonPath("$.error.details").doesNotExist());
        }
    }

    private String 詳細(String employeeCode) {
        return "/v1/employees/" + employee(employeeCode).getEmployeeId();
    }
}
