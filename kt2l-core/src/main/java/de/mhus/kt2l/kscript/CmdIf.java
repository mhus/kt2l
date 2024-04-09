package de.mhus.kt2l.kscript;

import de.mhus.commons.matcher.Condition;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tree.IReadonly;

public class CmdIf extends Block {
    private Condition condition;

    @Override
    public void run(RunContext context, IReadonly args) throws Exception {
        var is = args.getString("is").orElse(null);
        if (condition.matches( context.getProperties() )) {
            super.run(context, args);
        } else {
            if (elseBlock != null)
                elseBlock.run(context);
        }
    }

    @Override
    protected Block init() throws Exception {
        var is = getOriginArgs().getString("is").get();
        condition = new Condition(is);
        return this;
    }

    public void dump(StringBuilder sb, int level) {
        super.dump(sb,level);
        if (elseBlock != null) {
            level = level + 2;
            sb.append(MString.rep(' ', level)).append("-- Else:\n");
            elseBlock.dump(sb, level);
        }
    }

}
