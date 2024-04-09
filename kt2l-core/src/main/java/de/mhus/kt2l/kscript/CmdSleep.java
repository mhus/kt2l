package de.mhus.kt2l.kscript;

import de.mhus.commons.tools.MPeriod;
import de.mhus.commons.tree.IReadonly;

public class CmdSleep extends Cmd {
    @Override
    void run(RunContext context, IReadonly args) throws Exception {
        Thread.sleep(MPeriod.toTime(args.getString("time").orElse("0"), 0));
    }

    @Override
    protected Block init() {
        return block;
    }
}
