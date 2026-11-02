package jp.co.frontier.ems.dto;

import jp.co.frontier.ems.entity.EmploymentType;

/** 雇用形態の返却用DTO。マスタ参照（API 7.6）でも共用する。 */
public record EmploymentTypeResponse(Long employmentTypeId, String employmentTypeName) {

    public static EmploymentTypeResponse from(EmploymentType employmentType) {
        if (employmentType == null) {
            return null;
        }
        return new EmploymentTypeResponse(
                employmentType.getEmploymentTypeId(), employmentType.getEmploymentTypeName());
    }
}
