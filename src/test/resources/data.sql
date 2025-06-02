INSERT INTO users (username, password, role)
VALUES
  ('test user', '$2a$12$NxmnYShYrOpRaslcxs7v/.M98/3OUb519S.m76FgqgN6IcA2Pon3.', 'ROLE_USER'), -- пароль: user123
  ('test admin', '$2a$12$yUyQT9/0FgYp9ba046DpfOy41LKjCBhCcRAKrk2yUNbo0iSpGVgrq', 'ROLE_ADMIN'); -- пароль: admin123

INSERT INTO tasks (title, description, status, created_at, updated_at, user_id)
VALUES
  ('Test Task 1', 'Test task description', 'PENDING', NOW(), NOW(), 1),
  ('Admin Task', 'Created by admin', 'IN_PROGRESS', NOW(), NOW(), 2);