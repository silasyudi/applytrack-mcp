# applytrack-mcp

MCP server to track job applications by chatting with an AI assistant such as Claude. Tool descriptions and messages are in Brazilian Portuguese.

Servidor MCP para registrar e acompanhar candidaturas a vagas de emprego conversando com uma IA, como o
Claude. Ele guarda a vaga, os requisitos e as etapas de cada processo, e calcula o status a partir das etapas.

## Casos de uso

A IA consulta as candidaturas registradas e responde perguntas que exigem cruzar várias delas, sem planilha. Por exemplo:

**Registrar e atualizar**

- "Me candidatei nessa vaga pelo LinkedIn hoje" (com o anúncio colado). A IA extrai empresa, cargo, contrato, faixa
  salarial e requisitos
- "Marcaram a entrevista técnica da Acme para quinta às 14h"
- "Recebi um teste técnico da Beta para entregar até sexta"
- "Fiz o fit cultural hoje, ficaram de responder até segunda"
- "Passei na técnica da Acme", "Fui reprovado no teste da Beta", "Desisti da vaga da Gama"
- "Recebi uma proposta da Acme"

**Acompanhar**

- "Quantas entrevistas tenho marcadas esta semana?"
- "Há quanto tempo estou esperando feedback das entrevistas?"
- "Quais etapas estão esperando por mim?"
- "Quais empresas prometeram retorno e já passaram do prazo?"
- "Quais candidaturas estão sem resposta há mais de duas semanas?"
- "Me dá um resumo dos processos em andamento"

**Consultar e analisar**

- "Já me candidatei na Acme?" ou "Já me candidatei nessa vaga?" (com o link ou o anúncio)
- "Em quais vagas me candidatei este mês?"
- "Quais vagas pedem RabbitMQ? E quais têm Kafka só como desejável?"
- "Quais tecnologias mais aparecem nas vagas em que me candidatei?"
- "Quantas candidaturas fiz por plataforma, e em quais cheguei mais longe?"
- "Quais vagas são PJ e qual a faixa salarial de cada uma?"

**Preparar**

- "Tenho entrevista técnica na Acme amanhã. Revise comigo os requisitos e a descrição da vaga"
- "O que anotei sobre o processo da Beta?"
- "Quais requisitos obrigatórios aparecem nas vagas e eu ainda não domino?" (contando para a IA o que você já sabe)

## Modelo

Cada candidatura tem uma vaga e um processo.

- Vaga: empresa, cliente final, cargo, senioridade, contrato, faixa salarial, link, descrição e requisitos
- Processo: plataforma, data da candidatura, situação, observações e etapas

As etapas têm nome livre, datas opcionais e um destes estados.

| Estado | Significado |
|---|---|
| `SCHEDULED` | Marcada, ou teste recebido para fazer |
| `AWAITING_FEEDBACK` | Feita ou entregue, esperando retorno |
| `PASSED` | Aprovado |
| `FAILED` | Reprovado |
| `CANCELLED` | Cancelada |

O status, quem precisa agir e a data da última atividade são calculados pelas etapas. Por exemplo, uma entrevista
marcada gera `Próxima etapa: Entrevista técnica em 2026-12-03`.

A situação (`ACTIVE`, `OFFER`, `HIRED`, `REJECTED`, `WITHDRAWN` ou `CLOSED`) é sempre informada pelo usuário. Uma etapa
reprovada não muda a situação.

## Requisitos

- Java 25
- Docker
- Um cliente compatível com MCP, como Claude Desktop ou Claude Code

## Instalação

### Pelo release (recomendado)

