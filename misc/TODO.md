
* Logo zuschneiden
// * Fix commons: properties parser, add \n \r \t, auch multiple line inputs
// * exec: rename run in exec, add exec=
// * exec: multi line commands
* exec: single execution, ohne pod - pod wird dann erst erstellt
* details: auch multi create zulassen d.h. yaml editor mit apply ---
// * exec: command return value in context, e.g. timeout als status nicht als fehler in WAIT
// * exec: EXEC tty, stdin als parameter
* help: hilfe button und panel + configs im header
* used resources anzeige ueber button im header ?

* Menu in sub menus packen mit pfad und order

* Config provider für spring boot aus konfiguration 

* nodes grid
* deployment grid
* cronjob, job, statefulset grid
* action von pod zu nodes, deployments, cronjobs, jobs, statefulsets

* Docu: Lizenzen info fuer logo (https://www.svgrepo.com/svg/164744/wrench, https://www.svgrepo.com/svg/376331/kubernetes)
* Docu: author info ?
* Lizenz auf GPL?!
* Doku automatisch erstellen
* Doku auch in jar packen für offline hilfe oder nur online? - versioniert!

* Cockpit view mit Buttons für seiten,zb. Grafana und automat. portforwards machen (config im cluster)
* port forward background manager
* port forward view

* status view mit grafischem status aller nodes und pods, mem, cpu und storage

* shell: enhance terminal komponente, add key listener, kein echo

## release

* als standalone app mit embedded browser (https://github.com/chromiumembedded/java-cef)
  (https://github.com/jcefmaven/jcefmaven)
* pom submodule ??? für launcher ?

## exec

* LDIR command
* CP command
* output abschnitte in lokale dateien umleiten (wohin?)
// * IF ELSE ELIF FI commands
* LET command bzw SET als LET