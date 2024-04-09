package de.mhus.kt2l.kscript;

import de.mhus.commons.tree.IReadonly;

public class CmdSend extends Cmd {
    @Override
    void run(RunContext context, IReadonly args) throws Exception {
        String line = args.getString("line").orElse(null);
        if (line != null) {
            context.getScope(scope).send(line + "\n");
        }
        String msg = args.getString("msg").orElse(null);
        if (msg != null) {
            context.getScope(scope).send(msg);
        }
    }

    @Override
    protected Block init() {
        return block;
    }
}
