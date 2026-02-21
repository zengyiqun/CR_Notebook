ALTER TABLE calendar_event ADD COLUMN end_date DATE NULL AFTER event_time;
ALTER TABLE calendar_event ADD COLUMN end_time TIME NULL AFTER end_date;
