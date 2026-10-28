package dev.bartmilo.trainingbooking.schedule;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateRoomRequest(
    @Size(max = 100) @Pattern(regexp = "(?sU).*\\S.*", message = "must not be blank") String name,
    Boolean active) {}
