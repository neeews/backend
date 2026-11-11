-- "AI", "IT" 같은 영문 검색이 항상 0건이었다.
-- ngram 파서는 기본 불용어(a, i, it, in, is ...)가 들어 있는 토큰을 색인과 검색어 양쪽에서 통째로 버린다.
-- 그래서 ai(a·i 포함), it(불용어) 토큰이 색인에 없고, OpenAI·Apple 같은 영문 검색도 일부 토큰을 잃는다.
--
-- 불용어 사용 여부(use_stopword)는 인덱스가 아니라 테이블의 FULLTEXT 설정에 한 번 저장된다.
-- FULLTEXT 인덱스가 하나라도 남아 있으면 새 인덱스를 만들어도 처음 저장된 값(1)을 이어받는다.
-- 같은 이름으로 DROP·ADD를 한 ALTER에 넣으면 MySQL이 바뀐 게 없다고 보고 아무것도 하지 않는다(0초).
-- 그래서 기존 인덱스를 완전히 지운 뒤, 같은 세션에서 SET SESSION으로 불용어를 끄고 새로 만든다.
-- 이후 이 인덱스를 다시 만들 일이 있으면 같은 방식으로 만들어야 한다.
--
-- 실행 시점: 인덱스를 만드는 동안(23만 건에 약 6분) 검색이 500을 내고 쓰기(RSS 수집·조회수)가 막힌다.
-- 트래픽이 적은 시간에 실행한다. 애플리케이션 코드 변경은 없어 배포 순서와 무관하다.
-- SSH로 실행하면 출력이 없는 동안 연결이 끊길 수 있으니 ServerAliveInterval을 켠다(끊겨도 DB 안에서는 계속 진행된다).
--
-- 실행 전 확인(0이면 원인 확정):
--   SELECT COUNT(*) FROM articles WHERE MATCH(title, description) AGAINST('"AI"' IN BOOLEAN MODE);
--   SELECT COUNT(*) FROM articles WHERE title LIKE '%AI%' OR description LIKE '%AI%';
-- 실행 후 첫 번째 쿼리가 0보다 크게 나오면 적용된 것이다.

SET SESSION innodb_ft_enable_stopword = OFF;

ALTER TABLE articles DROP INDEX ft_article_title_description;
ALTER TABLE articles ADD FULLTEXT INDEX ft_article_title_description (title, description) WITH PARSER ngram;
