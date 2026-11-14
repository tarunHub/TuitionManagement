package com.smartstart.portal.centre.tenancy;

import com.smartstart.portal.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

/** A row that belongs to one centre. Hibernate fills {@code centreId} on insert and filters every query by it. */
@Getter
@MappedSuperclass
public abstract class TenantEntity extends BaseEntity {

    @TenantId
    @Column(name = "centre_id", nullable = false, updatable = false)
    private Long centreId;
}
