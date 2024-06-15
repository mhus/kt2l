package de.mhus.kt2l.development;

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
