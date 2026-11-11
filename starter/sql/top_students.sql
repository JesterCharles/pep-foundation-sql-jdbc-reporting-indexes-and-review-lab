-- Task 2: top two graded students per course (README.md, Task 2).
-- Columns, in order: course_code, place, name, grade, course_average.
-- The step's rows are supplied. TODO: add the place and course_average columns where marked,
-- then write the outer SELECT that keeps places 1 and 2, below the step.
WITH ranked AS (
  SELECT e.course_code, s.name, e.grade
         -- TODO: , place, course_average
  FROM enrollments AS e
  JOIN students AS s ON s.id = e.student_id
  WHERE e.grade IS NOT NULL
)
