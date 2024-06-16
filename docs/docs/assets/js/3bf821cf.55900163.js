"use strict";(self.webpackChunkKT2L=self.webpackChunkKT2L||[]).push([[9970],{2862:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>c,contentTitle:()=>s,default:()=>h,frontMatter:()=>a,metadata:()=>r,toc:()=>d});var i=n(5893),o=n(1151);const a={sidebar_position:7,title:"Navigation"},s="Navigation",r={id:"contribute/navigation",title:"Navigation",description:"The Core provides a navigation bar at the left side of the screen. The navigation bar is a Tab component that",source:"@site/docs/contribute/navigation.md",sourceDirName:"contribute",slug:"/contribute/navigation",permalink:"/docs/contribute/navigation",draft:!1,unlisted:!1,editUrl:"https://github.com/mhus/kt2l/blob/main/docs/docs/contribute/navigation.md",tags:[],version:"current",sidebarPosition:7,frontMatter:{sidebar_position:7,title:"Navigation"},sidebar:"tutorialSidebar",previous:{title:"Kubernetes Resource Support",permalink:"/docs/contribute/k8s-resource-support"},next:{title:"Cluster Actions",permalink:"/docs/contribute/cluster-action"}},c={},d=[{value:"Panels",id:"panels",level:2}];function l(e){const t={code:"code",h1:"h1",h2:"h2",p:"p",...(0,o.a)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(t.h1,{id:"navigation",children:"Navigation"}),"\n",(0,i.jsxs)(t.p,{children:["The Core provides a navigation bar at the left side of the screen. The navigation bar is a Tab component that\ndisplays the available views. To add a new view to the navigation bar, you need to use the ",(0,i.jsx)(t.code,{children:"PanelService"})," to register\nnew views."]}),"\n",(0,i.jsx)(t.h2,{id:"panels",children:"Panels"}),"\n",(0,i.jsxs)(t.p,{children:["If you create a new panel add the code to create the panel to the ",(0,i.jsx)(t.code,{children:"PanelService"}),". Use method signatures like ",(0,i.jsx)(t.code,{children:"show...Panel"}),"\nfor unique panels and otherwise ",(0,i.jsx)(t.code,{children:"add...Panel"}),"."]}),"\n",(0,i.jsxs)(t.p,{children:["Panels will be added to the navigation bar and the UI panel will be added to the content layout. If the tab\nis not selected the panel will be hidden. To save memory you can define the panel ",(0,i.jsx)(t.code,{children:"reproducable"}),". In this\ncase the panel will be removed from content while it is not selected."]}),"\n",(0,i.jsxs)(t.p,{children:["Add a help context and a help section in ",(0,i.jsx)(t.code,{children:"help.yaml"})," to provide help for the new panel."]})]})}function h(e={}){const{wrapper:t}={...(0,o.a)(),...e.components};return t?(0,i.jsx)(t,{...e,children:(0,i.jsx)(l,{...e})}):l(e)}},1151:(e,t,n)=>{n.d(t,{Z:()=>r,a:()=>s});var i=n(7294);const o={},a=i.createContext(o);function s(e){const t=i.useContext(a);return i.useMemo((function(){return"function"==typeof e?e(t):{...t,...e}}),[t,e])}function r(e){let t;return t=e.disableParentContext?"function"==typeof e.components?e.components(o):e.components||o:s(e.components),i.createElement(a.Provider,{value:t},e.children)}}}]);