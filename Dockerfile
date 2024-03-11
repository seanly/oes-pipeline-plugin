FROM seanly/toolset:openjdk8u372 as build

COPY ./ /code
WORKDIR /code

RUN ./mvnw clean package -DskipTests

FROM seanly/scratch

COPY --from=build /code/target/oes-pipeline.hpi /target/