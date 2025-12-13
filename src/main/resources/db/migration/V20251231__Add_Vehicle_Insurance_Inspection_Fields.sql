-- Add vehicle inspection and insurance tracking fields
-- These fields are used by VehicleServiceRecordServiceImpl to track vehicle status

ALTER TABLE public.vehicles 
ADD COLUMN last_inspection_date DATE,
ADD COLUMN inspection_expiry_date DATE,
ADD COLUMN insurance_expiry_date DATE,
ADD COLUMN insurance_policy_number VARCHAR(50),
ADD COLUMN last_maintenance_date DATE;
