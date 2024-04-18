
cd kt2l-desktop\target

jpackage  --name KT2L ^
  --input . ^
  --main-jar kt2l-desktop-0.0.1-SNAPSHOT.jar ^
  --type exe ^
  --java-options "-XstartOnFirstThread -Dspring.profiles.active=prod" ^
  --vendor "www.kt2l.org"

copy KT2L-1.0.exe KT2L.exe

