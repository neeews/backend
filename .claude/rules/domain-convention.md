# Domain (Entity) Convention

## Entity 기본 형태

```java
@Entity
@Table(name = "테이블명", indexes = { ... })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Xxx { ... }
```

- `@Setter`는 엔티티에 사용하지 않는다. 상태 변경은 의도가 드러나는 메서드로 노출한다 (`incrementViewCount()`, `updateCachedImage(...)`, `markContentCrawled()` 같은 이름).
- 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 막아, JPA 프록시 생성 외 외부에서 직접 호출하지 못하게 한다.
- 객체 생성은 `@Builder`를 통해서만 한다.
- 기본값이 있는 필드는 `@Builder.Default`를 명시한다 (빠뜨리면 빌더로 생성 시 값이 null/0으로 초기화됨).
- 생성 시각처럼 저장 시점에 결정되는 필드는 `@PrePersist` 메서드에서 채운다.

## 컬럼/인덱스

- 자주 조회/필터링되는 컬럼에는 `@Table(indexes = {...})`로 인덱스를 명시한다.
- 유니크 제약이 필요한 컬럼은 `@Index(..., unique = true)` 또는 `@Column(unique = true)`로 명시한다.
- `TEXT`/긴 문자열 컬럼은 `@Column(columnDefinition = "TEXT")` 또는 `length`를 명시한다.

## 연관관계

- 다른 도메인 엔티티와의 연관관계는 꼭 필요한 경우만 추가한다. Enum 참조로 충분하면(예: `NewsSource`) 연관관계 대신 Enum을 쓴다.
- 연관관계를 추가할 땐 지연 로딩(`FetchType.LAZY`)을 기본으로 하고, N+1 문제가 우려되면 fetch join이나 `@EntityGraph`를 Repository 쿼리에서 사용한다.
