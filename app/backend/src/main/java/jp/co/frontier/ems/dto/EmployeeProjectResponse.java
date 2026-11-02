package jp.co.frontier.ems.dto;

import java.time.LocalDate;
import jp.co.frontier.ems.entity.EmployeeProject;

/**
 * 社員が担当したプロジェクト。
 *
 * <p>形は API 6.8 のリクエスト例に合わせ、表示用にプロジェクト名を加えている。
 *
 * @param endDate 離任日。継続中は null
 */
public record EmployeeProjectResponse(
        Long projectId,
        String projectName,
        String roleInProject,
        LocalDate startDate,
        LocalDate endDate) {

    public static EmployeeProjectResponse from(EmployeeProject employeeProject) {
        var project = employeeProject.getProject();
        return new EmployeeProjectResponse(
                project.getProjectId(),
                project.getProjectName(),
                employeeProject.getRoleInProject(),
                employeeProject.getStartDate(),
                employeeProject.getEndDate());
    }
}
