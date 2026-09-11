package com.kkobak.app.notification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, String> {
    Optional<PushSubscription> findByEndpoint(String endpoint);
    List<PushSubscription> findByOwnerKey(String ownerKey);
    @Modifying @Query("update PushSubscription subscription set subscription.ownerKey = :newOwner where subscription.ownerKey = :legacyOwner")
    int reassignOwner(@Param("legacyOwner") String legacyOwner, @Param("newOwner") String newOwner);
}
