package de.mhus.kt2l.system;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import de.mhus.commons.util.MEventHandler;

public class LogbackKt2lAppender extends AppenderBase<ILoggingEvent> {

    private final MEventHandler<ILoggingEvent> eventHandler;

    public LogbackKt2lAppender(MEventHandler<ILoggingEvent> eventHandler) {
        this.eventHandler = eventHandler;
        setName("kt2l");
        start();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
//        System.out.println("Yooooo");
        eventHandler.fire(eventObject);
    }

}
