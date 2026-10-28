package dev.bartmilo.trainingbooking.schedule;

import dev.bartmilo.trainingbooking.shared.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {
  private final RoomService rooms;

  RoomController(RoomService rooms) {
    this.rooms = rooms;
  }

  @GetMapping
  @PreAuthorize("isAuthenticated() and (!#includeInactive or hasRole('OWNER'))")
  public PageResponse<RoomResponse> list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(defaultValue = "false") boolean includeInactive) {
    return rooms.list(page, size, includeInactive);
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public RoomResponse get(@PathVariable UUID id, Authentication authentication) {
    boolean owner =
        authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_OWNER"));
    return rooms.get(id, owner);
  }

  @PostMapping
  @PreAuthorize("hasRole('OWNER')")
  public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
    RoomResponse room = rooms.create(request);
    return ResponseEntity.created(URI.create("/api/v1/rooms/" + room.id())).body(room);
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('OWNER')")
  public RoomResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateRoomRequest request) {
    return rooms.update(id, request);
  }
}
