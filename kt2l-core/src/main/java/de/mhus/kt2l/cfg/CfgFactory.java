package de.mhus.kt2l.cfg;

public interface CfgFactory {

    String handledConfigType();

    CfgPanel createPanel();

    boolean isUserRelated();

    boolean isProtected();
}
