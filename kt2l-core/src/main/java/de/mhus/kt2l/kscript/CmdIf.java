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
