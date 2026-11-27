package silas.yudi.applytrackmcp.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Campos a alterar: primeiro os da vaga, depois os do processo.
 * Nulo mantém o valor atual; texto em branco remove os campos opcionais
 * (cliente final, senioridade, contrato, faixa salarial, local e link). O modelo de trabalho só é substituído.
 * Descrição e requisitos andam juntos e vêm agrupados no conteúdo. Observação em branco remove a observação.
 * Steps, quando enviada, substitui a lista inteira; lista vazia remove todas.
 * Para montar, prefira o {@link #builder()}: cada valor fica ao lado do nome do campo.
 */
public record JobApplicationChanges(String company,
                                    String finalClient,
                                    String jobTitle,
                                    String seniority,
                                    String contractType,
                                    String salaryRange,
                                    WorkModel workModel,
                                    String location,
                                    String link,
                                    ContentChange content,
                                    String platform,
                                    LocalDate appliedOn,
                                    Situation situation,
                                    String notes,
                                    List<ProcessStep> steps) {

    /** Começa sem nenhuma alteração: só o que for informado muda. */
    public static Builder builder() {
        return new Builder();
    }

    boolean isEmpty() {
        return Stream.of(company,
                finalClient,
                jobTitle,
                seniority,
                contractType,
                salaryRange,
                workModel,
                location,
                link,
                content,
                platform,
                appliedOn,
                situation,
                notes,
                steps
        ).allMatch(Objects::isNull);
    }

    public static final class Builder {

        private String company;
        private String finalClient;
        private String jobTitle;
        private String seniority;
        private String contractType;
        private String salaryRange;
        private WorkModel workModel;
        private String location;
        private String link;
        private ContentChange content;
        private String platform;
        private LocalDate appliedOn;
        private Situation situation;
        private String notes;
        private List<ProcessStep> steps;

        private Builder() {
        }

        public Builder company(String company) {
            this.company = company;
            return this;
        }

        public Builder finalClient(String finalClient) {
            this.finalClient = finalClient;
            return this;
        }

        public Builder jobTitle(String jobTitle) {
            this.jobTitle = jobTitle;
            return this;
        }

        public Builder seniority(String seniority) {
            this.seniority = seniority;
            return this;
        }

        public Builder contractType(String contractType) {
            this.contractType = contractType;
            return this;
        }

        public Builder salaryRange(String salaryRange) {
            this.salaryRange = salaryRange;
            return this;
        }

        public Builder workModel(WorkModel workModel) {
            this.workModel = workModel;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder link(String link) {
            this.link = link;
            return this;
        }

        public Builder content(ContentChange content) {
            this.content = content;
            return this;
        }

        public Builder platform(String platform) {
            this.platform = platform;
            return this;
        }

        public Builder appliedOn(LocalDate appliedOn) {
            this.appliedOn = appliedOn;
            return this;
        }

        public Builder situation(Situation situation) {
            this.situation = situation;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder steps(List<ProcessStep> steps) {
            this.steps = steps;
            return this;
        }

        public JobApplicationChanges build() {
            return new JobApplicationChanges(
                    company,
                    finalClient,
                    jobTitle,
                    seniority,
                    contractType,
                    salaryRange,
                    workModel,
                    location,
                    link,
                    content,
                    platform,
                    appliedOn,
                    situation,
                    notes,
                    steps
            );
        }
    }
}
