INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('kkobak-media', 'kkobak-media', true, 10485760, ARRAY['image/jpeg', 'image/png', 'image/webp'])
ON CONFLICT (id) DO UPDATE SET
  public = EXCLUDED.public,
  file_size_limit = EXCLUDED.file_size_limit,
  allowed_mime_types = EXCLUDED.allowed_mime_types;

DROP POLICY IF EXISTS "Public read kkobak media" ON storage.objects;
CREATE POLICY "Public read kkobak media"
ON storage.objects FOR SELECT
USING (bucket_id = 'kkobak-media');
