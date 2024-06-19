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
package de.mhus.kt2l.development;

import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.CoreListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CoreCounterListener implements CoreListener {

    private static AtomicInteger counter = new AtomicInteger(0);

    @Override
    public void onCoreCreated(Core core) {
        counter.incrementAndGet();
    }

    @Override
    public void onCoreDestroyed(Core core) {
        counter.decrementAndGet();
    }

    public static int getCounter() {
        return counter.get();
    }

}
