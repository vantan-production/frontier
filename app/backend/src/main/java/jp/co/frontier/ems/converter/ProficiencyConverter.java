package jp.co.frontier.ems.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jp.co.frontier.ems.entity.Proficiency;

/**
 * {@link Proficiency} と SMALLINT の相互変換。
 */
@Converter(autoApply = true)
public class ProficiencyConverter implements AttributeConverter<Proficiency, Short> {

    @Override
    public Short convertToDatabaseColumn(Proficiency attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public Proficiency convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : Proficiency.fromCode(dbData);
    }
}
