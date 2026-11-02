package jp.co.frontier.ems.security;

import jp.co.frontier.ems.entity.Role;

/**
 * 認証済みの利用者。Spring Security の principal として
 * {@code SecurityContextHolder} に入る。
 *
 * <p>エンティティではなくこの値オブジェクトを置いているのは、トランザクション外で
 * 遅延ロードが発火して {@code LazyInitializationException} になるのを避けるため。
 * コントローラやサービスで必要になるのは ID・社員番号・権限だけ。
 *
 * @param employeeId   社員ID
 * @param employeeCode 社員番号
 * @param role         権限
 * @param tokenId      認証に使われたアクセストークンのID。ログアウト時の失効対象
 */
public record AuthenticatedEmployee(
        Long employeeId, String employeeCode, Role role, Long tokenId) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
