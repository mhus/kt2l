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
package de.mhus.kt2l.system;

import org.springframework.stereotype.Component;

@Component
public class UpTimeService {

        private long startTime = System.currentTimeMillis();

        public long getUpTime() {
            return System.currentTimeMillis() - startTime;
        }

        public String getUpTimeFormatted() {
            long time = getUpTime();
            long sec = time / 1000;
            long min = sec / 60;
            long hour = min / 60;
            long day = hour / 24;
            return String.format("%d days %02d:%02d:%02d", day, hour % 24, min % 60, sec % 60);
        }

}
