package jp.co.frontier.ems.dto;

/**
 * アクセストークン再発行のレスポンス（API 1.2）。
 *
 * <p>リフレッシュトークンはローテーションしないため返さない。
 * クライアントは期限（14日）まで同じものを使い続ける。
 */
public record RefreshResponse(String accessToken, long expiresIn) {
}
