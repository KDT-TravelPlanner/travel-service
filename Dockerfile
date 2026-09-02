FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S -g 10001 spring \
    && adduser -S -D -H -u 10001 -G spring spring \
    && mkdir -p /var/log/travel-planner \
    && chown -R spring:spring /var/log/travel-planner

COPY --chown=spring:spring build/libs/app.jar /app/app.jar

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
