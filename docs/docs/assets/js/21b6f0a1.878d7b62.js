"use strict";(self.webpackChunkKT2L=self.webpackChunkKT2L||[]).push([[8850],{7840:(e,n,o)=>{o.r(n),o.d(n,{assets:()=>c,contentTitle:()=>s,default:()=>l,frontMatter:()=>t,metadata:()=>a,toc:()=>u});var i=o(5893),r=o(1151);const t={sidebar_position:1,title:"Authorization"},s="Authorization Configuration",a={id:"configuration/config-aaa",title:"Authorization",description:"The authorization configuration is used to configure the role permissions on resources. The configuration is stored in the config/aaa.yaml file.",source:"@site/docs/configuration/config-aaa.md",sourceDirName:"configuration",slug:"/configuration/config-aaa",permalink:"/docs/configuration/config-aaa",draft:!1,unlisted:!1,editUrl:"https://github.com/mhus/kt2l/blob/main/docs/docs/configuration/config-aaa.md",tags:[],version:"current",sidebarPosition:1,frontMatter:{sidebar_position:1,title:"Authorization"},sidebar:"tutorialSidebar",previous:{title:"Configuration",permalink:"/docs/category/configuration"},next:{title:"Configuration Files",permalink:"/docs/configuration/files"}},c={},u=[];function d(e){const n={code:"code",h1:"h1",li:"li",p:"p",pre:"pre",ul:"ul",...(0,r.a)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(n.h1,{id:"authorization-configuration",children:"Authorization Configuration"}),"\n",(0,i.jsxs)(n.p,{children:["The authorization configuration is used to configure the role permissions on resources. The configuration is stored in the ",(0,i.jsx)(n.code,{children:"config/aaa.yaml"})," file."]}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-yaml",children:"default:\n  resource: READ\n  namespace: READ\n  cluster: READ\n  cluster_action: READ\nresource_action:\nresource:\nresource_grid:\nnamespace:\ncluster:\n"})}),"\n",(0,i.jsxs)(n.p,{children:["In the sections are resource types. Under each are the permissions for the resource\nids. The ",(0,i.jsx)(n.code,{children:"default"})," section is used for the default permissions for types.\nDefine a resource id and a comma separated list of roles. The roles are the permissions\nthat are needed to access the resource with the given id."]}),"\n",(0,i.jsxs)(n.p,{children:["Some resource types have default actions define for each resource itself other onec\nneed a default configured in the ",(0,i.jsx)(n.code,{children:"default"})," section."]}),"\n",(0,i.jsxs)(n.p,{children:["The resource id can be a class name or a specific id. In case of ",(0,i.jsx)(n.code,{children:"resource"})," is the\nkubernetes resource plural. In case of ",(0,i.jsx)(n.code,{children:"namespace"})," is the namespace name."]}),"\n",(0,i.jsx)(n.p,{children:"Special resource ids are:"}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsx)(n.li,{children:'resource_action: "de.mhus.kt2l.resources.ResourceDetailsPanel_write": WRITE'}),"\n"]})]})}function l(e={}){const{wrapper:n}={...(0,r.a)(),...e.components};return n?(0,i.jsx)(n,{...e,children:(0,i.jsx)(d,{...e})}):d(e)}},1151:(e,n,o)=>{o.d(n,{Z:()=>a,a:()=>s});var i=o(7294);const r={},t=i.createContext(r);function s(e){const n=i.useContext(t);return i.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function a(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(r):e.components||r:s(e.components),i.createElement(t.Provider,{value:n},e.children)}}}]);