package jp.co.frontier.ems.common;

import org.springframework.data.domain.Page;

/**
 * 一覧取得APIのページネーション情報。
 *
 * <p>API設計仕様書「ページネーション共通仕様」の
 * {@code { "page":1, "per_page":20, "total_count":42, "total_pages":3 }} に対応する。
 *
 * @param page       1始まりのページ番号
 * @param perPage    1ページあたりの件数
 * @param totalCount 総件数
 * @param totalPages 総ページ数
 */
public record PageMeta(int page, int perPage, long totalCount, int totalPages) {

    /**
     * Spring Data の {@link Page} から生成する。
     *
     * <p>Spring Data のページ番号は0始まり、API仕様は1始まりなので +1 する。
     */
    public static PageMeta from(Page<?> page) {
        return new PageMeta(
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
