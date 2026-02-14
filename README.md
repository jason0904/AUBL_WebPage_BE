# AUBL Webpage BE

## API 명세

기본 URL
- `http://localhost:8080`

공통 응답
- 200: 성공
- 400: 잘못된 요청 (필수 값 누락/유효성 오류)
- 404: 미존재 (FK 대상, match 미존재 등)

## 실행 방법

1) 환경 변수 설정 (`.env`)
- 프로젝트 루트에 `.env` 파일 생성
```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=webpage
DB_USER=root
DB_PASSWORD=
FIREBASE_CREDENTIALS_PATH=C:/path/to/service-account.json
```

2) DB 스키마 준비
- `spring.jpa.hibernate.ddl-auto=none` 설정이므로 DB에 DDL을 먼저 생성해야 함
- `db.sql`을 실행해서 스키마를 생성

3) 실행
```bash
./gradlew bootRun
```

## Firebase 설정

필수 설정
- Firebase Admin SDK 서비스 계정 키(JSON) 준비
- `.env`에 `FIREBASE_CREDENTIALS_PATH` 추가

예시
```properties
FIREBASE_CREDENTIALS_PATH=C:/path/to/service-account.json
```

## 인증/인가
- Firebase ID Token 기반 Bearer 인증 사용. 요청 시 `Authorization: Bearer <ID_TOKEN>` 헤더 전달.
- 커스텀 클레임 `admin=true`이면 `ROLE_ADMIN` 부여, 아니면 기본 `ROLE_USER`.
- 보호 경로: `POST /api/seasons/**`, `POST /api/games/**` 는 `ROLE_ADMIN` 필요.
- 기타 엔드포인트는 현재 오픈(필요 시 추가 인가 정책 적용 가능).

### 0) 회원가입
- Method/Path: `POST /api/auth/signup`
- 요청
```json
{
  "email": "user@example.com",
  "password": "plain-text-password",
  "name": "홍길동",
  "phoneNumber": "010-1234-5678"
}
```
- 응답
```json
{ "id": 1 }
```
- 에러
- 400: `email`, `password`, `name` 누락
- 409: 이메일 중복

### 1) 시즌 생성
- Method/Path: `POST /api/seasons`
- 인증: 관리자(Authorization: Bearer Firebase ID token with admin=true)
- 요청
```json
{ "year": 2026 }
```
- 응답
```json
{ "id": 1 }
```
- 에러
- 400: `year` 누락
- 409/500: 연도 중복(DB 유니크 제약)

### 2) 팀 생성
- Method/Path: `POST /api/teams`
- 요청
```json
{
  "teamName": "Alpha College",
  "teamCode": "team-1",
  "managerId": null
}
```
- 응답
```json
{ "id": 1 }
```
- 에러
- 404: `managerId` 미존재

### 3) 선수 생성
- Method/Path: `POST /api/players`
- 요청
```json
{
  "userId": null,
  "playerName": "김지찬",
  "birthDate": "2000-01-01",
  "position": "2B",
  "height": 170,
  "weight": 70,
  "school": "AUBL HS",
  "isPlayer": true
}
```
- 응답
```json
{ "id": 1 }
```
- 에러
- 404: `userId` 미존재

### 4) 팀-선수 등록
- Method/Path: `POST /api/team-players`
- 요청
```json
{
  "teamId": 1,
  "playerId": 1,
  "seasonId": 1,
  "jerseyNumber": 1
}
```
- 응답
```json
{ "id": 1 }
```
- 에러
- 400: `teamId`, `playerId`, `seasonId` 누락
- 404: team/player/season 미존재
- 409/500: 중복 등록(유니크 제약)

### 5) 경기 생성
- Method/Path: `POST /api/games`
- 인증: 관리자(Authorization: Bearer Firebase ID token with admin=true)
- 요청
```json
{
  "seasonId": 1,
  "gameDate": "2026-02-01",
  "gameNumber": 1,
  "homeTeamId": 1,
  "awayTeamId": 2,
  "homeScore": 10,
  "awayScore": 7,
  "gameType": "정규시즌",
  "csvFilePath": null
}
```
- 응답
```json
{ "id": 1 }
```
- 에러
- 400: `seasonId`, `homeTeamId`, `awayTeamId` 누락
- 404: season/team 미존재

