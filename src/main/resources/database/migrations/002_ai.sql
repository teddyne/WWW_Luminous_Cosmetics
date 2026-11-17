use LUMINOUS_DB;
CREATE TABLE IF NOT EXISTS ai_document_vector (
                                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                  doc_id VARCHAR(100) NOT NULL UNIQUE,
                                                  doc_type VARCHAR(20) NOT NULL,
                                                  ref_id INT NULL,
                                                  content TEXT NOT NULL,
                                                  embedding LONGTEXT NOT NULL,
                                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                                  INDEX idx_ai_vector_type (doc_type)
);
CREATE TABLE IF NOT EXISTS ai_chat_message (
                                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               user_id INT NULL,
                                               session_id VARCHAR(64) NOT NULL,
                                               role VARCHAR(10) NOT NULL,
                                               content TEXT NOT NULL,
                                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               INDEX idx_ai_chat_session (session_id, created_at),
                                               INDEX idx_ai_chat_user (user_id, created_at)
);

CREATE TABLE IF NOT EXISTS ai_order_draft (
                                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              user_id INT NULL,
                                              session_id VARCHAR(64) NOT NULL,
                                              items_json TEXT NOT NULL,
                                              subtotal DECIMAL(12,0) NOT NULL DEFAULT 0,
                                              shipping_fee DECIMAL(12,0) NOT NULL DEFAULT 30000,
                                              total DECIMAL(12,0) NOT NULL DEFAULT 0,
                                              status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                                              expires_at TIMESTAMP NOT NULL,
                                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                              INDEX idx_ai_draft_session (session_id, status),
                                              INDEX idx_ai_draft_expires (expires_at)
);
