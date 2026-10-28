-- 검색이 LIKE '%q%' 전체 스캔이라 기사가 10만 건대에서 요청당 수 초가 걸렸다 (초당 3건이면 p95 20초).
-- ngram 파서(토큰 2글자)는 한글 부분 문자열 검색을 인덱스로 처리한다. ArticleRepository.searchFulltext가 이 인덱스를 쓴다.
--
-- 배포 순서: 이 SQL을 먼저 실행한 뒤 애플리케이션을 배포한다. 인덱스가 없으면 /search가 500을 낸다.
-- 첫 FULLTEXT 인덱스를 만들 땐 테이블이 다시 만들어지며 그동안 쓰기(RSS 수집·조회수)가 막히므로 트래픽이 적은 시간에 실행한다.
-- 확인: SELECT INDEX_NAME, INDEX_TYPE FROM information_schema.STATISTICS WHERE TABLE_NAME = 'articles' AND INDEX_TYPE = 'FULLTEXT';

ALTER TABLE articles ADD FULLTEXT INDEX ft_article_title_description (title, description) WITH PARSER ngram;
