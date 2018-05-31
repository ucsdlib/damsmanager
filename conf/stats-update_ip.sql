ALTER TABLE stats_dlp_object_access ADD COLUMN client_ip varchar (45);
CREATE INDEX idx_stats_dlp_object_access_client_ip ON stats_dlp_object_access (client_ip);

ALTER TABLE stats_file_download ADD COLUMN client_ip varchar (45);
CREATE INDEX idx_stats_file_download_client_ip ON stats_file_download (client_ip);

