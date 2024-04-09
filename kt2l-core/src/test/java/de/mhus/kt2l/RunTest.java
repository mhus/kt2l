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
