package dev.bartmilo.trainingbooking.schedule;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface RoomRepository extends JpaRepository<Room, UUID> {
  Page<Room> findAllByActiveTrue(Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT room FROM Room room WHERE room.id = :id")
  Optional<Room> findByIdForUpdate(UUID id);
}
