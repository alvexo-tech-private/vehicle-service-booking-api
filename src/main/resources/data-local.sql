-- =============================================================================
-- SEED DATA — Spring Boot compatible (PostgreSQL)
-- =============================================================================

-- ── USERS ─────────────────────────────────────────────────────────────────────

INSERT INTO users (
    email, mobile_number, password, first_name, last_name,
    role, active, email_verified, mobile_verified,
    city, state, country,
    created_at, updated_at
) VALUES
('admin@alvexo.com','9000000001',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lF36',
 'Admin','Alvexo','ADMINISTRATOR',true,true,true,
 'Chennai','Tamil Nadu','India',NOW(),NOW()),

('ravi.kumar@gmail.com','9000000002',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lF36',
 'Ravi','Kumar','VEHICLE_USER',true,true,true,
 'Chennai','Tamil Nadu','India',NOW(),NOW()),

('priya.suresh@gmail.com','9000000003',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lF36',
 'Priya','Suresh','VEHICLE_USER',true,true,true,
 'Madurai','Tamil Nadu','India',NOW(),NOW()),

('arun.venkat@gmail.com','9000000004',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lF36',
 'Arun','Venkat','VEHICLE_USER',true,false,true,
 'Coimbatore','Tamil Nadu','India',NOW(),NOW()),

('suresh.mech@gmail.com','9000000005',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lF36',
 'Suresh','Mech','MECHANIC',true,true,true,
 'Chennai','Tamil Nadu','India',NOW(),NOW()),

('murugan.auto@gmail.com','9000000006',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lF36',
 'Murugan','Auto','MECHANIC',true,true,true,
 'Madurai','Tamil Nadu','India',NOW(),NOW()),

('vijay.workshop@gmail.com','9000000007',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lF36',
 'Vijay','Workshop','MECHANIC',true,true,true,
 'Coimbatore','Tamil Nadu','India',NOW(),NOW())

ON CONFLICT (email) DO NOTHING;

-- ── MECHANIC PROFILE UPDATES ─────────────────────────────────────────────────

UPDATE users SET
    workshop_name = 'Suresh Auto Service',
    area = 'Anna Nagar',
    specialization = 'Two-Wheeler & Car Service',
    experience_years = 8,
    hourly_rate = 350.00,
    bio = 'Experienced mechanic specialising in Honda and Yamaha two-wheelers.',
    latitude = 13.0850,
    longitude = 80.2101,
    rating = 4.50,
    total_reviews = 42,
    total_bookings_completed = 120
WHERE email = 'suresh.mech@gmail.com';

UPDATE users SET
    workshop_name = 'Murugan Motor Works',
    area = 'K.K. Nagar',
    specialization = 'Car AC & Electrical',
    experience_years = 12,
    hourly_rate = 400.00,
    bio = 'AC specialist with 12 years of experience.',
    latitude = 9.9190,
    longitude = 78.1195,
    rating = 4.70,
    total_reviews = 65,
    total_bookings_completed = 210
WHERE email = 'murugan.auto@gmail.com';

UPDATE users SET
    workshop_name = 'Vijay Multi Brand Workshop',
    area = 'RS Puram',
    specialization = 'Multi-Brand Car Repair',
    experience_years = 6,
    hourly_rate = 300.00,
    bio = 'General car repairs and service.',
    latitude = 11.0025,
    longitude = 76.9527,
    rating = 4.20,
    total_reviews = 18,
    total_bookings_completed = 55
WHERE email = 'vijay.workshop@gmail.com';

-- ── VEHICLES ─────────────────────────────────────────────────────────────────

INSERT INTO vehicles (
    make, model, year, vehicle_type, engine_type, fuel_type,
    active, created_by, created_at, updated_at
)
SELECT
    v.make, v.model, v.year,
    v.vehicle_type,
    v.engine_type, v.fuel_type,
    true,
    u.id, NOW(), NOW()
FROM (VALUES
    ('Honda','Activa 6G',2022,'MOTORCYCLE','110cc OHC','Petrol'),
    ('Honda','City 5th Gen',2023,'CAR','1.5L i-VTEC','Petrol'),
    ('Yamaha','FZ-S FI V3',2021,'MOTORCYCLE','149cc SOHC','Petrol'),
    ('Maruti','Swift ZXI',2022,'CAR','1.2L DualJet','Petrol'),
    ('TVS','Apache RTR 160',2023,'MOTORCYCLE','159.7cc','Petrol'),
    ('Hyundai','i20 Asta',2022,'CAR','1.0L Turbo GDi','Petrol'),
    ('Tata','Nexon EV',2023,'SUV','Electric','Electric'),
    ('Royal Enfield','Classic 350',2021,'MOTORCYCLE','349cc','Petrol')
) AS v(make, model, year, vehicle_type, engine_type, fuel_type)
CROSS JOIN (SELECT id FROM users WHERE email='admin@alvexo.com' LIMIT 1) u
ON CONFLICT DO NOTHING;

-- ── MECHANIC AVAILABILITY (Example: Suresh only) ─────────────────────────────

INSERT INTO mechanic_availability (
    mechanic_id, day_of_week, start_time, end_time, is_available,
    slot_duration_minutes, max_slots_per_day, break_windows,
    created_at, updated_at
)
SELECT
    u.id,
    a.day_of_week,
    a.start_time::TIME,
    a.end_time::TIME,
    a.is_available,
    a.slot_duration_minutes,
    a.max_slots_per_day,
    a.break_windows::jsonb,
    NOW(), NOW()
FROM (VALUES
    ('MONDAY','09:00','18:00',true,60,8,'[{"start":"13:00","end":"14:00"}]'),
    ('TUESDAY','09:00','18:00',true,60,8,'[{"start":"13:00","end":"14:00"}]')
) AS a(day_of_week,start_time,end_time,is_available,slot_duration_minutes,max_slots_per_day,break_windows)
CROSS JOIN (SELECT id FROM users WHERE email='suresh.mech@gmail.com' LIMIT 1) u
ON CONFLICT DO NOTHING;