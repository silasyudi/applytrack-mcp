package silas.yudi.applytrackmcp.adapter.in.mcp;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DateParamTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 20);

    @Nested
    class Required {

        @ParameterizedTest
        @ValueSource(strings = {"2026-11-20", " 2026-11-20 "})
        void parsesIsoDateWithoutSurroundingSpaces(String value) {
            assertThat(DateParam.required("appliedOn", value)).isEqualTo(DAY);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        void rejectsMissingDateNamingTheField(String value) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> DateParam.required("appliedOn", value))
                    .withMessage("Campo obrigatório: appliedOn");
        }

        @ParameterizedTest
        @ValueSource(strings = {"20/11/2026", "2026-02-30", "ontem", "2026-11-20T10:00"})
        void rejectsInvalidDate(String value) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> DateParam.required("appliedOn", value))
                    .withMessage("Data inválida: " + value + ". Use o formato AAAA-MM-DD");
        }
    }

    @Nested
    class Optional {

        @Test
        void keepsNullAsNull() {
            assertThat(DateParam.optional("statusUpdatedOn", null)).isNull();
        }

        @Test
        void parsesIsoDate() {
            assertThat(DateParam.optional("statusUpdatedOn", "2026-11-20")).isEqualTo(DAY);
        }

        // Em branco não é "manter": seria confirmar uma alteração que não aconteceu
        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void rejectsBlankDateNamingTheField(String value) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> DateParam.optional("statusUpdatedOn", value))
                    .withMessage("Campo obrigatório: statusUpdatedOn");
        }

        @Test
        void rejectsInvalidDate() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> DateParam.optional("statusUpdatedOn", "20/11/2026"))
                    .withMessage("Data inválida: 20/11/2026. Use o formato AAAA-MM-DD");
        }
    }

    // Para campos dentro de objetos, como a data de uma etapa
    @Nested
    class BlankAsNull {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        void treatsMissingOrBlankAsNoDate(String value) {
            assertThat(DateParam.blankAsNull(value)).isNull();
        }

        @Test
        void parsesIsoDate() {
            assertThat(DateParam.blankAsNull(" 2026-11-20 ")).isEqualTo(DAY);
        }

        @Test
        void rejectsInvalidDate() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> DateParam.blankAsNull("2026-02-30"))
                    .withMessage("Data inválida: 2026-02-30. Use o formato AAAA-MM-DD");
        }
    }
}
