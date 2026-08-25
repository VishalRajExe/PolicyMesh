package com.policymesh.webhook;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

  Optional<WebhookDelivery> findByDeliveryId(String deliveryId);

  boolean existsByDeliveryId(String deliveryId);

  List<WebhookDelivery> findByCommitShaOrderByReceivedAtDesc(String commitSha);

  Page<WebhookDelivery> findAllByOrderByReceivedAtDesc(Pageable pageable);
}