-- SQL Script per creare la tabella user_lists mancante

-- Verificare se la tabella NON esiste, poi crearla
CREATE TABLE IF NOT EXISTS user_lists (
    user_id BIGINT NOT NULL,
    list_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, list_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (list_id) REFERENCES lists(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_list_id (list_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Output confirmando che la tabella esiste
SELECT 'user_lists table created/verified successfully' AS status;

