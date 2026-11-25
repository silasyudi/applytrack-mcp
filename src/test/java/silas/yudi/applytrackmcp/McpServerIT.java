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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Sobe o JAR empacotado e conversa com ele por stdio, como o Claude faz. Roda com "mvnw verify". */
class McpServerIT {

    private static final String REGISTER = "register_job_application";
    private static final String SEARCH = "search_job_applications";
    private static final String GET = "get_job_application";
    private static final String UPDATE = "update_job_application";
    private static final String DELETE = "delete_job_application";

    private static final Map<String, Object> ACME = Map.of(
            "company", "Acme",
            "jobTitle", "Java Developer",
            "description", "Acme description",
            "platform", "Gupy",
            "link", "https://acme.com/jobs/1",
            "appliedOn", "2026-11-19",
            "requirements", List.of());

    // Acentos também provam que o texto sobrevive ao stdio no Windows
    private static final Map<String, Object> BETA = Map.of(
            "company", "Soluções Beta",
            "jobTitle", "Pessoa Desenvolvedora Sênior",
            "description", "Descrição completa",
            "platform", "WhatsApp",
            "link", "https://beta.com/jobs/2",
            "appliedOn", "2026-11-20",
            "requirements", List.of(
                    Map.of("name", "Java", "level", "Avançado", "importance", "REQUIRED"),
                    Map.of("name", "Inglês", "level", "C1", "importance", "DESIRABLE")));

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
    void exposesExactlyFiveTools() {
        assertThat(client.listTools().tools()).extracting(Tool::name)
                .containsExactlyInAnyOrder(REGISTER, SEARCH, GET, UPDATE, DELETE);
    }

    @Test
    void confirmsRegistration() {
        var result = call(REGISTER, BETA);

        assertThat(result.isError()).isFalse();
        assertThat(text(result)).isEqualTo("Candidatura registrada: Soluções Beta - Pessoa Desenvolvedora Sênior");
    }

    @Test
    void findsNothingWhenThereAreNoApplications() throws IOException {
        assertThat(searchAndGet(Map.of())).isEmpty();
    }

    @Test
    void findsNothingWhenNoApplicationMatches() throws IOException {
        register(ACME);

        assertThat(searchAndGet(Map.of("term", "unknown"))).isEmpty();
    }

    @Test
    void findsTermOnlyInCompany() throws IOException {
        register(ACME);
        register(BETA);

        assertThat(searchAndGet(Map.of("term", "solucoes"))).containsExactly(BETA);
    }

    @Test
    void findsApplicationByTechnologyThatIsOnlyInTheDescription() throws IOException {
        var generic = new HashMap<>(BETA);
        generic.put("description", "Requisitos:\n- Java com Spring Boot\n- Mensageria com Kafka");
        register(ACME);
        register(generic);

        assertThat(searchAndGet(Map.of("term", "kafka"))).containsExactly(generic);
    }

    @Test
    void findsTermOnlyInJobTitle() throws IOException {
        register(ACME);
        register(BETA);

        assertThat(searchAndGet(Map.of("term", "desenvolvedora"))).containsExactly(BETA);
    }

    @Test
    void returnsOnceAnApplicationThatMatchesInSeveralFieldsAndTimes() throws IOException {
        var everywhere = new HashMap<>(BETA);
        everywhere.put("company", "Kafka House");
        everywhere.put("jobTitle", "Engenheiro Kafka");
        everywhere.put("description", "Kafka, Kafka Streams e Kafka Connect");
        register(ACME);
        register(everywhere);

        assertThat(searchAndGet(Map.of("term", "kafka"))).containsExactly(everywhere);
    }

    @Test
    void findsAllApplicationsMostRecentFirstWhenThereIsNoTerm() throws IOException {
        register(ACME);
        register(BETA);

        assertThat(searchAndGet(Map.of())).containsExactly(BETA, ACME);
    }

    @Test
    void registersApplicationWithoutLink() throws IOException {
        var withoutLink = new HashMap<>(ACME);
        withoutLink.remove("link");
        register(withoutLink);

        var expected = new HashMap<>(ACME);
        expected.put("link", null);
        assertThat(searchAndGet(Map.of())).containsExactly(expected);
    }

    @Test
    void rejectsMissingDateAndRegistersNothing() throws IOException {
        var invalid = new HashMap<>(ACME);
        invalid.remove("appliedOn");

        assertThat(call(REGISTER, invalid).isError()).isTrue();
        assertThat(searchAndGet(Map.of())).isEmpty();
    }

