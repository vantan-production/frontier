package jp.co.frontier.ems.dto;

import jp.co.frontier.ems.entity.Employee;
import jp.co.frontier.ems.entity.Role;

/**
 * ログインのレスポンス（API 1.1）。
 *
 * <p>仕様書の例が {@code data} で包まない形になっているため、それに合わせている
 * （トークン発行のレスポンスを包まないのは OAuth2 などでも一般的）。
 *
 * <pre>
 * { "access_token":"...", "refresh_token":"...", "expires_in":3600,
 *   "employee":{ "employee_id":123, "name":"山田 太郎", "role":"EMPLOYEE" } }
 * </pre>
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        EmployeeSummary employee) {

    /** ログイン直後に画面が必要とする最小限の社員情報。 */
    public record EmployeeSummary(Long employeeId, String name, Role role) {

        public static EmployeeSummary from(Employee employee) {
            return new EmployeeSummary(
                    employee.getEmployeeId(), employee.getName(), employee.getRole());
        }
    }
}
