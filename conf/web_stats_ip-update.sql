ALTER TABLE web_stats ADD COLUMN client_ip varchar (45);
CREATE INDEX idx_web_stats_client_ip ON web_stats (client_ip);

