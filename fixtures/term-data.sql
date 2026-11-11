-- Spring term data for the reporting lab. Supplied: do not edit.
-- The tests load it into a fresh copy of fixtures/schema.sql before every task.
INSERT INTO courses (code, title, capacity, prerequisite_code) VALUES
  ('SQL101', 'SQL Foundations', 6, NULL),
  ('PGA110', 'PostgreSQL Admin Basics', 2, NULL),
  ('SQL201', 'Reporting with SQL', 4, 'SQL101'),
  ('JDB301', 'JDBC Data Access', 4, 'SQL201');

INSERT INTO students (id, name, email, city) VALUES
  (1, 'Ada Lovelace', 'ada@registrar.test', 'London'),
  (2, 'Grace Hopper', 'grace@registrar.test', 'New York'),
  (3, 'Alan Turing', 'alan@registrar.test', 'London'),
  (4, 'Katherine Johnson', 'katherine@registrar.test', NULL),
  (5, 'Edsger Dijkstra', 'edsger@registrar.test', 'Rotterdam'),
  (6, 'Barbara Liskov', 'barbara@registrar.test', 'New York'),
  (7, 'Donald Knuth', 'donald@registrar.test', NULL),
  (8, 'Frances Allen', 'frances@registrar.test', 'Rotterdam');

INSERT INTO enrollments (student_id, course_code, enrolled_at, grade) VALUES
  (1, 'SQL101', '2026-01-12 09:00', 92),
  (2, 'SQL101', '2026-01-12 09:05', 75),
  (3, 'SQL101', '2026-01-12 09:10', 88),
  (4, 'SQL101', '2026-01-13 11:00', NULL),
  (6, 'SQL101', '2026-01-13 12:00', 88),
  (8, 'SQL101', '2026-01-14 08:45', 60),
  (1, 'SQL201', '2026-03-02 09:00', 81),
  (2, 'SQL201', '2026-03-02 09:10', NULL),
  (5, 'SQL201', '2026-03-03 08:30', 95),
  (3, 'JDB301', '2026-04-20 10:00', 77),
  (6, 'JDB301', '2026-04-20 10:30', 90),
  (7, 'PGA110', '2026-02-02 14:00', 65);
