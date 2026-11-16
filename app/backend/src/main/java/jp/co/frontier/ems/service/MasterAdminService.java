package jp.co.frontier.ems.service;

import jp.co.frontier.ems.common.error.ApiException;
import jp.co.frontier.ems.dto.CertificationResponse;
import jp.co.frontier.ems.dto.DepartmentResponse;
import jp.co.frontier.ems.dto.EmploymentTypeResponse;
import jp.co.frontier.ems.dto.FloorResponse;
import jp.co.frontier.ems.dto.MasterUpsertRequests.CertificationUpsert;
import jp.co.frontier.ems.dto.MasterUpsertRequests.DepartmentUpsert;
import jp.co.frontier.ems.dto.MasterUpsertRequests.EmploymentTypeUpsert;
import jp.co.frontier.ems.dto.MasterUpsertRequests.FloorUpsert;
import jp.co.frontier.ems.dto.MasterUpsertRequests.PositionUpsert;
import jp.co.frontier.ems.dto.MasterUpsertRequests.ProjectUpsert;
import jp.co.frontier.ems.dto.MasterUpsertRequests.SeatUpsert;
import jp.co.frontier.ems.dto.MasterUpsertRequests.SiteUpsert;
import jp.co.frontier.ems.dto.MasterUpsertRequests.SkillUpsert;
import jp.co.frontier.ems.dto.PositionResponse;
import jp.co.frontier.ems.dto.ProjectResponse;
import jp.co.frontier.ems.dto.SeatResponse;
import jp.co.frontier.ems.dto.SiteResponse;
import jp.co.frontier.ems.dto.SkillResponse;
import jp.co.frontier.ems.entity.Certification;
import jp.co.frontier.ems.entity.Department;
import jp.co.frontier.ems.entity.EmploymentType;
import jp.co.frontier.ems.entity.Floor;
import jp.co.frontier.ems.entity.Position;
import jp.co.frontier.ems.entity.Project;
import jp.co.frontier.ems.entity.Seat;
import jp.co.frontier.ems.entity.Site;
import jp.co.frontier.ems.entity.Skill;
import jp.co.frontier.ems.repository.CertificationRepository;
import jp.co.frontier.ems.repository.DepartmentRepository;
import jp.co.frontier.ems.repository.EmployeeRepository;
import jp.co.frontier.ems.repository.EmploymentTypeRepository;
import jp.co.frontier.ems.repository.FloorRepository;
import jp.co.frontier.ems.repository.PositionRepository;
import jp.co.frontier.ems.repository.ProjectRepository;
import jp.co.frontier.ems.repository.SeatRepository;
import jp.co.frontier.ems.repository.SiteRepository;
import jp.co.frontier.ems.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * マスタの登録・更新・削除（API 7.1〜7.9 の書込系）。ADMIN 専用。
 *
 * <p>参照は {@link MasterQueryService} 側。書込をこちらに分けているのは、
 * 重複チェックや削除可否の判定がマスタごとに変わるため。
 *
 * <p><b>削除の扱い</b>：参照が残っているマスタは外部キー制約（RESTRICT）で
 * 削除できない。制約違反をそのまま投げると 500 になるため、
 * 削除後に明示的に {@code flush()} して違反をこのメソッド内で発生させ、
 * {@code GlobalExceptionHandler} が 409 に変換する。
 * 事前に件数を数える方法もあるが、マスタ9種それぞれに参照元の確認を
 * 書くことになるため、制約に任せる形にしている。
 */
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Transactional
public class MasterAdminService {

    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final EmploymentTypeRepository employmentTypeRepository;
    private final SkillRepository skillRepository;
    private final CertificationRepository certificationRepository;
    private final ProjectRepository projectRepository;
    private final SiteRepository siteRepository;
    private final FloorRepository floorRepository;
    private final SeatRepository seatRepository;
    private final EmployeeRepository employeeRepository;

