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