Baixe o zip da versão mais recente em [Releases](https://github.com/silasyudi/applytrack-mcp/releases/latest) e
extraia numa pasta fixa. Ele traz o `applytrack-mcp.jar` e o `compose.yaml`.

### Pelo código-fonte

Indicado para quem quer modificar o projeto.

```shell
git clone https://github.com/silasyudi/applytrack-mcp.git
cd applytrack-mcp
./mvnw package -DskipTests
```

No Windows, use `.\mvnw.cmd package -DskipTests`. O JAR é gerado em `dist/applytrack-mcp.jar` e o `compose.yaml` fica
na raiz do projeto.

## Configuração

Os dois passos abaixo são necessários, qualquer que tenha sido a forma de instalação.

### Banco de dados

Na pasta do `compose.yaml`, suba o DynamoDB Local.

```shell
docker compose up -d
```

Ele roda na porta 8000, acessível só pela própria máquina, e volta a subir sempre que o Docker inicia.

### Cliente de IA

Abaixo está a configuração para o Claude. Em outros clientes compatíveis com MCP, consulte a documentação do cliente
para adicionar um servidor stdio que execute `java -jar` com o caminho do `applytrack-mcp.jar`.

#### Claude

O Claude inicia o servidor executando `java -jar` com o caminho do JAR. Por isso, informe o caminho absoluto do
`applytrack-mcp.jar`, já que o Claude não roda na pasta do projeto. Se `java -version` não mostrar a versão 25, troque
`java` pelo caminho completo do executável do Java 25.

##### Claude Desktop

Abra `Configurações > Aplicativo para Desktop > Extensões > Servidores MCP locais > Editar Configuração`. Se essa opção
não aparecer, ative antes o modo de desenvolvedor em `Ajuda > Solução de problemas > Ativar modo de desenvolvedor`.

O botão abre o `claude_desktop_config.json`. Prefira abrir por ele, porque no Windows a pasta muda conforme a forma de
instalação do Claude Desktop. Os locais mais comuns são estes:

- Windows, instalador do site: `%APPDATA%\Claude\claude_desktop_config.json`
- Windows, versão empacotada (Microsoft Store ou MSIX): `%LOCALAPPDATA%\Claude-Data\claude_<id>\claude_desktop_config.json`
- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`

Adicione o `applytrack` dentro de `mcpServers`, mantendo o que já existir no arquivo, e reinicie o Claude Desktop por
completo, inclusive o ícone da bandeja.

```json
{
  "mcpServers": {
    "applytrack": {
      "command": "java",
      "args": ["-jar", "C:\\caminho\\applytrack-mcp.jar"]
    }
  }
}
```

##### Claude Code

```shell
claude mcp add applytrack --scope user -- java -jar /caminho/applytrack-mcp.jar
```

## Tools

| Tool | Descrição |
|---|---|
| `register_job_application` | Registra uma candidatura |
| `search_job_applications` | Busca por empresa, cliente final, cargo, descrição ou requisito, ou lista todas |
| `get_job_application` | Retorna uma candidatura completa |
| `update_job_application` | Altera os campos informados |
| `add_job_application_step` | Adiciona uma etapa |
| `update_job_application_step` | Altera uma etapa pelo nome |
| `delete_job_application` | Remove uma candidatura |

Datas no formato `AAAA-MM-DD`. A data da candidatura e a conclusão de uma etapa não podem ser futuras.

## Dados

Os dados ficam no volume Docker `applytrack-mcp_dynamodb-data` e o log em `~/.applytrack-mcp/applytrack-mcp.log`.

Backup

```shell
docker compose stop
docker compose cp dynamodb:/data ./backup
docker compose start
```

Restauração

```shell
docker compose stop
docker compose cp ./backup/shared-local-instance.db dynamodb:/data/
docker compose start
```

Remoção de todos os dados

```shell
docker compose down -v
```

## Problemas comuns

- "Banco de dados indisponível": o Docker está fechado ou o contêiner parou. Rode `docker compose up -d` na pasta do
  `compose.yaml`, sem reiniciar o servidor.
- Tools não aparecem no Claude: confira o caminho do JAR e a versão do Java (`java -version`) e reinicie o cliente.
- `dist/applytrack-mcp.jar` não atualiza depois do build: o Windows trava o arquivo enquanto o servidor roda. Feche o cliente e gere o
  JAR de novo.

## Desenvolvimento

```shell
./mvnw test     # testes unitários
./mvnw verify   # testes unitários e de integração, com Docker
```

Os testes de integração usam um DynamoDB Local próprio (Testcontainers) e chamam o JAR por stdio.

Estrutura (arquitetura hexagonal)

- `domain`: modelo e regras, sem framework
- `application`: casos de uso e porta do repositório
- `adapter`: tools MCP (Spring AI) e DynamoDB (AWS SDK). As tools chamam o serviço direto, sem porta de entrada
- `config`: beans do Spring

## Licença

[MIT](LICENSE)
