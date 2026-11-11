-- Task 1: the SQL201 waitlist (README.md, Task 1). One WITH query with named steps.
-- Columns, in order: id, name, sql101_grade.
-- The first step is supplied. TODO: add the step sql201_students, then the final SELECT, below it.
WITH sql101_average AS (
  SELECT AVG(grade) AS average FROM enrollments WHERE course_code = 'SQL101'
)
