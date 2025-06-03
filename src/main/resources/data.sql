INSERT INTO users (username, password, role) VALUES
('john', '$2a$12$fvpKUc2aL6k2F9SsdmEs8.LeaQxiiS/XcwOc6ThTxKW9x9si7XOT6', 'ROLE_USER'),
('alice', '$2a$12$j6ZbzpWnISmYqD8T0V16zeR0kNZbKES.Fw2mS9GC9Q9Q9pWhXOeCa', 'ROLE_ADMIN')
ON CONFLICT (username) DO NOTHING;

INSERT INTO tasks (title, description, status, created_at, updated_at, user_id)
VALUES
  ('Cleaning', 'Clean the room', 'PENDING', NOW(), NOW(), 1),
  ('Delivery', 'Deliver products to the grocery store', 'IN_PROGRESS', NOW(), NOW(), 2);
