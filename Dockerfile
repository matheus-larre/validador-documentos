# ===== Estágio 1: build (Maven + JDK 17) =====
# Compila, roda os testes e gera o .jar. Nada deste estágio vai para a imagem final.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copia primeiro só o pom.xml e baixa as dependências: o Docker guarda essa camada em cache,
# então mudar só o código não baixa tudo de novo
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Os testes rodam no build: imagem com teste quebrado não é gerada
RUN mvn -B package

# ===== Estágio 2: execução (só JRE 17 + Tesseract) =====
FROM eclipse-temurin:17-jre

# Tesseract com o idioma português (a Tess4J usa a libtesseract instalada aqui)
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-por \
    && rm -rf /var/lib/apt/lists/*

# Caminho real dos idiomas nesta imagem (conferido com: dpkg -L tesseract-ocr-por)
ENV TESSDATA_PATH=/usr/share/tesseract-ocr/5/tessdata

# Usuário sem privilégios de administrador: se a aplicação for comprometida, o estrago é menor
RUN useradd --system --create-home --uid 10001 validador
WORKDIR /app
RUN mkdir uploads && chown validador:validador uploads

COPY --from=build /build/target/validador-documentos-*.jar app.jar

USER validador
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
