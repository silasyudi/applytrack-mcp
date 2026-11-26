package silas.yudi.applytrackmcp.adapter.in.mcp;

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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import silas.yudi.applytrackmcp.support.DynamoDbLocalContainer;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Sobe o JAR empacotado e conversa com ele por stdio, como o Claude faz. Roda com "mvnw verify". */
class JobApplicationToolsIT {

    private static final String REGISTER = "register_job_application";
    private static final String SEARCH = "search_job_applications";
    private static final String GET = "get_job_application";
    private static final String UPDATE = "update_job_application";
    private static final String DELETE = "delete_job_application";

    private static final Map<String, Object> ACME = merge(Map.of(
            "company", "Acme",
            "jobTitle", "Java Developer",
            "description", "Acme description",
            "platform", "Gupy",
            "link", "https://acme.com/jobs/1",
            "appliedOn", "2026-11-19",
            "requirements", List.of()),
            Map.of("finalClient", "Banco Alfa", "seniority", "Pleno", "contractType", "CLT",
                    "salaryRange", "R$ 8.000 a R$ 10.000"),
            Map.of("status", "Candidatura enviada", "statusUpdatedOn", "2026-11-19", "situation", "ACTIVE",
                    "steps", List.of()));

    // Acentos também provam que o texto sobrevive ao stdio no Windows
    private static final Map<String, Object> BETA = merge(Map.of(
            "company", "Soluções Beta",
            "jobTitle", "Pessoa Desenvolvedora Sênior",
            "description", "Descrição completa",
            "platform", "WhatsApp",
            "link", "https://beta.com/jobs/2",
            "appliedOn", "2026-11-20",
            "requirements", List.of(
                    Map.of("name", "Java", "level", "Avançado", "importance", "REQUIRED"),
                    Map.of("name", "Inglês", "level", "C1", "importance", "DESIRABLE"))),
            Map.of("finalClient", "Loja Delta", "seniority", "Sênior", "contractType", "PJ",
                    "salaryRange", "R$ 15.000"),
            Map.of("status", "Entrevista técnica realizada, aguardando feedback", "statusUpdatedOn", "2026-11-24",
                    "situation", "ACTIVE", "steps", List.of(
                            Map.of("name", "Candidatura", "date", "2026-11-20", "done", true, "notes", "Pelo WhatsApp"),
                            Map.of("name", "Entrevista técnica", "date", "2026-11-24", "done", true, "notes", "Kafka"))));

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

    @Nested
    class Catalog {

        @Test
        void exposesExactlyFiveTools() {
            assertThat(client.listTools().tools()).extracting(Tool::name)
                    .containsExactlyInAnyOrder(REGISTER, SEARCH, GET, UPDATE, DELETE);
        }

        @Test
        void marksDeleteAsDestructive() {
            var delete = client.listTools().tools().stream()
                    .filter(tool -> tool.name().equals(DELETE)).findFirst().orElseThrow();

            assertThat(delete.annotations().destructiveHint()).isTrue();
            assertThat(delete.annotations().readOnlyHint()).isFalse();
        }

        // Diz ao cliente o que cada tool faz: ler, criar ou alterar. Nenhuma acessa algo fora do banco
        @Test
        void describesTheEffectOfEachTool() {
            var hints = client.listTools().tools().stream().collect(Collectors.toMap(Tool::name,
                    tool -> List.of(tool.annotations().readOnlyHint(), tool.annotations().destructiveHint(),
                            tool.annotations().openWorldHint())));

            assertThat(hints).containsExactlyInAnyOrderEntriesOf(Map.of(
                    REGISTER, List.of(false, false, false),
                    SEARCH, List.of(true, false, false),
                    GET, List.of(true, false, false),
                    UPDATE, List.of(false, true, false),
                    DELETE, List.of(false, true, false)));
        }
    }

    @Nested
    class Register {

