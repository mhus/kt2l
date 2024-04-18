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