    // ---------- 7.1 部署 ----------

    public DepartmentResponse createDepartment(DepartmentUpsert request) {
        Department entity = new Department();
        entity.setDepartmentName(request.departmentName());
        return DepartmentResponse.from(departmentRepository.save(entity));
    }

    public DepartmentResponse updateDepartment(Long id, DepartmentUpsert request) {
        Department entity = find(departmentRepository, id, "部署");
        entity.setDepartmentName(request.departmentName());
        return DepartmentResponse.from(entity);
    }

    public void deleteDepartment(Long id) {
        delete(departmentRepository, id, "部署");
    }

    // ---------- 7.2 役職 ----------

    public PositionResponse createPosition(PositionUpsert request) {
        Position entity = new Position();
        entity.setPositionName(request.positionName());
        entity.setRank(request.rank());
        return PositionResponse.from(positionRepository.save(entity));
    }

    public PositionResponse updatePosition(Long id, PositionUpsert request) {
        Position entity = find(positionRepository, id, "役職");
        entity.setPositionName(request.positionName());
        entity.setRank(request.rank());
        return PositionResponse.from(entity);
    }

    public void deletePosition(Long id) {
        delete(positionRepository, id, "役職");
    }

    // ---------- 7.3 拠点 ----------

    public SiteResponse createSite(SiteUpsert request) {
        Site entity = new Site();
        entity.setSiteName(request.siteName());
        entity.setAddress(request.address());
        return SiteResponse.from(siteRepository.save(entity));
    }

    public SiteResponse updateSite(Long id, SiteUpsert request) {
        Site entity = find(siteRepository, id, "拠点");
        entity.setSiteName(request.siteName());
        entity.setAddress(request.address());
        return SiteResponse.from(entity);
    }

    public void deleteSite(Long id) {
        delete(siteRepository, id, "拠点");
    }

    // ---------- 7.4 フロア ----------

    public FloorResponse createFloor(FloorUpsert request) {
        Floor entity = new Floor();
        apply(entity, request);
        return FloorResponse.from(floorRepository.save(entity));
    }

    public FloorResponse updateFloor(Long id, FloorUpsert request) {
        Floor entity = find(floorRepository, id, "フロア");
        apply(entity, request);
        return FloorResponse.from(entity);
    }

    public void deleteFloor(Long id) {
        delete(floorRepository, id, "フロア");
    }

    private void apply(Floor entity, FloorUpsert request) {
        entity.setSite(siteRepository.findById(request.siteId())
                .orElseThrow(() -> ApiException.validation("指定された拠点が存在しません", null)));
        entity.setFloorName(request.floorName());
        entity.setFloorNumber(request.floorNumber());
        entity.setMapImageUrl(request.mapImageUrl());
    }

    // ---------- 7.5 座席 ----------

    public SeatResponse createSeat(SeatUpsert request) {
        Seat entity = new Seat();
        apply(entity, request);
        return SeatResponse.from(seatRepository.save(entity));
    }

    public SeatResponse updateSeat(Long id, SeatUpsert request) {
        Seat entity = find(seatRepository, id, "座席");
        apply(entity, request);
        return SeatResponse.from(entity);
    }

    /**
     * 座席を削除する。着席中の座席は削除できない。
     *
     * <p>employees.seat_id の外部キーは RESTRICT だが、ここは「誰が座っているか」を
     * 伝えられた方が運用しやすいので事前に確認している。
     */
    public void deleteSeat(Long id) {
        Seat entity = find(seatRepository, id, "座席");

        employeeRepository.findBySeatSeatId(id).ifPresent(occupant -> {
            throw ApiException.conflict(
                    "この座席には %s が着席しています。先に座席の割当を解除してください"
                            .formatted(occupant.getName()));
        });

        seatRepository.delete(entity);
        seatRepository.flush();
    }

