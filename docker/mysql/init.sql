-- Grant the flowboard application user full access to all flowboard databases
-- These databases are auto-created by each service via createDatabaseIfNotExist=true
GRANT ALL PRIVILEGES ON `flowboard_%`.* TO 'flowboard'@'%';
FLUSH PRIVILEGES;
