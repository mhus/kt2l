package de.mhus.kt2l.kscript;

import de.mhus.commons.tree.IReadonly;

public class CmdSet extends Cmd {
    @Override
    void run(RunContext context, IReadonly args) throws Exception {
        args.forEach((entry) -> {
            if (entry.getKey().startsWith(RunCompiler.PROP_SCOPE))
                return;
            context.getProperties().put(entry.getKey(), entry.getValue());
        });
    }

    @Override
    protected Block init() {
        return block;
    }
}
