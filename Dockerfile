FROM gradle:8.7-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle installDist --no-daemon
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /home/gradle/src/app/build/install/app /app
EXPOSE 7070
CMD ["./bin/app"]