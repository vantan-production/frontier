package jp.co.frontier.ems.dto;

import java.time.LocalDate;
import jp.co.frontier.ems.entity.Project;

/**
 * プロジェクトマスタの返却用DTO。API 7.9。
 *
 * @param endDate 終了日。継続中は null
 */
public record ProjectResponse(
        Long projectId,
        String projectName,
        LocalDate startDate,
        LocalDate endDate,
        String description) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getProjectId(),
                project.getProjectName(),
                project.getStartDate(),
                project.getEndDate(),
                project.getDescription());
    }
}
