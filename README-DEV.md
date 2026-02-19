# Cat Food Recommend — 개발자 문서

## 프로젝트 개요

고양이의 체중·나이·성별·중성화 여부·예산을 입력받아 NRC/AAFCO 칼로리 계산 공식을 기반으로 적정 사료를 추천하는 Spring Boot 웹 애플리케이션.

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.0 |
| Template | Thymeleaf |
| Build | Maven |
| Frontend | Vanilla JS / CSS |
| Container | Docker |

---

## 프로젝트 구조

```
cat-food-recommend/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── Procfile
├── README.md                          # 일반 사용자용
├── README-DEV.md                      # 개발자용 (현재 파일)
└── src/
    ├── main/
    │   ├── java/com/catfood/
    │   │   ├── CatFoodApplication.java
    │   │   ├── controller/
    │   │   │   ├── IndexController.java       # GET /  → index.html
    │   │   │   └── CatFoodController.java     # POST /api/recommend
    │   │   ├── dto/
    │   │   │   ├── RecommendRequest.java      # 요청 DTO (검증 포함)
    │   │   │   ├── RecommendResponse.java     # 응답 DTO
    │   │   │   └── FoodRecommendation.java    # 개별 사료 추천 결과
    │   │   ├── model/
    │   │   │   └── CatFood.java              # 사료 데이터 모델
    │   │   ├── service/
    │   │   │   ├── CalorieCalculationService.java  # RER/MER 계산
    │   │   │   └── CatFoodRecommendService.java    # 추천 로직 + 사료 DB
    │   │   └── exception/
    │   │       └── GlobalExceptionHandler.java
    │   └── resources/
    │       ├── application.properties
    │       ├── application-prod.properties
    │       ├── templates/index.html
    │       └── static/
    │           ├── css/style.css
    │           └── js/app.js
    └── test/
        ├── java/com/catfood/
        │   ├── controller/CatFoodControllerTest.java
        │   └── service/CalorieCalculationServiceTest.java
        └── resources/application-test.properties
```

---

## 칼로리 계산 공식

### RER (Resting Energy Requirement / 기초대사량)

```
RER = 70 × BW(kg)^0.75  [kcal/day]
```

### MER (Maintenance Energy Requirement / 일일 권장 칼로리)

```
MER = RER × 생애 단계 계수
```

| 생애 단계 | 계수 |
|---|---|
| 신생 키튼 (0~3개월) | 3.0 |
| 성장기 키튼 (4~11개월) | 2.5 |
| 성체 중성화 수컷 (12~83개월) | 1.6 |
| 성체 중성화 암컷 (12~83개월) | 1.4 |
| 성체 미중성화 수컷 (12~83개월) | 1.8 |
| 성체 미중성화 암컷 (12~83개월) | 1.6 |
| 노령묘 (84개월+) | 1.4 |

출처: NRC (National Research Council) - *Nutrient Requirements of Cats* / AAFCO

---

## API 명세

### POST `/api/recommend`

**Request Body**

```json
{
  "weightKg": 4.0,
  "ageMonths": 36,
  "gender": "MALE",
  "neutered": true,
  "monthlyBudget": 50000
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| `weightKg` | Double | 0.1 ~ 20.0 |
| `ageMonths` | Integer | 1 ~ 300 |
| `gender` | String | `MALE` 또는 `FEMALE` |
| `neutered` | Boolean | required |
| `monthlyBudget` | Integer | 1000 ~ 1000000 (원) |

**Response Body**

```json
{
  "dailyCalories": 317.5,
  "rerCalories": 198.5,
  "lifeFactor": 1.6,
  "lifeStageDescription": "성체 중성화 수컷 (1~7세)",
  "formulaDescription": "NRC/AAFCO 권장 칼로리 계산 공식\n...",
  "recommendations": [
    {
      "rank": 1,
      "foodName": "하이큐 슈프림 어덜트",
      "brand": "Hiq",
      "type": "DRY",
      "dailyAmountGrams": 84.7,
      "dailyCost": 3812,
      "monthlyCost": 38120,
      "proteinPercent": 34.0,
      "fatPercent": 16.0,
      "reason": "균형 잡힌 단백질 함량, 합리적인 가격 (예산의 76% 사용)",
      "score": 30.0
    }
  ]
}
```

---

## 추천 점수 산정

```
score = proteinPercent × 0.6 + budgetRemainingRatio × 0.4

budgetRemainingRatio = (monthlyBudget - monthlyCost) / monthlyBudget × 100
```

- 예산 초과 사료는 후보에서 제외
- 나이 적합성 필터 적용 후 점수 순 정렬, 상위 5개 반환

### 나이 적합성 필터

| 사료 태그 | 키튼 (<12개월) | 성체 (12~83개월) | 노령묘 (84개월+) |
|---|---|---|---|
| `KITTEN` | ✅ | ❌ | ❌ |
| `ADULT` | ❌ | ✅ | ✅ |
| `SENIOR` | ❌ | ❌ | ✅ |
| `ALL` | ✅ | ✅ | ✅ |

---

## 실행 방법

### 로컬 실행 (Maven)

```bash
cd cat-food-recommend
./mvnw spring-boot:run
# 또는
mvn spring-boot:run
```

`http://localhost:8080` 접속

### 테스트 실행

```bash
mvn test
```

### Docker 실행

```bash
docker-compose up --build
```

### JAR 빌드 후 실행

```bash
mvn clean package -DskipTests
java -jar target/cat-food-recommend-1.0.0.jar
```

---

## 사료 데이터 추가/수정

`CatFoodRecommendService.java` 내 `initializeFoodDatabase()` 메서드에 하드코딩되어 있음.

```java
db.add(new CatFood(
    "id",           // 고유 ID
    "사료 이름",
    "브랜드",
    "DRY",          // DRY 또는 WET
    "ADULT",        // KITTEN / ADULT / SENIOR / ALL
    385.0,          // 100g당 칼로리 (kcal)
    50000,          // kg당 가격 (원)
    35.0,           // 단백질 함량 (%)
    13.0,           // 지방 함량 (%)
    "설명"
));
```

> 향후 DB 연동 시 `CatFoodRepository` 인터페이스 추가 후 이 메서드를 대체하면 됨.

---

## 환경 변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `PORT` | `8080` | 서버 포트 (운영 환경) |
| `SPRING_PROFILES_ACTIVE` | `default` | `prod` 설정 시 캐시 활성화 |
