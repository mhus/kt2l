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

import de.mhus.commons.tools.MFile;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.resources.GitProgressMonitor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SnippetsService {

    @Autowired
    private Configuration configuration;

    private Map<String, Snippets> snippets = new HashMap<>();

    public synchronized Snippets getSnippets(String gitRepo, String gitBranch, String gitPath, String codeType) {
        var gitPathNormalized = MFile.normalizePath(gitPath);
        var id = gitRepo + "/" + gitBranch + "/" + gitPathNormalized + "/" + codeType;
        Snippets out = snippets.computeIfAbsent(id, k -> {
            Snippets sn = new Snippets();
            sn.gitRepo = gitRepo;
            sn.gitBranch = gitBranch;
            sn.gitPath = gitPathNormalized;
            sn.codeType = codeType;
            sn.repoDir = checkGitRepo(gitRepo, gitBranch);
            sn.snippetPath = new File(sn.repoDir, gitPathNormalized);
            sn.reload();

            return sn;
        });
        return out;
    }

    private File checkGitRepo(String repo, String branch) {
        if (repo.isEmpty()) {
            LOGGER.warn("No repo defined");
            return null;
        }

        var targetDir = new File(configuration.getTmpDirectoryFile(), "git_" + MFile.normalize(repo) + "_" + MFile.normalize(branch) );
        if (!targetDir.exists()) {
            cloneRepository(repo, branch, targetDir);
        } else {
            LOGGER.info("Pull {}", repo);
            try (Git git = Git.open(targetDir)) {
                git.pull().setProgressMonitor(new GitProgressMonitor()).call();
            } catch (RepositoryNotFoundException e) {
                MFile.deleteDir(targetDir);
                cloneRepository(repo, branch, targetDir);
            } catch (Exception e) {
                LOGGER.error("Pull failed", e);
            }
        }

        return targetDir;
    }

    private static void cloneRepository(String repo, String branch, File targetDir) {
        LOGGER.info("Clone {} to {}", repo, targetDir);
        try (Git result = Git.cloneRepository()
                .setURI(repo)
                .setBranch(branch)
                .setDirectory(targetDir)
                .setProgressMonitor(new GitProgressMonitor())
                .call()) {
            // Note: the call() returns an opened repository already which needs to be closed to avoid file handle leaks!
            LOGGER.info("Having repository: " + result.getRepository().getDirectory());
        } catch (Exception e) {
            LOGGER.error("Clone failed", e);
        }
    }


    public static class Snippets {
        private File repoDir;
        @Setter
        private String codeType;
        private String gitRepo;
        private String gitBranch;
        private String gitPath;
        private File snippetPath;
        private final List<Snippet> snippets = new LinkedList<>();

        public void reload() {
            if (snippetPath == null) return;
            if (!snippetPath.exists()) {
                LOGGER.warn("Snippet not found {}", snippetPath);
                return;
            }
            LOGGER.debug("Load snippet {}", snippetPath);
            try {
                MFile.findAllFiles(snippetPath, (f) -> f.isDirectory() || f.getName().endsWith(".md")).forEach(file -> {
                    if (file.isDirectory()) return;
                    LOGGER.debug("Load snippet {}", file);
                    var content = MFile.readFile(file);
                    var snippet = loadSnippet(content);
                    if (snippet != null) {
                        snippets.add(snippet);
                    } else {
                        LOGGER.warn("Snippet not loaded from file {}", file);
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Load snippet failed", e);
            }
        }

        private void loadSnippets() {
            if (snippetPath == null) return;
            if (!snippetPath.exists()) {
                LOGGER.warn("Snippet not found {}", snippetPath);
                return;
            }
            LOGGER.debug("Load snippet {}", snippetPath);
            try {
                MFile.findAllFiles(snippetPath, (f) -> f.isDirectory() || f.getName().endsWith(".md")).forEach(file -> {
                    if (file.isDirectory()) return;
                    LOGGER.debug("Load snippet {}", file);
                    var content = MFile.readFile(file);
                    var snippet = loadSnippet(content);
                    if (snippet != null) {
                        snippets.add(snippet);
                    } else {
                        LOGGER.warn("Snippet not loaded from file {}", file);
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Load snippet failed", e);
            }
        }

        public Snippet loadSnippet(String content) {
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
                LOGGER.warn("Snippet not found in snippet for code type {}", codeType);
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

        public List<Snippet> getSnippets() {
            return List.copyOf(snippets);
        }
    }

    public record Snippet(String title, String description, String snippet, String tags) {
    }

}
