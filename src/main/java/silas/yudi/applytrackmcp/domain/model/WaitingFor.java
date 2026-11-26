package silas.yudi.applytrackmcp.domain.model;

/** Quem precisa agir agora no processo. Calculado a partir das etapas e da situação. */
public enum WaitingFor {
    /** O usuário: há etapa marcada, teste a entregar ou proposta a responder. */
    ME,
    /** A empresa: retorno de etapa feita, ou contato depois da candidatura. */
    COMPANY,
    /** Ninguém: processo encerrado. */
    NOTHING
}
