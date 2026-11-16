package jp.co.frontier.ems.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jp.co.frontier.ems.entity.ImportJob;

/**
 * {@link ImportJob.Status} と DB の文字列の相互変換。
 *
 * <p>{@code @Enumerated(EnumType.STRING)} では enum 名（大文字の
 * {@code PROCESSING}）が保存されるが、DBの CHECK 制約は小文字の
 * {@code 'processing','completed','failed'} を要求するため制約違反になる。
 * 小文字の値を明示的に保存する必要がある。
 *
 * <p>autoApply = true なので、この型のフィールドには自動で適用される。
 */
@Converter(autoApply = true)
public class ImportJobStatusConverter
        implements AttributeConverter<ImportJob.Status, String> {

    @Override
    public String convertToDatabaseColumn(ImportJob.Status attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ImportJob.Status convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ImportJob.Status.fromValue(dbData);
    }
}
