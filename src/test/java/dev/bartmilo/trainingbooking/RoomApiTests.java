package dev.bartmilo.trainingbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
@WithMockUser(roles = "OWNER")
class RoomApiTests {
  @Autowired private MockMvc mvc;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void createsRoomWithUuidAndReturnsAddressableResource() throws Exception {
    var result =
        mvc.perform(
                post("/api/v1/rooms")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"  Main Hall  \",\"capacity\":20}"))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.name").value("Main Hall"))
            .andExpect(jsonPath("$.active").value(true))
            .andReturn();
    String location = result.getResponse().getHeader("Location");
    UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    assertThat(jdbc.queryForObject("SELECT capacity FROM room WHERE id = ?", Integer.class, id))
        .isEqualTo(20);
    mvc.perform(get(location))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()));
  }

  @Test
  void listsOnlyActiveRoomsWithStablePagination() throws Exception {
    room("Beta", 20, true);
    room("Alpha", 10, true);
    room("Archived", 15, false);
    mvc.perform(get("/api/v1/rooms").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].name").value("Alpha"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(2));
    mvc.perform(get("/api/v1/rooms").param("page", "1").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].name").value("Beta"));
  }

  @Test
  void ownerCanIncludeInactiveRooms() throws Exception {
    room("Archived", 15, false);
    mvc.perform(get("/api/v1/rooms").param("includeInactive", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  @WithMockUser(roles = "MEMBER")
  void memberCanReadActiveRoomCreatedDirectlyThroughSql() throws Exception {
    UUID id = room("Main Hall", 20, true);
    mvc.perform(get("/api/v1/rooms/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.capacity").value(20));
  }

  @Test
  @WithMockUser(roles = "MEMBER")
  void memberCannotIncludeInactiveRooms() throws Exception {
    mvc.perform(get("/api/v1/rooms").param("includeInactive", "true"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "MEMBER")
  void inactiveRoomIsHiddenFromMember() throws Exception {
    UUID id = room("Archived", 20, false);
    mvc.perform(get("/api/v1/rooms/{id}", id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
  }

  @Test
  void ownerCanReadInactiveRoom() throws Exception {
    UUID id = room("Archived", 20, false);
    mvc.perform(get("/api/v1/rooms/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  @WithAnonymousUser
  void requiresAuthentication() throws Exception {
    mvc.perform(get("/api/v1/rooms").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "MEMBER")
  void memberCannotCreateRoom() throws Exception {
    mvc.perform(
            post("/api/v1/rooms")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Main Hall\",\"capacity\":20}"))
        .andExpect(status().isForbidden());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM room", Long.class)).isZero();
  }

  @Test
  void creationRequiresCsrf() throws Exception {
    mvc.perform(
            post("/api/v1/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Main Hall\",\"capacity\":20}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsInvalidFieldsWithProblemDetails() throws Exception {
    mvc.perform(
            post("/api/v1/rooms")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \",\"capacity\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "capacity")));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"name\":\"Main Hall\",\"capacity\":2.5}",
        "{\"name\":\"Main Hall\",\"capacity\":20,\"ownerId\":\"anything\"}",
        "{broken-json}"
      })
  void rejectsMalformedOrUnsupportedRequestFields(String body) throws Exception {
    mvc.perform(
            post("/api/v1/rooms")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  void duplicateNameIsConflict() throws Exception {
    room("Main Hall", 10, true);
    mvc.perform(
            post("/api/v1/rooms")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Main Hall\",\"capacity\":20}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ROOM_NAME_ALREADY_EXISTS"));
  }

  @Test
  void updatesOnlyNameAndActiveFlag() throws Exception {
    UUID id = room("Main Hall", 20, true);
    mvc.perform(
            patch("/api/v1/rooms/{id}", id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  Small Hall  \",\"active\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Small Hall"))
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.capacity").value(20));
    assertThat(jdbc.queryForObject("SELECT name FROM room WHERE id = ?", String.class, id))
        .isEqualTo("Small Hall");
  }

  @Test
  void rejectsCapacityChanges() throws Exception {
    UUID id = room("Main Hall", 20, true);
    mvc.perform(
            patch("/api/v1/rooms/{id}", id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"capacity\":30}"))
        .andExpect(status().isBadRequest());
    assertThat(jdbc.queryForObject("SELECT capacity FROM room WHERE id = ?", Integer.class, id))
        .isEqualTo(20);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "\u2003"})
  void rejectsBlankRoomNameChanges(String name) throws Exception {
    UUID id = room("Main Hall", 20, true);
    mvc.perform(
            patch("/api/v1/rooms/{id}", id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  @WithMockUser(roles = "MEMBER")
  void memberCannotUpdateRoom() throws Exception {
    UUID id = room("Main Hall", 20, true);
    mvc.perform(
            patch("/api/v1/rooms/{id}", id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void unknownRoomIsNotFound() throws Exception {
    mvc.perform(
            patch("/api/v1/rooms/{id}", UUID.randomUUID())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
  }

  @Test
  void invalidUuidIsBadRequest() throws Exception {
    mvc.perform(get("/api/v1/rooms/not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"page=-1", "size=0", "size=101"})
  void rejectsInvalidPagination(String query) throws Exception {
    mvc.perform(get("/api/v1/rooms?" + query))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "INSERT INTO room (name, capacity) VALUES ('   ', 10)",
        "INSERT INTO room (name, capacity) VALUES ('Hall', 0)",
        "INSERT INTO room (name, capacity) VALUES (NULL, 10)"
      })
  void databaseRejectsInvalidRowsEvenWithoutHttpValidation(String sql) {
    assertThatThrownBy(() -> jdbc.update(sql)).isInstanceOf(DataIntegrityViolationException.class);
  }

  private UUID room(String name, int capacity, boolean active) {
    return jdbc.queryForObject(
        "INSERT INTO room (name, capacity, active) VALUES (?, ?, ?) RETURNING id",
        UUID.class,
        name,
        capacity,
        active);
  }
}
