package jp.co.frontier.ems.dto;

import jp.co.frontier.ems.entity.Employee;

/**
 * 社員の要約。フロアマップの在席者表示（API 4.3）などで使う。
 *
 * <p>機密項目（{@code email} / {@code tel}）は含めない。一覧や地図上に
 * 出すものなので、閲覧者によらず常に非表示でよい。
 */
public record EmployeeSummaryResponse(
        Long employeeId,
        String name,
        String imageUrl,
        DepartmentResponse department,
        PositionResponse position) {

    public static EmployeeSummaryResponse from(Employee employee) {
        if (employee == null) {
            return null;
        }
        return new EmployeeSummaryResponse(
                employee.getEmployeeId(),
                employee.getName(),
                employee.getImageUrl(),
                DepartmentResponse.from(employee.getDepartment()),
                PositionResponse.from(employee.getPosition()));
    }
}