    @Test
    void rejectsInvalidDateAndRegistersNothing() throws IOException {
        var invalid = new HashMap<>(ACME);
        invalid.put("appliedOn", "20/11/2026");

        var result = call(REGISTER, invalid);

        assertThat(result.isError()).isTrue();
        assertThat(text(result).lines().distinct())
                .containsExactly("Data inválida: 20/11/2026. Use o formato AAAA-MM-DD");
        assertThat(searchAndGet(Map.of())).isEmpty();
    }

    @Test
    void rejectsBlankRequiredFieldAndRegistersNothing() throws IOException {
        var invalid = new HashMap<>(ACME);
        invalid.put("company", " ");

        var result = call(REGISTER, invalid);

        assertThat(result.isError()).isTrue();
        // O Spring AI repete a mensagem em duas linhas
        assertThat(text(result).lines().distinct()).containsExactly("Campo obrigatório: company");
        assertThat(searchAndGet(Map.of())).isEmpty();
    }

    @Test
    void rejectsMissingRequiredFieldAndRegistersNothing() throws IOException {
        var invalid = new HashMap<>(ACME);
        invalid.remove("platform");

        // A mensagem vem da validação do schema e muda com o idioma: não é verificada
        assertThat(call(REGISTER, invalid).isError()).isTrue();
        assertThat(searchAndGet(Map.of())).isEmpty();
    }

    @Test
    void keepsApplicationsAfterServerRestart() throws IOException {
        register(ACME);

        stopServer();
        startServer();

        assertThat(searchAndGet(Map.of())).containsExactly(ACME);
    }

    @Test
    void updatesOnlyTheGivenFieldsOfOneApplication() throws IOException {
        register(ACME);
        register(BETA);
        var id = idOf("acme");

        var result = call(UPDATE, Map.of("id", id, "jobTitle", "Analista Sênior"));

        assertThat(result.isError()).isFalse();
        assertThat(text(result)).isEqualTo("Candidatura atualizada: Acme - Analista Sênior");
        var expected = new HashMap<>(ACME);
        expected.put("jobTitle", "Analista Sênior");
        assertThat(searchAndGet(Map.of())).containsExactly(BETA, expected);
        assertThat(idOf("acme")).isEqualTo(id);
    }

    @Test
    void updatesAllFieldsOfOneApplication() throws IOException {
        register(ACME);
        register(BETA);
        var id = idOf("acme");
        Map<String, Object> changed = Map.of(
                "company", "Gama Soluções",
                "jobTitle", "Analista Sênior",
                "description", "Nova descrição",
                "platform", "LinkedIn",
                "link", "https://gama.com/jobs/3",
                "appliedOn", "2026-11-18",
                "requirements", List.of(Map.of("name", "Kafka", "level", "Sênior", "importance", "REQUIRED")));

        var arguments = new HashMap<>(changed);
        arguments.put("id", id);
        var result = call(UPDATE, arguments);

        assertThat(result.isError()).isFalse();
        assertThat(text(result)).isEqualTo("Candidatura atualizada: Gama Soluções - Analista Sênior");
        assertThat(searchAndGet(Map.of())).containsExactly(BETA, changed);
        assertThat(idOf("gama")).isEqualTo(id);
    }

    @Test
    void rejectsFutureDateOnRegistration() throws IOException {
        var invalid = new HashMap<>(ACME);
        invalid.put("appliedOn", "2999-01-01");

        var result = call(REGISTER, invalid);

        assertThat(result.isError()).isTrue();
        assertThat(text(result).lines().distinct())
                .containsExactly("A data da candidatura não pode ser futura: 2999-01-01");
        assertThat(searchAndGet(Map.of())).isEmpty();
    }

    @Test
    void rejectsFutureDateOnUpdate() throws IOException {
        register(ACME);

        var result = call(UPDATE, Map.of("id", idOf("acme"), "appliedOn", "2999-01-01"));

        assertThat(result.isError()).isTrue();
        assertThat(text(result).lines().distinct())
                .containsExactly("A data da candidatura não pode ser futura: 2999-01-01");
        assertThat(searchAndGet(Map.of())).containsExactly(ACME);
    }

    @Test
    void rejectsRegistrationWithoutAnyArgument() throws IOException {
        assertThat(call(REGISTER, Map.of()).isError()).isTrue();
        assertThat(searchAndGet(Map.of())).isEmpty();
    }

    @Test
    void removesLinkWithBlankText() throws IOException {
        register(ACME);

        assertThat(call(UPDATE, Map.of("id", idOf("acme"), "link", "")).isError()).isFalse();

        var expected = new HashMap<>(ACME);
        expected.put("link", null);
        assertThat(searchAndGet(Map.of())).containsExactly(expected);
    }

