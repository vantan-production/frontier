package jp.co.frontier.ems.dto;

import jp.co.frontier.ems.entity.Site;

/** 拠点の返却用DTO。API 4.1 / 7.3。 */
public record SiteResponse(Long siteId, String siteName, String address) {

    public static SiteResponse from(Site site) {
        if (site == null) {
            return null;
        }
        return new SiteResponse(site.getSiteId(), site.getSiteName(), site.getAddress());
    }
}
