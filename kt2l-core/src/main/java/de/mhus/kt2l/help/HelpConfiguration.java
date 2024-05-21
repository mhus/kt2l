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

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.commons.tree.TreeNodeList;
import de.mhus.kt2l.config.AbstractUserRelatedConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HelpConfiguration extends AbstractUserRelatedConfig {

    public String getWindowWidth() {
        return config().getString("windowWidth", "300px");
    }

    public boolean isEnabled() {
        return config().getBoolean("enabled", true);
    }

    public HelpConfiguration() {
        super("help");
    }

    public HelpContext getContext(String name) {
        return new HelpContext(
                config().getObject("contexts").orElse(MTree.EMPTY_MAP)
                        .getArray(name).orElse(MTree.EMPTY_LIST));
    }


    public static class HelpContext {

        private final TreeNodeList context;
        private final Map<String, HelpLink> links = new HashMap<>();
        private final List<String> order = new ArrayList<>();

        private HelpContext(TreeNodeList context) {
            this.context = context;
            context.forEach(o -> {
                var link = new HelpLink(o);
                links.put(link.getName(), link);
                order.add(link.getName());
            });
        }

        public List<HelpLink> getLinks() {
            return order.stream().map(name -> links.get(name)).toList();
        }

    }

    public static class HelpLink {

        private final ITreeNode link;
        private HelpAction helpAction;

        private HelpLink(ITreeNode link) {
            this.link = link;
        }

        public String getName() {
            return link.getString("name").get();
        }

        public String getAction() {
            return link.getString("action").orElse("not specified");
        }

        public ITreeNode getNode() {
            return link;
        }

        public boolean isEnabled() {
            return link.getBoolean("enabled", true);
        }

        public boolean isDefault() {
            return link.getBoolean("default", true);
        }

        public void setHelpAction(HelpAction helpAction) {
            this.helpAction = helpAction;
        }

        public HelpAction getHelpAction() {
            return helpAction;
        }
    }
}