    @Test
    void reportsUnknownIdAndChangesNothing() throws IOException {
        register(ACME);

        var result = call(UPDATE, Map.of("id", "unknown", "jobTitle", "Analyst"));

        assertThat(result.isError()).isTrue();
        assertThat(text(result).lines().distinct()).containsExactly("Candidatura não encontrada: unknown");
        assertThat(searchAndGet(Map.of())).containsExactly(ACME);
    }

    @Test
    void rejectsUpdateWithoutChanges() throws IOException {
        register(ACME);

        var result = call(UPDATE, Map.of("id", idOf("acme")));

        assertThat(result.isError()).isTrue();
        assertThat(text(result).lines().distinct()).containsExactly("Informe ao menos um campo para alterar");
        assertThat(searchAndGet(Map.of())).containsExactly(ACME);
    }

    @Test
    void marksDeleteAsDestructive() {
        var delete = client.listTools().tools().stream()
                .filter(tool -> tool.name().equals(DELETE)).findFirst().orElseThrow();

        assertThat(delete.annotations().destructiveHint()).isTrue();
        assertThat(delete.annotations().readOnlyHint()).isFalse();
    }

    @Test
    void deletesOnlyTheGivenApplicationEvenAfterServerRestart() throws IOException {
        register(ACME);
        register(BETA);

        var result = call(DELETE, Map.of("id", idOf("acme")));

        assertThat(result.isError()).isFalse();
        assertThat(text(result)).isEqualTo("Candidatura removida: Acme - Java Developer");
        assertThat(searchAndGet(Map.of())).containsExactly(BETA);

        stopServer();
        startServer();
        assertThat(searchAndGet(Map.of())).containsExactly(BETA);
    }

    @Test
    void reportsAlreadyDeletedApplication() throws IOException {
        register(ACME);
        register(BETA);
        var id = idOf("acme");
        assertThat(call(DELETE, Map.of("id", id)).isError()).isFalse();

        var result = call(DELETE, Map.of("id", id));

        assertThat(result.isError()).isTrue();
        assertThat(text(result).lines().distinct()).containsExactly("Candidatura não encontrada: " + id);
        assertThat(searchAndGet(Map.of())).containsExactly(BETA);
    }

    @Test
    void rejectsDeleteWithBlankOrMissingIdAndDeletesNothing() throws IOException {
        register(ACME);

        var blank = call(DELETE, Map.of("id", " "));
        assertThat(blank.isError()).isTrue();
        assertThat(text(blank).lines().distinct()).containsExactly("Campo obrigatório: id");
        assertThat(call(DELETE, Map.of()).isError()).isTrue();
        assertThat(searchAndGet(Map.of())).containsExactly(ACME);
    }

    @Test
    void searchReturnsEveryFieldExceptTheDescription() throws IOException {
        register(ACME);

        var found = search(Map.of("term", "acme description"));

        assertThat(found).hasSize(1);
        var expected = new HashMap<>(ACME);
        expected.remove("description");
        expected.put("id", found.getFirst().get("id"));
        assertThat(found).containsExactly(expected);
        UUID.fromString((String) found.getFirst().get("id"));
    }

    @Test
    void getReturnsOnlyTheRequestedApplicationWithItsDescription() throws IOException {
        register(ACME);
        register(BETA);
        var id = idOf("solucoes");

        var expected = new HashMap<>(BETA);
        expected.put("id", id);
        assertThat(get(id)).isEqualTo(expected);
    }

    @Test
    void getReportsUnknownBlankAndMissingId() {
        register(ACME);

        var unknown = call(GET, Map.of("id", "unknown"));
        assertThat(unknown.isError()).isTrue();
        assertThat(text(unknown).lines().distinct()).containsExactly("Candidatura não encontrada: unknown");

        var blank = call(GET, Map.of("id", " "));
        assertThat(blank.isError()).isTrue();
        assertThat(text(blank).lines().distinct()).containsExactly("Campo obrigatório: id");

        assertThat(call(GET, Map.of()).isError()).isTrue();
    }

