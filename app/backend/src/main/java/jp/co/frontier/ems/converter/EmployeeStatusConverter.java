package jp.co.frontier.ems.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jp.co.frontier.ems.entity.EmployeeStatus;

/**
 * {@link EmployeeStatus} と SMALLINT の相互変換。
 *
 * <p>autoApply = true なので、EmployeeStatus 型のフィールドには自動で適用される。
 * エンティティ側で @Convert を書く必要はない。
 */
@Converter(autoApply = true)
public class EmployeeStatusConverter implements AttributeConverter<EmployeeStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(EmployeeStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public EmployeeStatus convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : EmployeeStatus.fromCode(dbData);
    }
}
