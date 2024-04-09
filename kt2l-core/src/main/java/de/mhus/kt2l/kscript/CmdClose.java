package de.mhus.kt2l.kscript;

import de.mhus.commons.tree.IReadonly;

public class CmdClose extends Cmd {
    @Override
    void run(RunContext context, IReadonly args) throws Exception {
        var scope = context.getScope(this.scope);
        scope.close();
    }

    @Override
    protected Block init() {
        return block;
    }
}
