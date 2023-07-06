FROM eclipse-temurin:20.0.1_9-jre
EXPOSE 8080:8080
RUN mkdir /app
COPY ./build/install/k33-backend/ /app/
WORKDIR /app/bin
CMD ["./k33-backend"]