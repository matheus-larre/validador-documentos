# Validador Inteligente de Documentos

Projeto do semestre 2026.2 — Residência Tecnológica (Porto Digital · CESAR School · mentoria Avanade).

Aplicação em **Java + Spring Boot** que recebe documentos digitais (PDF/imagem), extrai as informações com **OCR (Tesseract)** e **IA (Gemini)**, aplica regras de negócio e devolve se o documento foi **aprovado ou reprovado**, guardando o resultado para consulta.

> 🚧 Em desenvolvimento — o projeto evolui por etapas, com commits frequentes.

---

## 1. Entendimento do problema

| Item | Definição |
|---|---|
| **Problema** | A validação manual de documentos no cadastro de clientes é lenta, sujeita a erro e gera retrabalho e risco operacional. |
| **Usuário** | Analista de cadastro / backoffice que precisa conferir documentos enviados por clientes. |
| **Documentos** | MVP: **comprovante de residência**. Próxima etapa: **documento de identificação (RG/CNH)**. |
| **Dados extraídos** | Nome, CPF, endereço, CEP, cidade, estado e data de emissão. |
| **Regras** | Documento legível · nome identificado · endereço identificado · CEP válido · emissão há no máximo 90 dias. |
| **Aprovação** | Todos os dados obrigatórios extraídos e todas as regras atendidas. |
| **Rejeição** | Documento ilegível, dados obrigatórios ausentes ou emissão com mais de 90 dias. |
| **MVP** | Processar comprovante de residência com validação básica e consulta do resultado. |

## 2. Fluxo da solução

```
Upload (PDF/imagem) → OpenCV (tratamento) → Tesseract (OCR) → Gemini (texto → JSON)
      → Regras de negócio (Java) → PostgreSQL → Resultado (aprovado/reprovado)
```

## 3. Tecnologias

| Tecnologia | Papel |
|---|---|
| Java 17 + Spring Boot | Backend, regras de negócio e API REST |
| Thymeleaf | Telas de upload e de resultado |
| OpenCV | Pré-processamento da imagem (ruído, rotação, contraste, binarização) |
| Tesseract (Tess4J) | OCR — transforma a imagem em texto |
| Google Gemini | Interpreta o texto e devolve JSON estruturado |
| PostgreSQL + Spring Data JPA | Persistência de documentos e resultados |
| JUnit | Testes das regras de validação |
| Docker | Ambiente de execução (API + banco) |

## 4. Como rodar

**Pré-requisitos:** Java 17+ e o [Tesseract OCR](https://github.com/UB-Mannheim/tesseract/wiki) instalado com o idioma **Portuguese**.
O caminho padrão dos idiomas é `C:/Program Files/Tesseract-OCR/tessdata`. Para usar outro, defina a variável de ambiente `TESSDATA_PATH`.

```bash
# Windows (PowerShell)
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Acesse: http://localhost:8080

## 5. Roadmap

- [x] Definição do problema e escopo
- [x] Estrutura inicial do projeto (Spring Boot)
- [x] Tela e endpoint de upload
- [x] OCR com Tesseract
- [ ] Pré-processamento com OpenCV
- [ ] Extração de dados com IA (Gemini)
- [ ] Regras de validação + testes
- [ ] Persistência com PostgreSQL
- [ ] Tela de consulta de resultados
- [ ] Docker Compose
- [ ] Segundo tipo de documento (RG/CNH)
- [ ] Deploy

## Autor

Matheus Rodrigues Larré — [github.com/matheus-larre](https://github.com/matheus-larre)
