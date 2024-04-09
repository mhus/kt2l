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
                context.getProperties().put(RunCompiler.PROP_RETURN, "closed");
                break;
            }
            MThread.sleep(100);
            if (content != null && run.contentContains(content)) {
                context.getProperties().put(RunCompiler.PROP_RETURN, "content");
                break;
            }
            if (content != null && run.stdinContains(stdin)) {
                context.getProperties().put(RunCompiler.PROP_RETURN, "stdin");
                break;
            }
            if (content != null && run.stderrContains(stderr)) {
                context.getProperties().put(RunCompiler.PROP_RETURN, "stderr");
                break;
            }
            if (System.currentTimeMillis() - start > timeout) {
                context.getProperties().put(RunCompiler.PROP_RETURN, "timeout");
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
