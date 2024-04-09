package de.mhus.kt2l.kscript;

import de.mhus.commons.tools.MCollection;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tree.IProperties;
import de.mhus.commons.tree.IReadonly;
import de.mhus.commons.tree.MProperties;
import lombok.Getter;

public abstract class Cmd {
    @Getter
    protected int line;
    private IProperties args;
    protected String scope;
    protected Block block;

    final void run(RunContext context) throws Exception {
        IProperties args = new MProperties(this.args);
        MCollection.replaceAll(args, (k,v) -> MString.substitute(v.toString(), context.getProperties()));
        run(context, args);
    }

    abstract void run(RunContext context, IReadonly args) throws Exception;

    Block init(Block block, int lineCnt, String cmdScope, IProperties args) throws Exception {
        this.line = lineCnt;
        this.args = args;
        this.scope = cmdScope;
        this.block = block;
        return init();
    }

    protected abstract Block init() throws Exception;

    IReadonly getOriginArgs() {
        return args;
    }

    public void dump(StringBuilder sb, int level) {
        sb.append(MString.rep(' ', level)).append(scope).append(".").append(getClass().getSimpleName()).append(" ").append(args).append("\n");
    }

    public void addToParent(Block block) {
        block.getCmds().add(this);
    }
}
