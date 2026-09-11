package com.kkobak.app.cycle;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CycleRepository extends JpaRepository<CycleItem, String> {
    List<CycleItem> findByOwnerKeyOrderByNextDueDateAsc(String ownerKey);
    Optional<CycleItem> findByIdAndOwnerKey(String id, String ownerKey);
    List<CycleItem> findByEndedAtIsNullAndNextDueDateLessThanEqual(LocalDate date);
    @Modifying @Query("update CycleItem item set item.ownerKey = :newOwner where item.ownerKey = :legacyOwner")
    int reassignOwner(@Param("legacyOwner") String legacyOwner, @Param("newOwner") String newOwner);
}
