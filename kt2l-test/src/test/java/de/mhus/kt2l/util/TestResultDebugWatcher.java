package de.mhus.kt2l.util;

import de.mhus.kt2l.DebugTestUtil;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.lang.reflect.Method;

public class TestResultDebugWatcher implements TestWatcher, AfterEachCallback, BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        System.out.println("----------------------------------------------------------------");
        var name =  extensionContext.getTestClass().get().getSimpleName() + "." + extensionContext.getTestMethod().map(Method::getName).orElse("unknown");
        System.out.println("Ⓘ End Test: " + name);
        System.out.println("----------------------------------------------------------------");
        DebugTestUtil.debugStep("After " + name);
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        System.out.println("----------------------------------------------------------------");
        System.out.println("Ⓘ Before All");
        System.out.println("----------------------------------------------------------------");
        DebugTestUtil.debugPrepare(extensionContext.getTestClass().get().getSimpleName());
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        System.out.println("----------------------------------------------------------------");
        System.out.println("Ⓘ After All");
        System.out.println("----------------------------------------------------------------");
        DebugTestUtil.debugClose();
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        System.out.println("----------------------------------------------------------------");
        var name =  extensionContext.getTestClass().get().getSimpleName() + "." + extensionContext.getTestMethod().map(Method::getName).orElse("unknown");
        System.out.println("Ⓘ Start Test: " + name);
        System.out.println("----------------------------------------------------------------");
        DebugTestUtil.debugPrepare(extensionContext.getTestClass().get().getSimpleName() + "." + extensionContext.getTestMethod().get().getName());
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        var name =  context.getTestClass().get().getSimpleName() + "." + context.getTestMethod().map(Method::getName).orElse("unknown");
        System.out.println("Ⓘ Aborted Test: " + name + " " + cause.getMessage());
        DebugTestUtil.debugStep("Aborted: " + cause.getMessage(), true);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        var name =  context.getTestClass().get().getSimpleName() + "." + context.getTestMethod().map(Method::getName).orElse("unknown");
        System.out.println("Ⓘ Failed Test: " + name + " " + cause.getMessage());
        DebugTestUtil.debugStep("Failed: " + cause.getMessage(), true);
    }

}
