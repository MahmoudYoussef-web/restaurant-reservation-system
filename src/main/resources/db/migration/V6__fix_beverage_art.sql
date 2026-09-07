-- Fix beverage artwork (previous Unsplash ID resolved to a wrong photo).
UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1437418747212-8d9709afab22?w=400&q=80&auto=format&fit=crop' WHERE id = 12;
