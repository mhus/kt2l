
## Run

```bash
java -Dspring.profiles.active=prod -jar kt2l-server-0.0.1-SNAPSHOT.jar

docker run -it --rm --name kt2l-server \
    -v "$HOME/.kube:/home/user/.kube" \
    -v "$HOME/.aws:/home/user/.aws" \
    -p 8080:8080 \
    --platform linux/amd64 \
    kt2l-server:snapshot
```
