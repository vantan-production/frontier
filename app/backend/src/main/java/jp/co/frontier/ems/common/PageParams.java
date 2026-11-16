package jp.co.frontier.ems.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.BindParam;

/**
 * ページネーションのクエリパラメータ（仕様書「ページネーション共通仕様」）。
 *
 * <p>一覧取得APIで共通して受け取る。{@code @ModelAttribute} で束縛すると
 * 検証違反が {@code MethodArgumentNotValidException} になり、
 * {@code GlobalExceptionHandler} が項目名つきの 422 に変換してくれる。
 * メソッド引数に {@code @Min} を直接付ける形だと別の例外になり、
 * 共通のエラー形式に乗らない。
 *
 * <p>{@code @BindParam} が必要なのは、クエリパラメータの束縛が Jackson を
 * 通らず {@code property-naming-strategy} が効かないため。
 */
public record PageParams(
        @Min(value = 1, message = "pageは1以上で指定してください")
        Integer page,

        @BindParam("per_page")
        @Min(value = 1, message = "per_pageは1以上で指定してください")
        @Max(value = 100, message = "per_pageは100以下で指定してください")
        Integer perPage) {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PER_PAGE = 20;

    public int pageOrDefault() {
        return page != null ? page : DEFAULT_PAGE;
    }

    public int perPageOrDefault() {
        return perPage != null ? perPage : DEFAULT_PER_PAGE;
    }
}
