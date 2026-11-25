package silas.yudi.applytrackmcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Sobe o JAR empacotado e conversa com ele por stdio, como o Claude faz. Roda com "mvnw verify". */
class McpServerIT {

    private static final String REGISTER = "register_job_application";
    private static final String SEARCH = "search_job_applications";

    private static final Map<String, Object> ACME = Map.of(
            "company", "Acme",
            "jobTitle", "Java Developer",
            "description", "Acme description",
            "platform", "Gupy",
            "link", "https://acme.com/jobs/1");

    // Acentos também provam que o texto sobrevive ao stdio no Windows
    private static final Map<String, Object> BETA = Map.of(
            "company", "Soluções Beta",
            "jobTitle", "Pessoa Desenvolvedora Sênior",
            "description", "Descrição completa",
            "platform", "WhatsApp",
            "link", "https://beta.com/jobs/2");

    // Uma tabela nova por teste: cada um começa sem candidaturas
    private final String tableName = "test-" + UUID.randomUUID();

    private McpSyncClient client;

    @BeforeEach
    void startServer() {
        var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        var parameters = ServerParameters.builder(java)
                .args("-jar", System.getProperty("app.jar"), "--logging.file.name=target/mcp-server-it.log",
                        "--applytrack.dynamodb.endpoint=" + DynamoDbLocalContainer.endpoint(),
                        "--applytrack.dynamodb.table-name=" + tableName)
                .build();
        client = McpClient.sync(new StdioClientTransport(parameters, McpJsonDefaults.getMapper()))
                .requestTimeout(Duration.ofSeconds(30))
                .initializationTimeout(Duration.ofSeconds(30))
                .build();
        client.initialize();
    }

    @AfterEach
    void stopServer() {
        client.closeGracefully();
    }

    @Test
    void exposesExactlyTwoTools() {
        assertThat(client.listTools().tools()).extracting(Tool::name)
                .containsExactlyInAnyOrder(REGISTER, SEARCH);
    }

    @Test
    void confirmsRegistration() {
        var result = call(REGISTER, BETA);

        assertThat(result.isError()).isFalse();
        assertThat(text(result)).isEqualTo("Candidatura registrada: Soluções Beta - Pessoa Desenvolvedora Sênior");
    }

    @Test
    void findsNothingWhenThereAreNoApplications() throws IOException {
        assertThat(search(Map.of())).isEmpty();
    }

    @Test
    void findsNothingWhenNoApplicationMatches() throws IOException {
        register(ACME);

        assertThat(search(Map.of("term", "unknown"))).isEmpty();
    }

    @Test
    void findsOnlyTheMatchingApplicationWithAllItsData() throws IOException {
        register(ACME);
        register(BETA);

        assertThat(search(Map.of("term", "solucoes"))).containsExactly(BETA);
    }

    @Test
    void findsAllApplicationsWhenThereIsNoTerm() throws IOException {
        register(ACME);
        register(BETA);

        assertThat(search(Map.of())).containsExactlyInAnyOrder(ACME, BETA);
    }

    @Test
    void registersApplicationWithoutLink() throws IOException {
        var withoutLink = new HashMap<>(ACME);
        withoutLink.remove("link");
        register(withoutLink);

        var expected = new HashMap<>(ACME);
        expected.put("link", null);
        assertThat(search(Map.of())).containsExactly(expected);
    }

    @Test
    void rejectsBlankRequiredFieldAndRegistersNothing() throws IOException {
        var invalid = new HashMap<>(ACME);
        invalid.put("company", " ");

        var result = call(REGISTER, invalid);

        assertThat(result.isError()).isTrue();
        // O Spring AI repete a mensagem em duas linhas
        assertThat(text(result).lines().distinct()).containsExactly("Campo obrigatório: company");
        assertThat(search(Map.of())).isEmpty();
    }

    @Test
    void rejectsMissingRequiredFieldAndRegistersNothing() throws IOException {
        var invalid = new HashMap<>(ACME);
        invalid.remove("platform");

        // A mensagem vem da validação do schema e muda com o idioma: não é verificada
        assertThat(call(REGISTER, invalid).isError()).isTrue();
        assertThat(search(Map.of())).isEmpty();
    }

    @Test
    void keepsApplicationsAfterServerRestart() throws IOException {
        register(ACME);

        stopServer();
        startServer();

        assertThat(search(Map.of())).containsExactly(ACME);
    }

    private void register(Map<String, Object> application) {
        assertThat(call(REGISTER, application).isError()).isFalse();
    }

    private List<Map<String, Object>> search(Map<String, Object> arguments) throws IOException {
        var result = call(SEARCH, arguments);
        assertThat(result.isError()).isFalse();
        return McpJsonDefaults.getMapper().readValue(text(result), new TypeRef<List<Map<String, Object>>>() {
        });
    }

    private CallToolResult call(String tool, Map<String, Object> arguments) {
        return client.callTool(new CallToolRequest(tool, arguments));
    }

    private static String text(CallToolResult result) {
        assertThat(result.content()).hasSize(1);
        return ((TextContent) result.content().getFirst()).text();
    }
}
