FROM eclipse-temurin:21.0.3_9-jre-alpine
RUN apk add --no-cache netcat-openbsd
EXPOSE 8080
RUN mkdir /app
COPY ./build/install/acceptance-tests/ /acceptance-tests/
COPY ./scripts/* /acceptance-tests/bin/
WORKDIR /acceptance-tests/bin
CMD ["./wait.sh"]