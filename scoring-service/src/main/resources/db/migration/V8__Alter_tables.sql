ALTER TABLE all_time_individual_score ADD IF NOT EXISTS tenant_id INT NOT NULL DEFAULT 1;
ALTER TABLE all_time_studio_score ADD IF NOT EXISTS tenant_id INT NOT NULL DEFAULT 1;
ALTER TABLE monthly_individual_score ADD IF NOT EXISTS tenant_id INT NOT NULL DEFAULT 1;
ALTER TABLE monthly_studio_score ADD IF NOT EXISTS tenant_id INT NOT NULL DEFAULT 1;
ALTER TABLE dynamic_scoring ADD IF NOT EXISTS tenant_id INT NOT NULL DEFAULT 1;
ALTER TABLE contribution_score ADD IF NOT EXISTS tenant_id INT NOT NULL DEFAULT 1;




