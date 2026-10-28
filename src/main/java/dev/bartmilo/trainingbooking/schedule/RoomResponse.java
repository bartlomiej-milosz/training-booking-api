package dev.bartmilo.trainingbooking.schedule;

import java.util.UUID;

public record RoomResponse(UUID id, String name, int capacity, boolean active) {
  static RoomResponse from(Room room) {
    return new RoomResponse(room.getId(), room.getName(), room.getCapacity(), room.isActive());
  }
}
