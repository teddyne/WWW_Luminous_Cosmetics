-- Luminous - Phan 1/4: Migration 002 (chay DAU TIEN, sau luminous_db.sql goc)
-- Them cot User.skinQuizAsked + tao bang SkinRoutine. Chi chay 1 lan.
USE LUMINOUS_DB;

-- Phan 1: Migration 002
USE LUMINOUS_DB;

ALTER TABLE `User`
    ADD COLUMN skinQuizAsked BOOLEAN NOT NULL DEFAULT FALSE AFTER gender;

CREATE TABLE IF NOT EXISTS SkinRoutine (
    routineID INT AUTO_INCREMENT PRIMARY KEY,
    userID INT NOT NULL UNIQUE,
    quizID INT NOT NULL,
    skinType VARCHAR(50) NOT NULL,
    goals VARCHAR(255),
    budget VARCHAR(20),
    routineJson TEXT,
    createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt DATETIME NULL,
    CONSTRAINT chk_routine_type CHECK (skinType IN ('OILY','DRY','COMBINATION','NORMAL','SENSITIVE')),
    CONSTRAINT chk_routine_budget CHECK (budget IN ('TIET_KIEM','CAN_BANG','CAO_CAP','KHONG_GIOI_HAN')),
    CONSTRAINT fk_routine_user FOREIGN KEY (userID) REFERENCES `User`(userID) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_routine_quiz FOREIGN KEY (quizID) REFERENCES SkinQuiz(quizID) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

