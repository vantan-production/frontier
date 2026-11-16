package jp.co.frontier.ems.dto;

import jp.co.frontier.ems.entity.Certification;

/** 資格マスタの返却用DTO。API 7.8。 */
public record CertificationResponse(Long certificationId, String certificationName) {

    public static CertificationResponse from(Certification certification) {
        return new CertificationResponse(
                certification.getCertificationId(), certification.getCertificationName());
    }
}
