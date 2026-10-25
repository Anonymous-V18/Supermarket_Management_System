-- =====================================================================
-- Expand chat_message.content from TEXT (64KB) to MEDIUMTEXT (16MB)
-- Allows storing rich Markdown answers with embedded tables and CSV data links
-- =====================================================================

ALTER TABLE `chat_message` MODIFY COLUMN `content` MEDIUMTEXT NOT NULL;
