SET @user_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'avatar_url');
SET @user_sql = IF(@user_col = 0, 'ALTER TABLE sys_user ADD COLUMN avatar_url VARCHAR(500) DEFAULT NULL', 'SELECT 1');
PREPARE user_stmt FROM @user_sql;
EXECUTE user_stmt;
DEALLOCATE PREPARE user_stmt;

SET @org_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_organization' AND COLUMN_NAME = 'avatar_url');
SET @org_sql = IF(@org_col = 0, 'ALTER TABLE sys_organization ADD COLUMN avatar_url VARCHAR(500) DEFAULT NULL', 'SELECT 1');
PREPARE org_stmt FROM @org_sql;
EXECUTE org_stmt;
DEALLOCATE PREPARE org_stmt;
