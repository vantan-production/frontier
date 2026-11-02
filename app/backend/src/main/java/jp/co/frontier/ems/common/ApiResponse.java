package jp.co.frontier.ems.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 成功レスポンスの共通形式。
 *
 * <p>API設計仕様書どおり、本体は必ず {@code data} で包む。
 * 一覧取得では {@code meta} も付く。
 *
 * <pre>
 * 単体 : { "data": { ... } }
 * 一覧 : { "data": [ ... ], "meta": { "page":1, "per_page":20, ... } }
 * </pre>
 *
 * <p>{@code meta} が null のときはキーごと省略される。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(T data, PageMeta meta) {

    /** ページネーションを伴わないレスポンス。 */
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, null);
    }

    /** ページネーションを伴う一覧レスポンス。 */
    public static <T> ApiResponse<T> of(T data, PageMeta meta) {
        return new ApiResponse<>(data, meta);
    }
}
