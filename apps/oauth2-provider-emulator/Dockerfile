FROM eclipse-temurin:25.0.2_10-jre-alpine-3.23
EXPOSE 8080
RUN mkdir /app
COPY ./build/install/oauth2-provider-emulator/ /app/
WORKDIR /app/bin
CMD ["./oauth2-provider-emulator"]