package silas.yudi.applytrackmcp.domain.model;

import java.time.LocalDate;

import static silas.yudi.applytrackmcp.domain.model.Fields.required;

/**
 * Mudança de status: data e situação só existem junto com o status.
 * On nulo significa "hoje", se o status ou a situação mudarem.
 */
public record StatusChange(String status, LocalDate on, Situation situation) {

    static final String DATE_WITHOUT_STATUS = "Para informar a data do status, envie também o status";
    static final String SITUATION_WITHOUT_STATUS = "Para informar a situação, envie também o status";

    public StatusChange {
        status = required("status", status);
    }

    /** Nulo quando nada do status foi enviado. Data ou situação sem status é erro. */
    public static StatusChange of(String status, LocalDate on, Situation situation) {
        if (status == null) {
            requireStatusFor(on, situation);
            return null;
        }
        return new StatusChange(status, on, situation);
    }

    static void requireStatusFor(LocalDate on, Situation situation) {
        if (on != null) {
            throw new IllegalArgumentException(DATE_WITHOUT_STATUS);
        }

        if (situation != null) {
            throw new IllegalArgumentException(SITUATION_WITHOUT_STATUS);
        }
    }
}
