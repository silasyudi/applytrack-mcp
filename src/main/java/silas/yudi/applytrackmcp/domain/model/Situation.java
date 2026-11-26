package silas.yudi.applytrackmcp.domain.model;

/** Informada pelo usuário: não muda sozinha com as etapas. Não valida transições. */
public enum Situation {
    /** Processo em andamento. */
    ACTIVE,
    /** Recebeu proposta. */
    OFFER,
    /** Aceitou e foi contratado. */
    HIRED,
    /** Foi reprovado. */
    REJECTED,
    /** Desistiu. */
    WITHDRAWN,
    /** Vaga fechada ou sem resposta definitiva. */
    CLOSED
}
