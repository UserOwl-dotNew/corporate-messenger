-- ============================================
-- Схема базы данных для Blockchain Messenger
-- ============================================

-- 1. Таблица чатов
CREATE TABLE IF NOT EXISTS chats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100),
    type VARCHAR(20) NOT NULL CHECK (type IN ('DIALOG', 'GROUP')),
    created_by UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 2. Таблица участников чатов
CREATE TABLE IF NOT EXISTS chat_participants (
    chat_id UUID REFERENCES chats(id) ON DELETE CASCADE,
    user_id UUID,
    joined_at TIMESTAMP DEFAULT NOW(),
    role VARCHAR(20) DEFAULT 'MEMBER' CHECK (role IN ('MEMBER', 'ADMIN')),
    PRIMARY KEY (chat_id, user_id)
);

-- 3. Таблица сообщений
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id UUID REFERENCES chats(id) ON DELETE CASCADE,
    sender_id UUID,
    encrypted_content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    blockchain_tx_id VARCHAR(255),
    sent_at TIMESTAMP DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);

-- 4. Таблица для эмуляции блокчейна
CREATE TABLE IF NOT EXISTS blockchain_emulator (
    hash VARCHAR(64) PRIMARY KEY,
    registered_at TIMESTAMP DEFAULT NOW(),
    registered_by VARCHAR(42) DEFAULT 'emulator'
);

-- 5. Создаем индексы для ускорения запросов
CREATE INDEX IF NOT EXISTS idx_messages_chat_id ON messages(chat_id);
CREATE INDEX IF NOT EXISTS idx_messages_sent_at ON messages(sent_at);
CREATE INDEX IF NOT EXISTS idx_chat_participants_user_id ON chat_participants(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_participants_chat_id ON chat_participants(chat_id);

-- 6. Вставляем тестовые данные
INSERT INTO chats (id, name, type, created_by)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'Test Chat', 'GROUP', '11111111-1111-1111-1111-111111111111')
ON CONFLICT (id) DO NOTHING;

INSERT INTO chat_participants (chat_id, user_id, role)
VALUES
    ('11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'ADMIN'),
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'MEMBER')
ON CONFLICT (chat_id, user_id) DO NOTHING;

-- 7. Показываем созданные таблицы
\dt

-- 8. Показываем данные
SELECT 'chats' as table_name, COUNT(*) as count FROM chats
UNION ALL
SELECT 'chat_participants', COUNT(*) FROM chat_participants
UNION ALL
SELECT 'messages', COUNT(*) FROM messages
UNION ALL
SELECT 'blockchain_emulator', COUNT(*) FROM blockchain_emulator;