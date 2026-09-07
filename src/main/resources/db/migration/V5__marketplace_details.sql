-- Professional imagery + cuisine metadata for the marketplace UI.
-- Safe to re-run: qualified UPDATEs only, no inserts.

ALTER TABLE restaurants
ADD COLUMN cuisine VARCHAR(50) NULL,
ADD COLUMN price_range VARCHAR(10) NULL;

UPDATE restaurants SET
    cuisine = 'Egyptian',
    price_range = '$$',
    image_url = 'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=1200&q=80&auto=format&fit=crop'
WHERE id = 1;

UPDATE restaurants SET
    cuisine = 'Italian',
    price_range = '$$',
    image_url = 'https://images.unsplash.com/photo-1552566626-52f8b828add9?w=1200&q=80&auto=format&fit=crop'
WHERE id = 2;

UPDATE restaurants SET
    cuisine = 'Japanese',
    price_range = '$$$',
    image_url = 'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?w=1200&q=80&auto=format&fit=crop'
WHERE id = 3;

UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1577805947697-89e18249d767?w=400&q=80&auto=format&fit=crop' WHERE id = 1;
UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1572449043416-55f4685c9bb7?w=400&q=80&auto=format&fit=crop' WHERE id = 2;
UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1496116218417-1a781b1c416c?w=400&q=80&auto=format&fit=crop' WHERE id = 3;
UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=400&q=80&auto=format&fit=crop' WHERE id = 4;
UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=400&q=80&auto=format&fit=crop' WHERE id = 5;
UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400&q=80&auto=format&fit=crop' WHERE id = 6;
UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1473093295043-cdd812d0e601?w=400&q=80&auto=format&fit=crop' WHERE id = 7;
UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1553621042-f6e147245754?w=400&q=80&auto=format&fit=crop' WHERE id = 8;
UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1532550907401-a500c9a57435?w=400&q=80&auto=format&fit=crop' WHERE id = 9;
UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1551024506-0bccd828d307?w=400&q=80&auto=format&fit=crop' WHERE id = 10;
UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=400&q=80&auto=format&fit=crop' WHERE id = 11;
UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1571068316344-75bc76f77890?w=400&q=80&auto=format&fit=crop' WHERE id = 12;
UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=400&q=80&auto=format&fit=crop' WHERE id = 13;
