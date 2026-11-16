package jp.co.frontier.ems.service;

import java.util.List;
import jp.co.frontier.ems.dto.CertificationResponse;
import jp.co.frontier.ems.dto.DepartmentResponse;
import jp.co.frontier.ems.dto.EmploymentTypeResponse;
import jp.co.frontier.ems.dto.PositionResponse;
import jp.co.frontier.ems.dto.ProjectResponse;
import jp.co.frontier.ems.dto.SkillResponse;
import jp.co.frontier.ems.repository.CertificationRepository;
import jp.co.frontier.ems.repository.DepartmentRepository;
import jp.co.frontier.ems.repository.EmploymentTypeRepository;
import jp.co.frontier.ems.repository.PositionRepository;
import jp.co.frontier.ems.repository.ProjectRepository;
import jp.co.frontier.ems.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * マスタの参照（API 7.1〜7.9 の GET）。
 *
 * <p>マスタごとにサービスを分けると同じ形のクラスが9個並ぶので、参照は
 * ここにまとめている。書込は入力検証や削除可否の判定が入り、マスタごとに
 * 内容が変わるため別クラスに分ける。
 *
 * <p>マスタは件数が少なくプルダウンで使うため、いずれもページネーションしない。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterQueryService {

    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final EmploymentTypeRepository employmentTypeRepository;
    private final SkillRepository skillRepository;
    private final CertificationRepository certificationRepository;
    private final ProjectRepository projectRepository;

    public List<DepartmentResponse> findDepartments() {
        return departmentRepository.findAll(by("departmentId")).stream()
                .map(DepartmentResponse::from).toList();
    }

    /** 役職は序列（rank）順。並べ替え用の項目なので画面側で並べ直さなくて済む。 */
    public List<PositionResponse> findPositions() {
        return positionRepository.findAll(by("rank")).stream()
                .map(PositionResponse::from).toList();
    }

    public List<EmploymentTypeResponse> findEmploymentTypes() {
        return employmentTypeRepository.findAll(by("employmentTypeId")).stream()
                .map(EmploymentTypeResponse::from).toList();
    }

    /** スキルは分類→名称順。分類ごとにまとめて表示できるようにするため。 */
    public List<SkillResponse> findSkills() {
        return skillRepository.findAll(Sort.by("category", "skillName")).stream()
                .map(SkillResponse::from).toList();
    }

    public List<CertificationResponse> findCertifications() {
        return certificationRepository.findAll(by("certificationName")).stream()
                .map(CertificationResponse::from).toList();
    }

    /** プロジェクトは新しいものから。継続中（start_date が最新）を上に出す。 */
    public List<ProjectResponse> findProjects() {
        return projectRepository.findAll(Sort.by(Sort.Direction.DESC, "startDate")).stream()
                .map(ProjectResponse::from).toList();
    }

    private Sort by(String property) {
        return Sort.by(Sort.Direction.ASC, property);
    }
}
