FROM eclipse-temurin:25.0.2_10-jre
EXPOSE 8080
RUN mkdir /app
COPY ./build/install/k33-backend/ /app/
WORKDIR /app/bin
CMD ["./k33-backend"]