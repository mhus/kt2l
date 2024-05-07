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

package de.mhus.kt2l.help;

public interface HelpResourceConnector {
    /**
     * Return the help content for the current editor.
     * Use this method in Vaadin UI thread.
     *
     * @return help content or null if not available
     */
    String getHelpContent();
    void setHelpContent(String content);

    /**
     * Return the cursor position in the current editor to insert content or -1 if not available.
     * Use this method in Vaadin UI thread.
     *
     * @return cursor position or -1
     */
    int getHelpCursorPos();
}
