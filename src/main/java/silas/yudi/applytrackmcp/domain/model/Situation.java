package silas.yudi.applytrackmcp.domain.model;

/** Rótulo fechado ao lado do status livre, para filtrar com segurança. Não valida transições. */
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
