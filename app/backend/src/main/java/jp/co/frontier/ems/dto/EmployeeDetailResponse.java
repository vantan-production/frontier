package jp.co.frontier.ems.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;
import jp.co.frontier.ems.entity.Employee;
import jp.co.frontier.ems.entity.EmployeeCertification;
import jp.co.frontier.ems.entity.EmployeeProject;
import jp.co.frontier.ems.entity.EmployeeSkill;
import jp.co.frontier.ems.entity.EmployeeStatus;

/**
 * 社員詳細の返却用DTO。API 3.1（社員詳細取得）と API 1.5（GET /me）で共用する。
 *
 * <p><b>機密項目の扱い</b>：{@code email} / {@code tel} は本人または ADMIN にのみ
 * 含める。それ以外には <b>キーごと省略</b>して 200 を返す（403 は返さない）。
 * 詳細取得そのものは全社員に許可されているため。
 * 省略は {@code @JsonInclude(NON_NULL)} を該当2項目だけに付けて実現している。
 * 他の項目は値が null でもキーを残す（画面側が項目の有無で分岐しなくて済むように）。
 *
 * @param status 0:退職・無効 / 1:在籍。JSONでは数値になる
 */
public record EmployeeDetailResponse(
        Long employeeId,
        String employeeCode,
        String name,
        String nameKana,

        /** 機密項目。閲覧権限が無い場合はキーごと省略される */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String email,

        /** 機密項目。閲覧権限が無い場合はキーごと省略される */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String tel,

        String imageUrl,
        String hobby,
        String selfPr,
        LocalDate hireDate,
        EmployeeStatus status,
        DepartmentResponse department,
        PositionResponse position,
        EmploymentTypeResponse employmentType,
        SeatResponse seat,
        List<EmployeeSkillResponse> skills,
        List<EmployeeCertificationResponse> certifications,
        List<EmployeeProjectResponse> projects) {

    /**
     * エンティティから組み立てる。
     *
     * @param includeConfidential true なら email / tel を含める。
     *                            本人または ADMIN の場合のみ true を渡す
     */
    public static EmployeeDetailResponse from(
            Employee employee,
            List<EmployeeSkill> skills,
            List<EmployeeCertification> certifications,
            List<EmployeeProject> projects,
            boolean includeConfidential) {

        return new EmployeeDetailResponse(
                employee.getEmployeeId(),
                employee.getEmployeeCode(),
                employee.getName(),
                employee.getNameKana(),
                includeConfidential ? employee.getEmail() : null,
                includeConfidential ? employee.getTel() : null,
                employee.getImageUrl(),
                employee.getHobby(),
                employee.getSelfPr(),
                employee.getHireDate(),
                employee.getStatus(),
                DepartmentResponse.from(employee.getDepartment()),
                PositionResponse.from(employee.getPosition()),
                EmploymentTypeResponse.from(employee.getEmploymentType()),
                SeatResponse.from(employee.getSeat()),
                skills.stream().map(EmployeeSkillResponse::from).toList(),
                certifications.stream().map(EmployeeCertificationResponse::from).toList(),
                projects.stream().map(EmployeeProjectResponse::from).toList());
    }
}
