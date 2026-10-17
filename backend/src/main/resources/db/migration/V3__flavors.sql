-- 향미 대분류·소분류·동의어 (기획서 3-2, 4-6)
-- SCA 플레이버 휠의 분류 개념을 참고해 자체 정의한 목록이다.

CREATE TABLE flavor_categories (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(30) NOT NULL,
    name_ko    VARCHAR(30) NOT NULL,
    sort_order INT         NOT NULL,
    CONSTRAINT uk_flavor_categories_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE flavor_tags (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT      NOT NULL,
    code        VARCHAR(30) NOT NULL,
    name_ko     VARCHAR(30) NOT NULL,
    name_en     VARCHAR(50) NOT NULL,
    sort_order  INT         NOT NULL,
    CONSTRAINT uk_flavor_tags_code UNIQUE (code),
    CONSTRAINT fk_flavor_tags_category FOREIGN KEY (category_id) REFERENCES flavor_categories (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- expression_normalized: NFKC + 소문자 + 공백 제거 (TextNormalizer 와 같은 규칙)
-- 태그 또는 대분류 중 정확히 하나를 가리킨다
CREATE TABLE flavor_synonyms (
    id                    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    flavor_tag_id         BIGINT,
    category_id           BIGINT,
    expression_normalized VARCHAR(100) NOT NULL,
    source                VARCHAR(10)  NOT NULL,
    CONSTRAINT uk_flavor_synonyms_expression UNIQUE (expression_normalized),
    CONSTRAINT fk_flavor_synonyms_tag FOREIGN KEY (flavor_tag_id) REFERENCES flavor_tags (id),
    CONSTRAINT fk_flavor_synonyms_category FOREIGN KEY (category_id) REFERENCES flavor_categories (id),
    CONSTRAINT ck_flavor_synonyms_target CHECK ((flavor_tag_id IS NULL) <> (category_id IS NULL)),
    CONSTRAINT ck_flavor_synonyms_source CHECK (source IN ('SEED', 'REVIEW'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO flavor_categories (code, name_ko, sort_order) VALUES
    ('FLORAL', '꽃', 1),
    ('BERRY', '베리', 2),
    ('CITRUS', '시트러스', 3),
    ('STONE_TROPICAL', '핵과·열대과일', 4),
    ('SWEET', '단맛', 5),
    ('NUTTY_COCOA', '견과·초콜릿', 6),
    ('SPICE', '향신료', 7),
    ('ROASTED', '로스팅', 8),
    ('FERMENTED', '발효·와인', 9),
    ('GREEN', '허브·녹색', 10);

INSERT INTO flavor_tags (category_id, code, name_ko, name_en, sort_order)
SELECT c.id, v.column_1, v.column_2, v.column_3, v.column_4
FROM (VALUES
    ROW('FLORAL', 'JASMINE', '자스민', 'Jasmine', 1),
    ROW('FLORAL', 'ROSE', '장미', 'Rose', 2),
    ROW('FLORAL', 'LAVENDER', '라벤더', 'Lavender', 3),
    ROW('FLORAL', 'BLACK_TEA', '홍차', 'Black Tea', 4),
    ROW('FLORAL', 'BERGAMOT', '베르가못', 'Bergamot', 5),
    ROW('BERRY', 'BLUEBERRY', '블루베리', 'Blueberry', 1),
    ROW('BERRY', 'STRAWBERRY', '딸기', 'Strawberry', 2),
    ROW('BERRY', 'RASPBERRY', '라즈베리', 'Raspberry', 3),
    ROW('BERRY', 'BLACKBERRY', '블랙베리', 'Blackberry', 4),
    ROW('BERRY', 'BLACKCURRANT', '블랙커런트', 'Blackcurrant', 5),
    ROW('CITRUS', 'LEMON', '레몬', 'Lemon', 1),
    ROW('CITRUS', 'ORANGE', '오렌지', 'Orange', 2),
    ROW('CITRUS', 'GRAPEFRUIT', '자몽', 'Grapefruit', 3),
    ROW('CITRUS', 'LIME', '라임', 'Lime', 4),
    ROW('CITRUS', 'TANGERINE', '귤', 'Tangerine', 5),
    ROW('STONE_TROPICAL', 'PEACH', '복숭아', 'Peach', 1),
    ROW('STONE_TROPICAL', 'APRICOT', '살구', 'Apricot', 2),
    ROW('STONE_TROPICAL', 'CHERRY', '체리', 'Cherry', 3),
    ROW('STONE_TROPICAL', 'PLUM', '자두', 'Plum', 4),
    ROW('STONE_TROPICAL', 'MANGO', '망고', 'Mango', 5),
    ROW('STONE_TROPICAL', 'PINEAPPLE', '파인애플', 'Pineapple', 6),
    ROW('STONE_TROPICAL', 'PASSION_FRUIT', '패션프루트', 'Passion Fruit', 7),
    ROW('STONE_TROPICAL', 'LYCHEE', '리치', 'Lychee', 8),
    ROW('SWEET', 'CARAMEL', '캐러멜', 'Caramel', 1),
    ROW('SWEET', 'HONEY', '꿀', 'Honey', 2),
    ROW('SWEET', 'BROWN_SUGAR', '흑설탕', 'Brown Sugar', 3),
    ROW('SWEET', 'MAPLE', '메이플', 'Maple', 4),
    ROW('SWEET', 'VANILLA', '바닐라', 'Vanilla', 5),
    ROW('NUTTY_COCOA', 'ALMOND', '아몬드', 'Almond', 1),
    ROW('NUTTY_COCOA', 'HAZELNUT', '헤이즐넛', 'Hazelnut', 2),
    ROW('NUTTY_COCOA', 'PEANUT', '땅콩', 'Peanut', 3),
    ROW('NUTTY_COCOA', 'MILK_CHOCOLATE', '밀크초콜릿', 'Milk Chocolate', 4),
    ROW('NUTTY_COCOA', 'DARK_CHOCOLATE', '다크초콜릿', 'Dark Chocolate', 5),
    ROW('NUTTY_COCOA', 'COCOA', '코코아', 'Cocoa', 6),
    ROW('SPICE', 'CINNAMON', '시나몬', 'Cinnamon', 1),
    ROW('SPICE', 'CLOVE', '정향', 'Clove', 2),
    ROW('SPICE', 'BLACK_PEPPER', '후추', 'Black Pepper', 3),
    ROW('SPICE', 'CARDAMOM', '카다멈', 'Cardamom', 4),
    ROW('ROASTED', 'BAKED_BREAD', '구운 빵', 'Baked Bread', 1),
    ROW('ROASTED', 'TOAST', '토스트', 'Toast', 2),
    ROW('ROASTED', 'SMOKY', '스모키', 'Smoky', 3),
    ROW('ROASTED', 'CEREAL', '곡물', 'Cereal', 4),
    ROW('FERMENTED', 'WINE', '와인', 'Wine', 1),
    ROW('FERMENTED', 'RUM', '럼', 'Rum', 2),
    ROW('FERMENTED', 'FERMENTED_FRUIT', '발효과일', 'Fermented Fruit', 3),
    ROW('FERMENTED', 'WHISKEY', '위스키', 'Whiskey', 4),
    ROW('GREEN', 'GRASS', '풀', 'Grass', 1),
    ROW('GREEN', 'HERB', '허브', 'Herb', 2),
    ROW('GREEN', 'PEA', '완두콩', 'Pea', 3),
    ROW('GREEN', 'GREEN_TEA', '녹차', 'Green Tea', 4)
) AS v
JOIN flavor_categories c ON c.code = v.column_0;

-- 소분류 동의어: 한글 이름, 영문, 표기 변형
INSERT INTO flavor_synonyms (flavor_tag_id, category_id, expression_normalized, source)
SELECT t.id, NULL, v.column_1, 'SEED'
FROM (VALUES
    ROW('JASMINE', '자스민'), ROW('JASMINE', '쟈스민'), ROW('JASMINE', '재스민'), ROW('JASMINE', 'jasmine'),
    ROW('ROSE', '장미'), ROW('ROSE', '로즈'), ROW('ROSE', 'rose'),
    ROW('LAVENDER', '라벤더'), ROW('LAVENDER', 'lavender'),
    ROW('BLACK_TEA', '홍차'), ROW('BLACK_TEA', '블랙티'), ROW('BLACK_TEA', 'blacktea'),
    ROW('BERGAMOT', '베르가못'), ROW('BERGAMOT', 'bergamot'), ROW('BERGAMOT', '얼그레이'), ROW('BERGAMOT', 'earlgrey'),
    ROW('BLUEBERRY', '블루베리'), ROW('BLUEBERRY', 'blueberry'),
    ROW('STRAWBERRY', '딸기'), ROW('STRAWBERRY', '스트로베리'), ROW('STRAWBERRY', 'strawberry'),
    ROW('RASPBERRY', '라즈베리'), ROW('RASPBERRY', '산딸기'), ROW('RASPBERRY', 'raspberry'),
    ROW('BLACKBERRY', '블랙베리'), ROW('BLACKBERRY', 'blackberry'),
    ROW('BLACKCURRANT', '블랙커런트'), ROW('BLACKCURRANT', 'blackcurrant'), ROW('BLACKCURRANT', '카시스'), ROW('BLACKCURRANT', 'cassis'),
    ROW('LEMON', '레몬'), ROW('LEMON', 'lemon'),
    ROW('ORANGE', '오렌지'), ROW('ORANGE', 'orange'),
    ROW('GRAPEFRUIT', '자몽'), ROW('GRAPEFRUIT', 'grapefruit'),
    ROW('LIME', '라임'), ROW('LIME', 'lime'),
    ROW('TANGERINE', '귤'), ROW('TANGERINE', '감귤'), ROW('TANGERINE', '탠저린'), ROW('TANGERINE', 'tangerine'), ROW('TANGERINE', '만다린'), ROW('TANGERINE', 'mandarin'),
    ROW('PEACH', '복숭아'), ROW('PEACH', '백도'), ROW('PEACH', 'peach'),
    ROW('APRICOT', '살구'), ROW('APRICOT', 'apricot'),
    ROW('CHERRY', '체리'), ROW('CHERRY', 'cherry'),
    ROW('PLUM', '자두'), ROW('PLUM', '플럼'), ROW('PLUM', 'plum'),
    ROW('MANGO', '망고'), ROW('MANGO', 'mango'),
    ROW('PINEAPPLE', '파인애플'), ROW('PINEAPPLE', 'pineapple'),
    ROW('PASSION_FRUIT', '패션프루트'), ROW('PASSION_FRUIT', '백향과'), ROW('PASSION_FRUIT', 'passionfruit'),
    ROW('LYCHEE', '리치'), ROW('LYCHEE', 'lychee'),
    ROW('CARAMEL', '캐러멜'), ROW('CARAMEL', '카라멜'), ROW('CARAMEL', 'caramel'),
    ROW('HONEY', '꿀'), ROW('HONEY', '허니'), ROW('HONEY', 'honey'),
    ROW('BROWN_SUGAR', '흑설탕'), ROW('BROWN_SUGAR', '황설탕'), ROW('BROWN_SUGAR', '비정제설탕'), ROW('BROWN_SUGAR', 'brownsugar'),
    ROW('MAPLE', '메이플'), ROW('MAPLE', '메이플시럽'), ROW('MAPLE', 'maple'), ROW('MAPLE', 'maplesyrup'),
    ROW('VANILLA', '바닐라'), ROW('VANILLA', 'vanilla'),
    ROW('ALMOND', '아몬드'), ROW('ALMOND', 'almond'),
    ROW('HAZELNUT', '헤이즐넛'), ROW('HAZELNUT', '헤즐넛'), ROW('HAZELNUT', 'hazelnut'),
    ROW('PEANUT', '땅콩'), ROW('PEANUT', 'peanut'),
    ROW('MILK_CHOCOLATE', '밀크초콜릿'), ROW('MILK_CHOCOLATE', '밀크초콜렛'), ROW('MILK_CHOCOLATE', '밀크쵸콜릿'), ROW('MILK_CHOCOLATE', 'milkchocolate'),
    ROW('DARK_CHOCOLATE', '다크초콜릿'), ROW('DARK_CHOCOLATE', '다크초콜렛'), ROW('DARK_CHOCOLATE', '다크쵸콜릿'), ROW('DARK_CHOCOLATE', 'darkchocolate'),
    ROW('COCOA', '코코아'), ROW('COCOA', 'cocoa'), ROW('COCOA', '카카오'), ROW('COCOA', 'cacao'), ROW('COCOA', '카카오닙스'), ROW('COCOA', 'cacaonibs'),
    ROW('CINNAMON', '시나몬'), ROW('CINNAMON', '계피'), ROW('CINNAMON', 'cinnamon'),
    ROW('CLOVE', '정향'), ROW('CLOVE', 'clove'),
    ROW('BLACK_PEPPER', '후추'), ROW('BLACK_PEPPER', '흑후추'), ROW('BLACK_PEPPER', 'blackpepper'), ROW('BLACK_PEPPER', 'pepper'),
    ROW('CARDAMOM', '카다멈'), ROW('CARDAMOM', '카더멈'), ROW('CARDAMOM', 'cardamom'),
    ROW('BAKED_BREAD', '구운빵'), ROW('BAKED_BREAD', '빵'), ROW('BAKED_BREAD', 'bakedbread'), ROW('BAKED_BREAD', 'bread'),
    ROW('TOAST', '토스트'), ROW('TOAST', 'toast'),
    ROW('SMOKY', '스모키'), ROW('SMOKY', '스모크'), ROW('SMOKY', 'smoky'), ROW('SMOKY', 'smoke'),
    ROW('CEREAL', '곡물'), ROW('CEREAL', '시리얼'), ROW('CEREAL', '보리'), ROW('CEREAL', 'cereal'), ROW('CEREAL', 'grain'), ROW('CEREAL', 'barley'),
    ROW('WINE', '와인'), ROW('WINE', '레드와인'), ROW('WINE', 'wine'), ROW('WINE', 'redwine'),
    ROW('RUM', '럼'), ROW('RUM', 'rum'),
    ROW('FERMENTED_FRUIT', '발효과일'), ROW('FERMENTED_FRUIT', 'fermentedfruit'),
    ROW('WHISKEY', '위스키'), ROW('WHISKEY', 'whiskey'), ROW('WHISKEY', 'whisky'),
    ROW('GRASS', '풀'), ROW('GRASS', '풀내음'), ROW('GRASS', 'grass'),
    ROW('HERB', '허브'), ROW('HERB', '허벌'), ROW('HERB', 'herb'), ROW('HERB', 'herbal'),
    ROW('PEA', '완두콩'), ROW('PEA', 'pea'),
    ROW('GREEN_TEA', '녹차'), ROW('GREEN_TEA', '말차'), ROW('GREEN_TEA', 'greentea'), ROW('GREEN_TEA', 'matcha')
) AS v
JOIN flavor_tags t ON t.code = v.column_0;

-- 대분류 동의어: 특정 소분류로 특정할 수 없는 넓은 표현
INSERT INTO flavor_synonyms (flavor_tag_id, category_id, expression_normalized, source)
SELECT NULL, c.id, v.column_1, 'SEED'
FROM (VALUES
    ROW('FLORAL', '꽃'), ROW('FLORAL', '꽃향'), ROW('FLORAL', '꽃향기'), ROW('FLORAL', '플로럴'), ROW('FLORAL', 'floral'),
    ROW('BERRY', '베리'), ROW('BERRY', '베리류'), ROW('BERRY', 'berry'), ROW('BERRY', 'berries'),
    ROW('CITRUS', '시트러스'), ROW('CITRUS', '시트릭'), ROW('CITRUS', 'citrus'),
    ROW('STONE_TROPICAL', '핵과'), ROW('STONE_TROPICAL', '열대과일'), ROW('STONE_TROPICAL', '트로피컬'), ROW('STONE_TROPICAL', 'stonefruit'), ROW('STONE_TROPICAL', 'tropicalfruit'),
    ROW('SWEET', '단맛'), ROW('SWEET', '달콤함'), ROW('SWEET', '스위트'), ROW('SWEET', 'sweet'),
    ROW('NUTTY_COCOA', '견과'), ROW('NUTTY_COCOA', '견과류'), ROW('NUTTY_COCOA', '고소함'), ROW('NUTTY_COCOA', '너티'), ROW('NUTTY_COCOA', 'nutty'), ROW('NUTTY_COCOA', '초콜릿'), ROW('NUTTY_COCOA', '초콜렛'), ROW('NUTTY_COCOA', 'chocolate'),
    ROW('SPICE', '향신료'), ROW('SPICE', '스파이스'), ROW('SPICE', '스파이시'), ROW('SPICE', 'spice'), ROW('SPICE', 'spicy'),
    ROW('ROASTED', '로스티'), ROW('ROASTED', '구운향'), ROW('ROASTED', 'roasted'),
    ROW('FERMENTED', '발효'), ROW('FERMENTED', '와이니'), ROW('FERMENTED', '부지'), ROW('FERMENTED', 'fermented'), ROW('FERMENTED', 'winey'), ROW('FERMENTED', 'boozy'),
    ROW('GREEN', '그린'), ROW('GREEN', '풋내'), ROW('GREEN', '식물성'), ROW('GREEN', 'green'), ROW('GREEN', 'vegetal')
) AS v
JOIN flavor_categories c ON c.code = v.column_0;
