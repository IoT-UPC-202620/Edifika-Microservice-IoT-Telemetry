package com.edifika.telemetry.infrastructure.persistence.jpa.repositories;

import com.edifika.telemetry.domain.model.entities.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
