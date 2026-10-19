package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

/** 認証トークン。API 1. 認証 */
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
}
