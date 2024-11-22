
# Build and run native executable

```shell
# In project root directory
./launcher/local-build -n

./kt2l-native/target/kt2l-native -Dspring.profiles.active=prod
```

# Build and run native image

```shell
# In project root directory
./launcher/local-build -i

docker run --rm -it -p 8080:8080 --name kt2l-native -t kt2l-native
```
