FROM eclipse-temurin:21.0.3_9-jre-alpine
EXPOSE 8080
RUN mkdir /app
COPY ./build/install/k33-backend/ /app/
WORKDIR /app/bin
CMD ["./k33-backend"]