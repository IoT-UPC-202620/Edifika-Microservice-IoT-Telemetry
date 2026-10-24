package com.edifika.telemetry.shared.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

@Getter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class AuditableModel {

    @Getter
    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Date createdAt;

    @Getter
    @LastModifiedDate
    @Column(nullable = false)
    private Date updatedAt;
}
