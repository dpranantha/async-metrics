# Getting Started

### Description

This project provides metrics libraries for Spring Reactor and coroutines in the package
`com.dpranantha.asyncmetrics.util`. They are unit tested and are used in real production system as a library.

REST API application is created as an example.

Commands:
1. Running example: `./mvnw spring-boot:run` or via IDE by running `AsyncMetricsApplication.kt` class.
2. Build and run unit-tests: `./mvnw clean install`. Integration test is not provided.

Urls:
1. Swagger UI: http://localhost:8080/internal
2. Actuator: http://localhost:8080/actuator
3. Prometheus: http://localhost:8080/actuator/prometheus

### Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.4.2/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/2.4.2/maven-plugin/reference/html/#build-image)
* [Coroutines section of the Spring Framework Documentation](https://docs.spring.io/spring/docs/5.3.3/spring-framework-reference/languages.html#coroutines)
* [Resilience4J](https://cloud.spring.io/spring-cloud-static/spring-cloud-circuitbreaker/current/reference/html)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/2.4.2/reference/htmlsingle/#production-ready)
* [Spring Configuration Processor](https://docs.spring.io/spring-boot/docs/2.4.2/reference/htmlsingle/#configuration-metadata-annotation-processor)
* [Prometheus](https://docs.spring.io/spring-boot/docs/2.4.2/reference/html/production-ready-features.html#production-ready-metrics-export-prometheus)

### Guides

The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)

