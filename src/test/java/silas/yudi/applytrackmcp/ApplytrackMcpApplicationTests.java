package silas.yudi.applytrackmcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Sem stdio: o stdin do processo de testes pertence ao Surefire
@SpringBootTest(properties = {
        "spring.ai.mcp.server.stdio=false",
        "logging.file.name=target/tests.log"
})
class ApplytrackMcpApplicationTests {

    @Test
    void contextLoads() {
    }

}
