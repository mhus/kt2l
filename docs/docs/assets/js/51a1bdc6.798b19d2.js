"use strict";(self.webpackChunkKT2L=self.webpackChunkKT2L||[]).push([[874],{4554:(n,o,e)=>{e.r(o),e.d(o,{assets:()=>a,contentTitle:()=>s,default:()=>d,frontMatter:()=>r,metadata:()=>c,toc:()=>u});var i=e(5893),t=e(1151);const r={sidebar_position:10,title:"Login"},s="Login Configuration",c={id:"configuration/config-login",title:"Login",description:"The configuration is used to configure the login process and the user interface.",source:"@site/docs/configuration/config-login.md",sourceDirName:"configuration",slug:"/configuration/config-login",permalink:"/docs/configuration/config-login",draft:!1,unlisted:!1,editUrl:"https://github.com/mhus/kt2l/blob/main/docs/docs/configuration/config-login.md",tags:[],version:"current",sidebarPosition:10,frontMatter:{sidebar_position:10,title:"Login"},sidebar:"tutorialSidebar",previous:{title:"AI Integration",permalink:"/docs/configuration/ai"},next:{title:"Pod Scorer",permalink:"/docs/configuration/config-pod_scorer"}},a={},u=[];function l(n){const o={a:"a",code:"code",h1:"h1",p:"p",pre:"pre",...(0,t.a)(),...n.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(o.h1,{id:"login-configuration",children:"Login Configuration"}),"\n",(0,i.jsxs)(o.p,{children:["The configuration is used to configure the login process and the user interface.\nThe configuration is stored in the ",(0,i.jsx)(o.code,{children:"config/login.yaml"})," file."]}),"\n",(0,i.jsx)(o.pre,{children:(0,i.jsx)(o.code,{className:"language-yaml",children:"autoLogin: true\nautoLoginUser: autologin\nautoLoginLocalhostOnly: true\n"})}),"\n",(0,i.jsxs)(o.p,{children:[(0,i.jsx)(o.code,{children:"autoLogin"})," enables automatic login with the user specified in ",(0,i.jsx)(o.code,{children:"autoLoginUser"}),". If\n",(0,i.jsx)(o.code,{children:"autoLoginLocalhostOnly"})," is enabled, the auto login is only possible from localhost.\nOther IP sources will be prompted for login."]}),"\n",(0,i.jsxs)(o.p,{children:["The user specified in ",(0,i.jsx)(o.code,{children:"autoLoginUser"})," must be a known user. The user must be defined in the ",(0,i.jsx)(o.code,{children:"config/users.yaml"})," file."]}),"\n",(0,i.jsxs)(o.p,{children:["See also the ",(0,i.jsx)(o.a,{href:"config-users",children:"User Configuration"})," for more information about the user configuration."]})]})}function d(n={}){const{wrapper:o}={...(0,t.a)(),...n.components};return o?(0,i.jsx)(o,{...n,children:(0,i.jsx)(l,{...n})}):l(n)}},1151:(n,o,e)=>{e.d(o,{Z:()=>c,a:()=>s});var i=e(7294);const t={},r=i.createContext(t);function s(n){const o=i.useContext(r);return i.useMemo((function(){return"function"==typeof n?n(o):{...o,...n}}),[o,n])}function c(n){let o;return o=n.disableParentContext?"function"==typeof n.components?n.components(t):n.components||t:s(n.components),i.createElement(r.Provider,{value:o},n.children)}}}]);