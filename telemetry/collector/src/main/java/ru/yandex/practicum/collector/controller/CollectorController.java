package ru.yandex.practicum.collector.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.collector.dto.HubEvent;
import ru.yandex.practicum.collector.dto.SensorEvent;
import ru.yandex.practicum.collector.service.CollectorService;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CollectorController {

    private final CollectorService collectorService;

    @PostMapping("/events/sensors")
    public ResponseEntity<Void> collectSensorEvent(@Valid @RequestBody SensorEvent event) {
        log.info("Received sensor event: type={}, id={}, hubId={}",
                event.getType(), event.getId(), event.getHubId());
        collectorService.processSensorEvent(event);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/events/hubs")
    public ResponseEntity<Void> collectHubEvent(@Valid @RequestBody HubEvent event) {
        log.info("Received hub event: type={}, hubId={}",
                event.getType(), event.getHubId());
        collectorService.processHubEvent(event);
        return ResponseEntity.ok().build();
    }
}