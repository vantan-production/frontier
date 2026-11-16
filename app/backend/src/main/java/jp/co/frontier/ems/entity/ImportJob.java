package jp.co.frontier.ems.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * CSV一括インポートのジョブ。
 *
 * <p>主キーは採番ではなくアプリが組み立てる文字列（{@code imp_20260727_001} 形式）。
 * クライアントに返す識別子で、連番より日付が入っている方が追いやすいため。
 *
 * <p>エラー詳細は件数・構造が可変なので JSONB で保持する（DB設計書 7章）。
 */
@Entity
@Table(name = "import_jobs")
@Getter
@Setter
@NoArgsConstructor
public class ImportJob {

    @Id
    @Column(name = "job_id", length = 50)
    private String jobId;

    /** 実行したADMIN */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "executed_by", nullable = false)
    private Employee executedBy;

    @Column(name = "file_name", length = 255)
    private String fileName;

    /**
     * 取り込みの状態。
     *
     * <p>{@code ImportJobStatusConverter} が小文字の値に変換して保存する。
     * {@code @Enumerated(EnumType.STRING)} だと enum 名（大文字）が入り、
     * DBの CHECK 制約に違反する。
     */
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "success_rows")
    private Integer successRows;

    @Column(name = "error_rows")
    private Integer errorRows;

    /**
     * エラー詳細。{@code [{"row":34,"reason":"..."}]} の形。
     *
     * <p>Hibernate 6 以降は {@code @JdbcTypeCode(SqlTypes.JSON)} で
     * JSONB に直接対応づけられる。
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "errors")
    private List<RowError> errors;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    /**
     * 取り込みの状態。
     *
     * <p>DBは VARCHAR で小文字の値を持つ（CHECK 制約が
     * {@code 'processing','completed','failed'}）ため、enum 名も小文字に
     * 合わせられるよう JSON と DB の両方で明示的に変換している。
     */
    public enum Status {
        PROCESSING("processing"),
        COMPLETED("completed"),
        FAILED("failed");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static Status fromValue(String value) {
            for (Status s : values()) {
                if (s.value.equals(value)) {
                    return s;
                }
            }
            throw new IllegalArgumentException("import_jobs.status に未定義の値: " + value);
        }
    }

    /**
     * 取り込めなかった行。
     *
     * @param row    CSVの行番号（ヘッダー行を除いた1始まり）
     * @param reason 取り込めなかった理由
     */
    public record RowError(Integer row, String reason) {
    }
}
