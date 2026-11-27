package silas.yudi.applytrackmcp.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static silas.yudi.applytrackmcp.support.JobPostingBuilder.aPosting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class JobPostingTest {

    private static final Requirement ENGLISH = new Requirement("Inglês", "C1", Importance.DESIRABLE);
    private static final Requirement JAVA = new Requirement("Java", "Avançado", Importance.REQUIRED);
    private static final Requirement NODE = new Requirement("Node.js", null, Importance.UNSPECIFIED);
    private static final String SALARY_WITHOUT_CONTRACT =
            "Para informar a faixa salarial, informe também o tipo de contrato";

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresCompany(String company) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication(
                        aPosting().company(company).build(),
                        ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20))))
                .withMessage("Campo obrigatório: company");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresJobTitle(String jobTitle) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication(
                        aPosting().jobTitle(jobTitle).build(),
                        ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20))))
                .withMessage("Campo obrigatório: jobTitle");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresDescription(String description) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication(
                        aPosting().description(description).build(),
                        ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20))))
                .withMessage("Campo obrigatório: description");
    }

    @Test
    void keepsPostingFieldsWithoutSurroundingSpaces() {
        var posting = posting(" Banco Alfa ", " Sênior ", " PJ ", " R$ 15.000 a R$ 18.000 ");

        assertThat(posting.finalClient()).isEqualTo("Banco Alfa");
        assertThat(posting.seniority()).isEqualTo("Sênior");
        assertThat(posting.contractType()).isEqualTo("PJ");
        assertThat(posting.salaryRange()).isEqualTo("R$ 15.000 a R$ 18.000");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void postingFieldsAreOptional(String empty) {
        var posting = posting(empty, empty, empty, empty);

        assertThat(posting.finalClient()).isNull();
        assertThat(posting.seniority()).isNull();
        assertThat(posting.contractType()).isNull();
        assertThat(posting.salaryRange()).isNull();
    }

    @Test
    void contractTypeAloneIsAccepted() {
        assertThat(posting(null, null, "CLT", null).contractType()).isEqualTo("CLT");
    }

    // "R$ 15 mil" muda muito entre CLT e PJ
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void salaryRangeRequiresContractType(String contractType) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> posting(null, null, contractType, "R$ 15.000"))
                .withMessage(SALARY_WITHOUT_CONTRACT);
    }

    @Test
    void keepsWorkModelAndLocationWithoutSurroundingSpaces() {
        var posting = aPosting().workModel(WorkModel.HYBRID).location(" São Paulo, SP ").build();

        assertThat(posting.workModel()).isEqualTo(WorkModel.HYBRID);
        assertThat(posting.location()).isEqualTo("São Paulo, SP");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void workModelAndLocationAreOptional(String location) {
        var posting = aPosting().workModel(null).location(location).build();

        assertThat(posting.workModel()).isNull();
        assertThat(posting.location()).isNull();
    }

    // Nem todo anúncio diz onde: exigir a cidade faria a IA inventar uma para cadastrar
    @Test
    void locationIsIndependentOfWorkModel() {
        assertThat(aPosting().workModel(WorkModel.ON_SITE).build().location()).isNull();
        assertThat(aPosting().location("Brasil").build().workModel()).isNull();
    }

    // Há processos que só chegam por e-mail ou WhatsApp
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void linkIsOptional(String link) {
        assertThat(new JobApplication(
                aPosting().link(link).build(),
                ApplicationProcess.start("WhatsApp", LocalDate.of(2026, 11, 20))).posting().link()).isNull();
    }

    @Test
    void requirementsAreOptional() {
        var application = new JobApplication(
                aPosting().requirements(null).build(),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20)));

        assertThat(application.posting().requirements()).isEmpty();
    }

    @Test
    void keepsRequirementsInOrderAndProtectedFromOutsideChanges() {
        var requirements = new ArrayList<>(List.of(JAVA, ENGLISH));
        var application = new JobApplication(
                aPosting().requirements(requirements).build(),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20)));

        requirements.add(NODE);

        assertThat(application.posting().requirements()).containsExactly(JAVA, ENGLISH);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> application.posting().requirements().add(NODE));
    }

    @Test
    void rejectsNullRequirement() {
        var requirements = new ArrayList<Requirement>();
        requirements.add(JAVA);
        requirements.add(null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication(
                        aPosting().requirements(requirements).build(),
                        ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20))))
                .withMessage("Campo obrigatório: requirement");
    }

    private static JobPosting posting(String finalClient, String seniority, String contractType, String salaryRange) {
        return aPosting()
                .finalClient(finalClient)
                .seniority(seniority)
                .contractType(contractType)
                .salaryRange(salaryRange)
                .build();
    }
}
