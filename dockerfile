FROM openjdk:28-ea-slim

WORKDIR /app

COPY ./busca-facil /app
COPY . .

RUN javac Main.java

CMD ["java", "Main"]
