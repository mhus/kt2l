/**
 * This file is part of kt2l-core.
 *
 * kt2l-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * kt2l-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with kt2l-core.  If not, see <http://www.gnu.org/licenses/>.
 */
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
