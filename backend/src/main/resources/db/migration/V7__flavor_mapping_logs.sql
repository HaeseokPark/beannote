-- 향미 표현 표준화 기록 (4-6). 노트 자유 입력과 원두 공식 설명을 표준 태그로 바꾼 결과를 남긴다.
-- REVIEW 항목은 관리자 검토 큐(F-4, P1)에서 확정한다.

CREATE TABLE flavor_mapping_logs (
    id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    raw_text      VARCHAR(100)  NOT NULL,
    normalized    VARCHAR(100)  NOT NULL,
    result        VARCHAR(10)   NOT NULL,
    score         DECIMAL(5, 2) NOT NULL,
    flavor_tag_id BIGINT,
    category_id   BIGINT,
    source        VARCHAR(10)   NOT NULL,
    source_id     BIGINT,
    reviewed_by   BIGINT,
    reviewed_at   DATETIME(6),
    created_at    DATETIME(6)   NOT NULL,
    CONSTRAINT fk_flavor_mapping_logs_tag FOREIGN KEY (flavor_tag_id) REFERENCES flavor_tags (id),
    CONSTRAINT fk_flavor_mapping_logs_category FOREIGN KEY (category_id) REFERENCES flavor_categories (id),
    CONSTRAINT fk_flavor_mapping_logs_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_flavor_mapping_logs_result CHECK (result IN ('EXACT', 'AUTO', 'REVIEW', 'UNMAPPED')),
    CONSTRAINT ck_flavor_mapping_logs_source CHECK (source IN ('NOTE', 'BEAN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_flavor_mapping_logs_result ON flavor_mapping_logs (result);
CREATE INDEX idx_flavor_mapping_logs_source ON flavor_mapping_logs (source, source_id);

ALTER TABLE tasting_note_free_flavors
    ADD CONSTRAINT fk_tasting_note_free_flavors_log
        FOREIGN KEY (mapping_log_id) REFERENCES flavor_mapping_logs (id) ON DELETE SET NULL;
