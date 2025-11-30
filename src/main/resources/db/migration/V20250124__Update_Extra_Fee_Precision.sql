-- Update extra_fee column precision to support VND amounts up to 999,999,999,999.99
ALTER TABLE category_pricing_detail 
ALTER COLUMN extra_fee TYPE NUMERIC(15,2);
