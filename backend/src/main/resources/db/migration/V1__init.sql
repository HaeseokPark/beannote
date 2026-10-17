-- Phase 0: 스키마 버전 관리 시작점. 도메인 테이블은 Phase 1부터 V2 이후로 추가한다.
CREATE TABLE app_info (
    id         TINYINT      NOT NULL PRIMARY KEY,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO app_info (id) VALUES (1);
