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

import de.mhus.commons.tree.IReadonly;
import io.kubernetes.client.Exec;

public class CmdExec extends Cmd {

    @Override
    public void run(RunContext context, IReadonly args) throws Exception {

        var useStdin = args.getBoolean("stdin", true);
        var useTty = args.getBoolean("tty", true);
        var execStr = args.getString("exec").orElse(null);
        var cmdStr = args.getString("cmd").orElse(null);
        var shell = args.getString("shell").orElse("/bin/bash");
        var cmdArray = execStr != null ? execStr.split(",") : cmdStr == null ? new String[] {shell} : new String[] {shell, "-c", cmdStr};
        Exec exec = new Exec(context.getApiProvider().getClient());
        var proc = exec.exec(
                context.getPod(),
                cmdArray,
                context.getProperties().getString(RunCompiler.PROP_CONTAINER).orElse(null),
                useStdin, useTty);

        context.setScope(scope, new ExecScope(context, proc));

    }

    @Override
    protected Block init() {
        return block;
    }

}
