package jp.co.frontier.ems.dto;

import jp.co.frontier.ems.entity.Department;

/**
 * 部署の返却用DTO。
 *
 * <p>エンティティを直接返さず必ずDTOを挟む。社員APIでは
 * {@code email} / {@code tel} を相手によって出し分ける必要があるため
 * （API設計仕様書「機密項目の扱い」）、エンティティをそのまま返す作りにすると
 * 機密項目が意図せず露出する。最初の1本からDTOを挟む形に揃えておく。
 *
 * <p>JSONのキーは snake_case に変換される（application.properties の
 * property-naming-strategy による）。このレコードは
 * {@code { "department_id": 1, "department_name": "人事部" }} になる。
 */
public record DepartmentResponse(Long departmentId, String departmentName) {

    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
                department.getDepartmentId(),
                department.getDepartmentName());
    }
}
