"use strict";(self.webpackChunkKT2L=self.webpackChunkKT2L||[]).push([[2125],{490:(e,n,s)=>{s.r(n),s.d(n,{assets:()=>d,contentTitle:()=>r,default:()=>h,frontMatter:()=>l,metadata:()=>c,toc:()=>o});var i=s(5893),t=s(1151);const l={sidebar_position:10},r="Commands",c={id:"usage/commands",title:"Commands",description:"EXEC",source:"@site/docs/usage/commands.md",sourceDirName:"usage",slug:"/usage/commands",permalink:"/docs/usage/commands",draft:!1,unlisted:!1,editUrl:"https://github.com/mhus/kt2l/blob/main/docs/docs/usage/commands.md",tags:[],version:"current",sidebarPosition:10,frontMatter:{sidebar_position:10},sidebar:"tutorialSidebar",previous:{title:"Contribute",permalink:"/docs/contribute"},next:{title:"exec",permalink:"/docs/usage/exec"}},d={},o=[{value:"EXEC",id:"exec",level:2},{value:"ATTACH",id:"attach",level:2},{value:"CLEAR",id:"clear",level:2},{value:"SEND",id:"send",level:2},{value:"WAIT",id:"wait",level:2},{value:"CLOSE",id:"close",level:2},{value:"ECHO",id:"echo",level:2},{value:"SET",id:"set",level:2},{value:"IF/ELSEIF/ELSE/ENDIF",id:"ifelseifelseendif",level:2},{value:"SLEEP",id:"sleep",level:2},{value:"ENV",id:"env",level:2}];function a(e){const n={code:"code",h1:"h1",h2:"h2",li:"li",ol:"ol",p:"p",pre:"pre",ul:"ul",...(0,t.a)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(n.h1,{id:"commands",children:"Commands"}),"\n",(0,i.jsx)(n.h2,{id:"exec",children:"EXEC"}),"\n",(0,i.jsxs)(n.p,{children:["Run a command like ",(0,i.jsx)(n.code,{children:"kubectl exec"})," direct or with shell and opens a new terminal for it.\nYou can either use the argument ",(0,i.jsx)(n.code,{children:"cmd"})," or ",(0,i.jsx)(n.code,{children:"exec"})," to define the command. If you do not\ndefine both a shell with prompt will be opened."]}),"\n",(0,i.jsx)(n.p,{children:"Arguments:"}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsx)(n.li,{children:"cmd: string - the command to run as a shell script."}),"\n",(0,i.jsx)(n.li,{children:"exec: string - the execution command directly, parts are comma separated."}),"\n",(0,i.jsxs)(n.li,{children:["tty: boolean - open a tty for the command, default is ",(0,i.jsx)(n.code,{children:"true"}),"."]}),"\n",(0,i.jsxs)(n.li,{children:["stdin: boolean - opens a stdin pipeline for the command, default is ",(0,i.jsx)(n.code,{children:"true"}),"."]}),"\n",(0,i.jsxs)(n.li,{children:["shell: string - command for the shell, default is ",(0,i.jsx)(n.code,{children:"/bin/bash"}),"."]}),"\n"]}),"\n",(0,i.jsxs)(n.p,{children:["The command will create a ",(0,i.jsx)(n.code,{children:"scope"})," to handle the open terminal."]}),"\n",(0,i.jsx)(n.h2,{id:"attach",children:"ATTACH"}),"\n",(0,i.jsxs)(n.p,{children:["Attach to a pod like ",(0,i.jsx)(n.code,{children:"kubectl attach"}),"."]}),"\n",(0,i.jsx)(n.p,{children:"Arguments:"}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsxs)(n.li,{children:["tty: boolean - open a tty for the command, default is ",(0,i.jsx)(n.code,{children:"true"}),"."]}),"\n",(0,i.jsxs)(n.li,{children:["stdin: boolean - opens a stdin pipeline for the command, default is ",(0,i.jsx)(n.code,{children:"true"}),"."]}),"\n"]}),"\n",(0,i.jsxs)(n.p,{children:["The command will create a ",(0,i.jsx)(n.code,{children:"scope"})," to handle the open terminal."]}),"\n",(0,i.jsx)(n.h2,{id:"clear",children:"CLEAR"}),"\n",(0,i.jsxs)(n.p,{children:["Clears the terminal content buffers of the ",(0,i.jsx)(n.code,{children:"scope"}),"."]}),"\n",(0,i.jsx)(n.h2,{id:"send",children:"SEND"}),"\n",(0,i.jsx)(n.p,{children:"Send characters to an open terminal."}),"\n",(0,i.jsx)(n.p,{children:"Arguments:"}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsx)(n.li,{children:"line: string - send a line inclusive a newline character"}),"\n",(0,i.jsx)(n.li,{children:"msg: string - send a message without the newline character"}),"\n"]}),"\n",(0,i.jsx)(n.h2,{id:"wait",children:"WAIT"}),"\n",(0,i.jsxs)(n.p,{children:["Wait for a specific content to appear in the terminal or the end of the terminal connection.\nYou can wait for ",(0,i.jsx)(n.code,{children:"content"})," (combined standard out and error out) or\none of ",(0,i.jsx)(n.code,{children:"stdout"})," or ",(0,i.jsx)(n.code,{children:"errout"}),". Or all of them."]}),"\n",(0,i.jsx)(n.p,{children:"Arguments:"}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsx)(n.li,{children:"content: string - the content to wait for in the terminal content buffer."}),"\n",(0,i.jsx)(n.li,{children:"stdout: string - the content to wait for in the terminal stdout buffer."}),"\n",(0,i.jsx)(n.li,{children:"stderr: string - the content to wait for in the terminal stderr buffer."}),"\n",(0,i.jsx)(n.li,{children:"timeout: number/period - the timeout period to wait for the content to appear, e.g. 5s, 10m, 1h"}),"\n"]}),"\n",(0,i.jsx)(n.p,{children:"Return parameter (<scope>.return):"}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"closed"})," - if the terminal was closed"]}),"\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"timeout"})," - on timeout"]}),"\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"content"})," - Match appears in content"]}),"\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"stdin"})," - Match appears in stdin"]}),"\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"stderr"})," - Match appears in stderr"]}),"\n"]}),"\n",(0,i.jsx)(n.h2,{id:"close",children:"CLOSE"}),"\n",(0,i.jsxs)(n.p,{children:["Closes the current terminal in the ",(0,i.jsx)(n.code,{children:"scope"}),"."]}),"\n",(0,i.jsx)(n.h2,{id:"echo",children:"ECHO"}),"\n",(0,i.jsx)(n.p,{children:"Echo a message to the local output."}),"\n",(0,i.jsx)(n.p,{children:"Arguments:"}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsx)(n.li,{children:"msg: string - The content to print"}),"\n"]}),"\n",(0,i.jsx)(n.h2,{id:"set",children:"SET"}),"\n",(0,i.jsx)(n.p,{children:"Set context values / environment variables"}),"\n",(0,i.jsx)(n.p,{children:"Arguments:"}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsx)(n.li,{children:"key: string"}),"\n",(0,i.jsx)(n.li,{children:"value: string"}),"\n"]}),"\n",(0,i.jsx)(n.h2,{id:"ifelseifelseendif",children:"IF/ELSEIF/ELSE/ENDIF"}),"\n",(0,i.jsx)(n.p,{children:"Conditional statements."}),"\n",(0,i.jsx)(n.p,{children:"Arguments for IF/ELSEIF:"}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsx)(n.li,{children:"is: string - the condition to evaluate"}),"\n"]}),"\n",(0,i.jsx)(n.p,{children:"Conditions:"}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsxs)(n.li,{children:["Use ",(0,i.jsx)(n.code,{children:"${name}"})," for substitution"]}),"\n",(0,i.jsxs)(n.li,{children:["Use ",(0,i.jsx)(n.code,{children:"< <= > >= == != =~"})," for comparison"]}),"\n",(0,i.jsxs)(n.li,{children:["Use ",(0,i.jsx)(n.code,{children:"|| &&"})," to connect multiple comparisons"]}),"\n"]}),"\n",(0,i.jsx)(n.h2,{id:"sleep",children:"SLEEP"}),"\n",(0,i.jsx)(n.p,{children:"Sleep a amount of time."}),"\n",(0,i.jsx)(n.p,{children:"Arguments:"}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsx)(n.li,{children:"time : string - time to sleep period."}),"\n"]}),"\n",(0,i.jsx)(n.h2,{id:"env",children:"ENV"}),"\n",(0,i.jsx)(n.p,{children:"Set an environment variable or if not set print all variables."}),"\n",(0,i.jsx)(n.p,{children:"Arguments:"}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsx)(n.li,{children:"key: string"}),"\n",(0,i.jsx)(n.li,{children:"value: string"}),"\n"]}),"\n",(0,i.jsx)(n.h1,{id:"scope",children:"Scope"}),"\n",(0,i.jsxs)(n.p,{children:["Using communication commands to interact with the terminal you can use scopes to separate different sessions.\nAdd the scope to the command name with a dot before the command, e.g. ",(0,i.jsx)(n.code,{children:"scope.exec"})," and ",(0,i.jsx)(n.code,{children:"scope.send"}),", ",(0,i.jsx)(n.code,{children:"scope.close"}),".\nIf no scope is provided the default scope ",(0,i.jsx)(n.code,{children:"default"})," is used."]}),"\n",(0,i.jsx)(n.h1,{id:"environment-variables",children:"Environment variables"}),"\n",(0,i.jsx)(n.p,{children:"The variables are global for all sessions."}),"\n",(0,i.jsx)(n.p,{children:"Variables:"}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"container"})," - Name of the container in the pod to connect to."]}),"\n",(0,i.jsx)(n.li,{children:"'<scope>.return' - Some commands will return a result in this variable."}),"\n"]}),"\n",(0,i.jsx)(n.h1,{id:"examples",children:"Examples"}),"\n",(0,i.jsxs)(n.ol,{children:["\n",(0,i.jsxs)(n.li,{children:["Open a terminal with shell, send ",(0,i.jsx)(n.code,{children:"ls -l"}),", wait for the next prompt and close the terminal."]}),"\n"]}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{children:'EXEC\nCLEAR\nSEND line="ls -l"\nWAIT content="root@nginx"\nCLOSE\n'})}),"\n",(0,i.jsxs)(n.ol,{start:"2",children:["\n",(0,i.jsxs)(n.li,{children:["Open a terminal with the shell command ",(0,i.jsx)(n.code,{children:"ls -la"})," and wait for the terminal to close."]}),"\n"]}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{children:'EXEC cmd="ls -la"\nWAIT\n'})}),"\n",(0,i.jsxs)(n.ol,{start:"3",children:["\n",(0,i.jsxs)(n.li,{children:["Attaches to a terminal and send in the background a SIGHUP signal to the first process. Wait\nfor the output ",(0,i.jsx)(n.code,{children:"startup"})," in the terminal."]}),"\n"]}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{children:'session1.ATTACH stdin=false tty=false\nsession2.EXEC cmd="kill -1 1"\nsession1.WAIT content="startup"\nsession1.CLOSE\nsession2.CLOSE\n'})})]})}function h(e={}){const{wrapper:n}={...(0,t.a)(),...e.components};return n?(0,i.jsx)(n,{...e,children:(0,i.jsx)(a,{...e})}):a(e)}},1151:(e,n,s)=>{s.d(n,{Z:()=>c,a:()=>r});var i=s(7294);const t={},l=i.createContext(t);function r(e){const n=i.useContext(l);return i.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function c(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(t):e.components||t:r(e.components),i.createElement(l.Provider,{value:n},e.children)}}}]);