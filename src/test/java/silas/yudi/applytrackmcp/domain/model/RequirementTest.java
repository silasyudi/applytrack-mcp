package silas.yudi.applytrackmcp.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RequirementTest {

    @Test
    void requirementKeepsDataWithoutSurroundingSpaces() {
        assertThat(new Requirement(" Spring Boot ", " Avançado ", Importance.REQUIRED))
                .isEqualTo(new Requirement("Spring Boot", "Avançado", Importance.REQUIRED));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requirementRequiresName(String name) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Requirement(name, "Avançado", Importance.REQUIRED))
                .withMessage("Campo obrigatório: requirement.name");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requirementLevelIsOptional(String level) {
        assertThat(new Requirement("Java", level, Importance.REQUIRED).level()).isNull();
    }

    @Test
    void requirementWithoutImportanceIsUnspecified() {
        assertThat(new Requirement("Java", null, null).importance()).isEqualTo(Importance.UNSPECIFIED);
    }
}
