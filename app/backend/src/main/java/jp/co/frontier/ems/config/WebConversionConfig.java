package jp.co.frontier.ems.config;

import jp.co.frontier.ems.dto.EmployeeSearchCriteria;
import jp.co.frontier.ems.dto.ThanksMessageDtos;
import jp.co.frontier.ems.entity.EmployeeStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * クエリパラメータを区分値に変換する設定。
 *
 * <p>区分値の enum には Jackson 用に {@code @JsonValue} / {@code @JsonCreator} を
 * 付けているが、<b>クエリパラメータの束縛は Jackson を通らない</b>ため
 * そのままでは変換されない（{@code status=0} が 422 になる）。
 * Spring の変換機構にも同じ対応を登録する必要がある。
 */
@Configuration
public class WebConversionConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StatusConverter());
        registry.addConverter(new MatchModeConverter());
        registry.addConverter(new HistoryTypeConverter());
    }

    /**
     * {@code status=0} / {@code status=1} を {@link EmployeeStatus} に変換する。
     *
     * <p>APIは区分値を数値で受け渡す仕様なので、enum 名（{@code ACTIVE}）ではなく
     * コードで受ける。
     */
    static class StatusConverter implements Converter<String, EmployeeStatus> {

        @Override
        public EmployeeStatus convert(String source) {
            if (source == null || source.isBlank()) {
                return null;
            }
            return EmployeeStatus.fromCode(Short.parseShort(source.trim()));
        }
    }

    /**
     * {@code skill_match=or} のように小文字で来るものを受け取る。
     *
     * <p>既定の enum 変換は完全一致なので、小文字だと 422 になってしまう。
     */
    static class MatchModeConverter
            implements Converter<String, EmployeeSearchCriteria.MatchMode> {

        @Override
        public EmployeeSearchCriteria.MatchMode convert(String source) {
            if (source == null || source.isBlank()) {
                return null;
            }
            return EmployeeSearchCriteria.MatchMode.valueOf(source.trim().toUpperCase());
        }
    }

    /**
     * {@code type=sent} のように小文字で来るものを受け取る（API 8.3）。
     *
     * <p>不正な値は 422 にしたいので、変換できない場合は例外にする。
     */
    static class HistoryTypeConverter
            implements Converter<String, ThanksMessageDtos.HistoryType> {

        @Override
        public ThanksMessageDtos.HistoryType convert(String source) {
            if (source == null || source.isBlank()) {
                return null;
            }
            return ThanksMessageDtos.HistoryType.valueOf(source.trim().toUpperCase());
        }
    }
}
