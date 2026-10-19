package jp.co.frontier.ems.entity;

/**
 * 認証トークンの種別。auth_tokens.token_type（VARCHAR）に対応する。
 */
public enum TokenType {

    /** アクセストークン（有効期限 1時間） */
    ACCESS,

    /** リフレッシュトークン（有効期限 14日） */
    REFRESH
}
