package de.mhus.kt2l.kscript;

import de.mhus.commons.tree.IReadonly;

public class CmdEcho extends Cmd {
    @Override
    void run(RunContext context, IReadonly args) throws Exception {
        context.addContent(args.getString("msg").orElse(""));
    }

    @Override
    protected Block init() {
        return block;
    }
}
