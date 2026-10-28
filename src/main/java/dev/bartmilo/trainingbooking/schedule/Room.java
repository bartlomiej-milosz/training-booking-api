package dev.bartmilo.trainingbooking.schedule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "room")
public class Room {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, updatable = false)
  private int capacity;

  @Column(nullable = false)
  private boolean active;

  protected Room() {}

  Room(String name, int capacity, boolean active) {
    this.name = name;
    this.capacity = capacity;
    this.active = active;
  }

  void updateDetails(String name, Boolean active) {
    if (name != null) {
      this.name = name;
    }
    if (active != null) {
      this.active = active;
    }
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getCapacity() {
    return capacity;
  }

  public boolean isActive() {
    return active;
  }
}
