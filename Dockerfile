FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY *.java ./
RUN javac -encoding UTF-8 *.java

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/*.class ./
COPY index.html style.css script.js ./
ENV PORT=10000
EXPOSE 10000
USER 10001
CMD ["java", "Main", "--web"]
