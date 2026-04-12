CREATE DATABASE IF NOT EXISTS ai_trip_planner
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ai_trip_planner;

CREATE TABLE IF NOT EXISTS trip_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL UNIQUE,
  city VARCHAR(64) NOT NULL,
  travel_time VARCHAR(128) NOT NULL,
  budget DECIMAL(12,2) NOT NULL,
  request_json JSON NOT NULL,
  plan_json JSON NOT NULL,
  total_cost INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_city_created_at (city, created_at),
  INDEX idx_created_at (created_at)
);
