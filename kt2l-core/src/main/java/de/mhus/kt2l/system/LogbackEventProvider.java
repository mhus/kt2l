package de.mhus.kt2l.system;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import de.mhus.commons.util.MEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class LogbackEventProvider {

    private MEventHandler<ILoggingEvent> eventHandler = new MEventHandler<>();

    @PostConstruct
    public void init() {
        LOGGER.debug("LogbackConfiguration init");
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).addAppender(new LogbackKt2lAppender(eventHandler));
    }


    public MEventHandler<ILoggingEvent> getEventHandler() {
        return eventHandler;
    }

}
