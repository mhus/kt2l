


mvn -Pnative -Pproduction spring-boot:build-image -Dspring-boot.build-image.imageName=kt2l-native

docker run --rm -it -p 8080:8080 --name kt2l-native -t kt2l-native
