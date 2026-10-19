package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

/** プロジェクトマスタ。API 7.9 */
public interface ProjectRepository extends JpaRepository<Project, Long> {
}
