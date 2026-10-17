CREATE TABLE roasteries (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    website_url VARCHAR(500),
    region      VARCHAR(100),
    is_seed     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_roasteries_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE beans (
    id                   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    roastery_id          BIGINT       NOT NULL,
    name                 VARCHAR(100) NOT NULL,
    name_normalized      VARCHAR(100) NOT NULL,
    origin_country       CHAR(2),
    region               VARCHAR(100),
    farm                 VARCHAR(100),
    process              VARCHAR(20)  NOT NULL,
    roast_level          VARCHAR(20)  NOT NULL,
    is_blend             BOOLEAN      NOT NULL,
    roasted_on           DATE,
    official_flavor_text VARCHAR(500),
    status               VARCHAR(20)  NOT NULL,
    created_by           BIGINT,
    merged_into_id       BIGINT,
    is_seed              BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    CONSTRAINT fk_beans_roastery FOREIGN KEY (roastery_id) REFERENCES roasteries (id),
    CONSTRAINT fk_beans_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_beans_merged_into FOREIGN KEY (merged_into_id) REFERENCES beans (id),
    CONSTRAINT ck_beans_process CHECK (process IN ('WASHED', 'NATURAL', 'HONEY', 'ANAEROBIC', 'OTHER')),
    CONSTRAINT ck_beans_roast_level CHECK (roast_level IN ('LIGHT', 'MEDIUM_LIGHT', 'MEDIUM', 'MEDIUM_DARK', 'DARK')),
    CONSTRAINT ck_beans_status CHECK (status IN ('ACTIVE', 'PENDING', 'DISCONTINUED')),
    -- 싱글오리진은 원산지 국가가 있고, 블렌드는 bean_blend_origins 에 여러 원산지를 둔다
    CONSTRAINT ck_beans_origin CHECK (is_blend OR origin_country IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_beans_name ON beans (name);
CREATE INDEX idx_beans_attrs ON beans (origin_country, process, roast_level);
CREATE INDEX idx_beans_roastery_name ON beans (roastery_id, name_normalized);

CREATE TABLE bean_varieties (
    bean_id BIGINT      NOT NULL,
    variety VARCHAR(50) NOT NULL,
    PRIMARY KEY (bean_id, variety),
    CONSTRAINT fk_bean_varieties_bean FOREIGN KEY (bean_id) REFERENCES beans (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE bean_blend_origins (
    bean_id      BIGINT  NOT NULL,
    country_code CHAR(2) NOT NULL,
    PRIMARY KEY (bean_id, country_code),
    CONSTRAINT fk_bean_blend_origins_bean FOREIGN KEY (bean_id) REFERENCES beans (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_bean_blend_origins_country ON bean_blend_origins (country_code);

CREATE TABLE bean_official_flavors (
    bean_id       BIGINT NOT NULL,
    flavor_tag_id BIGINT NOT NULL,
    PRIMARY KEY (bean_id, flavor_tag_id),
    CONSTRAINT fk_bean_official_flavors_bean FOREIGN KEY (bean_id) REFERENCES beans (id) ON DELETE CASCADE,
    CONSTRAINT fk_bean_official_flavors_tag FOREIGN KEY (flavor_tag_id) REFERENCES flavor_tags (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_bean_official_flavors_tag ON bean_official_flavors (flavor_tag_id);
