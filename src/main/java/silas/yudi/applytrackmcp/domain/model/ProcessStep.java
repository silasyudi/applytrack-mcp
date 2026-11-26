package silas.yudi.applytrackmcp.domain.model;

import java.time.LocalDate;

import static silas.yudi.applytrackmcp.domain.model.Fields.optional;
import static silas.yudi.applytrackmcp.domain.model.Fields.required;

/** Etapa do processo. Ex.: Entrevista técnica, 2026-12-03, feita, "Perguntaram sobre Kafka". A data pode ser futura. */
public record ProcessStep(String name, LocalDate date, boolean done, String notes) {

    public ProcessStep {
        name = required("step.name", name);
        notes = optional(notes);
    }
}
