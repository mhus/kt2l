"use strict";(self.webpackChunkKT2L=self.webpackChunkKT2L||[]).push([[3880],{3677:(e,t,r)=>{r.r(t),r.d(t,{assets:()=>h,contentTitle:()=>l,default:()=>a,frontMatter:()=>n,metadata:()=>o,toc:()=>d});var s=r(5893),i=r(1151);const n={sidebar_position:7},l="Server",o={id:"installation/server",title:"Server",description:"Install Java",source:"@site/docs/installation/server.md",sourceDirName:"installation",slug:"/installation/server",permalink:"/docs/installation/server",draft:!1,unlisted:!1,editUrl:"https://github.com/mhus/kt2l/blob/main/docs/docs/installation/server.md",tags:[],version:"current",sidebarPosition:7,frontMatter:{sidebar_position:7},sidebar:"tutorialSidebar",previous:{title:"Container",permalink:"/docs/installation/container"},next:{title:"Desktop",permalink:"/docs/installation/desktop"}},h={},d=[{value:"Install Java",id:"install-java",level:2},{value:"Download the server",id:"download-the-server",level:2},{value:"Unzip the file",id:"unzip-the-file",level:2},{value:"Control the server",id:"control-the-server",level:2},{value:"Configure the server startup",id:"configure-the-server-startup",level:2},{value:"Update the server",id:"update-the-server",level:2}];function c(e){const t={a:"a",code:"code",em:"em",h1:"h1",h2:"h2",li:"li",p:"p",strong:"strong",ul:"ul",...(0,i.a)(),...e.components};return(0,s.jsxs)(s.Fragment,{children:[(0,s.jsx)(t.h1,{id:"server",children:"Server"}),"\n",(0,s.jsx)(t.h2,{id:"install-java",children:"Install Java"}),"\n",(0,s.jsxs)(t.p,{children:["The server should run on every target platform where Java 21 is available. First install\nJava JRE 21 from oracle or Eclipse ",(0,s.jsx)(t.a,{href:"https://adoptium.net/de/temurin/releases/",children:"Temurin Latest Release"}),"."]}),"\n",(0,s.jsxs)(t.p,{children:["if the command ",(0,s.jsx)(t.code,{children:"java --version"})," returns something like ",(0,s.jsx)(t.em,{children:(0,s.jsx)(t.strong,{children:"java 21.0.2 2024-01-16 LTS"})})," it\nlooks good for now. Be sure the java home variable JAVA_HOME is set to the correct path.\n",(0,s.jsx)(t.code,{children:"$JAVA_HOME/bin/java --version"})," should result the same output then before."]}),"\n",(0,s.jsx)(t.h2,{id:"download-the-server",children:"Download the server"}),"\n",(0,s.jsxs)(t.p,{children:["Download the latest ",(0,s.jsx)(t.em,{children:(0,s.jsx)(t.strong,{children:"Server Bundled"})})," ZIP-File from the website ",(0,s.jsx)(t.a,{href:"https://kt2l.org#downloads",children:"KT2L Website"}),"."]}),"\n",(0,s.jsx)(t.h2,{id:"unzip-the-file",children:"Unzip the file"}),"\n",(0,s.jsxs)(t.p,{children:["Use the unzip command to unzip ",(0,s.jsx)(t.code,{children:"unzip kt2l-server-...zip"}),"."]}),"\n",(0,s.jsx)(t.h2,{id:"control-the-server",children:"Control the server"}),"\n",(0,s.jsxs)(t.p,{children:["Navigate to the unzipped folder and start the server with the command ",(0,s.jsx)(t.code,{children:"./bin/service.sh start"}),"."]}),"\n",(0,s.jsxs)(t.p,{children:["The script ",(0,s.jsx)(t.code,{children:"service.sh"})," is a wrapper script for the server. It is possible to:"]}),"\n",(0,s.jsxs)(t.ul,{children:["\n",(0,s.jsxs)(t.li,{children:["start the server with ",(0,s.jsx)(t.code,{children:"./bin/service.sh start"})]}),"\n",(0,s.jsxs)(t.li,{children:["stop the server with ",(0,s.jsx)(t.code,{children:"./bin/service.sh stop"})]}),"\n",(0,s.jsxs)(t.li,{children:["restart the server with ",(0,s.jsx)(t.code,{children:"./bin/service.sh restart"})]}),"\n",(0,s.jsxs)(t.li,{children:["show the server status with ",(0,s.jsx)(t.code,{children:"./bin/service.sh status"})]}),"\n",(0,s.jsxs)(t.li,{children:["kill the server with ",(0,s.jsx)(t.code,{children:"./bin/service.sh kill"})]}),"\n",(0,s.jsxs)(t.li,{children:["cleanup the server pid with ",(0,s.jsx)(t.code,{children:"./bin/service.sh zap"})]}),"\n",(0,s.jsxs)(t.li,{children:["show the server log with ",(0,s.jsx)(t.code,{children:"./bin/service.sh log"})," (the logfiles could rotate, the command is not following the log rotate)"]}),"\n",(0,s.jsxs)(t.li,{children:["show the server stdout with ",(0,s.jsx)(t.code,{children:"./bin/service.sh stdout"})]}),"\n"]}),"\n",(0,s.jsx)(t.h2,{id:"configure-the-server-startup",children:"Configure the server startup"}),"\n",(0,s.jsxs)(t.p,{children:["The server can be configured with the file ",(0,s.jsx)(t.code,{children:"env.sh"}),". The file is located in the root folder of the server.\nYou can set environment variables for the server in this file."]}),"\n",(0,s.jsxs)(t.ul,{children:["\n",(0,s.jsx)(t.li,{children:"JAVA_HOME - the path to the java home folder (not the binary folder)"}),"\n",(0,s.jsxs)(t.li,{children:["PID_FILE - the path to the pid file (default is ",(0,s.jsx)(t.code,{children:"var/run/kt2l-server.pid"}),")"]}),"\n",(0,s.jsxs)(t.li,{children:["LOG_FILE - the path to the stdout log file (default is ",(0,s.jsx)(t.code,{children:"logs/stdout.log"}),")"]}),"\n",(0,s.jsx)(t.li,{children:"SERVER_PORT - the port the server is listening on (default 9080)"}),"\n",(0,s.jsxs)(t.li,{children:["KT2L_ROTATE_STDOUT - set to ",(0,s.jsx)(t.code,{children:"true"})," to rotate the stdout at startup, otherwise ",(0,s.jsx)(t.code,{children:"false"})," or leave empty."]}),"\n",(0,s.jsxs)(t.li,{children:["KT2L_LOG_DIRECTORY - the path to the log directory (default is ",(0,s.jsx)(t.code,{children:"logs"})," in the server directory)"]}),"\n",(0,s.jsxs)(t.li,{children:["KT2L_LOG_MAX_FILE_SIZE - the maximum size of a log file (default is ",(0,s.jsx)(t.code,{children:"1GB"}),")"]}),"\n",(0,s.jsxs)(t.li,{children:["KT2L_LOG_MAX_FILE_SIZE - the maximum number of log files (default is ",(0,s.jsx)(t.code,{children:"10GB"}),")"]}),"\n",(0,s.jsxs)(t.li,{children:["KT2L_LOG_LEVEL - the log level of the server (default is ",(0,s.jsx)(t.code,{children:"INFO"}),", options are ",(0,s.jsx)(t.code,{children:"TRACE"}),", ",(0,s.jsx)(t.code,{children:"DEBUG"}),", ",(0,s.jsx)(t.code,{children:"INFO"}),", ",(0,s.jsx)(t.code,{children:"WARN"}),", ",(0,s.jsx)(t.code,{children:"ERROR"}),")"]}),"\n",(0,s.jsxs)(t.li,{children:["CONFIGURATION_DIRECTORY - the path to the configuration directory (default is ",(0,s.jsx)(t.code,{children:"config"})," in the server directory. It can't be overwritten directly use ",(0,s.jsx)(t.code,{children:"confic/local"})," or ",(0,s.jsx)(t.code,{children:"config/users/<username>"}),")."]}),"\n",(0,s.jsx)(t.li,{children:"CONFIGURATION_LOCAL_DIRECTORY - the path to the local configuration directory (to overwrite the default configuration)"}),"\n",(0,s.jsx)(t.li,{children:"CONFIGURATION_USER_DIRECTORY - the path to the user configuration directory (to overwrite the default configuration for each user)"}),"\n",(0,s.jsx)(t.li,{children:"KT2L_TMP_DIRECTORY - the path to the temporary directory (default is the java system temporary directory)"}),"\n",(0,s.jsxs)(t.li,{children:["KT2L_STORAGE_DIRECTORY - the path to the storage directory (default is ",(0,s.jsx)(t.code,{children:"var/storage"})," in the server directory)"]}),"\n",(0,s.jsxs)(t.li,{children:["(KT2L_SPRING_PROFILE - additional spring profiles of the server (default is ",(0,s.jsx)(t.code,{children:"prod"}),") - bin/run.sh only)"]}),"\n",(0,s.jsx)(t.li,{children:"KT2L_CLASSPATH - additional classpath for the server (default is empty)"}),"\n"]}),"\n",(0,s.jsx)(t.h2,{id:"update-the-server",children:"Update the server"}),"\n",(0,s.jsxs)(t.p,{children:["To update the server, download the latest ",(0,s.jsx)(t.em,{children:(0,s.jsx)(t.strong,{children:"Server Bundled"})})," ZIP-File from the website ",(0,s.jsx)(t.a,{href:"https://kt2l.org#downloads",children:"KT2L Website"})," and\nunzip the file into a temporary folder. Copy the content of the ",(0,s.jsx)(t.code,{children:"bin"})," and ",(0,s.jsx)(t.code,{children:"lib"})," directory to your current server folder."]})]})}function a(e={}){const{wrapper:t}={...(0,i.a)(),...e.components};return t?(0,s.jsx)(t,{...e,children:(0,s.jsx)(c,{...e})}):c(e)}},1151:(e,t,r)=>{r.d(t,{Z:()=>o,a:()=>l});var s=r(7294);const i={},n=s.createContext(i);function l(e){const t=s.useContext(n);return s.useMemo((function(){return"function"==typeof e?e(t):{...t,...e}}),[t,e])}function o(e){let t;return t=e.disableParentContext?"function"==typeof e.components?e.components(i):e.components||i:l(e.components),s.createElement(n.Provider,{value:t},e.children)}}}]);