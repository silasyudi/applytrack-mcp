package silas.yudi.applytrackmcp.domain.model;

/** Onde uma etapa está. As duas primeiras ainda esperam algo; as outras são finais. */
public enum StepState {
    /** Marcada (entrevista) ou recebida e a fazer (teste). */
    SCHEDULED,
    /** Feita ou entregue, esperando retorno da empresa. */
    AWAITING_FEEDBACK,
    /** Aprovado na etapa. */
    PASSED,
    /** Reprovado na etapa. */
    FAILED,
    /** Cancelada pela empresa ou pelo usuário. */
    CANCELLED
}
