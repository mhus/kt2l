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

package de.mhus.kt2l.kscript;

import de.mhus.commons.tools.MThread;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class ExecScope extends Scope {
    private final StringBuffer content;
    private final StringBuffer stdin;
    private final StringBuffer stderr;
    private final int maxSize;
    private final RunContext context;
    private Thread stdinThread;
    private Thread stderrThread;
    private Process proc;

    public ExecScope(RunContext context, Process proc) {
        this.proc = proc;
        content = new StringBuffer();
        stdin = new StringBuffer();
        stderr = new StringBuffer();
        maxSize = context.getProperties().getInt("maxContentSize").orElse(RunCompiler.DEFAULT_MAX_CONTENT_SIZE);
        this.context = context;
        stdinThread = Thread.startVirtualThread(this::captureStdin);
        stderrThread = Thread.startVirtualThread(this::captureStderr);
        LOGGER.info("Scope created");
    }

    @Override
    public boolean isRunning() {
        return proc.isAlive();
    }

    private void captureStderr() {
        try {
            byte[] buffer = new byte[1024];
            final var is = proc.getErrorStream();
            while (true) {
                int len = is.read(buffer);
                if (len < 0) break;
                if (len == 0) {
                    MThread.sleep(100);
                } else {
                    String line = new String(buffer, 0, len);
                    stderr.append(line);
                    if (stderr.length() > maxSize)
                        stderr.delete(0, stderr.length() - maxSize);
                    content.append(line);
                    if (content.length() > maxSize)
                        content.delete(0, content.length() - maxSize);
                    context.addContent(line);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Capture Stderr", e);
        }
    }

    private void captureStdin() {
        try {
            byte[] buffer = new byte[1024];
            final var is = proc.getInputStream();
            while (true) {
                int len = is.read(buffer);
                if (len < 0) break;
                if (len == 0) {
                    MThread.sleep(100);
                } else {
                    String line = new String(buffer, 0, len);
                    stdin.append(line);
                    if (stdin.length() > maxSize)
                        stdin.delete(0, stdin.length() - maxSize);
                    content.append(line);
                    if (content.length() > maxSize)
                        content.delete(0, content.length() - maxSize);
                    context.addContent(line);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Capture Stderr", e);
        }
    }

    @Override
    public void close() throws IOException {
        if (stdinThread != null)
            stdinThread.interrupt();
        stdinThread = null;
        if (stderrThread != null)
            stderrThread.interrupt();
        stderrThread = null;
        if (proc != null)
            proc.destroy();
    }

    @Override
    public boolean contentContains(String search) {
        return content.indexOf(search) > -1;
    }

    @Override
    public boolean stdinContains(String search) {
        return stdin.indexOf(search) > -1;
    }

    @Override
    public boolean stderrContains(String search) {
        return stderr.indexOf(search) > -1;
    }

    @Override
    public void send(String msg) throws IOException {
        proc.getOutputStream().write(msg.getBytes());
    }

    @Override
    public void clear() {
        content.setLength(0);
        stdin.setLength(0);
        stderr.setLength(0);
    }

    @Override
    public String toString() {
        if (proc.isAlive())
            return "exec scope: " + content.length() + " bytes (running)";
        else
            return "exec scope: " + content.length() + " bytes";
    }
}
