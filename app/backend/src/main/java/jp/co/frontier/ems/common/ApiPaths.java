package jp.co.frontier.ems.common;

/**
 * APIのパス接頭辞。
 *
 * <p>API設計仕様書のベースURL（.../v1）に対応する。
 * コントローラ側では {@code @RequestMapping(ApiPaths.V1 + "/employees")} のように使う。
 *
 * <p>context-path には入れていない。バージョンはAPIの仕様であって配置の話ではなく、
 * 将来 v1 と v2 を同一アプリで併存させられるようにしておくため。
 * actuator などの管理用パスが /v1 配下にずれないという利点もある。
 */
public final class ApiPaths {

    /** APIバージョン1 */
    public static final String V1 = "/v1";

    private ApiPaths() {
    }
}
