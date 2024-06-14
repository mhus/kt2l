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
