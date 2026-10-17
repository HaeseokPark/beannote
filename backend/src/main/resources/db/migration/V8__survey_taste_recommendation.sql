-- 온보딩 취향 설문 (4-2), 취향 프로필 (4-5), 추천 피드백 (4-7)

CREATE TABLE taste_surveys (
    user_id         BIGINT      NOT NULL PRIMARY KEY,
    coffee_habit    VARCHAR(20) NOT NULL,
    pref_acidity    INT         NOT NULL,
    pref_sweetness  INT         NOT NULL,
    pref_body       INT         NOT NULL,
    pref_bitterness INT         NOT NULL,
    pref_aftertaste INT         NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    CONSTRAINT fk_taste_surveys_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_taste_surveys_habit CHECK (coffee_habit IN ('AMERICANO', 'LATTE', 'POUR_OVER', 'ESPRESSO')),
    CONSTRAINT ck_taste_surveys_prefs CHECK (
        pref_acidity BETWEEN 1 AND 5 AND pref_sweetness BETWEEN 1 AND 5 AND pref_body BETWEEN 1 AND 5
        AND pref_bitterness BETWEEN 1 AND 5 AND pref_aftertaste BETWEEN 1 AND 5)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE taste_survey_flavors (
    user_id     BIGINT      NOT NULL,
    category_id BIGINT      NOT NULL,
    preference  VARCHAR(10) NOT NULL,
    PRIMARY KEY (user_id, category_id),
    CONSTRAINT fk_taste_survey_flavors_survey FOREIGN KEY (user_id) REFERENCES taste_surveys (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_taste_survey_flavors_category FOREIGN KEY (category_id) REFERENCES flavor_categories (id),
    CONSTRAINT ck_taste_survey_flavors_pref CHECK (preference IN ('LIKE', 'DISLIKE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 사용자 취향 벡터. 본인 노트·설문이 바뀔 때 다시 계산한다
CREATE TABLE taste_profiles (
    user_id    BIGINT      NOT NULL PRIMARY KEY,
    vector     JSON,
    note_count INT         NOT NULL,
    confidence VARCHAR(10) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_taste_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_taste_profiles_confidence CHECK (confidence IN ('NONE', 'SURVEY', 'FORMING', 'STABLE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE recommendation_feedback (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    bean_id    BIGINT      NOT NULL,
    type       VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_recommendation_feedback UNIQUE (user_id, bean_id, type),
    CONSTRAINT fk_recommendation_feedback_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_recommendation_feedback_bean FOREIGN KEY (bean_id) REFERENCES beans (id) ON DELETE CASCADE,
    CONSTRAINT ck_recommendation_feedback_type CHECK (type IN ('NOT_INTERESTED', 'ALREADY_TRIED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
