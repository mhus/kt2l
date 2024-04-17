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

import java.io.Closeable;
import java.io.IOException;

public abstract class Scope implements Closeable {
    public abstract boolean isRunning();

    public abstract boolean contentContains(String search);

    public abstract boolean stdinContains(String search);

    public abstract boolean stderrContains(String search);

    public abstract void send(String msg) throws IOException;

    public abstract void clear();
}
