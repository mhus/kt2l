"use strict";(self.webpackChunkKT2L=self.webpackChunkKT2L||[]).push([[9970],{2862:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>c,contentTitle:()=>r,default:()=>u,frontMatter:()=>o,metadata:()=>s,toc:()=>d});var a=n(5893),i=n(1151);const o={sidebar_position:7,title:"Navigation"},r="Navigation",s={id:"contribute/navigation",title:"Navigation",description:"The mainView provides a navigation bar at the left side of the screen. The navigation bar is a Tab component that",source:"@site/docs/contribute/navigation.md",sourceDirName:"contribute",slug:"/contribute/navigation",permalink:"/docs/contribute/navigation",draft:!1,unlisted:!1,editUrl:"https://github.com/mhus/kt2l/blob/main/docs/docs/contribute/navigation.md",tags:[],version:"current",sidebarPosition:7,frontMatter:{sidebar_position:7,title:"Navigation"},sidebar:"tutorialSidebar",previous:{title:"Kubernetes Resource Support",permalink:"/docs/contribute/k8s-resource-support"},next:{title:"Cluster Actions",permalink:"/docs/contribute/cluster-action"}},c={},d=[];function l(e){const t={code:"code",h1:"h1",p:"p",pre:"pre",...(0,i.a)(),...e.components};return(0,a.jsxs)(a.Fragment,{children:[(0,a.jsx)(t.h1,{id:"navigation",children:"Navigation"}),"\n",(0,a.jsxs)(t.p,{children:["The mainView provides a navigation bar at the left side of the screen. The navigation bar is a Tab component that\ndisplays the available views. To add a new view to the navigation bar, you need to use the ",(0,a.jsx)(t.code,{children:"PanelService"})," to register\nnew views."]}),"\n",(0,a.jsx)(t.pre,{children:(0,a.jsx)(t.code,{className:"language-java",children:'panelService.addPanel(\n        context.getSelectedTab(),\n        context.getClusterConfiguration().name() + ":" + selected.getMetadata().getNamespace() + "." + selected.getMetadata().getName() + ":logs",\n        selected.getMetadata().getName(),\n        true,\n        VaadinIcon.FORWARD.create(),\n        () ->\n                new PodExecPanel(\n                        context.getClusterConfiguration(),\n                        context.getApi(),\n                        context.getMainView(),\n                        containers\n                )).setHelpContext("exec").select();\n'})})]})}function u(e={}){const{wrapper:t}={...(0,i.a)(),...e.components};return t?(0,a.jsx)(t,{...e,children:(0,a.jsx)(l,{...e})}):l(e)}},1151:(e,t,n)=>{n.d(t,{Z:()=>s,a:()=>r});var a=n(7294);const i={},o=a.createContext(i);function r(e){const t=a.useContext(o);return a.useMemo((function(){return"function"==typeof e?e(t):{...t,...e}}),[t,e])}function s(e){let t;return t=e.disableParentContext?"function"==typeof e.components?e.components(i):e.components||i:r(e.components),a.createElement(o.Provider,{value:t},e.children)}}}]);