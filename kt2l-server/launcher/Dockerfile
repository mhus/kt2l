#
# kt2l-server - kt2l as server
# Copyright © 2024 Mike Hummel (mh@mhus.de)
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#

FROM eclipse-temurin:21.0.4_7-jdk

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
    rm /app/kt2l-server.zip && \
    chown -R $UID /app/kt2l-server/config && \
    chown -R $UID /app/kt2l-server/var

EXPOSE 8080

USER $UID
ENTRYPOINT ["/app/kt2l-server/bin/run.sh"]
