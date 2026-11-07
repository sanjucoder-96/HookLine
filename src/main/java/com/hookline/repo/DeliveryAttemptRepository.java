package com.hookline.repo;

import com.hookline.domain.DeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {

    List<DeliveryAttempt> findByOutboxEventIdOrderByAttemptNumber(UUID outboxEventId);

}
