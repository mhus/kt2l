/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.help;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.core.Core;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public abstract class AbstractGitSnippetsHelpPanel extends VerticalLayout {
    protected final Core core;
    protected final HelpConfiguration.HelpLink link;

    @Autowired
    private SnippetsService snippetsService;
    @Autowired
    private ViewsConfiguration viewsConfiguration;

    protected List<SnippetsService.Snippet> snippets = null;
    protected TextField search;
    protected VerticalLayout content;
    @Getter
    private final String codeType;
    private long maxVisibleResults = 30;

    public AbstractGitSnippetsHelpPanel(String codeType, Core core, HelpConfiguration.HelpLink link) {
        this.codeType = codeType;
        this.core = core;
        this.link = link;
    }

    public void init() {
        addClassName("snipped-help-panel");
        maxVisibleResults = viewsConfiguration.getConfig("snippets").getInt("maxVisibleResults", 30);

        var repo = link.getNode().getString("repo");
        if (repo.isEmpty()) {
            LOGGER.warn("No repo defined for help link {}", link);
            return;
        }
        var branch = link.getNode().getString("branch", "main");
        var path = link.getNode().getString("path");
        if (path.isEmpty()) {
            LOGGER.warn("No path defined for help link {}", link);
            return;
        }

        initUi();
        Thread.startVirtualThread(() -> {
            snippets = snippetsService.getSnippets(repo.get(), branch, path.get(), codeType).getSnippets();
            core.ui().access(() -> {
                updateContent();
            });
        });
    }

    protected void initUi() {
        setPadding(false);
        setMargin(false);
        search = new TextField("Search");
        search.addKeyPressListener(Key.ENTER, e -> {
            updateContent();
        });
        search.setWidthFull();
        add(search);
        content = new VerticalLayout();
        content.setSizeFull();
        add(content);
        content.add(new Text("Loading..."));
    }

    private void updateContent() {
        if (snippets == null) return;
        content.removeAll();
        var text = search.getValue().toLowerCase();
        snippets.stream()
                .filter(s -> filterSnippet(s, text))
                .limit(maxVisibleResults)
                .forEach(s -> {
                    addContentEntry(s);
                });
    }

    protected boolean filterSnippet(SnippetsService.Snippet snippet, String text) {
        return  snippet.title().toLowerCase().contains(text) || snippet.description().toLowerCase().contains(text) || snippet.snippet().toLowerCase().contains(text) || snippet.tags().toLowerCase().contains(text);
    }

    protected void addContentEntry(SnippetsService.Snippet snippet) {
        content.add(new Text(snippet.description()));
        var button = new Button(snippet.title());
        button.addClickListener(c -> {
            transferContent(snippet.snippet());
        });
        content.add(button);
    }

    protected abstract void transferContent(String snippet);

}
