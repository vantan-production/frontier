package jp.co.frontier.ems.dto;

import java.time.LocalDate;
import jp.co.frontier.ems.entity.EmployeeCertification;

/**
 * 社員が保有する資格。
 *
 * <p>形は API 6.6 のレスポンス例に合わせている
 * （{@code {"certification_id":3,"certification_name":"基本情報技術者","acquire_date":"2018-10-01"}}）。
 */
public record EmployeeCertificationResponse(
        Long certificationId,
        String certificationName,
        LocalDate acquireDate) {

    public static EmployeeCertificationResponse from(EmployeeCertification employeeCertification) {
        var certification = employeeCertification.getCertification();
        return new EmployeeCertificationResponse(
                certification.getCertificationId(),
                certification.getCertificationName(),
                employeeCertification.getAcquireDate());
    }
}
