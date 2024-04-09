FROM eclipse-temurin:21-jdk

ENV UID=1001
ENV GCLOUD_VERSION=471.0.0

RUN apt-get update && apt-get install -y unzip ca-certificates awscli sudo \
    && useradd -u $UID -m user
#    && cd / && curl -O https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-cli-$GCLOUD_VERSION-linux-x86_64.tar.gz \
#    && tar -xzf google-cloud-cli-${GCLOUD_VERSION}-linux-x86_64.tar.gz \
#    && rm google-cloud-cli-${GCLOUD_VERSION}-linux-x86_64.tar.gz \
#    && sudo -u user /google-cloud-sdk/install.sh --quiet \
#    && sudo -u user /google-cloud-sdk/bin/gcloud init

RUN mkdir -p /app

COPY target/kt2l-server.zip /app/kt2l-server.zip
RUN unzip /app/kt2l-server.zip -d /app/ && \
    rm /app/kt2l-server.zip

EXPOSE 8080

USER $UID
ENTRYPOINT ["/app/kt2l-server/run.sh"]
