"use strict";(self.webpackChunkKT2L=self.webpackChunkKT2L||[]).push([[6399],{5509:(e,n,t)=>{t.r(n),t.d(n,{assets:()=>d,contentTitle:()=>s,default:()=>p,frontMatter:()=>o,metadata:()=>l,toc:()=>c});var i=t(5893),a=t(1151);const o={sidebar_position:8},s="Desktop",l={id:"installation/desktop",title:"Desktop",description:"The desktop is a bundled server version with a browser frontend and a java SDK.",source:"@site/docs/installation/desktop.md",sourceDirName:"installation",slug:"/installation/desktop",permalink:"/docs/installation/desktop",draft:!1,unlisted:!1,editUrl:"https://github.com/mhus/kt2l/blob/main/docs/docs/installation/desktop.md",tags:[],version:"current",sidebarPosition:8,frontMatter:{sidebar_position:8},sidebar:"tutorialSidebar",previous:{title:"Server",permalink:"/docs/installation/server"},next:{title:"Configuration",permalink:"/docs/category/configuration"}},d={},c=[{value:"Mac OSX Bundle",id:"mac-osx-bundle",level:2},{value:"Windows Bundle",id:"windows-bundle",level:2},{value:"Linux DEB",id:"linux-deb",level:2}];function r(e){const n={code:"code",h1:"h1",h2:"h2",img:"img",p:"p",pre:"pre",...(0,a.a)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(n.h1,{id:"desktop",children:"Desktop"}),"\n",(0,i.jsx)(n.p,{children:"The desktop is a bundled server version with a browser frontend and a java SDK.\nThey are all together bundled to a desktop application for the target OS."}),"\n",(0,i.jsx)(n.h2,{id:"mac-osx-bundle",children:"Mac OSX Bundle"}),"\n",(0,i.jsx)(n.p,{children:"The bundle is a dmg image file which contains the application. If you install\nthe application in /Applications you can run it like each other application."}),"\n",(0,i.jsxs)(n.p,{children:["It will use autologin and create a ",(0,i.jsx)(n.code,{children:".kt2l"})," directory in user home by default."]}),"\n",(0,i.jsxs)(n.p,{children:["The first time you start the application MacOS will warn you that the application\nis from an unknown developer and it's not possible to open it. You can open it\nusing the ",(0,i.jsx)(n.code,{children:"command"})," key and the ",(0,i.jsx)(n.code,{children:"right mouse button"})," and select open. The you\nwill see the same warning but you have the option to open it anyway."]}),"\n",(0,i.jsx)(n.p,{children:(0,i.jsx)(n.img,{alt:"MacOSX Warning",src:t(9934).Z+"",width:"744",height:"1076"})}),"\n",(0,i.jsxs)(n.p,{children:["Click ",(0,i.jsx)(n.code,{children:"Open"})," to start the application. The next time you start the application you\ncan start it like each other application."]}),"\n",(0,i.jsx)(n.h2,{id:"windows-bundle",children:"Windows Bundle"}),"\n",(0,i.jsx)(n.p,{children:"The bundle is a exe file which contains the application. If you download it\nyou can use it as native windows application."}),"\n",(0,i.jsxs)(n.p,{children:["It will use autologin and create a ",(0,i.jsx)(n.code,{children:".kt2l"})," directory in user home by default."]}),"\n",(0,i.jsx)(n.h2,{id:"linux-deb",children:"Linux DEB"}),"\n",(0,i.jsx)(n.p,{children:"Linux distributions supporting deb packages can use the deb package to install the desktop.\nTo install the latest snapshot version of the desktop download the DEB file from the website and\ninstall it using"}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-bash",children:"sudo apt install ./Download/kt2l-desktop-linux-amd64_...deb\n"})}),"\n",(0,i.jsx)(n.p,{children:"Now the application can be startet with"}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-bash",children:"/opt/kt2l-desktop/bin/kt2l-desktop &\n"})}),"\n",(0,i.jsx)(n.p,{children:"Before you update remove the package with"}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-bash",children:"sudo apt remove kt2l-desktop\n"})}),"\n",(0,i.jsx)(n.p,{children:"WIP: To use a desktop icon install (once) a desktop file:"}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-bash",children:'echo "[Desktop Entry]\nName=KT2L\nComment=KT2L Desktop\nTerminal=false\nExec=/opt/kt2l-deskop/bin/kt2l-desktop\nIcon=/opt/kt2l-desktop/lib/kt2l-desktop.png\nCategories=Utility;Security;\nType=Application\n" > kt2l-desktop.desktop\n\ndesktop-file-install ./kt2l-desktop.desktop --dir=~/.local/share/applications\nupdate-desktop-database ~/.local/share/applications\n'})})]})}function p(e={}){const{wrapper:n}={...(0,a.a)(),...e.components};return n?(0,i.jsx)(n,{...e,children:(0,i.jsx)(r,{...e})}):r(e)}},9934:(e,n,t)=>{t.d(n,{Z:()=>i});const i=t.p+"assets/images/macosx-warn-01-5fccca5aa6ca4a8b50d05a5be00c0ec3.png"},1151:(e,n,t)=>{t.d(n,{Z:()=>l,a:()=>s});var i=t(7294);const a={},o=i.createContext(a);function s(e){const n=i.useContext(o);return i.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function l(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(a):e.components||a:s(e.components),i.createElement(o.Provider,{value:n},e.children)}}}]);