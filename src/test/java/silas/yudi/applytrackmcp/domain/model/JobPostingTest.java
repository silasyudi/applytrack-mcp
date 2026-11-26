package silas.yudi.applytrackmcp.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
                new JobPosting(company, null, "Java Developer", null, null, null, null, "Description", List.of()),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20))))
                .withMessage("Campo obrigatório: company");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresJobTitle(String jobTitle) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication(
                new JobPosting("Acme", null, jobTitle, null, null, null, null, "Description", List.of()),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20))))
                .withMessage("Campo obrigatório: jobTitle");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresDescription(String description) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, null, description, List.of()),
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

    // Há processos que só chegam por e-mail ou WhatsApp
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void linkIsOptional(String link) {
        assertThat(new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, link, "Description", List.of()),
                ApplicationProcess.start("WhatsApp", LocalDate.of(2026, 11, 20))).posting().link()).isNull();
    }

    @Test
    void requirementsAreOptional() {
        var application = new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, null, "Description", null),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20)));

        assertThat(application.posting().requirements()).isEmpty();
    }

    @Test
    void keepsRequirementsInOrderAndProtectedFromOutsideChanges() {
        var requirements = new ArrayList<>(List.of(JAVA, ENGLISH));
        var application = new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, null, "Description", requirements),
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
                new JobPosting("Acme", null, "Java Developer", null, null, null, null, "Description", requirements),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20))))
                .withMessage("Campo obrigatório: requirement");
    }

    private static JobPosting posting(String finalClient, String seniority, String contractType, String salaryRange) {
        return new JobPosting("Acme", finalClient, "Java Developer", seniority, contractType, salaryRange, null,
                "Description", List.of());
    }
}
