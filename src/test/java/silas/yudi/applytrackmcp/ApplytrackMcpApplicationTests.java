package silas.yudi.applytrackmcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Sem stdio: o stdin do processo de testes pertence ao Surefire. Sem criar tabela: não depende do Docker
@SpringBootTest(properties = {
        "spring.ai.mcp.server.stdio=false",
        "applytrack.dynamodb.endpoint=http://localhost:1", // nunca o banco de verdade
        "applytrack.dynamodb.create-table=false",
        "logging.file.name=target/tests.log"
})
class ApplytrackMcpApplicationTests {

    @Test
    void contextLoads() {
    }

}