        @Test
        void confirmsRegistration() throws IOException {
            var result = call(REGISTER, BETA);

            assertThat(result.isError()).isFalse();
            assertThat(text(result)).isEqualTo("Candidatura registrada: Soluções Beta - Pessoa Desenvolvedora Sênior (id: "
                    + idOf("beta") + ")");
        }

        @Test
        void keepsApplicationsAfterServerRestart() throws IOException {
            register(ACME);

            stopServer();
            startServer();

            assertThat(searchAndGet(Map.of())).containsExactly(ACME);
        }

        @Test
        void registersApplicationWithoutPostingFields() throws IOException {
            var minimal = new HashMap<>(BETA);
            minimal.keySet().removeAll(List.of("finalClient", "seniority", "contractType", "salaryRange"));
            register(minimal);

            var expected = new HashMap<String, Object>(minimal);
            for (var key : List.of("finalClient", "seniority", "contractType", "salaryRange")) {
                expected.put(key, null);
            }
            assertThat(searchAndGet(Map.of())).containsExactly(expected);
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
        void registersApplicationWithoutRequirements() throws IOException {
            var withoutRequirements = new HashMap<>(BETA);
            withoutRequirements.remove("requirements");
            register(withoutRequirements);

            var expected = new HashMap<>(BETA);
            expected.put("requirements", List.of());
            assertThat(searchAndGet(Map.of())).containsExactly(expected);
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
        void registersWithDefaultProcess() throws IOException {
            var minimal = new HashMap<>(BETA);
            minimal.keySet().removeAll(List.of("status", "statusUpdatedOn", "situation", "steps"));
            register(minimal);

            var expected = new HashMap<>(minimal);
            expected.putAll(Map.of("status", "Candidatura enviada", "statusUpdatedOn", "2026-11-20", "situation", "ACTIVE",
                    "steps", List.of()));
            assertThat(searchAndGet(Map.of())).containsExactly(expected);
        }

        @Test
        void acceptsScheduledStepInTheFuture() throws IOException {
            var scheduled = new HashMap<>(ACME);
            var step = new HashMap<String, Object>(Map.of("name", "Entrevista com líder", "date", "2999-01-01"));
            scheduled.put("steps", List.of(step));
            register(scheduled);

            var expectedStep = new HashMap<>(step);
            expectedStep.put("done", false);
            expectedStep.put("notes", null);
            var expected = new HashMap<>(ACME);
            expected.put("steps", List.of(expectedStep));
            assertThat(searchAndGet(Map.of())).containsExactly(expected);
        }

        @Test
        void rejectsRegistrationWithoutAnyArgument() throws IOException {
            assertThat(call(REGISTER, Map.of()).isError()).isTrue();
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
        void rejectsSalaryRangeWithoutContractTypeAndChangesNothing() throws IOException {
            var noContract = new HashMap<>(ACME);
            noContract.remove("contractType");
            assertThat(text(call(REGISTER, noContract)).lines().distinct())
                    .containsExactly("Para informar a faixa salarial, informe também o tipo de contrato");

            register(ACME);
            var removal = call(UPDATE, Map.of("id", idOf("acme"), "contractType", ""));
            assertThat(text(removal).lines().distinct())
                    .containsExactly("Para informar a faixa salarial, informe também o tipo de contrato");
            assertThat(searchAndGet(Map.of())).containsExactly(ACME);
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
        void rejectsFutureDateOnRegistration() throws IOException {
            var invalid = new HashMap<>(ACME);
            invalid.put("appliedOn", "2999-01-01");
            invalid.remove("statusUpdatedOn"); // sem ela, a data do status acompanha a da candidatura

            var result = call(REGISTER, invalid);

            assertThat(result.isError()).isTrue();
            assertThat(text(result).lines().distinct())
                    .containsExactly("A data da candidatura não pode ser futura: 2999-01-01");
            assertThat(searchAndGet(Map.of())).isEmpty();
        }

        @Test
        void rejectsStatusDateBeforeApplicationAndInvalidStatusDate() throws IOException {
            var before = new HashMap<>(ACME);
            before.put("statusUpdatedOn", "2026-11-18");
            var invalid = new HashMap<>(ACME);
            invalid.put("statusUpdatedOn", "19/11/2026");

            assertThat(text(call(REGISTER, before)).lines().distinct())
                    .containsExactly("A data do status não pode ser anterior à candidatura: 2026-11-18");
            assertThat(text(call(REGISTER, invalid)).lines().distinct())
                    .containsExactly("Data inválida: 19/11/2026. Use o formato AAAA-MM-DD");
            assertThat(searchAndGet(Map.of())).isEmpty();
        }

        @Test
        void rejectsFutureStatusDateAndChangesNothing() throws IOException {
            var invalid = new HashMap<>(ACME);
            invalid.put("statusUpdatedOn", "2999-01-01");
            var registration = call(REGISTER, invalid);
            assertThat(registration.isError()).isTrue();
            assertThat(text(registration).lines().distinct())
                    .containsExactly("A data do status não pode ser futura: 2999-01-01");

            register(ACME);
            var update = call(UPDATE, Map.of("id", idOf("acme"), "statusUpdatedOn", "2999-01-01", "status", "x"));
            assertThat(update.isError()).isTrue();
            assertThat(searchAndGet(Map.of())).containsExactly(ACME);
        }

        @Test
        void rejectsUnknownSituationAndRegistersNothing() throws IOException {
            var invalid = new HashMap<>(ACME);
            invalid.put("situation", "APROVADO");

            assertThat(call(REGISTER, invalid).isError()).isTrue();
            assertThat(searchAndGet(Map.of())).isEmpty();
        }

        @Test
        void rejectsStepBeforeApplicationAndDoneStepInTheFuture() throws IOException {
            var early = new HashMap<>(ACME);
            early.put("steps", List.of(Map.of("name", "Triagem", "date", "2026-11-18", "done", true)));
            var future = new HashMap<>(ACME);
            future.put("steps", List.of(Map.of("name", "Técnica", "date", "2999-01-01", "done", true)));

            assertThat(text(call(REGISTER, early)).lines().distinct())
                    .containsExactly("A etapa não pode ser anterior à candidatura: Triagem (2026-11-18)");
            assertThat(text(call(REGISTER, future)).lines().distinct())
                    .containsExactly("Etapa marcada como feita não pode ter data futura: Técnica (2999-01-01)");
            assertThat(searchAndGet(Map.of())).isEmpty();
        }
    }

    @Nested
    class Search {

        @Test
        void findsNothingWhenThereAreNoApplications() throws IOException {
            assertThat(searchAndGet(Map.of())).isEmpty();
        }

        @Test
        void findsAllApplicationsMostRecentFirstWhenThereIsNoTerm() throws IOException {
            register(ACME);
            register(BETA);

            assertThat(searchAndGet(Map.of())).containsExactly(BETA, ACME);
        }

        @Test
        void findsTermOnlyInCompany() throws IOException {
            register(ACME);
            register(BETA);

            assertThat(searchAndGet(Map.of("term", "solucoes"))).containsExactly(BETA);
        }

        @Test
        void findsTermOnlyInFinalClient() throws IOException {
            register(ACME);
            register(BETA);

            assertThat(searchAndGet(Map.of("term", "delta"))).containsExactly(BETA);
        }

        @Test
        void findsTermOnlyInJobTitle() throws IOException {
            register(ACME);
            register(BETA);

            assertThat(searchAndGet(Map.of("term", "desenvolvedora"))).containsExactly(BETA);
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
        void findsTermOnlyInRequirementName() throws IOException {
            register(ACME);
            register(BETA);

            assertThat(searchAndGet(Map.of("term", "ingles"))).containsExactly(BETA);
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
        void findsNothingWhenNoApplicationMatches() throws IOException {
            register(ACME);

            assertThat(searchAndGet(Map.of("term", "unknown"))).isEmpty();
        }

        // Contrato, faixa, link, plataforma, status, etapas e nível dos requisitos ficam fora da busca
        @Test
        void doesNotFindTermsThatAreOnlyInOtherFields() throws IOException {
            register(BETA);

            for (var term : List.of("pj", "15.000", "jobs/2", "whatsapp", "aguardando", "pelo", "avancado", "desirable")) {
                assertThat(searchAndGet(Map.of("term", term))).as(term).isEmpty();
            }
        }

        @Test
        void searchReturnsEveryFieldExceptTheDescription() throws IOException {
            register(ACME);

            var found = search(Map.of("term", "acme description"));

            assertThat(found).hasSize(1);
            var expected = new HashMap<>(ACME);
            expected.remove("description");
            expected.remove("steps");
            expected.remove("salaryRange");
            expected.put("id", found.getFirst().get("id"));
            assertThat(found).containsExactly(expected);
            UUID.fromString((String) found.getFirst().get("id"));
        }

        @Test
        void searchSummaryIncludesPostingFieldsButNotSalary() throws IOException {
            register(BETA);

            assertThat(search(Map.of("term", "beta")).getFirst())
                    .containsEntry("finalClient", "Loja Delta")
                    .containsEntry("seniority", "Sênior")
                    .containsEntry("contractType", "PJ")
                    .doesNotContainKey("salaryRange");
        }

        @Test
        void searchSummaryIncludesRequirements() throws IOException {
            register(BETA);

            var found = search(Map.of("term", "beta"));

            assertThat(found).hasSize(1);
            assertThat(found.getFirst().get("requirements")).isEqualTo(BETA.get("requirements"));
        }

        @Test
        void searchSummaryIncludesStatusButNotSteps() throws IOException {
            register(BETA);

            var summary = search(Map.of("term", "beta")).getFirst();

            assertThat(summary).containsEntry("status", "Entrevista técnica realizada, aguardando feedback")
                    .containsEntry("statusUpdatedOn", "2026-11-24")
                    .containsEntry("situation", "ACTIVE")
                    .doesNotContainKeys("steps", "description");
        }
    }

    @Nested
    class Get {

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
        void getGroupsPostingAndProcess() throws IOException {
            register(BETA);
            var id = idOf("beta");

            var nested = getNested(id);

            assertThat(nested).containsOnlyKeys("id", "posting", "process");
            assertThat(asMap(nested.get("posting")))
                    .containsOnlyKeys("company", "finalClient", "jobTitle", "seniority", "contractType", "salaryRange",
                            "description", "link", "requirements")
                    .containsEntry("company", "Soluções Beta");
            assertThat(asMap(nested.get("process")))
                    .containsOnlyKeys("platform", "appliedOn", "status", "statusUpdatedOn", "situation", "steps")
                    .containsEntry("platform", "WhatsApp")
                    .containsEntry("appliedOn", "2026-11-20");
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
    }

    @Nested
    class Update {

        @Test
        void updatesOnlyTheGivenFieldsOfOneApplication() throws IOException {
            register(ACME);
            register(BETA);
            var id = idOf("acme");

            var result = call(UPDATE, Map.of("id", id, "jobTitle", "Analista Sênior"));

            assertThat(result.isError()).isFalse();
            assertThat(text(result)).isEqualTo("Candidatura atualizada: Acme - Analista Sênior (id: " + id + ")");
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
            Map<String, Object> changed = merge(Map.of(
                    "company", "Gama Soluções",
                    "jobTitle", "Analista Sênior",
                    "description", "Nova descrição",
                    "platform", "LinkedIn",
                    "link", "https://gama.com/jobs/3",
                    "appliedOn", "2026-11-18",
                    "requirements", List.of(Map.of("name", "Kafka", "level", "Sênior", "importance", "REQUIRED"))),
                    Map.of("status", "Candidatura enviada", "statusUpdatedOn", "2026-11-18", "situation", "ACTIVE",
                            "steps", List.of()),
                    Map.of("finalClient", "Rede Gama", "seniority", "Especialista", "contractType", "PJ",
                            "salaryRange", "R$ 12.000"));

            var arguments = new HashMap<>(changed);
            arguments.put("id", id);
            var result = call(UPDATE, arguments);

            assertThat(result.isError()).isFalse();
            assertThat(text(result)).isEqualTo("Candidatura atualizada: Gama Soluções - Analista Sênior (id: " + id + ")");
            assertThat(searchAndGet(Map.of())).containsExactly(BETA, changed);
            assertThat(idOf("gama")).isEqualTo(id);
        }

        @Test
        void updatesAndRemovesPostingFields() throws IOException {
            register(BETA);

            assertThat(call(UPDATE, Map.of("id", idOf("beta"), "seniority", "Especialista", "finalClient", "",
                    "salaryRange", "")).isError()).isFalse();

            var expected = new HashMap<String, Object>(BETA);
            expected.put("seniority", "Especialista");
            expected.put("finalClient", null);
            expected.put("salaryRange", null);
            assertThat(searchAndGet(Map.of())).containsExactly(expected);
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
        void addsALinkToAnApplicationWithoutLink() throws IOException {
            var withoutLink = new HashMap<>(ACME);
            withoutLink.remove("link");
            register(withoutLink);

            assertThat(call(UPDATE, Map.of("id", idOf("acme"), "link", "https://acme.com/jobs/9")).isError()).isFalse();

            var expected = new HashMap<>(ACME);
            expected.put("link", "https://acme.com/jobs/9");
            assertThat(searchAndGet(Map.of())).containsExactly(expected);
        }

        @Test
        void changesDescriptionTogetherWithRequirements() throws IOException {
            register(BETA);
            var kafka = List.of(Map.of("name", "Kafka", "level", "Sênior", "importance", "REQUIRED"));

            assertThat(call(UPDATE, Map.of("id", idOf("beta"), "description", "Mensageria com Kafka",
                    "requirements", kafka)).isError()).isFalse();

            var expected = new HashMap<>(BETA);
            expected.put("description", "Mensageria com Kafka");
            expected.put("requirements", kafka);
            assertThat(searchAndGet(Map.of())).containsExactly(expected);
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

        @Test
        void movingTheApplicationDateMovesAnUnchangedStatusDate() throws IOException {
            register(ACME);

            assertThat(call(UPDATE, Map.of("id", idOf("acme"), "appliedOn", "2026-11-17")).isError()).isFalse();

            var expected = new HashMap<>(ACME);
            expected.put("appliedOn", "2026-11-17");
            expected.put("statusUpdatedOn", "2026-11-17");
            assertThat(searchAndGet(Map.of())).containsExactly(expected);
        }

        @Test
        void changingStatusMovesStatusDateToTodayInBrazil() throws IOException {
            register(ACME);
            var zone = ZoneId.of("America/Sao_Paulo");
            var before = LocalDate.now(zone).toString();

            assertThat(call(UPDATE, Map.of("id", idOf("acme"), "status", "Fit cultural marcado")).isError()).isFalse();

            var after = LocalDate.now(zone).toString();
            var stored = searchAndGet(Map.of()).getFirst();
            assertThat(stored).containsEntry("status", "Fit cultural marcado");
            // Duas datas só se o teste atravessar a meia-noite
            assertThat(stored.get("statusUpdatedOn")).isIn(before, after);
        }

        @Test
        void resendingTheSameStatusKeepsTheStatusDate() throws IOException {
            register(BETA);

            assertThat(call(UPDATE, Map.of("id", idOf("beta"), "status", BETA.get("status"), "situation", "ACTIVE"))
                    .isError()).isFalse();

            assertThat(searchAndGet(Map.of())).containsExactly(BETA);
        }

        @Test
        void changesSituationTogetherWithStatus() throws IOException {
            register(BETA);
            var zone = ZoneId.of("America/Sao_Paulo");
            var before = LocalDate.now(zone).toString();

            assertThat(call(UPDATE, Map.of("id", idOf("beta"), "status", "Desisti", "situation", "WITHDRAWN")).isError())
                    .isFalse();

            var after = LocalDate.now(zone).toString();
            var stored = searchAndGet(Map.of()).getFirst();
            assertThat(stored).containsEntry("status", "Desisti").containsEntry("situation", "WITHDRAWN");
            assertThat(stored.get("statusUpdatedOn")).isIn(before, after);
        }

        @Test
        void updatesSituationAndStepsWithGivenStatusDate() throws IOException {
            register(BETA);
            var steps = List.of(Map.of("name", "Entrevista técnica", "date", "2026-11-24", "done", true, "notes", "Kafka"));

            assertThat(call(UPDATE, Map.of("id", idOf("beta"), "status", "Reprovado na técnica", "statusUpdatedOn",
                    "2026-11-25", "situation", "REJECTED", "steps", steps)).isError()).isFalse();

            var expected = new HashMap<>(BETA);
            expected.putAll(Map.of("status", "Reprovado na técnica", "statusUpdatedOn", "2026-11-25", "situation", "REJECTED",
                    "steps", steps));
            assertThat(searchAndGet(Map.of())).containsExactly(expected);
        }

        @Test
        void updateWithEmptyStepsRemovesThem() throws IOException {
            register(BETA);

            assertThat(call(UPDATE, Map.of("id", idOf("beta"), "steps", List.of())).isError()).isFalse();

            var expected = new HashMap<>(BETA);
            expected.put("steps", List.of());
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
        void rejectsDescriptionWithoutRequirementsAndChangesNothing() throws IOException {
            register(BETA);

            var result = call(UPDATE, Map.of("id", idOf("beta"), "description", "Nova descrição"));

            assertThat(text(result).lines().distinct()).containsExactly(
                    "Para informar a descrição, envie também os requisitos (lista vazia se a vaga não citar nenhum)");
            assertThat(searchAndGet(Map.of())).containsExactly(BETA);
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
        void rejectsStatusDateOrSituationWithoutStatusAndChangesNothing() throws IOException {
            register(BETA);
            var id = idOf("beta");

            var dateOnly = call(UPDATE, Map.of("id", id, "statusUpdatedOn", "2026-11-25"));
            var situationOnly = call(UPDATE, Map.of("id", id, "situation", "REJECTED"));

            assertThat(text(dateOnly).lines().distinct())
                    .containsExactly("Para informar a data do status, envie também o status");
            assertThat(text(situationOnly).lines().distinct())
                    .containsExactly("Para informar a situação, envie também o status");
            assertThat(searchAndGet(Map.of())).containsExactly(BETA);
        }
    }

    @Nested
    class Delete {

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
    }

    @SafeVarargs
    private static Map<String, Object> merge(Map<String, Object>... parts) {
        var merged = new HashMap<String, Object>();
        for (var part : parts) {
            merged.putAll(part);
        }
        return Map.copyOf(merged);
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

    // Junta posting e process num mapa só, para comparar com os mapas planos dos testes
    private Map<String, Object> get(String id) throws IOException {
        var nested = getNested(id);
        var flat = new HashMap<String, Object>();
        flat.put("id", nested.get("id"));
        flat.putAll(asMap(nested.get("posting")));
        flat.putAll(asMap(nested.get("process")));
        return flat;
    }

    private Map<String, Object> getNested(String id) throws IOException {
        var result = call(GET, Map.of("id", id));
        assertThat(result.isError()).isFalse();
        return McpJsonDefaults.getMapper().readValue(text(result), new TypeRef<Map<String, Object>>() {
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
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
