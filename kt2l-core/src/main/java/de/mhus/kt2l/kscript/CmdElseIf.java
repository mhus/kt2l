/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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
