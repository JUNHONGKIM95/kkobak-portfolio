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
    @Query("select completion.cycleId from CycleCompletion completion where completion.ownerKey = :ownerKey and completion.completedDate = :date")
    List<String> findCycleIdsByOwnerKeyAndCompletedDate(@Param("ownerKey") String ownerKey, @Param("date") LocalDate date);
    void deleteByCycleIdAndOwnerKey(String cycleId, String ownerKey);
    List<CycleCompletion> findByOwnerKeyOrderByCompletedDateAsc(String ownerKey);
    @Modifying @Query("update CycleCompletion completion set completion.ownerKey = :newOwner where completion.ownerKey = :legacyOwner")
    int reassignOwner(@Param("legacyOwner") String legacyOwner, @Param("newOwner") String newOwner);
}
