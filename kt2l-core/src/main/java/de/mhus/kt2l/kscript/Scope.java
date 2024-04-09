package de.mhus.kt2l.kscript;

import java.io.Closeable;
import java.io.IOException;

public abstract class Scope implements Closeable {
    public abstract boolean isRunning();

    public abstract boolean contentContains(String search);

    public abstract boolean stdinContains(String search);

    public abstract boolean stderrContains(String search);

    public abstract void send(String msg) throws IOException;

    public abstract void clear();
}
