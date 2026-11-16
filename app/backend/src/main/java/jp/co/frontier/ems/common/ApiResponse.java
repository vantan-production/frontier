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
 * 該当なし : { "data": null }
 * </pre>
 *
 * <p>{@code @JsonInclude} を {@code meta} だけに付けているのは、
 * レコード全体に付けると {@code data} が null のときにキーごと消えてしまうため。
 * API 4.4（社員の座席取得）は「座席未登録の社員は 200 で {@code "data": null}」と
 * 定めており、<b>キーの不在と null は区別される</b>。
 */
public record ApiResponse<T>(
        T data,

        /** ページネーション情報。一覧以外では null になり、キーごと省略される */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        PageMeta meta) {

    /** ページネーションを伴わないレスポンス。 */
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, null);
    }

    /** ページネーションを伴う一覧レスポンス。 */
    public static <T> ApiResponse<T> of(T data, PageMeta meta) {
        return new ApiResponse<>(data, meta);
    }
}
