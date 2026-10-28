package dev.bartmilo.trainingbooking.schedule;

import dev.bartmilo.trainingbooking.shared.ApiException;
import dev.bartmilo.trainingbooking.shared.PageResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class RoomService {
  private final RoomRepository rooms;

  RoomService(RoomRepository rooms) {
    this.rooms = rooms;
  }

  PageResponse<RoomResponse> list(int page, int size, boolean includeInactive) {
    PageRequest pageable = PageRequest.of(page, size, Sort.by("name").and(Sort.by("id")));
    Page<Room> result =
        includeInactive ? rooms.findAll(pageable) : rooms.findAllByActiveTrue(pageable);
    return PageResponse.from(result.map(RoomResponse::from));
  }

  RoomResponse get(UUID id, boolean includeInactive) {
    Room room = rooms.findById(id).orElseThrow(RoomService::notFound);
    if (!room.isActive() && !includeInactive) {
      throw notFound();
    }
    return RoomResponse.from(room);
  }

  @Transactional
  RoomResponse create(CreateRoomRequest request) {
    Room room =
        new Room(
            request.name().strip(),
            request.capacity(),
            request.active() == null || request.active());
    return RoomResponse.from(rooms.saveAndFlush(room));
  }

  @Transactional
  RoomResponse update(UUID id, UpdateRoomRequest request) {
    Room room = rooms.findByIdForUpdate(id).orElseThrow(RoomService::notFound);
    room.updateDetails(request.name() == null ? null : request.name().strip(), request.active());
    rooms.flush();
    return RoomResponse.from(room);
  }

  private static ApiException notFound() {
    return new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "The room was not found.");
  }
}
