# Code-driven learning notes

Optional follow-up topics for the implemented room slice. Add a question when it comes up, discuss it against the actual code, then check it off or record the answer. This is not a separate course or a prerequisite for finishing the project.

## Current walkthrough: read one room

Start with the persisted data, then follow GET /api/v1/rooms/{id} with a valid active room and authenticated user.

| Order | Code | Question to answer |
|---|---|---|
| 1 | [V1 migration](src/main/resources/db/migration/V1__create_room.sql) and [Room](src/main/java/dev/bartmilo/trainingbooking/schedule/Room.java) | How do the columns map to fields? What do Entity, Table, Id, GeneratedValue and Column change? |
| 2 | [RoomResponse](src/main/java/dev/bartmilo/trainingbooking/schedule/RoomResponse.java) | What is a record, and why return a DTO rather than the entity? |
| 3 | [RoomController.get](src/main/java/dev/bartmilo/trainingbooking/schedule/RoomController.java) | How do the URL, UUID argument, authentication and role check reach the service? |
| 4 | [RoomService.get](src/main/java/dev/bartmilo/trainingbooking/schedule/RoomService.java) | What do constructor injection, read-only transaction, Optional and orElseThrow do here? |
| 5 | [RoomRepository](src/main/java/dev/bartmilo/trainingbooking/schedule/RoomRepository.java) | Where does inherited findById come from, who implements the interface and which SQL runs? |
| 6 | [RoomApiTests](src/test/java/dev/bartmilo/trainingbooking/RoomApiTests.java) | Follow memberCanReadActiveRoomCreatedDirectlyThroughSql: which parts are real, and which identity is simulated? |

- [ ] Explain Room field/column mapping, its constructors and both UUID generation paths.
- [ ] Trace one successful room read from the controller to SQL and back to JSON.
- [ ] Trace the missing/inactive-room path and its 404 response.

For each line distinguish Java syntax, framework behavior and the reason this project uses it. Imports can be reviewed alongside the first actual use of the imported type.

## Revisit when the next operation needs it

- [ ] Creation: request records, Valid, database constraints, saveAndFlush and Location.
- [ ] Updates: dirty checking, flush, pessimistic row locks and immutable capacity.
- [ ] Lists: Page/Pageable, generics, method references and active filtering.
- [ ] Security: method authorization and CSRF versus real authentication/session behavior.

## Parking lot

Add a short question here rather than interrupting an unrelated explanation with a broad detour.

| Question | Relevant code / observation | Answer or next experiment |
|---|---|---|
