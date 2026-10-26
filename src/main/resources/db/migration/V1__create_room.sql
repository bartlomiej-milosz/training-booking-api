CREATE TABLE room (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    name varchar(100) NOT NULL,
    capacity integer NOT NULL,
    active boolean NOT NULL DEFAULT true,

    CONSTRAINT uk_room_name UNIQUE (name),
    CONSTRAINT ck_room_not_blank CHECK (name <> ''),
    CONSTRAINT ck_room_capacity_positive CHECK (capacity > 0)
);
