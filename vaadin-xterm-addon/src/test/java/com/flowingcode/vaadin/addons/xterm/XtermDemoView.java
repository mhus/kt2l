/*-
 * #%L
 * XTerm Console Addon
 * %%
 * Copyright (C) 2020 - 2023 Flowing Code
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.flowingcode.vaadin.addons.xterm;

import com.flowingcode.vaadin.addons.DemoLayout;
import com.flowingcode.vaadin.addons.GithubLink;
import com.flowingcode.vaadin.addons.xterm.ITerminalClipboard.UseSystemClipboard;
import com.flowingcode.vaadin.addons.xterm.ITerminalOptions.BellStyle;
import com.flowingcode.vaadin.addons.xterm.ITerminalOptions.CursorStyle;
import com.flowingcode.vaadin.addons.xterm.utils.TerminalKeyMapping;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;

@SuppressWarnings("serial")
@Route(value = "xterm", layout = DemoLayout.class)
@GithubLink("https://github.com/FlowingCode/XTermConsoleAddon")
public class XtermDemoView extends VerticalLayout {

  private XTerm echoXterm;

  public XtermDemoView() {
    setSizeFull();
    setPadding(false);
    getElement().getStyle().set("background", "black");

    echoXterm = new XTerm();
    echoXterm.setKeyboardSelectionEnabled(false);

    echoXterm.writeln("xterm add-on by Flowing Code S.A.\n\n");

    echoXterm.setCursorBlink(true);
    echoXterm.setCursorStyle(CursorStyle.UNDERLINE);
    echoXterm.setBellStyle(BellStyle.SOUND);

    echoXterm.setSizeFull();

    echoXterm.setCopySelection(true);
    echoXterm.setUseSystemClipboard(UseSystemClipboard.READWRITE);
    echoXterm.setPasteWithMiddleClick(true);
    echoXterm.setPasteWithRightClick(true);

    // TerminalHistory.extend(xterm);
    echoXterm.addAnyKeyListener(
        ev -> {
          System.out.println("Key: " + ev.getKey());
          echoXterm.write(TerminalKeyMapping.map(ev.getKey()));
        });

    echoXterm.focus();
    echoXterm.fit();

    TabSheet tabSheet = new TabSheet();
    tabSheet.setSizeFull();
    tabSheet.add("Echo", echoXterm);
    tabSheet.add("Bash", new VerticalLayout());

    add(tabSheet);
  }

}
