-- 시음 노트 (4-4) 와 원두 통계 (2-3)

CREATE TABLE tasting_notes (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    bean_id         BIGINT        NOT NULL,
    tasted_on       DATE          NOT NULL,
    brew_method     VARCHAR(20)   NOT NULL,
    dose_g          DECIMAL(5, 1),
    water_g         DECIMAL(6, 1),
    water_temp_c    INT,
    grind_text      VARCHAR(50),
    brew_time_sec   INT,
    acidity         INT           NOT NULL,
    sweetness       INT           NOT NULL,
    body            INT           NOT NULL,
    bitterness      INT           NOT NULL,
    aftertaste      INT           NOT NULL,
    rating          DECIMAL(2, 1) NOT NULL,
    would_buy_again BOOLEAN,
    memo            VARCHAR(1000),
    visibility      VARCHAR(10)   NOT NULL,
    photo_url       VARCHAR(500),
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,
    CONSTRAINT fk_tasting_notes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_tasting_notes_bean FOREIGN KEY (bean_id) REFERENCES beans (id),
    CONSTRAINT ck_tasting_notes_brew_method CHECK (brew_method IN
        ('POUR_OVER', 'ESPRESSO', 'AEROPRESS', 'FRENCH_PRESS', 'COLD_BREW', 'MOKA_POT', 'CAFE', 'OTHER')),
    CONSTRAINT ck_tasting_notes_axes CHECK (
        acidity BETWEEN 1 AND 5 AND sweetness BETWEEN 1 AND 5 AND body BETWEEN 1 AND 5
        AND bitterness BETWEEN 1 AND 5 AND aftertaste BETWEEN 1 AND 5),
    -- 0.5 ~ 5.0, 0.5 단위
    CONSTRAINT ck_tasting_notes_rating CHECK (rating BETWEEN 0.5 AND 5.0 AND rating * 2 = FLOOR(rating * 2)),
    CONSTRAINT ck_tasting_notes_visibility CHECK (visibility IN ('PRIVATE', 'PUBLIC')),
    CONSTRAINT ck_tasting_notes_recipe CHECK (
        (dose_g IS NULL OR dose_g > 0) AND (water_g IS NULL OR water_g > 0)
        AND (water_temp_c IS NULL OR water_temp_c BETWEEN 0 AND 100)
        AND (brew_time_sec IS NULL OR brew_time_sec > 0))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_tasting_notes_user_created ON tasting_notes (user_id, created_at DESC, id DESC);
CREATE INDEX idx_tasting_notes_bean_visibility ON tasting_notes (bean_id, visibility);

-- 노트당 최대 5개 (서비스 계층에서 검증)
CREATE TABLE tasting_note_flavors (
    note_id       BIGINT NOT NULL,
    flavor_tag_id BIGINT NOT NULL,
    PRIMARY KEY (note_id, flavor_tag_id),
    CONSTRAINT fk_tasting_note_flavors_note FOREIGN KEY (note_id) REFERENCES tasting_notes (id) ON DELETE CASCADE,
    CONSTRAINT fk_tasting_note_flavors_tag FOREIGN KEY (flavor_tag_id) REFERENCES flavor_tags (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 향미 자유 입력 원문. mapping_log_id 는 Phase 3 (향미 표준화) 에서 채운다.
CREATE TABLE tasting_note_free_flavors (
    note_id        BIGINT      NOT NULL,
    raw_text       VARCHAR(50) NOT NULL,
    mapping_log_id BIGINT,
    PRIMARY KEY (note_id, raw_text),
    CONSTRAINT fk_tasting_note_free_flavors_note FOREIGN KEY (note_id) REFERENCES tasting_notes (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 원두별 노트 통계. 노트가 바뀔 때 해당 원두만 다시 계산한다 (비공개 노트도 집계에 포함, 개별 노트는 노출하지 않음)
CREATE TABLE bean_stats (
    bean_id        BIGINT        NOT NULL PRIMARY KEY,
    note_count     INT           NOT NULL,
    avg_rating     DECIMAL(3, 2),
    avg_acidity    DECIMAL(3, 2),
    avg_sweetness  DECIMAL(3, 2),
    avg_body       DECIMAL(3, 2),
    avg_bitterness DECIMAL(3, 2),
    avg_aftertaste DECIMAL(3, 2),
    vector         JSON,
    updated_at     DATETIME(6)   NOT NULL,
    CONSTRAINT fk_bean_stats_bean FOREIGN KEY (bean_id) REFERENCES beans (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_bean_stats_popular ON bean_stats (note_count DESC);
CREATE INDEX idx_bean_stats_rating ON bean_stats (avg_rating DESC);
