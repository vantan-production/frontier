package jp.co.frontier.ems.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jp.co.frontier.ems.entity.Employee;
import jp.co.frontier.ems.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * API テストの共通土台。
 *
 * <p>{@code @Transactional} を付けているのでテストごとにロールバックされる。
 * パスワード変更のようにデータを書き換えるテストでも、後続のテストに影響しない。
 *
 * <p>社員IDはシードの採番に依存する（INSERT が JOIN で解決しているため
 * 社員番号の順とは一致しない）。テストでは必ず社員番号から引くこと。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
public abstract class ApiTestBase {

    /** 開発用シードデータのパスワード（V3__dev_employees.sql） */
    protected static final String SEED_PASSWORD = "Password1!";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected EmployeeRepository employeeRepository;

    protected JsonNode json(String body) {
        return objectMapper.readTree(body);
    }

    /** 社員番号から社員を引く。IDを直接書かないため。 */
    protected Employee employee(String employeeCode) {
        return employeeRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new IllegalStateException(
                        "シードに存在しない社員番号: " + employeeCode));
    }

    /** ログインしてアクセストークンを取得する。 */
    protected String loginAndGetAccessToken(String email) throws Exception {
        return loginJson(email, SEED_PASSWORD).get("access_token").asString();
    }

    /** ログインのレスポンス全体を取得する。 */
    protected JsonNode loginJson(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andReturn().getResponse().getContentAsString();
        return json(body);
    }

    @BeforeEach
    void シードデータが投入されていることを確認() {
        // マイグレーション V3 が流れていなければ全テストが無意味になるため、
        // 先に前提を確認して落とす
        if (employeeRepository.count() == 0) {
            throw new IllegalStateException("開発用シードデータが投入されていません");
        }
    }
}
