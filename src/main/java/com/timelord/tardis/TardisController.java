package com.timelord.tardis;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tardis")
class TardisController {
    private final TardisService tardisService;

    public TardisController(TardisService tardisService) {
        this.tardisService = tardisService;
    }

    @PostMapping("/awareness")
    public EntitySelfAwareness getAwareness(@RequestBody EntityTemporalContext context) {
        return tardisService.getAwareness(context);
    }

    @PostMapping("/schedules")
    public ResponseEntity<Void> createSchedule(@RequestBody FutureSchedule schedule) {
        tardisService.registerSchedule(schedule);
        return ResponseEntity.accepted().build();
    }
    
    @PostMapping("/emails/{chainId}/analyze")
    public EmailChainMetadata analyzeEmailChain(
            @PathVariable String chainId, 
            @RequestBody List<java.time.LocalDateTime> points) {
        return tardisService.analyzeEmailChain(chainId, points);
    }
}
