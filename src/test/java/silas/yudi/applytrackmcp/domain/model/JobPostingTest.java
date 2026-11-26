package silas.yudi.applytrackmcp.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class JobPostingTest {

    private static final String SALARY_WITHOUT_CONTRACT =
            "Para informar a faixa salarial, informe também o tipo de contrato";

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
    void contractTypeAloneIsAccepted() {
        assertThat(posting(null, null, "CLT", null).contractType()).isEqualTo("CLT");
    }

    private static JobPosting posting(String finalClient, String seniority, String contractType, String salaryRange) {
        return new JobPosting("Acme", finalClient, "Java Developer", seniority, contractType, salaryRange,
                "Description", null, List.of());
    }
}
