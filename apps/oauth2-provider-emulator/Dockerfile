FROM eclipse-temurin:21.0.5_11-jre-alpine
EXPOSE 8080
RUN mkdir /app
COPY ./build/install/oauth2-provider-emulator/ /app/
WORKDIR /app/bin
CMD ["./oauth2-provider-emulator"]