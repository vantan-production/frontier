package jp.co.frontier.ems;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import jp.co.frontier.ems.service.EmployeeCsvImportWorker;
import jp.co.frontier.ems.support.ApiTestBase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * CSV一括取込・出力（API 10.1〜10.3）のテスト。
 *
 * <p><b>取り込みの完了まではここでは確認していない。</b> 取り込みは非同期で
 * 別スレッド・別トランザクションで動くため、テストのトランザクション内で
 * 作ったジョブが非同期側から見えず成立しない。さらに取り込まれた社員は
 * テストのロールバック対象外で確定してしまい、他のテストの件数を狂わせる
 * （実際に在籍者が9名から13名に増えて社員検索のテストが落ちた）。
 *
 * <p>そのため取込ワーカーを差し替えて<b>受付までの挙動に限って</b>確認し、
 * CSVの解析・組み立ては {@code EmployeeCsvParsingTest} で単体テストしている。
 * 受付から完了までを通した確認は、非トランザクションのテストを別途
 * 用意する必要がある（未実施）。
 */
class EmployeeCsvTest extends ApiTestBase {

    /**
     * 取込ワーカーを差し替える。
     *
     * <p>本物を動かすと、非同期側が別トランザクションで社員を登録し、
     * テストのロールバックが効かないまま他のテストに影響する。
     * ここで見たいのは受付時の検証なので、ワーカーは呼ばれるだけでよい。
     */
    @MockitoBean
    private EmployeeCsvImportWorker importWorker;

    private static final String ADMIN_EMAIL = "yamamoto@frontier.example.co.jp";
    private static final String 一般社員のメール = "watanabe@frontier.example.co.jp";

    private static final String ヘッダー =
            "employee_code,name,name_kana,email,tel,hire_date,"
                    + "department_id,position_id,employment_type_id,seat_id";

    @Nested
    @DisplayName("取込の受付")
    class 受付 {

