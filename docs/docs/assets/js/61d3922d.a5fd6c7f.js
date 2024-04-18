"use strict";(self.webpackChunkKT2L=self.webpackChunkKT2L||[]).push([[6195],{4890:(e,n,t)=>{t.r(n),t.d(n,{assets:()=>c,contentTitle:()=>i,default:()=>h,frontMatter:()=>s,metadata:()=>a,toc:()=>l});var o=t(5893),r=t(1151);const s={sidebar_position:6},i="Run Container",a={id:"installation/container",title:"Run Container",description:"The simplest way to run the server is to use the container bundle.",source:"@site/docs/installation/container.md",sourceDirName:"installation",slug:"/installation/container",permalink:"/docs/installation/container",draft:!1,unlisted:!1,editUrl:"https://github.com/mhus/kt2l/blob/main/docs/docs/installation/container.md",tags:[],version:"current",sidebarPosition:6,frontMatter:{sidebar_position:6},sidebar:"tutorialSidebar",previous:{title:"Installation",permalink:"/docs/category/installation"},next:{title:"Run Server",permalink:"/docs/installation/server"}},c={},l=[];function d(e){const n={code:"code",em:"em",h1:"h1",p:"p",pre:"pre",strong:"strong",...(0,r.a)(),...e.components};return(0,o.jsxs)(o.Fragment,{children:[(0,o.jsx)(n.h1,{id:"run-container",children:"Run Container"}),"\n",(0,o.jsx)(n.p,{children:"The simplest way to run the server is to use the container bundle."}),"\n",(0,o.jsx)(n.p,{children:"Prepare a local directory for config files:"}),"\n",(0,o.jsx)(n.pre,{children:(0,o.jsx)(n.code,{className:"language-bash",children:"mkdir -p ~/.kt2l/config\n"})}),"\n",(0,o.jsxs)(n.p,{children:["On mac it is a problem that the userid inside the container is 1001 and not\n501 (default on mac). This means the process in the container can't access\nfiles in the new config directory. use ",(0,o.jsx)(n.code,{children:"chmod -R 777 ~/.kt2l/config"})," to give\naccess. But keep in mind that the files have different user rights."]}),"\n",(0,o.jsx)(n.p,{children:"The following command will run the server in the background using you kube config:"}),"\n",(0,o.jsx)(n.pre,{children:(0,o.jsx)(n.code,{className:"language-bash",children:'\ndocker run -d --rm --name kt2l-server \\\n    -p 8080:8080 \\\n    -v "$HOME/.kube:/home/user/.kube" \\\n    -v "$HOME/.kt2l/config:/home/user/config" \\\n    -e CONFIGURATION_DIRECTORY=/home/user/config \\\n    --platform linux/amd64 \\\n    mhus/kt2l-server:snapshot\n'})}),"\n",(0,o.jsxs)(n.p,{children:["You can access the server with the browser ",(0,o.jsx)(n.code,{children:"http://localhost:8080"}),". You have to login if you\nuse the container version. Use ",(0,o.jsx)(n.em,{children:(0,o.jsx)(n.strong,{children:"admin"})})," and the password from the container log. After a\nfew seconds you can get the password with the following command:\n",(0,o.jsx)(n.code,{children:"docker logs kt2l-server|grep \"Set login password for user admin\"|cut -d \\} -f 2|cut -d ' ' -f 1"}),"."]}),"\n",(0,o.jsxs)(n.p,{children:["To stop and remove the docker container simply run ",(0,o.jsx)(n.code,{children:"docker stop kt2l-server"}),"."]})]})}function h(e={}){const{wrapper:n}={...(0,r.a)(),...e.components};return n?(0,o.jsx)(n,{...e,children:(0,o.jsx)(d,{...e})}):d(e)}},1151:(e,n,t)=>{t.d(n,{Z:()=>a,a:()=>i});var o=t(7294);const r={},s=o.createContext(r);function i(e){const n=o.useContext(s);return o.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function a(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(r):e.components||r:i(e.components),o.createElement(s.Provider,{value:n},e.children)}}}]);