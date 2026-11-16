package jp.co.frontier.ems.service;

import java.util.List;
import jp.co.frontier.ems.common.PageMeta;
import jp.co.frontier.ems.common.error.ApiException;
import jp.co.frontier.ems.common.error.ErrorResponse;
import jp.co.frontier.ems.dto.ThanksMessageDtos.HistoryType;
import jp.co.frontier.ems.dto.ThanksMessageDtos.Response;
import jp.co.frontier.ems.dto.ThanksMessageDtos.SendRequest;
import jp.co.frontier.ems.entity.Employee;
import jp.co.frontier.ems.entity.EmployeeStatus;
import jp.co.frontier.ems.entity.ThanksMessage;
import jp.co.frontier.ems.repository.EmployeeRepository;
import jp.co.frontier.ems.repository.ThanksMessageRepository;
import jp.co.frontier.ems.security.AuthenticatedEmployee;
import jp.co.frontier.ems.service.EmployeeService.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** サンクスメッセージの送信・閲覧（API 8.1〜8.3）。 */
@Service
@RequiredArgsConstructor
public class ThanksMessageService {

    private final ThanksMessageRepository thanksMessageRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * 送信（API 8.1）。
     *
     * <p>送信者はトークンから判定する。宛先が退職者の場合は 404
     * （既に居ない人には送れない）。自分宛ては 422。
     */
    @Transactional
    public Response send(AuthenticatedEmployee sender, SendRequest request) {
        Employee from = employeeRepository.findById(sender.employeeId())
                .orElseThrow(() -> ApiException.unauthorized(
                        "ログイン中の社員が見つかりません。再度ログインしてください"));

        Employee to = employeeRepository.findById(request.toEmployeeId())
                .orElseThrow(() -> ApiException.notFound("指定された宛先が見つかりません"));

        // 退職者には送れない。存在を伏せるためではなく、宛先として無効なため404
        if (to.getStatus() != EmployeeStatus.ACTIVE) {
            throw ApiException.notFound("指定された宛先が見つかりません");
        }

        // 自分宛ての送信はDBの CHECK 制約でも防がれるが、
        // 制約違反をそのまま投げると500になるためここで弾く
        if (from.getEmployeeId().equals(to.getEmployeeId())) {
            throw ApiException.validation("自分自身には送信できません",
                    List.of(new ErrorResponse.ErrorDetail(
                            "to_employee_id", "自分以外の社員を指定してください")));
        }

        ThanksMessage message = new ThanksMessage();
        message.setFromEmployee(from);
        message.setToEmployee(to);
        message.setBody(request.body());

        return Response.from(thanksMessageRepository.save(message));
    }

    /** タイムライン（API 8.2）。新着順固定。 */
    @Transactional(readOnly = true)
    public PagedResult<Response> findTimeline(int page, int perPage) {
        Page<ThanksMessage> result = thanksMessageRepository.findAll(pageable(page, perPage));
        return toPagedResult(result);
    }

    /**
     * 社員別の送受信履歴（API 8.3）。
     *
     * @param type 取得範囲。既定は両方
     */
    @Transactional(readOnly = true)
    public PagedResult<Response> findByEmployee(
            Long employeeId, HistoryType type, int page, int perPage) {

        if (!employeeRepository.existsById(employeeId)) {
            throw ApiException.notFound("指定された社員が見つかりません");
        }

        HistoryType 範囲 = type != null ? type : HistoryType.ALL;
        Page<ThanksMessage> result = thanksMessageRepository.findByEmployee(
                employeeId, 範囲.includesSent(), 範囲.includesReceived(),
                pageable(page, perPage));

        return toPagedResult(result);
    }

    /** 新着順固定（仕様書 8.2）。ページ番号は仕様が1始まり、Spring Data は0始まり。 */
    private Pageable pageable(int page, int perPage) {
        return PageRequest.of(page - 1, perPage, Sort.by(Sort.Direction.DESC, "sentAt"));
    }

    private PagedResult<Response> toPagedResult(Page<ThanksMessage> page) {
        return new PagedResult<>(
                page.getContent().stream().map(Response::from).toList(),
                PageMeta.from(page));
    }
}
