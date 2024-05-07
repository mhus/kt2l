package de.mhus.kt2l.help;

import de.mhus.commons.tools.MString;
import de.mhus.kt2l.core.Core;

import static de.mhus.commons.tools.MString.isSetTrim;

public class YamlSnippetsHelpPanel extends AbstractGitSnippetsHelpPanel {

    public YamlSnippetsHelpPanel(Core core, HelpConfiguration.HelpLink link) {
        super("yaml", core, link);
    }

    @Override
    protected void transferContent(String snippet) {
        var current = HelpUtil.getHelpResourceConnector(core).get().getHelpContent();
        if (current == null) return;

        if (MString.isEmptyTrim(current)) {
            HelpUtil.setResourceContent(core, snippet);
            return;
        }
        var pos = HelpUtil.getHelpResourceConnector(core).get().getHelpCursorPos();
        if (pos < 0) {
            HelpUtil.setResourceContent(core, current + "\n---\n" + snippet);
            return;
        }
        var before = current.substring(0, pos);
        var after = current.substring(pos);

        HelpUtil.setResourceContent(core, (isSetTrim(before) ? before + "\n---\n" : "") + snippet + (isSetTrim(after) ? "\n---\n" + after : ""));
    }
}
