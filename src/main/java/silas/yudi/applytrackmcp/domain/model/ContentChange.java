package silas.yudi.applytrackmcp.domain.model;

import java.util.List;

/**
 * Mudança do conteúdo da vaga: os requisitos vêm da descrição, então descrição nova exige requisitos.
 * Requisitos sozinhos são aceitos (correção da extração); a descrição nula mantém a atual.
 */
public record ContentChange(String description, List<Requirement> requirements) {

    public ContentChange {
        if (requirements == null) {
            throw new IllegalArgumentException(
                    "Para informar a descrição, envie também os requisitos (lista vazia se a vaga não citar nenhum)");
        }
    }

    /** Nulo quando nada do conteúdo foi enviado. Descrição sem requisitos é erro. */
    public static ContentChange of(String description, List<Requirement> requirements) {
        if (description == null && requirements == null) {
            return null;
        }
        return new ContentChange(description, requirements);
    }
}
