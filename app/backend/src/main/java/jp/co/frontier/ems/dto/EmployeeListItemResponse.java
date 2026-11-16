package jp.co.frontier.ems.dto;

import java.util.List;
import jp.co.frontier.ems.entity.Employee;

/**
 * 社員検索の一覧行（API 2.1）。
 *
 * <p>仕様書の例が
 * {@code {"employee_id":123,"name":"...","image_url":"...","department":{...},"position":{...},"skills":[...]}}
 * なのでそれに合わせている。
 *
 * <p><b>機密項目（{@code email} / {@code tel}）は閲覧者によらず含めない。</b>
 * 仕様書 2.1 に明記されている。一覧に出す情報なので、ADMIN であっても
 * ここには出さない（必要なら詳細取得 3.1 を使う）。
 */
public record EmployeeListItemResponse(
        Long employeeId,
        String name,
        String nameKana,
        String imageUrl,
        DepartmentResponse department,
        PositionResponse position,
        List<EmployeeSkillResponse> skills) {

    public static EmployeeListItemResponse from(
            Employee employee, List<EmployeeSkillResponse> skills) {

        return new EmployeeListItemResponse(
                employee.getEmployeeId(),
                employee.getName(),
                employee.getNameKana(),
                employee.getImageUrl(),
                DepartmentResponse.from(employee.getDepartment()),
                PositionResponse.from(employee.getPosition()),
                skills);
    }
}