### 6) 선수 시즌 누적 기록 조회
- Method/Path: `GET /api/players/{playerId}/stats`
- Query: `seasonId` (옵션)
- 응답: 상단 `playerName`/`teamName`을 항상 채워서 반환(해당 데이터가 존재하는 경우)
```json
{
  "playerName": "김지찬",
  "teamName": "Alpha College",
  "batterStats": [
    {
      "id": 1,
      "teamPlayerId": 10,
      "seasonId": 1,
      "seasonType": "정규시즌",
      "gamesPlayed": 12,
      "plateAppearance": 45,
      "atBats": 40,
      "hits": 14,
      "homeRuns": 2,
      "battingAverage": 0.350,
      "onBasePct": 0.400,
      "sluggingPct": 0.500,
      "ops": 0.900
    }
  ],
  "pitcherStats": [
    {
      "id": 2,
      "teamPlayerId": 10,
      "seasonId": 1,
      "seasonType": "정규시즌",
      "gamesPlayed": 8,
      "gamesStarted": 3,
      "inningsPitched": 25.2,
      "wins": 2,
      "losses": 1,
      "saves": 0,
      "era": 2.45,
      "whip": 1.05,
      "kPer9": 8.70,
      "bbPer9": 2.10
    }
  ]
}
```
- 에러
- 404: `playerId` 미존재
- 404: `seasonId` 미존재(지정 시)

### 7) 선수 경기별 기록 조회
- Method/Path: `GET /api/players/{playerId}/game-logs`
- Query: `gameId` (옵션)
- 응답
```json
{
  "batterLogs": [
    {
      "id": 10,
      "gameId": 3,
      "teamId": 1,
      "teamSide": "home",
      "playerId": 5,
      "playerName": "Kim",
      "playerPosition": "SS",
      "atBats": 4,
      "runs": 1,
      "hits": 2,
      "rbi": 1,
      "walks": 0,
      "strikeouts": 1
    }
  ],
  "pitcherLogs": [
    {
      "id": 11,
      "gameId": 3,
      "teamId": 1,
      "teamSide": "home",
      "playerId": 5,
      "playerName": "Kim",
      "playerPosition": "SP",
      "inningsPitched": 6.0,
      "hitsAllowed": 5,
      "runsAllowed": 2,
      "earnedRuns": 2,
      "walks": 1,
      "strikeouts": 7
    }
  ]
}
```
- 에러
- 404: `playerId` 미존재
- 404: `gameId` 미존재(지정 시)

### 8) Firestore 경기 전체 Import (완료 경기)
- Method/Path: `POST /api/import/firestore/matches`
- 응답
```json
{
  "gamesProcessed": 10,
  "batterLogsInserted": 180,
  "pitcherLogsInserted": 40
}
```
- 에러
- 400: 데이터 오류(팀 코드/등번호 누락 등)
- 404: team/game 미존재

### 9) Firestore 경기 단건 Import
- Method/Path: `POST /api/import/firestore/matches/{matchId}`
- 응답
```json
{
  "gamesProcessed": 1,
  "batterLogsInserted": 18,
  "pitcherLogsInserted": 4
}
```
- 에러
- 400: 경기 미완료 또는 데이터 오류
- 404: match/team/game 미존재

### 10) 팀 목록 조회 (팀 코드 포함)
- Method/Path: `GET /api/teams` (또는 `/api/team`)
- 요청: 없음
- 응답
```json
[
  {
    "id": 1,
    "teamName": "Alpha College",
    "teamCode": "team-1"
  }
]
```
- 에러
- 기본적으로 없음 (비어 있으면 빈 배열)

### 11) 시즌 목록 조회 (기록실 시즌 선택용)
- Method/Path: `GET /api/seasons`
- 요청: 없음
- 응답
```json
[
  {
    "id": 1,
    "year": 2026
  }
]
```
- 에러
- 기본적으로 없음 (비어 있으면 빈 배열)

