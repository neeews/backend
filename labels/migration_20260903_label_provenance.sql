-- article_importance_labels 에 라벨 출처(labeled_by, origin)와 재라벨 회차(round_no)를 추가한다.
--
-- 배경: 이 테이블에는 기사당 라벨 1건만 담기고 누가 매겼는지 기록이 없다.
-- 기존 300건은 claude-opus-5 가 매긴 것인데(백엔드 c616972 커밋), 그 사실이 컬럼에 없어
-- 사람이 매긴 라벨과 구분할 방법이 사라졌다. 그 상태로 모델을 평가하면 정확도가 아니라
-- "모델이 opus 를 얼마나 흉내내나"를 재게 된다.
--
-- ddl-auto=update 는 PK를 바꾸지 못하므로 이 스크립트를 직접 실행해야 한다.
-- 실행 전 서비스를 내리거나, 최소한 라벨을 쓰는 배치가 돌지 않는 시간에 한다.

-- 0) 백업. 문제가 생기면 이 테이블에서 되돌린다.
CREATE TABLE article_importance_labels_bak_20260903
    AS SELECT * FROM article_importance_labels;

-- 1) FK가 기대는 인덱스를 미리 만든다.
--    article_id 는 PK 겸 FK라, 인덱스 없이 PK를 떼면 FK 제약이 깨진다(errno 1553).
ALTER TABLE article_importance_labels
    ADD INDEX idx_importance_label_article (article_id);

-- 2) 출처 컬럼 추가 — DEFAULT 로 기존 300건을 그대로 백필한다.
ALTER TABLE article_importance_labels
    ADD COLUMN labeled_by    VARCHAR(100) NOT NULL DEFAULT 'claude-opus-5',
    ADD COLUMN origin        VARCHAR(10)  NOT NULL DEFAULT 'AI',
    ADD COLUMN round_no      INT          NOT NULL DEFAULT 0,
    ADD COLUMN guide_version VARCHAR(40)  NULL;

-- 3) 기본값 제거. 앞으로 들어오는 행은 출처를 반드시 명시해야 한다.
ALTER TABLE article_importance_labels
    ALTER COLUMN labeled_by DROP DEFAULT,
    ALTER COLUMN origin     DROP DEFAULT,
    ALTER COLUMN round_no   DROP DEFAULT;

-- 4) PK 교체: article_id 단독 → 대리키.
--    기사 하나에 (AI 라벨 + 사람 라벨), (1회차 + 2회차)가 공존해야 하기 때문이다.
ALTER TABLE article_importance_labels
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- 5) 한 기사에 같은 사람이 같은 회차로 두 번 매기는 것만 막는다.
ALTER TABLE article_importance_labels
    ADD UNIQUE KEY uk_importance_label_article_labeler_round (article_id, labeled_by, round_no);

ALTER TABLE article_importance_labels
    ADD INDEX idx_importance_label_origin (origin);

-- 6) 확인 — 기존 라벨이 전부 AI/claude-opus-5 로 찍혔는지 본다.
SELECT origin, labeled_by, round_no, COUNT(*) AS cnt
FROM article_importance_labels
GROUP BY origin, labeled_by, round_no;
