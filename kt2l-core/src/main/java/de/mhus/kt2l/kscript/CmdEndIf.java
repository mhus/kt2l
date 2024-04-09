package de.mhus.kt2l.kscript;

import de.mhus.commons.tree.IReadonly;

public class CmdEndIf extends Cmd {

    @Override
    public void run(RunContext context, IReadonly args) throws Exception {
        // nothing to do
    }

    @Override
    protected Block init() {
        var parent = block.getParent();
        if (parent == null)
            throw new RuntimeException("No parent block found");
        if (! (block instanceof CmdIf || block instanceof CmdElse || block instanceof CmdElseIf))
            throw new RuntimeException("Parent block is not a If/Else block");
        return parent;
    }

}
