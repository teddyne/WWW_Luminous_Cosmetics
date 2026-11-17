
CREATE TABLE IF NOT EXISTS AdminLoginHistory (
                                                 loginHistoryID BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                 userID INT NULL,
                                                 username VARCHAR(200) NOT NULL,
                                                 method VARCHAR(20) NOT NULL DEFAULT 'FORM',
                                                 success TINYINT(1) NOT NULL DEFAULT 1,
                                                 reason VARCHAR(200) NULL,
                                                 ipAddress VARCHAR(45) NULL,
                                                 userAgent VARCHAR(500) NULL,
                                                 loginAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                 INDEX idx_login_username (username),
                                                 INDEX idx_login_at (loginAt)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS AdminAuditLog (
                                             auditLogID BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             userID INT NULL,
                                             username VARCHAR(200) NOT NULL,
                                             httpMethod VARCHAR(10) NOT NULL,
                                             path VARCHAR(500) NOT NULL,
                                             handler VARCHAR(200) NULL,
                                             status INT NULL,
                                             ipAddress VARCHAR(45) NULL,
                                             createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             INDEX idx_audit_username (username),
                                             INDEX idx_audit_created (createdAt)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ShopSetting (
                                           settingID INT AUTO_INCREMENT PRIMARY KEY,
                                           settingKey VARCHAR(100) NOT NULL,
                                           settingValue TEXT NULL,
                                           updatedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                           CONSTRAINT uq_setting_key UNIQUE (settingKey)
) ENGINE=InnoDB;