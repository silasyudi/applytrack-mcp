package silas.yudi.applytrackmcp.adapter.in.mcp;

/**
 * Textos que a IA lê para escolher a tool e preencher os parâmetros.
 * Ficam aqui para as assinaturas das tools caberem na tela; textos comuns a cadastro e edição aparecem uma vez.
 */
final class JobApplicationToolDescriptions {

    private JobApplicationToolDescriptions() {
    }

    // ---------- comuns

    static final String ID = "Id da candidatura, obtido na busca ou no cadastro";

    private static final String REQUIREMENTS = "Requisitos citados na descrição: tecnologias, idiomas, "
            + "certificações. Extraia só o que está escrito, sem inventar";

    private static final String STATUS = "Status atual em texto livre, como o usuário descreveria: "
            + "\"Entrevista técnica realizada, aguardando feedback\"";

    private static final String SITUATIONS = "ACTIVE (em andamento), OFFER (proposta), HIRED (contratado), "
            + "REJECTED (reprovado), WITHDRAWN (desistiu) ou CLOSED (vaga fechada)";

    private static final String STEPS = "Etapas do processo, em ordem: candidatura, fit cultural, entrevista "
            + "técnica etc.";

    private static final String DATE_FORMAT = "no formato AAAA-MM-DD";

    // ---------- tools

    static final String REGISTER = "Registra uma candidatura que o usuário fez a uma vaga de emprego e devolve o id. "
            + "Antes, use search_job_applications com o nome da empresa: se houver candidatura parecida "
            + "(mesma empresa e cargo, ou descrição muito semelhante), mostre-a ao usuário e pergunte se é "
            + "outra vaga. Cadastros repetidos são permitidos: a decisão é do usuário. "
            + "Preencha os campos da vaga com o que está no anúncio, sem inventar; "
            + "se faltar algum campo obrigatório, pergunte ao usuário.";

    static final String SEARCH = "Busca candidaturas já registradas, por exemplo para responder \"já me candidatei "
            + "nesta empresa?\" ou \"quais vagas pedem Kafka?\". Procura o termo como trecho na empresa, no "
            + "cliente final, no cargo, na descrição da vaga e nos nomes dos requisitos, ignorando acentos e "
            + "maiúsculas; por ser trecho, \"java\" também encontra \"JavaScript\". Sem termo, lista todas. "
            + "Resultados da candidatura mais recente para a mais antiga. "
            + "Cada resultado traz id, vaga (sem descrição e sem faixa salarial), requisitos e status; "
            + "para a descrição, a faixa salarial e as etapas, use get_job_application.";

    static final String GET = "Traz uma candidatura completa: vaga com descrição e faixa salarial, e processo com "
            + "etapas. Use quando precisar dos detalhes; para listar ou saber se uma vaga existe, "
            + "search_job_applications basta.";

    static final String UPDATE = "Altera uma candidatura já registrada e devolve o id. Antes, use "
            + "search_job_applications para obter o id; se mais de uma servir, pergunte ao usuário qual é. Envie "
            + "só os campos que ele pediu para mudar: os omitidos ficam como estão. Exceções, porque alguns campos "
            + "andam juntos: descrição nova exige requirements (lista vazia se a vaga não citar nenhum); data do "
            + "status e situação exigem status (reenvie o atual, se ele não mudou).";

    static final String DELETE = "Remove definitivamente uma candidatura; não dá para desfazer. Antes, use "
            + "search_job_applications para obter o id, mostre ao usuário a empresa, o cargo e a data, e só "
            + "remova depois que ele confirmar. Para corrigir dados, use update_job_application.";

    // ---------- parâmetros do cadastro

    static final class Register {

        private Register() {
        }

        static final String COMPANY = "Nome da empresa que anunciou a vaga";
        static final String FINAL_CLIENT = "Empresa onde se trabalha de fato, quando a vaga é de consultoria ou "
                + "alocação. Omita se a vaga não citar";
        static final String JOB_TITLE = "Nome do cargo, como no anúncio";
        static final String SENIORITY = "Senioridade como está na vaga: Júnior, Pleno, Sênior, Especialista. "
                + "Omita se a vaga não citar";
        static final String CONTRACT_TYPE = "Tipo de contrato como está na vaga: CLT, PJ, Cooperado. "
                + "Obrigatório quando houver faixa salarial";
        static final String SALARY_RANGE = "Faixa salarial anunciada, como está no texto (ex.: R$ 10.000 a "
                + "R$ 12.000). Exige contractType. Omita se a vaga não citar";
        static final String LINK = "Link da vaga, se houver";
        static final String DESCRIPTION = "Descrição completa da vaga, sem resumir";
        static final String REQUIREMENTS = JobApplicationToolDescriptions.REQUIREMENTS;
        static final String PLATFORM = "Por onde se candidatou: Gupy, LinkedIn, e-mail, WhatsApp etc.";
        static final String APPLIED_ON = "Data em que o usuário se candidatou, " + DATE_FORMAT
                + ". Se ele não disse quando, pergunte";
        static final String STATUS = JobApplicationToolDescriptions.STATUS
                + ". Omita no cadastro inicial: o padrão é \"Candidatura enviada\"";
        static final String STATUS_UPDATED_ON = "Data do status, " + DATE_FORMAT
                + ". Exige status. Omita para usar a data da candidatura";
        static final String SITUATION = SITUATIONS + ". Exige status. Omita para ACTIVE";
        static final String STEPS = JobApplicationToolDescriptions.STEPS;
    }

    // ---------- parâmetro da busca

    static final class Search {

        private Search() {
        }

        static final String TERM = "Um único trecho a procurar na empresa, no cliente final, no cargo, na "
                + "descrição ou nos nomes dos requisitos. Para várias tecnologias, faça uma busca para cada";
    }

    // ---------- parâmetros da edição

    static final class Update {

        private Update() {
        }

        static final String COMPANY = "Novo nome da empresa";
        static final String FINAL_CLIENT = "Novo cliente final; texto vazio remove";
        static final String JOB_TITLE = "Novo nome do cargo";
        static final String SENIORITY = "Nova senioridade; texto vazio remove";
        static final String CONTRACT_TYPE = "Novo tipo de contrato; texto vazio remove (não é possível manter "
                + "faixa salarial sem contrato)";
        static final String SALARY_RANGE = "Nova faixa salarial; exige contrato na candidatura (o atual ou um "
                + "novo); texto vazio remove";
        static final String LINK = "Novo link; texto vazio remove";
        static final String DESCRIPTION = "Nova descrição completa da vaga, sem resumir. Exige requirements";
        static final String REQUIREMENTS = JobApplicationToolDescriptions.REQUIREMENTS
                + ". Substitui a lista inteira; lista vazia remove todos. Pode ser enviada sozinha, para corrigir "
                + "a lista";
        static final String PLATFORM = "Nova plataforma";
        static final String APPLIED_ON = "Nova data da candidatura, " + DATE_FORMAT;
        static final String STATUS = JobApplicationToolDescriptions.STATUS
                + ". Ao mudar, a data do status passa a ser hoje, se não for informada";
        static final String STATUS_UPDATED_ON = "Data do status, " + DATE_FORMAT
                + ". Exige status. Informe quando a mudança não foi hoje";
        static final String SITUATION = SITUATIONS
                + ". Exige status. Ao mudar, a data do status passa a ser hoje, se não for informada";
        static final String STEPS = JobApplicationToolDescriptions.STEPS
                + " Substitui a lista inteira; lista vazia remove todas";
    }
}
