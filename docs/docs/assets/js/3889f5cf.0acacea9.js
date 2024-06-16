"use strict";(self.webpackChunkKT2L=self.webpackChunkKT2L||[]).push([[5602],{209:(e,n,t)=>{t.r(n),t.d(n,{assets:()=>h,contentTitle:()=>l,default:()=>d,frontMatter:()=>s,metadata:()=>c,toc:()=>r});var i=t(5893),o=t(1151);const s={sidebar_position:3,title:"Shell"},l="Shell Configuration",c={id:"configuration/config-shell",title:"Shell",description:"The shell configuration is used to find the shell that will be used to run the commands in the local terminal or",source:"@site/docs/configuration/config-shell.md",sourceDirName:"configuration",slug:"/configuration/config-shell",permalink:"/docs/configuration/config-shell",draft:!1,unlisted:!1,editUrl:"https://github.com/mhus/kt2l/blob/main/docs/docs/configuration/config-shell.md",tags:[],version:"current",sidebarPosition:3,frontMatter:{sidebar_position:3,title:"Shell"},sidebar:"tutorialSidebar",previous:{title:"Help",permalink:"/docs/configuration/config-help"},next:{title:"Views",permalink:"/docs/configuration/config-views"}},h={},r=[];function a(e){const n={code:"code",h1:"h1",p:"p",pre:"pre",...(0,o.a)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(n.h1,{id:"shell-configuration",children:"Shell Configuration"}),"\n",(0,i.jsx)(n.p,{children:"The shell configuration is used to find the shell that will be used to run the commands in the local terminal or\nin pod containers."}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-yaml",children:"shell: /bin/bash\ndefault: /bin/sh\nimages:\ncontains:\n    ubuntu: /bin/bash\n    centos: /bin/bash\n"})}),"\n",(0,i.jsxs)(n.p,{children:["The ",(0,i.jsx)(n.code,{children:"shell"})," key is used to specify the shell that will be used to run the commands in the local terminal or in pod\ncontainers. All other keys are used to specify the shell that will be used to run the commands in the pod containers."]}),"\n",(0,i.jsxs)(n.p,{children:["Shells can also be defined in the ",(0,i.jsx)(n.code,{children:"cluster"})," configuration in section ",(0,i.jsx)(n.code,{children:"shell"}),". If a entry in the ",(0,i.jsx)(n.code,{children:"images"})," section matches\nthe image of the pod container, the shell defined in the ",(0,i.jsx)(n.code,{children:"images"})," section will be used. If no entry in the ",(0,i.jsx)(n.code,{children:"images"})," section\nmatches the image of the pod container, the shell defined in the ",(0,i.jsx)(n.code,{children:"contains"})," can match the image of the pod container.\nIf no entry in the ",(0,i.jsx)(n.code,{children:"contains"})," section matches the image of the pod container, the shell defined in the ",(0,i.jsx)(n.code,{children:"default"})," key will\nbe used. Finally, if no shell is defined in the ",(0,i.jsx)(n.code,{children:"default"})," key, the shell ",(0,i.jsx)(n.code,{children:"/bin/sh"})," will be used."]})]})}function d(e={}){const{wrapper:n}={...(0,o.a)(),...e.components};return n?(0,i.jsx)(n,{...e,children:(0,i.jsx)(a,{...e})}):a(e)}},1151:(e,n,t)=>{t.d(n,{Z:()=>c,a:()=>l});var i=t(7294);const o={},s=i.createContext(o);function l(e){const n=i.useContext(s);return i.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function c(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(o):e.components||o:l(e.components),i.createElement(s.Provider,{value:n},e.children)}}}]);