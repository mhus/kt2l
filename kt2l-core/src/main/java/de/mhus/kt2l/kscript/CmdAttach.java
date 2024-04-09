package de.mhus.kt2l.kscript;

import de.mhus.commons.tree.IReadonly;
import io.kubernetes.client.Attach;

public class CmdAttach extends Cmd {

    @Override
    public void run(RunContext context, IReadonly args) throws Exception {

        var useStdin = args.getBoolean("stdin", true);
        var useTty = args.getBoolean("tty", true);

        Attach attach = new Attach(context.getApi().getApiClient());
        var result = attach.attach(
                context.getPod(),
                context.getProperties().getString(RunCompiler.PROP_CONTAINER).orElse(""),
                useStdin, useTty);

        context.setScope(scope, new AttachScope(context, result));

    }

    @Override
    protected Block init() {
        return block;
    }

}
