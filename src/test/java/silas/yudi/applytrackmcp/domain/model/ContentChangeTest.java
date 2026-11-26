package silas.yudi.applytrackmcp.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ContentChangeTest {

    private static final List<Requirement> JAVA = List.of(new Requirement("Java", null, Importance.REQUIRED));

    @Test
    void nothingAboutContentMeansNoContentChange() {
        assertThat(ContentChange.of(null, null)).isNull();
    }

    @Test
    void requirementsAloneAreAValidChange() {
        assertThat(ContentChange.of(null, JAVA)).isEqualTo(new ContentChange(null, JAVA));
    }

    @Test
    void descriptionWithEmptyRequirementsIsAValidChange() {
        assertThat(ContentChange.of("Nova descrição", List.of())).isEqualTo(new ContentChange("Nova descrição", List.of()));
    }

    @Test
    void descriptionWithoutRequirementsIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ContentChange.of("Nova descrição", null))
                .withMessage("Para informar a descrição, envie também os requisitos (lista vazia se a vaga não citar nenhum)");
    }
}