### 12) 시즌 기록실 오버뷰
- Method/Path: `GET /api/records/overview?seasonId=`
- 요청: `seasonId` 필수
- 응답 예시
```json
{
  "seasonId": 1,
  "totalGames": 20,
  "totalTeams": 4,
  "topBatter": {
    "playerId": 5,
    "playerName": "Kim",
    "teamId": 1,
    "teamName": "Alpha College",
    "seasonId": 1,
    "gamesPlayed": 12,
    "plateAppearance": 45,
    "atBats": 40,
    "hits": 14,
    "homeRuns": 2,
    "runsBattedIn": 10,
    "battingAverage": 0.350,
    "onBasePct": 0.400,
    "sluggingPct": 0.500,
    "ops": 0.900
  },
  "topPitcher": {
    "playerId": 6,
    "playerName": "Lee",
    "teamId": 2,
    "teamName": "Beta College",
    "seasonId": 1,
    "gamesPlayed": 8,
    "inningsPitched": 25.2,
    "wins": 2,
    "losses": 1,
    "saves": 0,
    "strikeouts": 30,
    "era": 2.45,
    "whip": 1.05
  }
}
```

### 13) 팀 순위
- Method/Path: `GET /api/records/teams?seasonId=&division=`
- 요청: `seasonId` 필수, `division` 현재 미사용(무시)
- 응답 예시
```json
[
  {
    "teamId": 1,
    "teamName": "Alpha",
    "wins": 10,
    "losses": 2,
    "ties": 0,
    "winPct": 0.833
  },
  {
    "teamId": 2,
    "teamName": "Beta",
    "wins": 7,
    "losses": 5,
    "ties": 0,
    "winPct": 0.583
  }
]
```

### 14) 타자 랭킹
- Method/Path: `GET /api/rankings/batters?seasonId=&limit=&sort=` (기존 `/api/records/batters`와 동일 동작)
- 요청: `seasonId` 필수, `limit` 기본 0(0 이하이면 전체, 생략 시 전체), 최대 100, `sort` 기본 `battingAverage`
- sort 옵션: `battingAverage`, `hits`, `homeRuns`, `rbi`, `ops`, `sluggingPct`, `onBasePct`
- 운영 권장: 실시간 계산보다 시즌 집계 테이블/뷰(예: `BATTER_STATS` 누적)에서 조회하도록 구성하세요.
- 응답 예시
```json
[
  {
    "rank": 1,
    "playerId": 5,
    "playerName": "Kim",
    "teamId": 1,
    "teamName": "Alpha College",
    "seasonId": 1,
    "gamesPlayed": 12,
    "plateAppearance": 45,
    "atBats": 40,
    "hits": 14,
    "homeRuns": 2,
    "runsBattedIn": 10,
    "stolenBases": 3,
    "walks": 6,
    "strikeouts": 8,
    "battingAverage": 0.350,
    "onBasePct": 0.400,
    "sluggingPct": 0.500,
    "ops": 0.900
  }
]
```

### 15) 투수 랭킹
- Method/Path: `GET /api/rankings/pitchers?seasonId=&limit=&sort=` (기존 `/api/records/pitchers`와 동일 동작)
- 요청: `seasonId` 필수, `limit` 기본 0(0 이하이면 전체, 생략 시 전체), 최대 100, `sort` 기본 `era`
- sort 옵션: `era`(오름차순), `whip`(오름차순), `strikeouts`, `wins`, `saves`
- 운영 권장: 실시간 계산보다 시즌 집계 테이블/뷰(예: `PITCHER_STATS` 누적)에서 조회하도록 구성하세요.
- 응답 예시
```json
[
  {
    "rank": 1,
    "playerId": 6,
    "playerName": "Lee",
    "teamId": 2,
    "teamName": "Beta College",
    "seasonId": 1,
    "gamesPlayed": 8,
    "inningsPitched": 25.2,
    "wins": 2,
    "losses": 1,
    "saves": 0,
    "strikeouts": 30,
    "walksAllowed": 8,
    "era": 2.45,
    "whip": 1.05
  }
]
```

## Firestore Import 참고
- 팀 매칭: `TEAM.team_code`
- 선수 매칭: `(teamId, seasonId, jerseyNumber, playerName)`
- 시즌: 경기 연도 기준 자동 생성
- 경기는 사전 등록 필요(자동 생성 안 함)

## CORS 설정
- 허용 도메인은 프로퍼티 `app.cors.allowed-origins`로 관리(쉼표 구분). 기본값: `http://localhost:3000`.
- 예시
```properties
app.cors.allowed-origins=https://www.example.com,https://admin.example.com,http://localhost:3000
```
- `setAllowCredentials(true)` 상태이므로 와일드카드(`*`) 대신 필요한 도메인만 명시하세요.
