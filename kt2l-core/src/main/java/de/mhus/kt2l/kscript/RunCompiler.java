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

import de.mhus.commons.tools.MObject;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tree.IProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RunCompiler {

    public static final String PROP_SHELL = "shell";
    public static final String PROP_CONTAINER = "container";
    public static final String SCOPE_DEFAULT = "default";
    public static final String PROP_SCOPE = "scope.";

    public static final int DEFAULT_MAX_CONTENT_SIZE = 10000;
    public static final String PROP_RETURN = ".return";

    private Map<String,Class<? extends Cmd>> commands = new HashMap<>();

    public RunCompiler() {
        commands.put("exec", CmdExec.class);
        commands.put("wait", CmdWait.class);
        commands.put("sleep", CmdSleep.class);
        commands.put("echo", CmdEcho.class);
        commands.put("send", CmdSend.class);
        commands.put("close", CmdClose.class);
        commands.put("clear", CmdClear.class);
        commands.put("set", CmdSet.class);
        commands.put("env", CmdEnv.class);
        commands.put("attach", CmdAttach.class);
        commands.put("if", CmdIf.class);
        commands.put("else", CmdElse.class);
        commands.put("elseif", CmdElseIf.class);
        commands.put("endif", CmdEndIf.class);
        commands.put("download", CmdDownload.class);
        commands.put("debug", CmdDebug.class);
    }

    public Block compile(String command) throws Exception {
        var block = new Block();
        var firstBlock = block;
        int lineCnt = 0;
        String lastLine = null;
        for (String line : command.split("\n")) {
            lineCnt++;
            line = line.trim();
            if (lastLine != null) {
                line = lastLine + line;
                lastLine = null;
            }
            if (line.length() == 0 || line.startsWith("#")) continue;
            if (line.endsWith("\\")) {
                lastLine = line.substring(0, line.length()-1);
                continue;
            }

            Class<? extends Cmd> cmdClass = null;
            String cmdArgs = null;
            String cmdScope = SCOPE_DEFAULT;
            if (line.startsWith("!")) {
                String[] parts = line.substring(1).trim().split(" ", 2);
                cmdArgs = parts.length > 1 ? parts[1].trim() : "";
                String cmdName = parts[0].trim().toLowerCase();
                int pos = cmdName.indexOf('.');
                if (pos > 0) {
                    cmdScope = cmdName.substring(pos + 1);
                    cmdName = cmdName.substring(0, pos);
                }

                cmdClass = commands.get(cmdName);
                if (cmdClass == null) {
                    throw new RuntimeException("Command not found in line " + lineCnt + ": " + parts[0]);
                }
            } else {
                cmdClass = CmdExec.class;
                cmdArgs = "cmd=\"" + MString.encodeDoubleQuotes(line) + "\"";
            }
            var cmd = MObject.newInstance(cmdClass);
            cmd.addToParent(block);
            var args = IProperties.toProperties(cmdArgs);
            block = cmd.init(block, lineCnt, cmdScope, args);
        }
        return firstBlock;
    }

}
