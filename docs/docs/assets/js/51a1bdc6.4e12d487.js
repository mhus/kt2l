"use strict";(self.webpackChunkKT2L=self.webpackChunkKT2L||[]).push([[874],{4554:(e,n,o)=>{o.r(n),o.d(n,{assets:()=>c,contentTitle:()=>s,default:()=>d,frontMatter:()=>r,metadata:()=>l,toc:()=>a});var i=o(5893),t=o(1151);const r={sidebar_position:10,title:"Login"},s="Login Configuration",l={id:"configuration/config-login",title:"Login",description:"The configuration is used to configure the login process and the user interface.",source:"@site/docs/configuration/config-login.md",sourceDirName:"configuration",slug:"/configuration/config-login",permalink:"/docs/configuration/config-login",draft:!1,unlisted:!1,editUrl:"https://github.com/mhus/kt2l/blob/main/docs/docs/configuration/config-login.md",tags:[],version:"current",sidebarPosition:10,frontMatter:{sidebar_position:10,title:"Login"},sidebar:"tutorialSidebar",previous:{title:"AI Integration",permalink:"/docs/configuration/ai"},next:{title:"Pod Scorer",permalink:"/docs/configuration/config-pod_scorer"}},c={},a=[{value:"SSO Configuration",id:"sso-configuration",level:2}];function u(e){const n={a:"a",code:"code",h1:"h1",h2:"h2",li:"li",p:"p",pre:"pre",ul:"ul",...(0,t.a)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(n.h1,{id:"login-configuration",children:"Login Configuration"}),"\n",(0,i.jsxs)(n.p,{children:["The configuration is used to configure the login process and the user interface.\nThe configuration is stored in the ",(0,i.jsx)(n.code,{children:"config/login.yaml"})," file."]}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-yaml",children:"autoLogin: true\nautoLoginUser: autologin\nautoLoginLocalhostOnly: true\n"})}),"\n",(0,i.jsxs)(n.p,{children:[(0,i.jsx)(n.code,{children:"autoLogin"})," enables automatic login with the user specified in ",(0,i.jsx)(n.code,{children:"autoLoginUser"}),". If\n",(0,i.jsx)(n.code,{children:"autoLoginLocalhostOnly"})," is enabled, the auto login is only possible from localhost.\nOther IP sources will be prompted for login."]}),"\n",(0,i.jsxs)(n.p,{children:["The user specified in ",(0,i.jsx)(n.code,{children:"autoLoginUser"})," must be a known user. The user must be defined in the ",(0,i.jsx)(n.code,{children:"config/users.yaml"})," file."]}),"\n",(0,i.jsxs)(n.p,{children:["See also the ",(0,i.jsx)(n.a,{href:"config-users",children:"User Configuration"})," for more information about the user configuration."]}),"\n",(0,i.jsx)(n.h2,{id:"sso-configuration",children:"SSO Configuration"}),"\n",(0,i.jsx)(n.p,{children:"If you run the application as server you can use the single sign-on (SSO) feature to login with OAuth2 providers.\nTo configure the single sign-on (SSO) you can use the following configuration:"}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-yaml",children:"autoLogin: false\noauth2Enabled: true\noauth2Providers:\n  - id: google\n    roleMapping:\n        role: [READ]\noauth2AcceptEmails:\n  - pattern: '.*@gmail.com'\n    userConfigPreset: google\n    defaultRoles: [READ]\n"})}),"\n",(0,i.jsxs)(n.p,{children:["Disable autologint to show the login page. Enable ",(0,i.jsx)(n.code,{children:"oauth2Enabled"})," to enable the OAuth2 login. With\n",(0,i.jsx)(n.code,{children:"oauth2Providers"})," you can define the OAuth2 providers to use. The ",(0,i.jsx)(n.code,{children:"id"})," is the identifier of the provider\nand must be unique. The ",(0,i.jsx)(n.code,{children:"roleMapping"})," maps the roles of the provider to the roles of the user.\nThe ",(0,i.jsx)(n.code,{children:"oauth2AcceptEmails"})," is a list of email patterns that are accepted for login. The list will be\nprocesses from top to bottom. The ",(0,i.jsx)(n.code,{children:"userConfigPreset"})," is the user configuration preset to use for the user. It\nwill be created if no user configuration exists. The ",(0,i.jsx)(n.code,{children:"defaultRoles"})," are the roles that are assigned to the user."]}),"\n",(0,i.jsxs)(n.p,{children:["The ",(0,i.jsx)(n.code,{children:"oauth2Providers"})," configuration is provider specific. The following providers are supported:"]}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsx)(n.li,{children:"google"}),"\n",(0,i.jsx)(n.li,{children:"github"}),"\n",(0,i.jsx)(n.li,{children:"facebook"}),"\n"]}),"\n",(0,i.jsx)(n.p,{children:"You also need to set the environment variables for the OAuth2 configuration."}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsx)(n.li,{children:"GOOGLE_CLIENT_ID"}),"\n",(0,i.jsx)(n.li,{children:"GOOGLE_CLIENT_SECRET"}),"\n",(0,i.jsx)(n.li,{children:"GITHUB_CLIENT_ID"}),"\n",(0,i.jsx)(n.li,{children:"GITHUB_CLIENT_SECRET"}),"\n",(0,i.jsx)(n.li,{children:"FACEBOOK_CLIENT_ID"}),"\n",(0,i.jsx)(n.li,{children:"FACEBOOK_CLIENT_SECRET"}),"\n"]})]})}function d(e={}){const{wrapper:n}={...(0,t.a)(),...e.components};return n?(0,i.jsx)(n,{...e,children:(0,i.jsx)(u,{...e})}):u(e)}},1151:(e,n,o)=>{o.d(n,{Z:()=>l,a:()=>s});var i=o(7294);const t={},r=i.createContext(t);function s(e){const n=i.useContext(r);return i.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function l(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(t):e.components||t:s(e.components),i.createElement(r.Provider,{value:n},e.children)}}}]);