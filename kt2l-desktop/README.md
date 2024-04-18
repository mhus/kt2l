
Run with:

`-XstartOnFirstThread`

Productive

Compile: mvn clean install -Pproduction -Dspring.profiles.active=prod

Run: java -XstartOnFirstThread -Dspring.profiles.active=prod -jar kt2l-desktop/target/kt2l-desktop-0.0.1-SNAPSHOT.jar

Create MacOS App: 
* https://centerkey.com/mac/java/)
* https://docs.oracle.com/en/java/javase/17/docs/specs/man/jpackage.html

```bash
./kt2l-desktop/launcher/prepare.sh
mvn clean install -Pproduction -Dspring.profiles.active=prod
./kt2l-desktop/launcher/create-mac.sh
./kt2l-desktop/launcher/deploy-mac.sh

open kt2l-desktop/target/launcher
```

`jpackage --name KT2L --input . --main-jar kt2l-desktop-0.0.1-SNAPSHOT.jar \
--resource-dir package/macos --type dmg --java-options "-XstartOnFirstThread -Dspring.profiles.active=prod"`

