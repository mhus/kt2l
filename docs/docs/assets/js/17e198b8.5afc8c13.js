"use strict";(self.webpackChunkKT2L=self.webpackChunkKT2L||[]).push([[7134],{766:(e,n,t)=>{t.r(n),t.d(n,{assets:()=>l,contentTitle:()=>i,default:()=>u,frontMatter:()=>s,metadata:()=>r,toc:()=>a});var o=t(5893),c=t(1151);const s={sidebar_position:2,title:"Clusters Configuration"},i="Clusters Configuration",r={id:"configuration/config-clusters",title:"Clusters Configuration",description:"In the file clusters.yaml you can define the clusters you want to use in the application. By default, the",source:"@site/docs/configuration/config-clusters.md",sourceDirName:"configuration",slug:"/configuration/config-clusters",permalink:"/docs/configuration/config-clusters",draft:!1,unlisted:!1,editUrl:"https://github.com/mhus/kt2l/blob/main/docs/docs/configuration/config-clusters.md",tags:[],version:"current",sidebarPosition:2,frontMatter:{sidebar_position:2,title:"Clusters Configuration"},sidebar:"tutorialSidebar",previous:{title:"Configuration Files",permalink:"/docs/configuration/files"},next:{title:"Local command execution",permalink:"/docs/configuration/config-cmd"}},l={},a=[];function d(e){const n={code:"code",h1:"h1",li:"li",p:"p",pre:"pre",ul:"ul",...(0,c.a)(),...e.components};return(0,o.jsxs)(o.Fragment,{children:[(0,o.jsx)(n.h1,{id:"clusters-configuration",children:"Clusters Configuration"}),"\n",(0,o.jsxs)(n.p,{children:["In the file ",(0,o.jsx)(n.code,{children:"clusters.yaml"})," you can define the clusters you want to use in the application. By default, the\nconfiguration can be overwritten in the user config directory. Set the environment variable\n",(0,o.jsx)(n.code,{children:"KT2L_PROTECTED_CLUSTERS_CONFIG"})," to ",(0,o.jsx)(n.code,{children:"true"})," to protect the configuration from being overwritten."]}),"\n",(0,o.jsx)(n.pre,{children:(0,o.jsx)(n.code,{className:"language-yaml",children:'defaultCluster: colima\ndefaultResourceType: "pods"\ndefaultNamespace: default\nclusters:\n  - name: colima\n    title: "Colima"\n    enabled: true\n    color: green\n'})}),"\n",(0,o.jsxs)(n.p,{children:["The example shows the configuration for the cluster ",(0,o.jsx)(n.code,{children:"colima"}),"."]}),"\n",(0,o.jsxs)(n.p,{children:["The following colors are available: ",(0,o.jsx)(n.code,{children:"red"}),", ",(0,o.jsx)(n.code,{children:"green"}),", ",(0,o.jsx)(n.code,{children:"blue"}),", ",(0,o.jsx)(n.code,{children:"yellow"}),", ",(0,o.jsx)(n.code,{children:"purple"}),", ",(0,o.jsx)(n.code,{children:"orange"}),", ",(0,o.jsx)(n.code,{children:"pink"}),", ",(0,o.jsx)(n.code,{children:"cyan"}),", ",(0,o.jsx)(n.code,{children:"gray"}),"."]}),"\n",(0,o.jsx)(n.p,{children:"In the cluster section you can define the following properties:"}),"\n",(0,o.jsxs)(n.ul,{children:["\n",(0,o.jsxs)(n.li,{children:[(0,o.jsx)(n.code,{children:"name"}),": The name of the cluster"]}),"\n",(0,o.jsxs)(n.li,{children:[(0,o.jsx)(n.code,{children:"title"}),": The title of the cluster"]}),"\n",(0,o.jsxs)(n.li,{children:[(0,o.jsx)(n.code,{children:"enabled"}),": If the cluster is enabled"]}),"\n",(0,o.jsxs)(n.li,{children:[(0,o.jsx)(n.code,{children:"color"}),": The color of the cluster title"]}),"\n",(0,o.jsxs)(n.li,{children:[(0,o.jsx)(n.code,{children:"defaultNamespace"}),": The default namespace for the cluster"]}),"\n",(0,o.jsxs)(n.li,{children:[(0,o.jsx)(n.code,{children:"defaultResourceType"}),": The default resource type for the cluster"]}),"\n",(0,o.jsxs)(n.li,{children:[(0,o.jsx)(n.code,{children:"shell"}),": A list of pods and the shell command to connect to the pod"]}),"\n"]}),"\n",(0,o.jsxs)(n.p,{children:["If you are inside a kubernetes cluster, the cluster name is ",(0,o.jsx)(n.code,{children:".local-cluster"}),"."]}),"\n",(0,o.jsxs)(n.p,{children:["If you want to reduce the number of clusters in the cluster list, you can configure the access rights in ",(0,o.jsx)(n.code,{children:"aaa.yaml"}),"\nfor cluster resources."]})]})}function u(e={}){const{wrapper:n}={...(0,c.a)(),...e.components};return n?(0,o.jsx)(n,{...e,children:(0,o.jsx)(d,{...e})}):d(e)}},1151:(e,n,t)=>{t.d(n,{Z:()=>r,a:()=>i});var o=t(7294);const c={},s=o.createContext(c);function i(e){const n=o.useContext(s);return o.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function r(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(c):e.components||c:i(e.components),o.createElement(s.Provider,{value:n},e.children)}}}]);