FROM openjdk:15-buster as build

ARG SBT_VERSION=1.4.5

# Install sbt
RUN \
  mkdir /working/ && \
  cd /working/ && \
  curl -L -o sbt-$SBT_VERSION.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get install sbt && \
  cd && \
  rm -r /working/ && \
  sbt sbtVersion

WORKDIR /app
COPY . .
RUN sbt dist

FROM openjdk:15-buster as runtime

COPY --from=build /app/target/universal/pop-1.0-SNAPSHOT.zip .
RUN unzip pop-1.0-SNAPSHOT.zip
WORKDIR pop-1.0-SNAPSHOT

ENV PLAY_HTTP_PORT=8080
EXPOSE 8080

ENTRYPOINT ["bin/pop", "-Dplay.http.secret.key='verysecurestringbecausenosessionsorencryption'"]