    private void apply(Seat entity, SeatUpsert request) {
        entity.setFloor(floorRepository.findById(request.floorId())
                .orElseThrow(() -> ApiException.validation("指定されたフロアが存在しません", null)));
        entity.setSeatNumber(request.seatNumber());
        entity.setPositionX(request.positionX());
        entity.setPositionY(request.positionY());
    }

    // ---------- 7.6 雇用形態 ----------

    public EmploymentTypeResponse createEmploymentType(EmploymentTypeUpsert request) {
        EmploymentType entity = new EmploymentType();
        entity.setEmploymentTypeName(request.employmentTypeName());
        return EmploymentTypeResponse.from(employmentTypeRepository.save(entity));
    }

    public EmploymentTypeResponse updateEmploymentType(Long id, EmploymentTypeUpsert request) {
        EmploymentType entity = find(employmentTypeRepository, id, "雇用形態");
        entity.setEmploymentTypeName(request.employmentTypeName());
        return EmploymentTypeResponse.from(entity);
    }

    public void deleteEmploymentType(Long id) {
        delete(employmentTypeRepository, id, "雇用形態");
    }

    // ---------- 7.7 スキル ----------

    public SkillResponse createSkill(SkillUpsert request) {
        Skill entity = new Skill();
        entity.setSkillName(request.skillName());
        entity.setCategory(request.category());
        return SkillResponse.from(skillRepository.save(entity));
    }

    public SkillResponse updateSkill(Long id, SkillUpsert request) {
        Skill entity = find(skillRepository, id, "スキル");
        entity.setSkillName(request.skillName());
        entity.setCategory(request.category());
        return SkillResponse.from(entity);
    }

    public void deleteSkill(Long id) {
        delete(skillRepository, id, "スキル");
    }

    // ---------- 7.8 資格 ----------

    public CertificationResponse createCertification(CertificationUpsert request) {
        Certification entity = new Certification();
        entity.setCertificationName(request.certificationName());
        return CertificationResponse.from(certificationRepository.save(entity));
    }

    public CertificationResponse updateCertification(Long id, CertificationUpsert request) {
        Certification entity = find(certificationRepository, id, "資格");
        entity.setCertificationName(request.certificationName());
        return CertificationResponse.from(entity);
    }

    public void deleteCertification(Long id) {
        delete(certificationRepository, id, "資格");
    }

    // ---------- 7.9 プロジェクト ----------

    public ProjectResponse createProject(ProjectUpsert request) {
        Project entity = new Project();
        apply(entity, request);
        return ProjectResponse.from(projectRepository.save(entity));
    }

    public ProjectResponse updateProject(Long id, ProjectUpsert request) {
        Project entity = find(projectRepository, id, "プロジェクト");
        apply(entity, request);
        return ProjectResponse.from(entity);
    }

    public void deleteProject(Long id) {
        delete(projectRepository, id, "プロジェクト");
    }

    private void apply(Project entity, ProjectUpsert request) {
        if (request.startDate() != null && request.endDate() != null
                && request.startDate().isAfter(request.endDate())) {
            throw ApiException.validation("開始日は終了日より前の日付を指定してください", null);
        }
        entity.setProjectName(request.projectName());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setDescription(request.description());
    }

    // ---------- 共通 ----------

    private <T> T find(JpaRepository<T, Long> repository, Long id, String 名称) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("指定された%sが見つかりません".formatted(名称)));
    }

    /**
     * マスタを削除する。
     *
     * <p>削除後に {@code flush()} するのは、参照が残っている場合の外部キー制約違反を
     * このメソッド内で発生させるため。flush しないとトランザクションのコミット時に
     * なり、発生場所が分かりにくくなる。違反は
     * {@code GlobalExceptionHandler} が 409 に変換する。
     */
    private <T> void delete(JpaRepository<T, Long> repository, Long id, String 名称) {
        T entity = find(repository, id, 名称);
        repository.delete(entity);
        repository.flush();
    }
}
