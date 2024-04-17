# Development

## JUnit Testing


* [https://docs.spring.io/spring-boot/how-to/testing.html]
* [https://vaadin.com/docs/latest/testing]
* [https://java.testcontainers.org/modules/k3s/]
* [https://www.baeldung.com/spring-boot-testing]
* [https://assertj.github.io/doc/]
* [https://www.baeldung.com/mockito-series]


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