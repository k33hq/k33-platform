FROM eclipse-temurin:25.0.2_10-jre-alpine-3.23
RUN apk add --no-cache netcat-openbsd
EXPOSE 8080
RUN mkdir /app
COPY ./build/install/acceptance-tests/ /acceptance-tests/
COPY ./scripts/* /acceptance-tests/bin/
WORKDIR /acceptance-tests/bin
CMD ["./wait.sh"]