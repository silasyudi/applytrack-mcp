package silas.yudi.applytrackmcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Sem stdio: o stdin do processo de testes pertence ao Surefire. Sobe sem banco: a tabela só é criada no primeiro uso
@SpringBootTest(properties = {
        "spring.ai.mcp.server.stdio=false",
        "applytrack.dynamodb.endpoint=http://localhost:1", // nunca o banco de verdade
        "logging.file.name=target/tests.log"
})
class ApplytrackMcpApplicationTest {

    @Test
    void contextLoads() {
    }

}
