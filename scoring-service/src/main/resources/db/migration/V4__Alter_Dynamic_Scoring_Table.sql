ALTER TABLE dynamic_scoring ADD IF NOT EXISTS process_document_score_multiplier FLOAT NOT NULL DEFAULT 1;
ALTER TABLE dynamic_scoring ADD IF NOT EXISTS pmo_template_score_multiplier FLOAT NOT NULL DEFAULT 1 ;
ALTER TABLE dynamic_scoring ADD IF NOT EXISTS proposal_score_multiplier FLOAT NOT NULL DEFAULT 1;