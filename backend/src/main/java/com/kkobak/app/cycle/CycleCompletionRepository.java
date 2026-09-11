package com.kkobak.app.cycle;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CycleCompletionRepository extends JpaRepository<CycleCompletion, String> {
    boolean existsByCycleIdAndOwnerKeyAndCompletedDate(String cycleId, String ownerKey, LocalDate completedDate);
    Optional<CycleCompletion> findFirstByCycleIdAndOwnerKeyAndCompletedDateOrderByCreatedAtDesc(String cycleId, String ownerKey, LocalDate completedDate);
    void deleteByCycleIdAndOwnerKey(String cycleId, String ownerKey);
    List<CycleCompletion> findByOwnerKeyOrderByCompletedDateAsc(String ownerKey);
    List<CycleCompletion> findByOwnerKeyAndCompletedDateBetweenOrderByCompletedDateAsc(String ownerKey, LocalDate from, LocalDate to);
    @Modifying @Query("update CycleCompletion completion set completion.ownerKey = :newOwner where completion.ownerKey = :legacyOwner")
    int reassignOwner(@Param("legacyOwner") String legacyOwner, @Param("newOwner") String newOwner);
}
