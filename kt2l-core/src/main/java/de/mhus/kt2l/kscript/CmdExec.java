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
        Exec exec = new Exec(context.getApi().getApiClient());
        var proc = exec.exec(
                context.getPod(),
                cmdArray,
                context.getProperties().getString(RunCompiler.PROP_CONTAINER).orElse(""),
                useStdin, useTty);

        context.setScope(scope, new ExecScope(context, proc));

    }

    @Override
    protected Block init() {
        return block;
    }

}
