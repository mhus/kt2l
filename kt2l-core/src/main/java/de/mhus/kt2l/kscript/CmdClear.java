package de.mhus.kt2l.kscript;

import de.mhus.commons.tree.IReadonly;

public class CmdClear extends Cmd {
    @Override
    void run(RunContext context, IReadonly args) throws Exception {
        var scope = context.getScope(this.scope);
        scope.clear();
    }

    @Override
    protected Block init() {
        return block;
    }
}
