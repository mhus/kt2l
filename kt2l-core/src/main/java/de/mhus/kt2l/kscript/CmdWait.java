/**
 * This file is part of kt2l-core.
 *
 * kt2l-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * kt2l-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with kt2l-core.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.kscript;

import de.mhus.commons.tools.MPeriod;
import de.mhus.commons.tools.MThread;
import de.mhus.commons.tree.IReadonly;

import java.util.concurrent.TimeoutException;

public class CmdWait extends Cmd {

    @Override
    public void run(RunContext context, IReadonly args) throws TimeoutException {
        var timeout = MPeriod.toTime(args.getString("timeout").orElse("60000"), 60000);
        var content = args.getString("content", null);
        var stdin = args.getString("stdin", null);
        var stderr = args.getString("stderr", null);
        var throwExceptionOnTimeout = args.getBoolean("throwExceptionOnTimeout", false);
        var run = context.getScope(scope);
        if (run == null) {
            throw new RuntimeException("Scope not found: " + scope);
        }
        var start = System.currentTimeMillis();
        while (true) {
            if (!run.isRunning()){
                context.getProperties().put(scope + RunCompiler.PROP_RETURN, "closed");
                break;
            }
            MThread.sleep(100);
            if (content != null && run.contentContains(content)) {
                context.getProperties().put(scope + RunCompiler.PROP_RETURN, "content");
                break;
            }
            if (content != null && run.stdinContains(stdin)) {
                context.getProperties().put(scope + RunCompiler.PROP_RETURN, "stdin");
                break;
            }
            if (content != null && run.stderrContains(stderr)) {
                context.getProperties().put(scope + RunCompiler.PROP_RETURN, "stderr");
                break;
            }
            if (System.currentTimeMillis() - start > timeout) {
                context.getProperties().put(scope + RunCompiler.PROP_RETURN, "timeout");
                if (throwExceptionOnTimeout)
                    throw new TimeoutException("Timeout");
                break;
            }
        }
    }

    @Override
    protected Block init() {
        return block;
    }

}
