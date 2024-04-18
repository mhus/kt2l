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

package de.mhus.kt2l;

import de.mhus.commons.util.Value;
import de.mhus.kt2l.kscript.Block;
import de.mhus.kt2l.kscript.RunCompiler;
import de.mhus.kt2l.kscript.RunContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RunTest {

    @Test
    public void testIf() throws Exception {
        String code = """
        if is='${abc} == abc'
          echo msg='abc'
        endif
        """;
        RunCompiler compiler = new RunCompiler();
        Block block = compiler.compile(code);
        RunContext context = new RunContext();
        context.getProperties().setString("abc", "abc");
        final Value<String> printText = new Value<>();
        context.setTextChangedObserver((text) -> {
            System.out.println("Text: " + text);
            printText.value = text;
        });
        block.run(context, null);

        assertThat(printText.value).isEqualTo("abc");
    }

    @Test
    public void testIfElse() throws Exception {
        String code =
        """
        if is='${abc} == abc'
          echo msg='abc'
        else
          echo msg='else'
        endif
        """;
        RunCompiler compiler = new RunCompiler();
        Block block = compiler.compile(code);
        RunContext context = new RunContext();
        context.getProperties().setString("abc", "xxx");
        final Value<String> printText = new Value<>();
        context.setTextChangedObserver((text) -> {
            System.out.println("Text: " + text);
            printText.value = text;
        });
        block.run(context, null);

        assertThat(printText.value).isEqualTo("else");
    }

    @Test
    public void testIfElseIf() throws Exception {
        String code =
        """
        if is='${abc} == abc'
          echo msg='abc'
        elseif is='${abc} == xxx'
          echo msg='xxx'
        else
          echo msg='else'
        endif
        """;
        RunCompiler compiler = new RunCompiler();
        Block block = compiler.compile(code);
        StringBuilder sb = new StringBuilder();
        block.dump(sb, 0);
        System.out.printf(sb.toString());
        RunContext context = new RunContext();
        context.getProperties().setString("abc", "xxx");
        final Value<String> printText = new Value<>();
        context.setTextChangedObserver((text) -> {
            System.out.println("Text: " + text);
            printText.value = text;
        });
        block.run(context, null);

        assertThat(printText.value).isEqualTo("xxx");
    }
}
