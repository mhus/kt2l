package de.mhus.kt2l.kscript;

public class CmdElseIf extends CmdIf {

    private Block ifBlock;

    @Override
    protected Block init() throws Exception {
        super.init();
        ifBlock = block;
        if (! (ifBlock instanceof CmdIf || ifBlock instanceof CmdElseIf))
            throw new RuntimeException("Parent block is not a If/ElseIf block");
        block = block.getParent();
        ifBlock.setElseBlock(this);
        return this;
    }

    public void addToParent(Block block) {
    }

}
