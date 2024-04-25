# Development

## JUnit Testing

* [https://docs.spring.io/spring-boot/how-to/testing.html]
* [https://vaadin.com/docs/latest/testing]
* [https://java.testcontainers.org/modules/k3s/]
* [https://www.baeldung.com/spring-boot-testing]
* [https://assertj.github.io/doc/]
* [https://www.baeldung.com/mockito-series]
* [https://vaadin.com/docs/next/flow/testing/selenium]


## Vaadin Development

* [https://vaadin.com/docs/latest/overview]
* [https://vaadin.com/directory/] # Vaadin Directory
* [https://vaadin.com/directory/component/xterm-console-addon]

## Kubernetes

* [https://github.com/kubernetes-client/java]
* [https://kubernetes.io/docs/home/]

## Spring Security

* [https://docs.spring.io/spring-boot/docs/3.2.3/reference/htmlsingle/index.html#web.security]
* [https://spring.io/guides/gs/securing-web/]
* [https://spring.io/guides/tutorials/spring-boot-oauth2/]

## Miscellaneous

* [https://github.com/walokra/markdown-page-generator-plugin]
* [lombok](https://projectlombok.org/features/all)
* [https://www.baeldung.com/spring-boot]

## Github Actions

* [https://docs.github.com/en/actions]
* [https://github.com/actions/runner-images/blob/main/images/macos/macos-14-arm64-Readme.md]

# Sample kubernetes pods

* `kubectl create deployment --image=chentex/random-logger:latest logger` - [https://github.com/Aiven-Labs/k8s-logging-demo]
* `kubectl run test --image mhus/playground-pod --rm -it`
* `kubectl create job throw-dice-job --image=kodekloud/throw-dice`
* [https://github.com/PSanetra/demo-pod] - `kubectl run --image psanetra/demo-pod demo-pod --port 8080 --expose`

# Update dependencies

```bash
mvn versions:display-dependency-updates
mvn versions:display-plugin-updates

mvn versions:use-next-releases
```

# Recreate custom.css in kt2l-core

```bash
mvn clean package -Pproduction -Dvaadin.force.production.build=true
```

and rerun the application.

# Local test of deb launcher script

* Compile the project before with `mvn clean install -Pproduction -Dvaadin.force.production.build=true`
* Start in the project directory if `kt2l` and run the following commands:

```bash
# Start docker container with java and ubuntu environment
docker run --rm -it --entrypoint=bash -v $(pwd):/root/kt2l --platform=linux/amd64 eclipse-temurin:21-amd64
# install packages
apt update
apt install -y fakeroot
# start launch creator for desktop 
cd /root/kt2l/
./kt2l-desktop/launcher/create-deb-amd64.sh

# install aws-cli
apt install -y awscli git
# export aws environment
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export AWS_DEFAULT_REGION=eu-central-1
# execute deploy script
./kt2l-desktop/launcher/deploy-deb-amd64.sh
```

