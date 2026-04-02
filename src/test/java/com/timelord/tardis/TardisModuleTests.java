package com.timelord.tardis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ApplicationModuleTest
class TardisModuleTests {

    @Autowired
    TardisService tardisService;

    @MockitoBean
    ClockPort clockPort;

    @Test
    void shouldReturnCorrectLifeStageForPerson(AssertablePublishedEvents events) {
        // Given: A person born 30 years ago
        LocalDateTime now = LocalDateTime.of(2026, 4, 1, 10, 0);
        when(clockPort.now()).thenReturn(now);
        
        LocalDateTime birth = now.minusYears(30);
        EntityTemporalContext context = new EntityTemporalContext(
            UUID.randomUUID().toString(),
            EntityType.PERSON,
            "UTC",
            birth,
            ConfidenceLevel.HIGHEST
        );

        // When
        EntitySelfAwareness awareness = tardisService.getAwareness(context);

        // Then
        assertThat(awareness.lifeStage()).isEqualTo(LifeStage.MIDDLE_AGED);
        assertThat(awareness.relativeAge()).contains("30 years");
    }

    @Test
    void shouldReturnYoungLifeStageForPerson(AssertablePublishedEvents events) {
        // Given: A person born 20 years ago
        LocalDateTime now = LocalDateTime.of(2026, 4, 1, 10, 0);
        when(clockPort.now()).thenReturn(now);
        
        LocalDateTime birth = now.minusYears(20);
        EntityTemporalContext context = new EntityTemporalContext(
            UUID.randomUUID().toString(),
            EntityType.PERSON,
            "UTC",
            birth,
            ConfidenceLevel.HIGHEST
        );

        // When
        EntitySelfAwareness awareness = tardisService.getAwareness(context);

        // Then
        assertThat(awareness.lifeStage()).isEqualTo(LifeStage.YOUNG);
    }

    @Test
    void shouldReportDaylightCorrecty(AssertablePublishedEvents events) {
        // Given: Noon time
        LocalDateTime noon = LocalDateTime.of(2026, 4, 1, 12, 0);
        when(clockPort.now()).thenReturn(noon);
        
        EntityTemporalContext context = new EntityTemporalContext(
            "test-id", EntityType.SYSTEM, "UTC", noon.minusDays(1), ConfidenceLevel.HIGHEST
        );

        // When/Then
        assertThat(tardisService.getAwareness(context).isDaylight()).isTrue();

        // Given: Midnight
        LocalDateTime midnight = LocalDateTime.of(2026, 4, 1, 0, 0);
        when(clockPort.now()).thenReturn(midnight);
        
        // When/Then
        assertThat(tardisService.getAwareness(context).isDaylight()).isFalse();
    }
}