    @Test
    void searchSummaryIncludesRequirements() throws IOException {
        register(BETA);

        var found = search(Map.of("term", "beta"));

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().get("requirements")).isEqualTo(BETA.get("requirements"));
    }

    @Test
    void storesRequirementWithoutLevel() throws IOException {
        var application = new HashMap<>(ACME);
        application.put("requirements", List.of(Map.of("name", "Node.js", "importance", "UNSPECIFIED")));
        register(application);

        var requirement = new HashMap<String, Object>();
        requirement.put("name", "Node.js");
        requirement.put("level", null);
        requirement.put("importance", "UNSPECIFIED");
        var expected = new HashMap<>(ACME);
        expected.put("requirements", List.of(requirement));
        assertThat(searchAndGet(Map.of())).containsExactly(expected);
    }

    @Test
    void registersApplicationWithoutRequirements() throws IOException {
        var withoutRequirements = new HashMap<>(BETA);
        withoutRequirements.remove("requirements");
        register(withoutRequirements);

        var expected = new HashMap<>(BETA);
        expected.put("requirements", List.of());
        assertThat(searchAndGet(Map.of())).containsExactly(expected);
    }

    // A IA precisa classificar cada requisito, nem que seja como UNSPECIFIED
    @Test
    void rejectsRequirementWithoutImportanceOrNameAndRegistersNothing() throws IOException {
        var withoutImportance = new HashMap<>(ACME);
        withoutImportance.put("requirements", List.of(Map.of("name", "Java")));
        var withoutName = new HashMap<>(ACME);
        withoutName.put("requirements", List.of(Map.of("importance", "REQUIRED")));

        assertThat(call(REGISTER, withoutImportance).isError()).isTrue();
        assertThat(call(REGISTER, withoutName).isError()).isTrue();
        assertThat(searchAndGet(Map.of())).isEmpty();
    }

    @Test
    void rejectsBlankRequirementNameAndRegistersNothing() throws IOException {
        var invalid = new HashMap<>(ACME);
        invalid.put("requirements", List.of(Map.of("name", " ", "importance", "REQUIRED")));

        var result = call(REGISTER, invalid);

        assertThat(result.isError()).isTrue();
        assertThat(text(result).lines().distinct()).containsExactly("Campo obrigatório: requirement.name");
        assertThat(searchAndGet(Map.of())).isEmpty();
    }

    @Test
    void rejectsUnknownImportanceAndRegistersNothing() throws IOException {
        var invalid = new HashMap<>(ACME);
        invalid.put("requirements", List.of(Map.of("name", "Java", "importance", "OBRIGATORIO")));

        assertThat(call(REGISTER, invalid).isError()).isTrue();
        assertThat(searchAndGet(Map.of())).isEmpty();
    }

    @Test
    void updateReplacesRequirementsAndKeepsTheOtherFields() throws IOException {
        register(BETA);
        var kafka = List.of(Map.of("name", "Kafka", "level", "Sênior", "importance", "DESIRABLE"));

        assertThat(call(UPDATE, Map.of("id", idOf("beta"), "requirements", kafka)).isError()).isFalse();

        var expected = new HashMap<>(BETA);
        expected.put("requirements", kafka);
        assertThat(searchAndGet(Map.of())).containsExactly(expected);
    }

    @Test
    void updateWithEmptyRequirementsRemovesThem() throws IOException {
        register(BETA);

        assertThat(call(UPDATE, Map.of("id", idOf("beta"), "requirements", List.of())).isError()).isFalse();

        var expected = new HashMap<>(BETA);
        expected.put("requirements", List.of());
        assertThat(searchAndGet(Map.of())).containsExactly(expected);
    }

    @Test
    void updateWithoutRequirementsKeepsThem() throws IOException {
        register(BETA);

        assertThat(call(UPDATE, Map.of("id", idOf("beta"), "jobTitle", "Analista")).isError()).isFalse();

        var expected = new HashMap<>(BETA);
        expected.put("jobTitle", "Analista");
        assertThat(searchAndGet(Map.of())).containsExactly(expected);
    }

    private void register(Map<String, Object> application) {
        assertThat(call(REGISTER, application).isError()).isFalse();
    }

    // Busca e lê cada candidatura completa; o id, aleatório, é verificado e retirado da comparação
    private List<Map<String, Object>> searchAndGet(Map<String, Object> arguments) throws IOException {
        var complete = new ArrayList<Map<String, Object>>();
        for (var summary : search(arguments)) {
            var id = (String) summary.get("id");
            var application = get(id);
            assertThat(application.remove("id")).isEqualTo(UUID.fromString(id).toString());
            complete.add(application);
        }
        return complete;
    }

    private Map<String, Object> get(String id) throws IOException {
        var result = call(GET, Map.of("id", id));
        assertThat(result.isError()).isFalse();
        return McpJsonDefaults.getMapper().readValue(text(result), new TypeRef<Map<String, Object>>() {
        });
    }

    private String idOf(String term) throws IOException {
        var found = search(Map.of("term", term));
        assertThat(found).hasSize(1);
        return (String) found.getFirst().get("id");
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
