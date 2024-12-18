package com.szs.szsapi.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Timestamped {
    @CreatedDate
    @Column(name="insDate", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @Schema(description = "등록일자")
    private LocalDateTime insDate;

    @LastModifiedDate
    @Column(name="lastLoginDate")
    @Temporal(TemporalType.TIMESTAMP)
    @Schema(description = "마지막 로그인 시간")
    private LocalDateTime lastLoginDate;
}
