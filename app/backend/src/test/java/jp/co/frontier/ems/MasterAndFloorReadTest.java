package jp.co.frontier.ems;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jp.co.frontier.ems.support.ApiTestBase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * マスタ参照（API 7.1〜7.9 の GET）とフロアマップ参照（API 4.1〜4.4）のテスト。
 *
 * <p>件数はシードデータ（V2__master_data.sql）に合わせている。
 */
class MasterAndFloorReadTest extends ApiTestBase {

    @Nested
    @DisplayName("マスタ参照")
    class マスタ参照 {

        @Test
        @DisplayName("部署一覧が返る")
        void 部署() throws Exception {
            mockMvc.perform(get("/v1/departments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(5)))
                    .andExpect(jsonPath("$.data[0].department_name").value("経営企画部"));
        }

        /** 役職は序列順。画面側で並べ直さなくて済むようにするため。 */
        @Test
        @DisplayName("役職一覧は序列（rank）順で返る")
        void 役職() throws Exception {
            mockMvc.perform(get("/v1/positions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(5)))
                    .andExpect(jsonPath("$.data[0].position_name").value("社長"))
                    .andExpect(jsonPath("$.data[0].rank").value(1))
                    .andExpect(jsonPath("$.data[4].position_name").value("一般"));
        }

        @Test
        @DisplayName("雇用形態一覧が返る")
        void 雇用形態() throws Exception {
            mockMvc.perform(get("/v1/employment-types"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(4)))
                    .andExpect(jsonPath("$.data[0].employment_type_name").value("正社員"));
        }

        @Test
        @DisplayName("スキル一覧は分類つきで返る")
        void スキル() throws Exception {
            mockMvc.perform(get("/v1/skills"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(14)))
                    .andExpect(jsonPath("$.data[0].skill_name").exists())
                    .andExpect(jsonPath("$.data[0].category").exists());
        }

        @Test
        @DisplayName("資格一覧が返る")
        void 資格() throws Exception {
            mockMvc.perform(get("/v1/certifications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(6)));
        }

        @Test
        @DisplayName("プロジェクト一覧は継続中（end_date が null）を含んで返る")
        void プロジェクト() throws Exception {
            mockMvc.perform(get("/v1/projects"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(3)))
                    // 開始日の新しい順なので先頭は人事システム更新（継続中）
                    .andExpect(jsonPath("$.data[0].project_name").value("人事システム更新"))
                    .andExpect(jsonPath("$.data[0].end_date").value(Matchers.nullValue()));
        }
    }

    @Nested
    @DisplayName("フロアマップ参照")
    class フロアマップ {

        @Test
        @DisplayName("拠点一覧が返る")
        void 拠点一覧() throws Exception {
            mockMvc.perform(get("/v1/sites"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(2)))
                    .andExpect(jsonPath("$.data[0].site_name").value("本社"))
                    .andExpect(jsonPath("$.data[0].address").exists());
        }

        @Test
        @DisplayName("拠点内のフロア一覧は階数順で返る")
        void フロア一覧() throws Exception {
            Long siteId = 本社のID();

            mockMvc.perform(get("/v1/sites/" + siteId + "/floors"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", Matchers.hasSize(3)))
                    .andExpect(jsonPath("$.data[0].floor_number").value(1))
                    .andExpect(jsonPath("$.data[2].floor_number").value(3))
                    .andExpect(jsonPath("$.data[0].map_image_url").exists());
        }

        @Test
        @DisplayName("存在しない拠点のフロア一覧は404")
        void 存在しない拠点() throws Exception {
            mockMvc.perform(get("/v1/sites/999999/floors"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("フロア内の座席一覧に在席者が入り、空席はemployeeがnull")
        void 座席一覧() throws Exception {
            Long floorId = 本社3階のID();

            mockMvc.perform(get("/v1/floors/" + floorId + "/seats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.floor.floor_name").value("3階"))
                    // 本社3階は座席6席、うち4席に在席（シードどおり）
                    .andExpect(jsonPath("$.data.seats", Matchers.hasSize(6)))
                    .andExpect(jsonPath("$.data.seats[0].seat_number").value("3F-A01"))
                    .andExpect(jsonPath("$.data.seats[0].employee.name").value("高橋 誠"))
                    .andExpect(jsonPath("$.data.seats[0].employee.department.department_name")
                            .value("経営企画部"))
                    .andExpect(jsonPath("$.data.seats[0].position_x").value(120))
                    // 3F-A05 / 3F-A06 は空席
                    .andExpect(jsonPath("$.data.seats[4].employee").value(Matchers.nullValue()))
                    .andExpect(jsonPath("$.data.seats[5].employee").value(Matchers.nullValue()));
        }

        /** 在席者の要約に機密項目が混ざっていないことの確認。地図上に出すデータなので。 */
        @Test
        @DisplayName("座席一覧の在席者にemail/telは含まれない")
        void 座席一覧に機密項目は含まれない() throws Exception {
            Long floorId = 本社3階のID();

            mockMvc.perform(get("/v1/floors/" + floorId + "/seats"))
                    .andExpect(jsonPath("$.data.seats[0].employee.email").doesNotExist())
                    .andExpect(jsonPath("$.data.seats[0].employee.tel").doesNotExist());
        }

        @Test
        @DisplayName("存在しないフロアの座席一覧は404")
        void 存在しないフロア() throws Exception {
            mockMvc.perform(get("/v1/floors/999999/seats"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("社員の座席が拠点・フロアつきで返る")
        void 社員の座席() throws Exception {
            Long id = employee("EMP00005").getEmployeeId();

            mockMvc.perform(get("/v1/employees/" + id + "/seat"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.seat_number").value("2F-B01"))
                    .andExpect(jsonPath("$.data.floor.floor_name").value("2階"))
                    .andExpect(jsonPath("$.data.site.site_name").value("本社"));
        }

        /** 仕様書 4.4: 座席未登録の社員は 200 で data が null（404ではない）。 */
        @Test
        @DisplayName("未着席の社員は200でdataがnull")
        void 未着席の社員() throws Exception {
            Long id = employee("EMP00008").getEmployeeId();

            mockMvc.perform(get("/v1/employees/" + id + "/seat"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(Matchers.nullValue()));
        }

        @Test
        @DisplayName("存在しない社員の座席取得は404")
        void 存在しない社員() throws Exception {
            mockMvc.perform(get("/v1/employees/999999/seat"))
                    .andExpect(status().isNotFound());
        }

        private Long 本社のID() {
            return json(sites()).get("data").get(0).get("site_id").asLong();
        }

        private Long 本社3階のID() throws Exception {
            String body = mockMvc.perform(get("/v1/sites/" + 本社のID() + "/floors"))
                    .andReturn().getResponse().getContentAsString();
            // 階数順なので3階は3件目
            return json(body).get("data").get(2).get("floor_id").asLong();
        }

        private String sites() {
            try {
                return mockMvc.perform(get("/v1/sites"))
                        .andReturn().getResponse().getContentAsString();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
