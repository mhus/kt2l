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
import de.mhus.commons.tools.MFile;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.resources.GitProgressMonitor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public abstract class AbstractGitSnippetsHelpPanel extends VerticalLayout {
    protected final Core core;
    protected final HelpConfiguration.HelpLink link;

    @Autowired
    private Configuration configuration;
    @Autowired
    private ViewsConfiguration viewsConfiguration;
    private File snippetPath;

    protected final List<Snippet> snippets = new LinkedList<>();
    protected TextField search;
    protected VerticalLayout content;
    private final String codeType;
    private long maxVisibleResults = 30;

    public AbstractGitSnippetsHelpPanel(String codeType, Core core, HelpConfiguration.HelpLink link) {
        this.codeType = codeType;
        this.core = core;
        this.link = link;
    }

    public void init() {
        maxVisibleResults = viewsConfiguration.getConfig("snippets").getInt("maxVisibleResults", 30);
        initGit();
        loadSnippets();

        initUi();
        updateContent();

    }

    private void loadSnippets() {
        if (snippetPath == null) return;
        if (!snippetPath.exists()) {
            LOGGER.warn("Snippet not found {}", snippetPath);
            return;
        }
        LOGGER.debug("Load snippet {}", snippetPath);
        try {
            MFile.findAllFiles(snippetPath, (f) -> f.getName().endsWith(".md")).forEach(file -> {
                LOGGER.debug("Load snippet {}", file);
                var content = MFile.readFile(file);
                var snippet = loadSnippet(codeType, content);
                if (snippet != null) {
                    snippets.add(snippet);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Load snippet failed", e);
        }
    }

    public static Snippet loadSnippet(String codeType, String content) {
        Pattern pattern = Pattern.compile("# (.*?)\n");
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            LOGGER.warn("Title not found in snippet");
            return null;
        }
        var title = matcher.group(1).trim();

        pattern = Pattern.compile("# (.*?)\n(.*?)```"+codeType, Pattern.DOTALL);
        matcher = pattern.matcher(content);
        if (!matcher.find()) {
            LOGGER.warn("Snippet not found in snippet");
            return null;
        }
        var description = matcher.group(2).trim();

        pattern = Pattern.compile("```"+codeType+"(.*?)```", Pattern.DOTALL);
        matcher = pattern.matcher(content);
        if (!matcher.find()) {
            LOGGER.warn("Snippet not found in snippet");
            return null;
        }
        var snippet = matcher.group(1).trim();

        pattern = Pattern.compile("```"+codeType+"(.*?)```(.*?)$", Pattern.DOTALL);
        matcher = pattern.matcher(content);
        var tags = matcher.find() ? matcher.group(2).trim() : "";

        return new Snippet(title, description, snippet, tags);
    }

    private void initGit() {
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

        var targetDir = new File(configuration.getTmpDirectoryFile(), "git_" + MFile.normalize(repo.get()) + "_" + MFile.normalize(branch) );
        if (!targetDir.exists()) {
            LOGGER.info("Clone {} to {}", repo, targetDir);
            try (Git result = Git.cloneRepository()
                    .setURI(repo.get())
                    .setBranch(branch)
                    .setDirectory(targetDir)
                    .setProgressMonitor(new GitProgressMonitor())
                    .call()) {
                // Note: the call() returns an opened repository already which needs to be closed to avoid file handle leaks!
                LOGGER.info("Having repository: " + result.getRepository().getDirectory());
            } catch (Exception e) {
                LOGGER.error("Clone failed", e);
            }
        } else {
            LOGGER.info("Pull {}", repo);
            try (Git git = Git.open(targetDir)) {
                git.pull().setProgressMonitor(new GitProgressMonitor()).call();
            } catch (Exception e) {
                LOGGER.error("Pull failed", e);
            }
        }

        snippetPath = new File(targetDir, path.get());
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
    }

    private void updateContent() {
        content.removeAll();
        var text = search.getValue().toLowerCase();
        snippets.stream()
                .filter(s -> filterSnippet(s, text))
                .limit(maxVisibleResults)
                .forEach(s -> {
                    addContentEntry(s);
                });
    }

    protected boolean filterSnippet(Snippet snippet, String text) {
        return  snippet.title().toLowerCase().contains(text) || snippet.description().toLowerCase().contains(text) || snippet.snippet().toLowerCase().contains(text) || snippet.tags().toLowerCase().contains(text);
    }

    protected void addContentEntry(Snippet snippet) {
        content.add(new Text(snippet.description()));
        var button = new Button(snippet.title());
        button.addClickListener(c -> {
            transferContent(snippet.snippet());
        });
        content.add(button);
    }

    protected abstract void transferContent(String snippet);

    public record Snippet(String title, String description, String snippet, String tags) {
    }

}
