-- Add payment_breakdown_snapshot column to contracts table
-- This stores all pricing details at contract creation time to ensure historical accuracy

-- Add the column
ALTER TABLE public.contracts 
ADD COLUMN IF NOT EXISTS payment_breakdown_snapshot jsonb;

-- Add comment
COMMENT ON COLUMN public.contracts.payment_breakdown_snapshot IS 
'Payment breakdown snapshot - stores all pricing details at contract creation time to ensure historical accuracy. Includes vehicles, prices, distances, multipliers, surcharges, insurance rates, etc.';

-- Verify the change
SELECT 
    table_name,
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'contracts'
  AND column_name = 'payment_breakdown_snapshot';
