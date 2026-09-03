package com.digitalwallet.domain.repository;

import com.digitalwallet.domain.entity.AuditLog;
import com.digitalwallet.domain.enums.AuditAction;
import com.digitalwallet.domain.enums.AuditActorType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic filter cho AuditLog dùng bởi admin audit log query (DWS-213).
 * Mỗi tham số null sẽ bị bỏ qua (không thêm điều kiện).
 */
public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> filter(AuditAction action, AuditActorType actorType,
            String resourceType, Instant from, Instant to) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (action != null) {
                predicates.add(criteriaBuilder.equal(root.get("actionType"), action));
            }
            if (actorType != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorType"), actorType));
            }
            if (resourceType != null && !resourceType.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("resourceType"), resourceType));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
