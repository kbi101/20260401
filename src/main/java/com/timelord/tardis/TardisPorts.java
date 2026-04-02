package com.timelord.tardis;

import java.time.LocalDateTime;
import java.time.ZoneId;

public interface ClockPort {
    LocalDateTime now();
    ZoneId currentZone();
}

@org.springframework.stereotype.Component
class SystemClockAdapter implements ClockPort {
    @Override
    public LocalDateTime now() { return LocalDateTime.now(); }
    @Override
    public ZoneId currentZone() { return ZoneId.systemDefault(); }
}