        @Test
        @DisplayName("受け付けると202とjob_idが返る")
        void 正常系() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(upload(token, ヘッダー + "\n"
                            + "EMP90001,新人 太郎,,n1@frontier.example.co.jp,,2026-04-01,1,,1,\n"
                            + "EMP90002,新人 次郎,,n2@frontier.example.co.jp,,2026-04-01,1,,1,\n"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.data.job_id", Matchers.startsWith("imp_")))
                    .andExpect(jsonPath("$.data.status").value("processing"))
                    .andExpect(jsonPath("$.data.total_rows").value(2))
                    // 処理中は件数が未確定なのでキーごと省略する
                    .andExpect(jsonPath("$.data.success_rows").doesNotExist());
        }

        @Test
        @DisplayName("BOM付きのファイルも受け付ける")
        void BOM付き() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(upload(token, "﻿" + ヘッダー + "\n"
                            + "EMP90003,新人,,n3@frontier.example.co.jp,,2026-04-01,1,,1,\n"))
                    .andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("CRLF改行でも受け付ける")
        void CRLF() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(upload(token, ヘッダー + "\r\n"
                            + "EMP90004,新人,,n4@frontier.example.co.jp,,2026-04-01,1,,1,\r\n"))
                    .andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("ヘッダーが違うと400 IMPORT_FILE_INVALID")
        void ヘッダー不正() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(upload(token, "name,email\n新人,n@example.co.jp\n"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("IMPORT_FILE_INVALID"));
        }

        @Test
        @DisplayName("空のファイルは400")
        void 空ファイル() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(upload(token, ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("IMPORT_FILE_INVALID"));
        }

        @Test
        @DisplayName("ヘッダーだけのファイルは0行として受け付ける")
        void ヘッダーのみ() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(upload(token, ヘッダー + "\n"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.data.total_rows").value(0));
        }

        @Test
        @DisplayName("行数上限を超えると422")
        void 行数上限() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            StringBuilder csv = new StringBuilder(ヘッダー).append('\n');
            for (int i = 0; i < 10_001; i++) {
                csv.append("EMP%05d,新人,,n%d@frontier.example.co.jp,,2026-04-01,1,,1,\n"
                        .formatted(i, i));
            }

            mockMvc.perform(upload(token, csv.toString()))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("一般社員は取込できない（403）")
        void 一般社員は403() throws Exception {
            String token = loginAndGetAccessToken(一般社員のメール);

            mockMvc.perform(upload(token, ヘッダー + "\n"
                            + "EMP90005,新人,,n5@frontier.example.co.jp,,2026-04-01,1,,1,\n"))
                    .andExpect(status().isForbidden());
        }

        private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
                upload(String token, String csv) {
            var file = new MockMultipartFile(
                    "file", "employees.csv", "text/csv",
                    csv.getBytes(StandardCharsets.UTF_8));

            return multipart("/v1/employees/import")
                    .file(file)
                    .header("Authorization", "Bearer " + token);
        }
    }

    @Nested
    @DisplayName("取込結果の取得")
    class 結果取得 {

        @Test
        @DisplayName("存在しないjob_idは404")
        void 存在しないジョブ() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            mockMvc.perform(get("/v1/employees/import/imp_19990101_001")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("一般社員は結果を見られない（403）")
        void 一般社員は403() throws Exception {
            String token = loginAndGetAccessToken(一般社員のメール);

            mockMvc.perform(get("/v1/employees/import/imp_19990101_001")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("CSV出力")
    class 出力 {

        @Test
        @DisplayName("Content-Typeとファイル名が仕様どおりで、BOM付きで出力される")
        void ヘッダーとBOM() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            byte[] body = mockMvc.perform(get("/v1/employees/export")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                    .andExpect(header().string("Content-Disposition",
                            Matchers.allOf(
                                    Matchers.startsWith("attachment; filename=\"employees_"),
                                    Matchers.endsWith(".csv\""))))
                    .andReturn().getResponse().getContentAsByteArray();

            String csv = new String(body, StandardCharsets.UTF_8);
            // Excel が UTF-8 と判定できるように BOM を先頭に付ける
            assertThat(csv).startsWith("﻿");
            assertThat(csv).contains("employee_code,name,name_kana");
        }

        @Test
        @DisplayName("在籍者が全件出力され、IDではなく名称が入る")
        void 内容() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);

            String csv = mockMvc.perform(get("/v1/employees/export")
                            .header("Authorization", "Bearer " + token))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

            // ヘッダー1行 + 在籍9名
            assertThat(csv.lines().count()).isEqualTo(10);
            // 人が開いて確認するファイルなので、部署IDではなく部署名を出す
            assertThat(csv).contains("伊藤 大輔").contains("開発部").contains("2F-B01");
            // 退職者は既定では含まれない
            assertThat(csv).doesNotContain("田中 隆");
        }

        @Test
        @DisplayName("検索条件で絞り込める（page/per_pageは無効で全件）")
        void 絞り込み() throws Exception {
            String token = loginAndGetAccessToken(ADMIN_EMAIL);
            Long 開発部 = 部署ID("開発部");

            String csv = mockMvc.perform(get("/v1/employees/export")
                            .param("department_id", 開発部.toString())
                            .param("per_page", "1")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

            // per_page=1 でも開発部の3名すべてが出る
            assertThat(csv.lines().count()).isEqualTo(4);
        }

        @Test
        @DisplayName("一般社員は出力できない（403）")
        void 一般社員は403() throws Exception {
            String token = loginAndGetAccessToken(一般社員のメール);

            mockMvc.perform(get("/v1/employees/export")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    private Long 部署ID(String name) throws Exception {
        String body = mockMvc.perform(get("/v1/departments"))
                .andReturn().getResponse().getContentAsString();
        for (var node : json(body).get("data")) {
            if (name.equals(node.get("department_name").asString())) {
                return node.get("department_id").asLong();
            }
        }
        throw new IllegalStateException("シードに存在しない: " + name);
    }
}
