-- "AI", "IT" 같은 영문 검색이 항상 0건이었다.
-- ngram 파서는 기본 불용어(a, i, it, in, is ...)가 들어 있는 토큰을 색인과 검색어 양쪽에서 통째로 버린다.
-- 그래서 ai(a·i 포함), it(불용어) 토큰이 색인에 없고, OpenAI·Apple 같은 영문 검색도 일부 토큰을 잃는다.
--
-- 불용어 목록은 인덱스를 만드는 순간 그 세션의 설정으로 인덱스에 묶인다.
-- SET GLOBAL은 이미 열린 세션에 적용되지 않으므로, 반드시 같은 세션에서 SET SESSION 후 인덱스를 다시 만든다.
-- 이후 이 인덱스를 다시 만들 일이 있으면 같은 방식(SET SESSION ... OFF)으로 만들어야 한다.
--
-- 실행 시점: 테이블이 다시 만들어지는 동안 쓰기(RSS 수집·조회수)가 막힌다(지난번 약 18만 건에 303초).
-- 트래픽이 적은 시간에 실행한다. 애플리케이션 코드 변경은 없어 배포 순서와 무관하다.
-- 하나의 ALTER로 DROP·ADD를 함께 해 인덱스가 없는 순간(/search 500)을 만들지 않는다.
--
-- 실행 전 확인(0이면 원인 확정):
--   SELECT COUNT(*) FROM articles WHERE MATCH(title, description) AGAINST('"AI"' IN BOOLEAN MODE);
--   SELECT COUNT(*) FROM articles WHERE title LIKE '%AI%' OR description LIKE '%AI%';
-- 실행 후 첫 번째 쿼리가 0보다 크게 나오면 적용된 것이다.

SET SESSION innodb_ft_enable_stopword = OFF;

ALTER TABLE articles
    DROP INDEX ft_article_title_description,
    ADD FULLTEXT INDEX ft_article_title_description (title, description) WITH PARSER ngram;
