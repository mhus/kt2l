# Development

## JUnit Testing / Integration Testing

* [https://docs.spring.io/spring-boot/how-to/testing.html]
* [https://vaadin.com/docs/latest/testing]
* [https://java.testcontainers.org/modules/k3s/]
* [https://www.baeldung.com/spring-boot-testing]
* [https://assertj.github.io/doc/]
* [https://www.baeldung.com/mockito-series]
* [https://vaadin.com/docs/next/flow/testing/selenium]
* [https://asterix.com/de/die-charaktere/] [https://asterix.fandom.com/wiki/Main_Page] [https://asterix.fandom.com/wiki/Indomitable_village]

## Vaadin Development

* [https://vaadin.com/docs/latest/overview]
* [https://vaadin.com/directory/] # Vaadin Directory
* [https://vaadin.com/directory/component/xterm-console-addon]
* [https://vaadin.com/directory/component/idle-notification]
* [https://vaadin.com/directory/component/vis-network-vaadin]
* [https://visjs.github.io/vis-network/docs/network/]
* [https://incubator.app.fi/togglebutton-demo/togglebutton]
* Google SSO [https://vaadin.com/blog/oauth-2-and-google-sign-in-for-a-vaadin-application]

## Kubernetes

* [https://github.com/kubernetes-client/java]
* [https://kubernetes.io/docs/home/]
* How To Create Helm Charts [https://devopscube.com/create-helm-chart/#Deploy_the_Helm_Chart]

## Spring Security

* [https://docs.spring.io/spring-boot/docs/3.2.3/reference/htmlsingle/index.html#web.security]
* [https://spring.io/guides/gs/securing-web/]
* [https://spring.io/guides/tutorials/spring-boot-oauth2/]

## Miscellaneous

* [https://github.com/walokra/markdown-page-generator-plugin]
* [lombok](https://projectlombok.org/features/all)
* [https://www.baeldung.com/spring-boot]
* [https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-s3.html]

## Logos

* Helm Logo: [https://www.svgrepo.com/svg/306187/helm]() Logo License
* Google Logo: [https://www.svgrepo.com/svg/303108/google-icon-logo]()  Logo License
* Github Logo [https://www.svgrepo.com/svg/303615/github-icon-1-logo]() Logo License
* Facebook Logo: [https://www.svgrepo.com/svg/303113/facebook-icon-logo]() Logo License
* Apple Logo: [https://www.svgrepo.com/svg/303110/apple-black-logo]() Logo License
* Slack Logo: [https://www.svgrepo.com/svg/303320/slack-new-logo-logo]() Logo License
* Microsoft Logo: [https://www.svgrepo.com/svg/448239/microsoft]() Logo License
* SAML Logo: [https://www.svgrepo.com/svg/448246/saml]() Logo License
* Flux Logo: [https://www.svgrepo.com/svg/367014/flux]() PD License

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

# Recreate vaadin frontend and custom.css in kt2l-core

```bash
mvn clean vaadin:clean-frontend  -Pintegration_tests
rm package-lock.json package.json tsconfig.json types.d.ts vite.config.ts vite.generated.ts
rm kt2l-core/package-lock.json kt2l-core/package.json kt2l-core/tsconfig.json kt2l-core/types.d.ts kt2l-core/vite.config.ts kt2l-core/vite.generated.ts
rm kt2l-server/package-lock.json kt2l-server/package.json kt2l-server/tsconfig.json kt2l-server/types.d.ts kt2l-server/vite.config.ts kt2l-server/vite.generated.ts
rm kt2l-desktop/package-lock.json kt2l-desktop/package.json kt2l-desktop/tsconfig.json kt2l-desktop/types.d.ts kt2l-desktop/vite.config.ts kt2l-desktop/vite.generated.ts
rm kt2l-test/package-lock.json kt2l-test/package.json kt2l-test/tsconfig.json kt2l-test/types.d.ts kt2l-test/vite.config.ts kt2l-test/vite.generated.ts
mvn -DskipTests -Pintegration_tests install
mvn vaadin:build-frontend -Pintegration_tests

mvn clean package -Pproduction -Pintegration_tests -Dvaadin.force.production.build=true -DskipTests
```

and rerun the application.

# Local test of deb launcher script (macosx)

* Maybe compile `mhus-commons` before with `mvn clean install`
* Use the prepare script `./kt2l-desktop/launcher/prepare.sh`
* Compile the project `mvn clean install -Pproduction
* You can start the java app now directly with 
  `java -XstartOnFirstThread -Dspring.profiles.active=prod -jar ./kt2l-desktop/target/kt2l-desktop-macosx-aarch64-0.0.1-SNAPSHOT.jar`
* Run launcher create script: `./kt2l-desktop/launcher/create-macosx-aarch64.sh`
* Mount the dmg file: `hdiutil attach ./kt2l-desktop/target/launcher/KT2L.dmg`
* Run the created app: `/Volumes/KT2L/KT2L.app/Contents/MacOS/KT2L` in terminal
* Unmount the dmg file: `hdiutil detach /Volumes/KT2L`

# Local test of deb launcher script (linux)

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

# Local run in Kubernetes cluster (mac)

Need to build the docker container and push it to the docker registry before.

```bash
# Start colima
colima start --kubernetes

# deploy the application
kubectl apply -f kt2l-server/launcher/k8s/deployment.yaml

# expose the service to local machine
kubectl port-forward -n kt2l service/kt2l 27017:80

# open the browser
open http://localhost:27017

```

# Create test data

```bash
# Create a lot of pods
for i in {1..100}; do kubectl run test-$i --image mhus/example-dice:latest --env "INFINITE=true"; done

# Create a lot of namespaces
for i in {1..100}; do kubectl create namespace "test-$i"; done

```
