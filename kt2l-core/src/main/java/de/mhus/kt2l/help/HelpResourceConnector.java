package de.mhus.kt2l.help;

public interface HelpResourceConnector {
    String getHelpContent();
    boolean canSetHelpContent();
    void setHelpContent(String content);
}
