# Pessoas API

API REST para cadastro, consulta e gerenciamento de pessoas, desenvolvida em Spring Boot 3.5.0, com integração a MySQL, Redis, mensageria via Redis Pub/Sub e consulta automática de cidade/estado pelo CEP usando ViaCEP.

Objetivo de estudar Java 21 e recursos adicionais para desenvolvimento back-end focado em performance.

---

## Funcionalidades

- **CRUD completo de pessoas**: cadastro, consulta, atualização e remoção.
- **Validações**: nome, CPF e CEP obrigatórios; regras de formato e tamanho.
- **Cache Redis**: operações de listagem cacheadas, com expiração configurável.
- **Mensageria**: ao criar uma pessoa, envia mensagem para fila Redis com ID e CEP.
- **Integração ViaCEP**: serviço consumidor busca cidade e estado automaticamente pelo CEP e atualiza o cadastro.
- **Documentação automática**: Swagger/OpenAPI disponível em `/swagger-ui.html`.

---

## Tecnologias e Integrações

- **Java 21**
- **Spring Boot 3.5.0**
- **MySQL** (armazenamento relacional)
- **JDBI 3.43.0** (persistência de dados via SQL via notacao)
- **Liquibase** (versionamento e migração automática do banco de dados, scripts em `src/main/resources/db/changelog/`)
- **Redis** (cache e Pub/Sub)
- **ViaCEP** (consulta de endereço por CEP)
- **Swagger/OpenAPI** (`springdoc-openapi`)
- **Docker Compose** (ambiente local com containers)
- **Maven Wrapper** (execução portátil)
- **Jakarta Validation** (validação de dados de entrada)

---

## Endpoints REST

- `POST /pessoas`  
  Cria uma nova pessoa.  
  **Body:**  
  ```json
  {
    "nome": "João da Silva",
    "cpf": "12345678901",
    "cep": "01001000"
  }
  ```

- `GET /pessoas`  
  Lista todas as pessoas.

- `GET /pessoas/{id}`  
  Busca pessoa por ID.

- `GET /pessoas/cep/{cep}`  
  Lista pessoas por CEP (com cache Redis por CEP).

- `PUT /pessoas/{id}`  
  Atualiza pessoa.

- `DELETE /pessoas/{id}`  
  Remove pessoa.

---

## Como executar localmente

### 1. Pré-requisitos

- [Docker](https://www.docker.com/products/docker-desktop)
- [Java 21+](https://adoptium.net/) (apenas para execução local sem Docker)
- [Maven](https://maven.apache.org/download.cgi) (apenas para execução local sem Docker)
- [Git](https://github.com/marcosbsdev/pessoas/tree/main) (para clonar o projeto)

---

### 2. Subindo bancos e cache com Docker Compose

Na raiz do projeto, execute:

```sh
docker compose up mysql redis adminer
```

- MySQL estará disponível em `localhost:3306`
- Redis em `localhost:6379`
- Adminer (client web para banco) em [http://localhost:8081](http://localhost:8081)
- As credenciais do Adminer
  - Servidor `mysql`
  - Usuario `user`
  - Senha `1234`
  - Base de Dados `db_pessoas`
---

### 3. Executando a aplicação via Maven Wrapper

Com os serviços acima rodando, execute apenas o comando abaixo:
```sh
./mvnw spring-boot:run
```
Se quiser apenas limpar as dependências
```sh
./mvnw clean
```
Se quiser apenas instalar as dependências
```sh
./mvnw install
```

Acesse a API em [http://localhost:8080](http://localhost:8080)  
Swagger: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

### 4. Executando tudo via Docker

Se quiser rodar tudo em containers (app, banco, cache):

```sh
docker compose up --build
```
- Acesse a API em [http://localhost:8080](http://localhost:8080)  
- Swagger: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- Adminer (client web para banco) em [http://localhost:8081](http://localhost:8081)

A aplicação, MySQL, Redis e Adminer subirão juntos.

---

## Configurações

- As configurações de banco (mysql), Redis Cache estão em `src/main/resources/application.yaml`.
- O tempo de expiração do cache pode ser ajustado nesse arquivo.

---

## Observações

- O projeto utiliza validação de dados com mensagens amigáveis.
- O cache é atualizado automaticamente ao criar/alterar pessoas.
- O serviço consumidor de fila busca cidade/estado via ViaCEP e atualiza o cadastro.
- O Adminer pode ser usado para visualizar e manipular o banco de dados MySQL via web.
- Caso não existir a tabela pessoa, o plugin liquibase irá criar automaticamente.

---

## Contribuição

Pull requests são bem-vindos!  
Para sugestões ou dúvidas, abra uma issue.

---