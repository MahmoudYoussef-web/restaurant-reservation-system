-- Demo seed data for local development and UI screenshots.
-- Idempotent-friendly: uses INSERT IGNORE so re-runs never fail.

INSERT IGNORE INTO restaurants (id, name, location, opening_time, closing_time, phone, description, image_url)
VALUES
    (1, 'El Saraya Lounge', 'Cairo - Zamalek', '12:00:00', '23:59:00', '+201001234567',
     'Modern Egyptian fine dining with Nile view, grilled specialties and fresh seafood.',
     'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800'),
    (2, 'Casa Italia', 'Cairo - Maadi', '11:00:00', '23:00:00', '+201005556667',
     'Authentic Italian kitchen: wood-fired pizza, handmade pasta and classic desserts.',
     'https://images.unsplash.com/photo-1552566626-52f8b828add9?w=800'),
    (3, 'Sushi Bay', 'Giza - Sheikh Zayed', '12:30:00', '22:30:00', '+201009998887',
     'Fresh sushi and Asian fusion, live teppanyaki counter and family seating.',
     'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?w=800');

INSERT IGNORE INTO dining_tables (id, table_number, capacity, restaurant_id, table_status)
VALUES
    (1, 1, 2, 1, 'AVAILABLE'),
    (2, 2, 4, 1, 'AVAILABLE'),
    (3, 3, 6, 1, 'AVAILABLE'),
    (4, 4, 8, 1, 'AVAILABLE'),
    (5, 1, 2, 2, 'AVAILABLE'),
    (6, 2, 4, 2, 'AVAILABLE'),
    (7, 3, 6, 2, 'AVAILABLE'),
    (8, 1, 2, 3, 'AVAILABLE'),
    (9, 2, 4, 3, 'AVAILABLE'),
    (10, 3, 10, 3, 'AVAILABLE');

INSERT IGNORE INTO menu_categories (id, name, description)
VALUES
    (1, 'Starters', 'Light bites to open your meal'),
    (2, 'Grills & Mains', 'Signature main courses'),
    (3, 'Pizza & Pasta', 'Italian classics'),
    (4, 'Sushi & Asian', 'Fresh rolls and Asian plates'),
    (5, 'Desserts', 'Sweet endings'),
    (6, 'Beverages', 'Fresh juices, soft drinks and hot drinks');

INSERT IGNORE INTO menu_items (id, category_id, name, description, price, image_url, available)
VALUES
    (1, 1, 'Hummus Beiruti', 'Creamy hummus with parsley and olive oil', 85.00, NULL, TRUE),
    (2, 1, 'Bruschetta al Pomodoro', 'Toasted bread with tomato, basil and garlic', 95.00, NULL, TRUE),
    (3, 1, 'Gyoza (6 pcs)', 'Pan-fried chicken dumplings with ponzu sauce', 140.00, NULL, TRUE),
    (4, 2, 'Mixed Grill Platter', 'Kofta, shish tawook and lamb chops with rice', 320.00, NULL, TRUE),
    (5, 2, 'Grilled Salmon', 'Norwegian salmon with lemon butter sauce', 350.00, NULL, TRUE),
    (6, 3, 'Margherita Pizza', 'San Marzano tomato, fior di latte, basil', 180.00, NULL, TRUE),
    (7, 3, 'Tagliatelle Alfredo', 'Handmade pasta with creamy parmesan sauce', 210.00, NULL, TRUE),
    (8, 4, 'California Roll (8 pcs)', 'Crab, avocado, cucumber and tobiko', 195.00, NULL, TRUE),
    (9, 4, 'Chicken Teriyaki', 'Glazed chicken with steamed rice and vegetables', 230.00, NULL, TRUE),
    (10, 5, 'Om Ali', 'Traditional Egyptian bread pudding with nuts', 90.00, NULL, TRUE),
    (11, 5, 'Tiramisu Classico', 'Espresso-soaked ladyfingers with mascarpone', 120.00, NULL, TRUE),
    (12, 6, 'Fresh Mango Juice', 'Seasonal mango, no added sugar', 60.00, NULL, TRUE),
    (13, 6, 'Turkish Coffee', 'Slow-brewed with cardamom option', 45.00, NULL, TRUE);
