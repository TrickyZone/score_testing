CREATE TABLE IF NOT EXISTS contribution_score(id SERIAL PRIMARY KEY NOT NULL,
                           	                full_name VARCHAR(100) NOT NULL,
                           	                email VARCHAR(100) NOT NULL,
                           	                contribution_id VARCHAR(50)  NULL,
                           	                contribution_type VARCHAR(100) NOT NULL,
                                           	title VARCHAR(255) NOT NULL,
                                        	contribution_date TIMESTAMP NOT NULL,
                                        	technology_details VARCHAR(200)  NULL,
                                        	url_details VARCHAR(1000)  NULL,
                                        	studio_name  VARCHAR(200)  NULL,
                                        	studio_id INT NOT NULL,
                                        	score DOUBLE PRECISION,
                                        	md5hash varchar(200),
                                        	CONSTRAINT md5hash_constraint UNIQUE (md5hash));

CREATE INDEX IF NOT EXISTS idx_contribution_score_email ON contribution_score(email, studio_id, contribution_date);

CREATE TABLE IF NOT EXISTS monthly_individual_score (
                                        email VARCHAR(100) NOT NULL,
                                        studio_id INT NOT NULL,
                                        score DOUBLE PRECISION,
                                        month INT NOT NULL,
                                        year INT NOT NULL,
                                        PRIMARY KEY(email,studio_id, month, year)
                                        );

CREATE INDEX IF NOT EXISTS idx_monthly_individual_score_email ON monthly_individual_score(email,studio_id, month, year);


CREATE TABLE IF NOT EXISTS monthly_studio_score (
                                         studio_id INT NOT NULL,
                                         score DOUBLE PRECISION,
                                         month INT NOT NULL,
                                         year INT NOT NULL,
                                         PRIMARY KEY(studio_id, month, year)
                                         );

CREATE TABLE IF NOT EXISTS all_time_individual_score (
                                     email VARCHAR(100)  NOT NULL,
                                     studio_id INT NOT NULL,
                                     score DOUBLE PRECISION,
                                     PRIMARY KEY(email,studio_id));

CREATE INDEX IF NOT EXISTS idx_all_time_individual_score_email ON monthly_individual_score(email,studio_id);

CREATE TABLE IF NOT EXISTS all_time_studio_score (
                                         studio_id INT PRIMARY KEY NOT NULL,
                                         score DOUBLE PRECISION);

CREATE TABLE IF NOT EXISTS dynamic_scoring(
 id Serial PRIMARY KEY,
 blog_score_multiplier FLOAT NOT NULL,
 knolx_score_multiplier FLOAT NOT NULL,
 webinar_score_multiplier FLOAT NOT NULL,
 os_contribution_score_multiplier FLOAT NOT NULL,
 techhub_score_multiplier FLOAT NOT NULL,
 conference_score_multiplier FLOAT NOT NULL,
 book_score_multiplier FLOAT NOT NULL,
 research_paper_score_multiplier FLOAT NOT NULL,
 meetup_score_multiplier FLOAT NOT NULL,
 month INT,
 year INT NOT NULL
);