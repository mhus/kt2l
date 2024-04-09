package de.mhus.kt2l.kscript;

import de.mhus.commons.tree.IReadonly;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Block extends Cmd {
    @Getter
    private List<Cmd> cmds = new ArrayList<>();
    @Getter @Setter
    protected Block elseBlock;

    @Override
    public void run(RunContext context, IReadonly args) throws Exception {
        for (Cmd cmd : cmds) {
            try {
                cmd.run(context);
            } catch (Exception e) {
                context.addError(cmd, e);
                LOGGER.error("Error in line " + cmd.line, e);
                throw e;
            }
        }
    }

    public Block getParent() {
        return block;
    }

    @Override
    protected Block init() throws Exception {
        return this;
    }

    public void dump(StringBuilder sb, int level) {
        super.dump(sb,level);
        level = level + 2;
        for (Cmd cmd : cmds) {
            cmd.dump(sb, level);
        }
    }

}
