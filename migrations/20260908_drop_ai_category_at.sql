-- ollama(exaone3.5:2.4b)로 기사 카테고리를 다시 분류하는 기능을 되돌리면서 남은 컬럼을 지운다.
-- 분류 정확도가 낮았고(첫 배치 30건 중 정치·경제·세계가 0건), 10분 주기로 도는 ollama가
-- 같은 VPS의 muni를 밀어내 중요도 판정까지 실패시켰다. 카테고리는 RSS 피드 값만 쓴다.
-- 잘못 분류된 60건은 피드 값으로 복구했고 이 컬럼의 값은 전부 NULL 이다.

ALTER TABLE articles DROP COLUMN ai_category_at;
