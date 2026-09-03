-- articles.source 가 MySQL enum 컬럼으로 만들어져, NewsSource 에 나중에 추가한 상수
-- (조선일보·SBS·아이뉴스24 등)를 저장할 때 "Data truncated for column 'source'" 로 실패했다.
-- ddl-auto=update 는 기존 enum 목록을 갱신하지 못하므로 VARCHAR 로 직접 바꾼다.
-- 엔티티에는 @JdbcTypeCode(SqlTypes.VARCHAR) 를 붙여 앞으로 enum 으로 생성되지 않게 했다.

ALTER TABLE articles MODIFY COLUMN source VARCHAR(50) NOT NULL;
