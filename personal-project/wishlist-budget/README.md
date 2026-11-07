
# 🎯 개인 과제 · 취미 위시리스트

Spring Boot 기반의 취미 아이템 관리 및 예산별 추천 REST API 개인 과제 입니다.

구매하고 싶은 아이템을 등록하고, 입력한 예산에 맞는 아이템을 가격이 저렴한 순서대로 최대 3개 추천받을 수 있습니다.

---

## ✨ 주요 기능

| 기능 | 구현 내용 |
|---|---|
| 아이템 등록 | 이름과 가격 등록, ID 자동 생성 |
| 전체 조회 | 등록된 모든 아이템 조회 |
| 단건 조회 | ID를 이용한 아이템 조회 |
| 예산별 추천 | 예산 이하 아이템을 저가순 최대 3개 추천 |
| 입력값 검증 | 빈 이름, 음수 가격 및 예산 방지 |
| 예외 처리 | 존재하지 않는 아이템 조회 시 404 반환 |
| 프론트엔드 | HTML·CSS·JavaScript 기반 웹 화면 구현 |
| 페이지 전환 | 위시리스트·아이템 등록·예산 추천 화면 전환 및 목록 페이징 |

### 💰 예산별 추천 규칙

1. 예산 이하의 아이템만 선택
2. 가격 오름차순 정렬
3. 최대 3개 추천
4. 동일 가격은 등록 순서 유지
5. 추천 가능한 아이템이 없으면 빈 목록 반환

> 예산은 아이템 가격의 합계가 아닌, 각 아이템의 개별 가격을 기준으로 합니다.

---

## 📡 REST API

| Method | Endpoint | 설명 |
|---|---|---|
| POST | `/collections` | 아이템 등록 |
| GET | `/collections` | 전체 아이템 조회 |
| GET | `/collections/{id}` | 특정 아이템 조회 |
| GET | `/collections/recommend?budget=50000` | 예산별 추천 |

---

## 🏗️ 프로젝트 구조

```text
com.example.wishlistbudget
├── controller
│   └── CollectionController
├── domain
│   └── CollectionItem
├── dto
│   ├── CollectionRequest
│   └── CollectionResponse
├── exception
│   └── CollectionItemNotFoundException
├── repository
│   ├── CollectionRepository
│   └── MemoryCollectionRepository
├── service
│   └── CollectionService
└── WishlistBudgetApplication

src/main/resources
├── static
│   ├── index.html
│   ├── style.css
│   └── script.js
└── application.properties
```

- **Controller** : HTTP 요청 및 응답 처리
- **Service** : 입력값 검증 및 추천 로직
- **Repository** : 메모리 기반 데이터 저장 및 조회
- **DTO** : 요청 및 응답 데이터 전달
- **Frontend** : 웹 화면 구성 및 Fetch API를 통한 백엔드 연동

---

## 🧪 테스트

JUnit 5와 Mockito를 활용하여 `CollectionService`의 주요 기능을 검증했습니다.

| 테스트 항목 | 검증 내용 |
|---|---|
| 아이템 등록 | 이름 공백 제거 및 저장 |
| 전체 조회 | 등록된 아이템 목록 조회 |
| 단건 조회 | ID로 아이템 조회 |
| 입력값 검증 | 잘못된 이름 및 가격 등록 방지 |
| 예외 처리 | 존재하지 않는 ID 조회 |
| 예산별 추천 | 가격 오름차순 및 최대 3개 |
| 무료 아이템 | 예산 0원일 때 추천 |
| 음수 예산 | 예외 발생 |

**테스트 실행**

```bash
./gradlew test
```

---

## 🚀 실행 방법

**1. 프로젝트 실행**

```bash
./gradlew bootRun
```

**2. API 호출**

```text
POST http://localhost:8080/collections

GET http://localhost:8080/collections

GET http://localhost:8080/collections/1

GET http://localhost:8080/collections/recommend?budget=50000
```

**3. 전체 빌드 및 테스트**

```bash
./gradlew clean build
```

> 메모리 기반 저장소를 사용하므로 서버를 종료하면 등록된 데이터는 초기화됩니다.

---

## 🖥️ 프론트엔드

순수 HTML·CSS·JavaScript로 SF 테마의 위시리스트 웹 화면을 구현했습니다.

외부 라이브러리 및 CDN을 사용하지 않고, JavaScript의 `fetch()`를 이용해 Spring Boot REST API와 연동했습니다.

- **위시리스트** : 등록된 아이템 목록 조회 및 4개 단위 페이징
- **아이템 등록** : 이름과 가격 입력 후 API를 통해 등록
- **예산 추천** : 예산 입력 후 추천 결과 표시
- **화면 전환** : 탭을 이용해 각 기능을 별도 화면으로 표시

Spring Boot 실행 후 아래 주소에서 웹 화면을 이용할 수 있습니다.

```text
http://localhost:8080
```

---

## 📝 구현 내용 정리

- **계층형 구조** : Controller, Service, Repository 역할 분리
- **의존성 주입** : Repository 인터페이스 기반 생성자 주입
- **입력값 검증** : Bean Validation 및 Service 검증
- **추천 알고리즘** : 예산 필터링, 가격 오름차순 정렬, 최대 3개 반환
- **테스트** : Mockito를 이용한 Service 단위 테스트
- **프론트엔드** : 순수 HTML·CSS·JavaScript로 웹 UI 구현
- **API 연동** : Fetch API를 이용한 아이템 등록·조회·추천 기능 연동
- **페이징** : JavaScript를 이용한 클라이언트 측 목록 페이징
