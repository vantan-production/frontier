package jp.co.frontier.ems.service;

import java.util.List;
import jp.co.frontier.ems.dto.DepartmentResponse;
import jp.co.frontier.ems.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 部署マスタの参照・更新。API 7.1 に対応する。
 *
 * <p>この一覧取得程度ならコントローラからRepositoryを直接呼んでも動くが、
 * 層を飛ばす書き方が最初の実装例になると以降もそれが踏襲される。
 * 社員検索のように必ずロジックが入るものが出てくるので、
 * コントローラ → サービス → リポジトリの形をここで揃えておく。
 */
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    /**
     * 全件取得。マスタは件数が少なくプルダウンで使うため、ページネーションしない。
     */
    @Transactional(readOnly = true)
    public List<DepartmentResponse> findAll() {
        return departmentRepository.findAll(Sort.by(Sort.Direction.ASC, "departmentId"))
                .stream()
                .map(DepartmentResponse::from)
                .toList();
    }
}
