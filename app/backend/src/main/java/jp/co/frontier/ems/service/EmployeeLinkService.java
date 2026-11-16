package jp.co.frontier.ems.service;

import jp.co.frontier.ems.common.error.ApiException;
import jp.co.frontier.ems.dto.EmployeeCertificationResponse;
import jp.co.frontier.ems.dto.EmployeeLinkRequests.CertificationLink;
import jp.co.frontier.ems.dto.EmployeeLinkRequests.ProjectLink;
import jp.co.frontier.ems.dto.EmployeeLinkRequests.SkillLink;
import jp.co.frontier.ems.dto.EmployeeLinkRequests.SkillLinkUpdate;
import jp.co.frontier.ems.dto.EmployeeProjectResponse;
import jp.co.frontier.ems.dto.EmployeeSkillResponse;
import jp.co.frontier.ems.entity.Employee;
import jp.co.frontier.ems.entity.EmployeeCertification;
import jp.co.frontier.ems.entity.EmployeeCertificationId;
import jp.co.frontier.ems.entity.EmployeeProject;
import jp.co.frontier.ems.entity.EmployeeProjectId;
import jp.co.frontier.ems.entity.EmployeeSkill;
import jp.co.frontier.ems.entity.EmployeeSkillId;
import jp.co.frontier.ems.entity.Proficiency;
import jp.co.frontier.ems.repository.CertificationRepository;
import jp.co.frontier.ems.repository.EmployeeCertificationRepository;
import jp.co.frontier.ems.repository.EmployeeProjectRepository;
import jp.co.frontier.ems.repository.EmployeeRepository;
import jp.co.frontier.ems.repository.EmployeeSkillRepository;
import jp.co.frontier.ems.repository.ProjectRepository;
import jp.co.frontier.ems.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 社員へのスキル・資格・プロジェクトの紐付け（API 6.4〜6.9）。ADMIN 専用。
 *
 * <p>重複登録は中間テーブルの複合主キーでも防がれるが、制約違反をそのまま
 * 投げると 500 になるため、事前に確認して 409 を返している。
 */
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeLinkService {

    private final EmployeeRepository employeeRepository;
    private final SkillRepository skillRepository;
    private final CertificationRepository certificationRepository;
    private final ProjectRepository projectRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final EmployeeCertificationRepository employeeCertificationRepository;
    private final EmployeeProjectRepository employeeProjectRepository;

    // ---------- スキル（API 6.4 / 6.5） ----------

    @Transactional
    public EmployeeSkillResponse addSkill(Long employeeId, SkillLink request) {
        Employee employee = employee(employeeId);
        var skill = skillRepository.findById(request.skillId())
                .orElseThrow(() -> ApiException.notFound("指定されたスキルが見つかりません"));

        if (employeeSkillRepository.existsById(
                new EmployeeSkillId(employeeId, request.skillId()))) {
            throw ApiException.conflict("このスキルは既に登録されています");
        }

        EmployeeSkill link = new EmployeeSkill();
        link.setEmployee(employee);
        link.setSkill(skill);
        link.setProficiency(Proficiency.fromCode(request.proficiency()));
        link.setExperienceYears(request.experienceYears());

        return EmployeeSkillResponse.from(employeeSkillRepository.save(link));
    }

    @Transactional
    public EmployeeSkillResponse updateSkill(
            Long employeeId, Long skillId, SkillLinkUpdate request) {

        EmployeeSkill link = employeeSkillRepository
                .findById(new EmployeeSkillId(employeeId, skillId))
                .orElseThrow(() -> ApiException.notFound("このスキルは登録されていません"));

        link.setProficiency(Proficiency.fromCode(request.proficiency()));
        link.setExperienceYears(request.experienceYears());

        return EmployeeSkillResponse.from(link);
    }

    @Transactional
    public void removeSkill(Long employeeId, Long skillId) {
        EmployeeSkillId id = new EmployeeSkillId(employeeId, skillId);
        if (!employeeSkillRepository.existsById(id)) {
            throw ApiException.notFound("このスキルは登録されていません");
        }
        employeeSkillRepository.deleteById(id);
    }

    // ---------- 資格（API 6.6 / 6.7） ----------

    @Transactional
    public EmployeeCertificationResponse addCertification(
            Long employeeId, CertificationLink request) {

        Employee employee = employee(employeeId);
        var certification = certificationRepository.findById(request.certificationId())
                .orElseThrow(() -> ApiException.notFound("指定された資格が見つかりません"));

        if (employeeCertificationRepository.existsById(
                new EmployeeCertificationId(employeeId, request.certificationId()))) {
            throw ApiException.conflict("この資格は既に登録されています");
        }

        EmployeeCertification link = new EmployeeCertification();
        link.setEmployee(employee);
        link.setCertification(certification);
        link.setAcquireDate(request.acquireDate());

        return EmployeeCertificationResponse.from(employeeCertificationRepository.save(link));
    }

    @Transactional
    public void removeCertification(Long employeeId, Long certificationId) {
        EmployeeCertificationId id = new EmployeeCertificationId(employeeId, certificationId);
        if (!employeeCertificationRepository.existsById(id)) {
            throw ApiException.notFound("この資格は登録されていません");
        }
        employeeCertificationRepository.deleteById(id);
    }

    // ---------- プロジェクト（API 6.8 / 6.9） ----------

    @Transactional
    public EmployeeProjectResponse addProject(Long employeeId, ProjectLink request) {
        Employee employee = employee(employeeId);
        var project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> ApiException.notFound("指定されたプロジェクトが見つかりません"));

        if (employeeProjectRepository.existsById(
                new EmployeeProjectId(employeeId, request.projectId()))) {
            throw ApiException.conflict("このプロジェクトは既に登録されています");
        }

        // 開始日 > 終了日 は不正（仕様書 6.8）。DBにも CHECK があるが、
        // 制約違反をそのまま投げると 500 になるためここで弾く
        if (request.startDate() != null && request.endDate() != null
                && request.startDate().isAfter(request.endDate())) {
            throw ApiException.validation("参画日は離任日より前の日付を指定してください", null);
        }

        EmployeeProject link = new EmployeeProject();
        link.setEmployee(employee);
        link.setProject(project);
        link.setRoleInProject(request.roleInProject());
        link.setStartDate(request.startDate());
        link.setEndDate(request.endDate());

        return EmployeeProjectResponse.from(employeeProjectRepository.save(link));
    }

    @Transactional
    public void removeProject(Long employeeId, Long projectId) {
        EmployeeProjectId id = new EmployeeProjectId(employeeId, projectId);
        if (!employeeProjectRepository.existsById(id)) {
            throw ApiException.notFound("このプロジェクトは登録されていません");
        }
        employeeProjectRepository.deleteById(id);
    }

    private Employee employee(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> ApiException.notFound("指定された社員が見つかりません"));
    }
}
