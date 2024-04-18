"use strict";(self.webpackChunkKT2L=self.webpackChunkKT2L||[]).push([[4381],{9095:(e,n,s)=>{s.r(n),s.d(n,{assets:()=>a,contentTitle:()=>t,default:()=>u,frontMatter:()=>r,metadata:()=>c,toc:()=>d});var i=s(5893),o=s(1151);const r={sidebar_position:11,title:"User Configuration"},t=void 0,c={id:"configuration/config-users",title:"User Configuration",description:"User Configuration",source:"@site/docs/configuration/config-users.md",sourceDirName:"configuration",slug:"/configuration/config-users",permalink:"/docs/configuration/config-users",draft:!1,unlisted:!1,editUrl:"https://github.com/mhus/kt2l/blob/main/docs/docs/configuration/config-users.md",tags:[],version:"current",sidebarPosition:11,frontMatter:{sidebar_position:11,title:"User Configuration"},sidebar:"tutorialSidebar",previous:{title:"Login Configuration",permalink:"/docs/configuration/config-login"},next:{title:"Configuration",permalink:"/docs/category/configuration-1"}},a={},d=[{value:"User Configuration",id:"user-configuration",level:2}];function l(e){const n={a:"a",code:"code",h2:"h2",li:"li",p:"p",pre:"pre",ul:"ul",...(0,o.a)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(n.h2,{id:"user-configuration",children:"User Configuration"}),"\n",(0,i.jsxs)(n.p,{children:["The user configuration is used to configure the user specific settings. The configuration is stored in the ",(0,i.jsx)(n.code,{children:"config/users.yaml"})," file."]}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-yaml",children:'users:\n  - name: "admin"\n    password: "{generate}"\n    roles:\n      - "READ"\n      - "WRITE"\n      - "LOCAL"\n      - "SETTINGS"\n      - "ADMIN"\n'})}),"\n",(0,i.jsxs)(n.p,{children:["Under the ",(0,i.jsx)(n.code,{children:"users"})," key you can define the users. The ",(0,i.jsx)(n.code,{children:"name"})," key is the username. The\n",(0,i.jsx)(n.code,{children:"password"})," key is the password. You can use the ",(0,i.jsx)(n.code,{children:"{generate}"})," keyword to generate a\nrandom password. It will be printed in the log output. The ",(0,i.jsx)(n.code,{children:"roles"})," key is a list of\nroles the user has. The following default roles are available:"]}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"READ"}),": The user can read files and directories."]}),"\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"WRITE"}),": The user can write files and directories."]}),"\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"LOCAL"}),": The user can execute local commands."]}),"\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"SETTINGS"}),": The user can change the user settings."]}),"\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"ADMIN"}),": The user is an admin and can change the configuration."]}),"\n"]}),"\n",(0,i.jsxs)(n.p,{children:["It's possible to define custom roles also. Use custom roles in the ",(0,i.jsx)(n.code,{children:"aaa"})," configuration\nto define the permissions."]}),"\n",(0,i.jsx)(n.p,{children:"The password can also be set with other types"}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"{noop}plain"}),": The password is stored in plain text."]}),"\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"{env}ENV_KEY"}),": The password is stored and loaded from an environment variable."]}),"\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"{bcrypt}hash"}),": The password is stored as a bcrypt hash."]}),"\n"]}),"\n",(0,i.jsxs)(n.p,{children:["For more options see the spring boot configuration\n",(0,i.jsx)(n.a,{href:"https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html",children:"Password Storage"}),"."]})]})}function u(e={}){const{wrapper:n}={...(0,o.a)(),...e.components};return n?(0,i.jsx)(n,{...e,children:(0,i.jsx)(l,{...e})}):l(e)}},1151:(e,n,s)=>{s.d(n,{Z:()=>c,a:()=>t});var i=s(7294);const o={},r=i.createContext(o);function t(e){const n=i.useContext(r);return i.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function c(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(o):e.components||o:t(e.components),i.createElement(r.Provider,{value:n},e.children)}}}]);