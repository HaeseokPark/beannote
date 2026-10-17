-- 로컬 개발용 DB와 계정을 만든다. MySQL root 계정으로 한 번만 실행한다.
--   mysql -u root -p < scripts/db/init-local.sql
-- 계정 비밀번호 'beanote' 는 localhost 전용 개발 기본값이다. 바꾸면 backend/.env 의 DB_PASSWORD 도 함께 바꾼다.

CREATE DATABASE IF NOT EXISTS beanote CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS beanote_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'beanote'@'localhost' IDENTIFIED BY 'beanote';
GRANT ALL PRIVILEGES ON beanote.* TO 'beanote'@'localhost';
GRANT ALL PRIVILEGES ON beanote_test.* TO 'beanote'@'localhost';
FLUSH PRIVILEGES;
