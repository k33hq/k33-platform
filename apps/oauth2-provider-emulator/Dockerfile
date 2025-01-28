FROM eclipse-temurin:21.0.6_7-jre-alpine-3.21
EXPOSE 8080
RUN mkdir /app
COPY ./build/install/oauth2-provider-emulator/ /app/
WORKDIR /app/bin
CMD ["./oauth2-provider-emulator"]