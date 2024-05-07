"use strict";(self.webpackChunkKT2L=self.webpackChunkKT2L||[]).push([[8135],{65:(e,n,i)=>{i.r(n),i.d(n,{assets:()=>l,contentTitle:()=>r,default:()=>a,frontMatter:()=>s,metadata:()=>c,toc:()=>d});var t=i(5893),o=i(1151);const s={sidebar_position:2,title:"Help Configuration"},r="Help Configuration",c={id:"configuration/config-help",title:"Help Configuration",description:"The help configuration is used to configure the help system. The configuration is stored in the config/help.yaml file.",source:"@site/docs/configuration/config-help.md",sourceDirName:"configuration",slug:"/configuration/config-help",permalink:"/docs/configuration/config-help",draft:!1,unlisted:!1,editUrl:"https://github.com/mhus/kt2l/blob/main/docs/docs/configuration/config-help.md",tags:[],version:"current",sidebarPosition:2,frontMatter:{sidebar_position:2,title:"Help Configuration"},sidebar:"tutorialSidebar",previous:{title:"Local command execution",permalink:"/docs/configuration/config-cmd"},next:{title:"Shell Configuration",permalink:"/docs/configuration/config-shell"}},l={},d=[];function h(e){const n={code:"code",h1:"h1",li:"li",p:"p",pre:"pre",ul:"ul",...(0,o.a)(),...e.components};return(0,t.jsxs)(t.Fragment,{children:[(0,t.jsx)(n.h1,{id:"help-configuration",children:"Help Configuration"}),"\n",(0,t.jsxs)(n.p,{children:["The help configuration is used to configure the help system. The configuration is stored in the ",(0,t.jsx)(n.code,{children:"config/help.yaml"})," file."]}),"\n",(0,t.jsx)(n.p,{children:"Example:"}),"\n",(0,t.jsx)(n.pre,{children:(0,t.jsx)(n.code,{className:"language-yaml",children:"windowWidth: 400px\nenabled: true\ncontexts:\n  default:\n    - name: Chat Agent\n      action: ai\n    - name: Documentation\n      action: docs\n      document: index\n    - name: Kubernetes\n      action: link\n      href: https://kubernetes.io/docs/home/\n"})}),"\n",(0,t.jsxs)(n.p,{children:["The property ",(0,t.jsx)(n.code,{children:"windowWidth"})," defines the width of the help window. The property ",(0,t.jsx)(n.code,{children:"enabled"})," defines if the help system\nis enabled. The property ",(0,t.jsx)(n.code,{children:"contexts"})," defines the help entries."]}),"\n",(0,t.jsxs)(n.p,{children:["Each entry has a ",(0,t.jsx)(n.code,{children:"name"})," and an ",(0,t.jsx)(n.code,{children:"action"}),". It presents an entry in the help menu. If the ",(0,t.jsx)(n.code,{children:"action"})," is not available\nit will be ignored."]}),"\n",(0,t.jsxs)(n.p,{children:["The ",(0,t.jsx)(n.code,{children:"action"})," can be one of the following:"]}),"\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.code,{children:"ai"})," - open the chat agent in the help system"]}),"\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.code,{children:"docs"})," - open a document in the help system"]}),"\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.code,{children:"link"})," - open a link in the system browser"]}),"\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.code,{children:"yaml-snippets"})," - opens a snippet view for the resource create panel."]}),"\n"]}),"\n",(0,t.jsxs)(n.p,{children:["The ",(0,t.jsx)(n.code,{children:"ai"})," supports the following properties:"]}),"\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.code,{children:"model"})," - the model to use"]}),"\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.code,{children:"prompt"})," - the initial prompt"]}),"\n"]}),"\n",(0,t.jsxs)(n.p,{children:["Snippet views are used to provide public snippets from a git repository. The ",(0,t.jsx)(n.code,{children:"create"})," action supports the following properties:"]}),"\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.code,{children:"repo"})," - the git repository url"]}),"\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.code,{children:"path"})," - the path to the snippet directory"]}),"\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.code,{children:"branch"})," - the branch to use (optional, default is main)"]}),"\n"]})]})}function a(e={}){const{wrapper:n}={...(0,o.a)(),...e.components};return n?(0,t.jsx)(n,{...e,children:(0,t.jsx)(h,{...e})}):h(e)}},1151:(e,n,i)=>{i.d(n,{Z:()=>c,a:()=>r});var t=i(7294);const o={},s=t.createContext(o);function r(e){const n=t.useContext(s);return t.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function c(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(o):e.components||o:r(e.components),t.createElement(s.Provider,{value:n},e.children)}}}]);