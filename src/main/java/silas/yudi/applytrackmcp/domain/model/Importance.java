package silas.yudi.applytrackmcp.domain.model;

/** Quanto a vaga exige um requisito. */
public enum Importance {
    REQUIRED,
    /** Desejável, diferencial ou "plus". */
    DESIRABLE,
    /** O texto não diz. */
    UNSPECIFIED
}
