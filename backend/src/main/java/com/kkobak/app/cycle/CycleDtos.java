package com.kkobak.app.cycle;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public final class CycleDtos {
    private CycleDtos() { }

    public record CycleRequest(
            @NotBlank String title,
            @NotBlank String category,
            @NotBlank String cycleType,
            @NotBlank String emoji,
            @Min(1) int intervalValue,
            @NotBlank String intervalUnit,
            @NotNull LocalDate startDate,
            String imageUrl,
            @NotBlank String color) { }

    public record ClaimRequest(@NotBlank String ownerKey) { }
    public record ClaimResponse(int cycles, int completions, int subscriptions) { }

    public record CycleResponse(
            String id, String title, String category, String cycleType, String emoji,
            int intervalValue, String intervalUnit, LocalDate startDate, LocalDate lastCompletedDate,
            LocalDate nextDueDate, String imageUrl, String color,
            boolean completedToday, long daysLeft) { }
}
