package jp.co.frontier.ems.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CSVの解析・組み立ての単体テスト。
 *
 * <p>Spring の文脈もDBも使わない。カンマや引用符の扱いは取り違えると
 * 列がずれて静かに壊れるため、ここだけを切り出して確認する。
 */
class EmployeeCsvParsingTest {

    @Nested
    @DisplayName("行の分割")
    class 分割 {

        @Test
        @DisplayName("単純なカンマ区切りを分割できる")
        void 単純() {
            assertThat(EmployeeCsvService.分割する("a,b,c"))
                    .containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("空の値を保持する")
        void 空の値() {
            assertThat(EmployeeCsvService.分割する("a,,c"))
                    .containsExactly("a", "", "c");
            assertThat(EmployeeCsvService.分割する("a,b,"))
                    .containsExactly("a", "b", "");
        }

        /** 氏名や住所にカンマが入ると、単純な split では列がずれる。 */
        @Test
        @DisplayName("引用符の中のカンマは区切りにしない")
        void 引用符の中のカンマ() {
            assertThat(EmployeeCsvService.分割する("a,\"大阪府大阪市,北区\",c"))
                    .containsExactly("a", "大阪府大阪市,北区", "c");
        }

        @Test
        @DisplayName("連続する二重引用符は値としての引用符になる")
        void 二重引用符() {
            assertThat(EmployeeCsvService.分割する("a,\"彼は\"\"すごい\"\"\",c"))
                    .containsExactly("a", "彼は\"すごい\"", "c");
        }
    }

    @Nested
    @DisplayName("行の変換")
    class 変換 {

        private static final String 正しい行 =
                "EMP90001,新人 太郎,しんじん たろう,n@example.co.jp,03-0000-0000,"
                        + "2026-04-01,1,2,3,4";

        @Test
        @DisplayName("全項目を変換できる")
        void 正常系() {
            var request = EmployeeCsvService.行を変換する(
                    EmployeeCsvService.分割する(正しい行));

            assertThat(request.employeeCode()).isEqualTo("EMP90001");
            assertThat(request.name()).isEqualTo("新人 太郎");
            assertThat(request.hireDate()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(request.departmentId()).isEqualTo(1L);
            assertThat(request.seatId()).isEqualTo(4L);
        }

        @Test
        @DisplayName("空欄はnullになる（任意項目を省略できる）")
        void 空欄はnull() {
            var request = EmployeeCsvService.行を変換する(EmployeeCsvService.分割する(
                    "EMP90002,新人,,n2@example.co.jp,,2026-04-01,1,,3,"));

            assertThat(request.nameKana()).isNull();
            assertThat(request.tel()).isNull();
            assertThat(request.positionId()).isNull();
            assertThat(request.seatId()).isNull();
        }

        @Test
        @DisplayName("前後の空白を取り除く")
        void 空白の除去() {
            var request = EmployeeCsvService.行を変換する(EmployeeCsvService.分割する(
                    " EMP90003 , 新人 , , n3@example.co.jp , , 2026-04-01 , 1 , , 3 , "));

            assertThat(request.employeeCode()).isEqualTo("EMP90003");
            assertThat(request.departmentId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("列数が足りないとその行のエラーになる")
        void 列数不足() {
            assertThatThrownBy(() -> EmployeeCsvService.行を変換する(
                    EmployeeCsvService.分割する("EMP90004,新人")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("列数が足りません");
        }

        @Test
        @DisplayName("日付の形式が不正だと項目名つきのエラーになる")
        void 日付不正() {
            assertThatThrownBy(() -> EmployeeCsvService.行を変換する(EmployeeCsvService.分割する(
                    "EMP90005,新人,,n5@example.co.jp,,2026/04/01,1,,3,")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("hire_date");
        }

        @Test
        @DisplayName("IDが数値でないと項目名つきのエラーになる")
        void ID不正() {
            assertThatThrownBy(() -> EmployeeCsvService.行を変換する(EmployeeCsvService.分割する(
                    "EMP90006,新人,,n6@example.co.jp,,2026-04-01,開発部,,3,")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("department_id");
        }
    }

    @Nested
    @DisplayName("出力時の引用")
    class 引用 {

        @Test
        @DisplayName("特殊文字が無ければそのまま出す")
        void そのまま() {
            assertThat(EmployeeCsvService.引用する("山田 太郎")).isEqualTo("山田 太郎");
        }

        @Test
        @DisplayName("nullは空文字にする")
        void nullは空文字() {
            assertThat(EmployeeCsvService.引用する(null)).isEmpty();
        }

        /** カンマを含む値を囲まないと、開いたときに列がずれる。 */
        @Test
        @DisplayName("カンマを含む値は引用符で囲む")
        void カンマ() {
            assertThat(EmployeeCsvService.引用する("大阪市,北区"))
                    .isEqualTo("\"大阪市,北区\"");
        }

        @Test
        @DisplayName("引用符を含む値は二重にして囲む")
        void 引用符() {
            assertThat(EmployeeCsvService.引用する("彼は\"すごい\""))
                    .isEqualTo("\"彼は\"\"すごい\"\"\"");
        }

        @Test
        @DisplayName("改行を含む値は引用符で囲む")
        void 改行() {
            assertThat(EmployeeCsvService.引用する("1行目\n2行目"))
                    .isEqualTo("\"1行目\n2行目\"");
        }

        /** 出力した値を読み直して元に戻ることの確認（往復）。 */
        @Test
        @DisplayName("引用した値は分割して元に戻る")
        void 往復() {
            String 元の値 = "大阪市,北区 \"ビル\"";
            String 行 = EmployeeCsvService.引用する(元の値) + ",次の列";

            assertThat(EmployeeCsvService.分割する(行))
                    .containsExactly(元の値, "次の列");
        }
    }
}
