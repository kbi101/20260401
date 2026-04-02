package com.timelord.tardis; import java.time.LocalDateTime; import java.time.ZoneId; public interface ClockPort { LocalDateTime now(); ZoneId currentZone(); }
