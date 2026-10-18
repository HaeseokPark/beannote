-- 협업 필터링 이웃 (5-3, 5-6). 매일 새벽 배치가 통째로 다시 계산한다.
CREATE TABLE user_neighbors (
    user_id     BIGINT        NOT NULL,
    neighbor_id BIGINT        NOT NULL,
    similarity  DECIMAL(7, 6) NOT NULL,
    computed_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (user_id, neighbor_id),
    CONSTRAINT fk_user_neighbors_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_neighbors_neighbor FOREIGN KEY (neighbor_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
