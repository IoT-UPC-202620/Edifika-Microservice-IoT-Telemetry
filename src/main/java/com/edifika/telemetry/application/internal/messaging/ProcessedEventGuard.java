package com.edifika.telemetry.application.internal.messaging;

import com.edifika.telemetry.domain.model.entities.ProcessedEvent;
import com.edifika.telemetry.infrastructure.persistence.jpa.repositories.ProcessedEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class ProcessedEventGuard {

    private final ProcessedEventRepository repository;

    public ProcessedEventGuard(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean firstTime(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        if (repository.existsById(eventId)) {
            return false;
        }
        try {
            repository.saveAndFlush(new ProcessedEvent(eventId, Instant.now()));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }
}
