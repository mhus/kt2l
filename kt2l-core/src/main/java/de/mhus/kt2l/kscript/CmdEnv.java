package de.mhus.kt2l.kscript;

import de.mhus.commons.tree.IReadonly;

public class CmdEnv extends Cmd {
    @Override
    void run(RunContext context, IReadonly args) throws Exception {
        args.forEach((entry) -> {
            if (entry.getKey().startsWith(RunCompiler.PROP_SCOPE))
                return;
            context.getProperties().put(entry.getKey(), entry.getValue());
        });

        if (args.size() == 0) {
            final StringBuilder sb = new StringBuilder();
            context.getProperties().forEach((entry) -> {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            });
            context.addContent(sb.toString());
        }
    }

    @Override
    protected Block init() {
        return block;
    }
}
