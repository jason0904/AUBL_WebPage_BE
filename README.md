# AUBL Webpage BE

## API 명세

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
- 보호 경로: `POST /api/seasons/**`, `POST /api/games/**`, `POST /api/admin/**` 는 `ROLE_ADMIN` 필요.
- 기타 엔드포인트는 현재 오픈(필요 시 추가 인가 정책 적용 가능).

---

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
  - 409/500: 이메일 중복

### 1) 시즌 생성
- Method/Path: `POST /api/seasons`
- 인증: 관리자(`Authorization: Bearer Firebase ID token with admin=true`)
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
- 인증: 관리자(`Authorization: Bearer Firebase ID token with admin=true`)
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
- 응답: 상단 `playerName`/`teamName`/`jerseyNumber`를 항상 채워서 반환(해당 데이터가 존재하는 경우)
```json
{
  "playerName": "김지찬",
  "teamName": "Alpha College",
  "jerseyNumber": 10,
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
      "ops": 0.900,
      "jerseyNumber": 10
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
      "bbPer9": 2.10,
      "jerseyNumber": 10
    }
  ]
}
```
- 에러
  - 404: `playerId` 미존재
  - 404: `seasonId` 미존재(지정 시)

> **참고**: `batterStats` 항목에는 `runsBattedIn`, `stolenBases`, `walks`, `strikeouts` 필드가 포함되지 않습니다. 해당 수치는 랭킹 API(`/api/rankings/batters`)에서 제공됩니다.

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
      "strikeouts": 1,
      "jerseyNumber": 10
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
      "strikeouts": 7,
      "jerseyNumber": 10
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

### 9) Firestore 경기 단건 Import (idempotent)
- Method/Path: `POST /api/import/firestore/matches/{matchId}`
- 동작: 동일 `matchId` 재호출 시 결과 불변(game log는 game 단위 delete+reinsert)
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

### 10) 팀 목록 조회
- Method/Path: `GET /api/teams`
- 요청: 없음
- 응답
```json
[
  {
    "id": 1,
    "teamName": "Alpha College",
    "teamCode": "team-1",
    "active": true
  }
]
```
- 에러: 비어 있으면 빈 배열

### 11) 시즌 목록 조회
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
- 에러: 비어 있으면 빈 배열

### 12) 시즌 기록실 오버뷰
- Method/Path: `GET /api/records/overview`
- Query: `seasonId` 필수, `scope` 옵션, `group` 옵션, `partCode` 옵션, `playoffDivision` 옵션, `division` 옵션
- 요청 예시
```
GET /api/records/overview?seasonId=1
GET /api/records/overview?seasonId=1&scope=LEAGUE&group=A
```
- 응답 예시
```json
{
  "seasonId": 1,
  "totalGames": 20,
  "totalTeams": 4,
  "topBatter": {
    "rank": 1,
    "playerId": 5,
    "playerName": "Kim",
    "teamId": 1,
    "teamName": "Alpha College",
    "jerseyNumber": 10,
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
    "ops": 0.900,
    "partCode": null,
    "group": null,
    "scope": null,
    "seasonType": null,
    "regulation": null
  },
  "topPitcher": {
    "rank": 1,
    "playerId": 6,
    "playerName": "Lee",
    "teamId": 2,
    "teamName": "Beta College",
    "jerseyNumber": 18,
    "seasonId": 1,
    "gamesPlayed": 8,
    "inningsPitched": 25.2,
    "wins": 2,
    "losses": 1,
    "saves": 0,
    "strikeouts": 30,
    "walksAllowed": 8,
    "era": 2.45,
    "whip": 1.05,
    "partCode": null,
    "group": null,
    "scope": null,
    "seasonType": null,
    "regulation": null
  }
}
```

### 13) 팀 순위
- Method/Path: `GET /api/records/teams`
- Query: `seasonId` 필수, `scope` 옵션, `group` 옵션, `partCode` 옵션, `playoffDivision` 옵션, `division` 옵션
- 요청 예시
```
GET /api/records/teams?seasonId=1
GET /api/records/teams?seasonId=1&scope=LEAGUE&group=A
```
- 응답 예시
```json
[
  {
    "teamId": 1,
    "teamName": "Alpha",
    "wins": 10,
    "losses": 2,
    "ties": 0,
    "winPct": 0.833,
    "partCode": "1",
    "group": "A",
    "scope": "LEAGUE",
    "seasonType": null
  },
  {
    "teamId": 2,
    "teamName": "Beta",
    "wins": 7,
    "losses": 5,
    "ties": 0,
    "winPct": 0.583,
    "partCode": "2",
    "group": "B",
    "scope": "LEAGUE",
    "seasonType": null
  }
]
```

### 14) 타자 랭킹
- Method/Path: `GET /api/rankings/batters`
- Query:
  - `seasonId` 필수
  - `limit` 기본 0 (0 이하이면 전체), 최대 100
  - `sort` 기본 `battingAverage`
  - `sortOrder` 옵션: `asc | desc`
  - `scope` 옵션: `ALL | LEAGUE | PLAYOFF`
  - `group` 옵션: `ALL | A | B | C | D | E | F | G | H`
  - `partCode` 옵션: `1`~`8` (group alias)
  - `playoffDivision` 옵션: `ALL | EUTTEUM | BEOGEUM`
  - `division` 옵션: `playoffDivision` legacy alias
  - `regulation` 옵션: `IN | OUT | ALL`
- sort 허용값: `battingAverage`, `hits`, `homeRuns`, `rbi`, `ops`, `sluggingPct`, `onBasePct`, `gamesPlayed`, `plateAppearance`, `stolenBases`
- 요청 예시
```
GET /api/rankings/batters?seasonId=1
GET /api/rankings/batters?seasonId=1&limit=5&sort=homeRuns&regulation=IN
GET /api/rankings/batters?seasonId=1&scope=LEAGUE&group=A
```
- 응답 예시
```json
[
  {
    "rank": 1,
    "playerId": 5,
    "playerName": "Kim",
    "teamId": 1,
    "teamName": "Alpha College",
    "jerseyNumber": 10,
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
    "ops": 0.900,
    "partCode": "1",
    "group": "A",
    "scope": "LEAGUE",
    "seasonType": null,
    "regulation": "IN"
  }
]
```

### 15) 투수 랭킹
- Method/Path: `GET /api/rankings/pitchers`
- Query:
  - `seasonId` 필수
  - `limit` 기본 0 (0 이하이면 전체), 최대 100
  - `sort` 기본 `era`
  - `sortOrder` 옵션: `asc | desc`
  - `scope` 옵션: `ALL | LEAGUE | PLAYOFF`
  - `group` 옵션: `ALL | A | B | C | D | E | F | G | H`
  - `partCode` 옵션: `1`~`8` (group alias)
  - `playoffDivision` 옵션: `ALL | EUTTEUM | BEOGEUM`
  - `division` 옵션: `playoffDivision` legacy alias
  - `regulation` 옵션: `IN | OUT | ALL`
- sort 허용값: `era`(오름차순), `whip`(오름차순), `strikeouts`, `wins`, `saves`, `inningsPitched`, `walksAllowed`, `gamesPlayed`
- 요청 예시
```
GET /api/rankings/pitchers?seasonId=1
GET /api/rankings/pitchers?seasonId=1&limit=5&sort=era&regulation=IN
GET /api/rankings/pitchers?seasonId=1&scope=PLAYOFF&playoffDivision=EUTTEUM
```
- 응답 예시
```json
[
  {
    "rank": 1,
    "playerId": 6,
    "playerName": "Lee",
    "teamId": 2,
    "teamName": "Beta College",
    "jerseyNumber": 18,
    "seasonId": 1,
    "gamesPlayed": 8,
    "inningsPitched": 25.2,
    "wins": 2,
    "losses": 1,
    "saves": 0,
    "strikeouts": 30,
    "walksAllowed": 8,
    "era": 2.45,
    "whip": 1.05,
    "partCode": "1",
    "group": "A",
    "scope": "LEAGUE",
    "seasonType": null,
    "regulation": "IN"
  }
]
```

### 16) 팀/시즌별 선수 목록 조회
- Method/Path: `GET /api/team-players?teamId=&seasonId=`
- 요청: `teamId` 필수, `seasonId` 필수
- 응답 예시
```json
[
  {
    "teamPlayerId": 10,
    "teamId": 1,
    "teamName": "Alpha College",
    "seasonId": 1,
    "playerId": 5,
    "playerName": "Kim",
    "jerseyNumber": 10,
    "position": "SS"
  }
]
```
- 에러
  - 400: `teamId`, `seasonId` 누락
  - 404: team 또는 season 미존재

### 17) 시즌 등록 팀 목록 조회 (경기 미기록 팀 포함)
- Method/Path: `GET /api/seasons/{seasonId}/teams`
- 요청: `seasonId` 필수(Path)
- 동작: `TEAM_PLAYER` 기준으로 시즌 등록 팀을 반환. `GAME` 기록 유무와 무관하게 포함.
- 요청 예시
```
GET /api/seasons/11/teams
```
- 응답 예시
```json
[
  {
    "seasonId": 11,
    "teamId": 34,
    "teamName": "연세대학교 EAGLES",
    "teamCode": "yonsei"
  }
]
```
- 에러: 400 seasonId 형식 오류, 404 season 미존재

### 18) 시즌 선수 로스터 조회 (무기록 선수 포함)
- Method/Path: `GET /api/players/roster?seasonId=&teamId=&q=&limit=&cursor=`
- 요청: `seasonId` 필수, `teamId` 옵션, `q` 옵션(이름 부분검색, 공백 무시), `limit` 기본 50/최대 500, `cursor` 옵션(Base64)
- 동작: `TEAM_PLAYER + PLAYER + TEAM` 기반 조회. 랭킹/기록 테이블 의존 금지.
- 요청 예시
```
GET /api/players/roster?seasonId=11
GET /api/players/roster?seasonId=11&teamId=34&q=강대건
```
- 응답 예시
```json
{
  "items": [
    {
      "seasonId": 11,
      "teamId": 34,
      "teamName": "연세대학교 EAGLES",
      "teamCode": "yonsei",
      "teamPlayerId": 12548,
      "playerId": 42,
      "playerName": "강대건",
      "jerseyNumber": "27",
      "hasBatterStats": true,
      "hasPitcherStats": false
    }
  ],
  "nextCursor": "eyJ0cElkIjoxMjU0OH0=",
  "hasNext": true,
  "totalCount": 412
}
```
- 에러: 400 seasonId 누락/형식오류, 404 season/team 미존재

### 19) 선수 기본 프로필 조회
- Method/Path: `GET /api/players/{playerId}/profile?seasonId=`
- 요청: `playerId` 필수(Path), `seasonId` 옵션(있으면 해당 시즌 소속 우선)
- 동작: 기록 유무와 상관없이 이름/등번호/소속팀 반환.
- 요청 예시
```
GET /api/players/42/profile
GET /api/players/42/profile?seasonId=11
```
- 응답 예시
```json
{
  "playerId": 42,
  "playerName": "강대건",
  "seasonId": 11,
  "teamId": 34,
  "teamName": "연세대학교 EAGLES",
  "teamCode": "yonsei",
  "jerseyNumber": 27
}
```
- 에러: 404 player 미존재, 404 season 지정 시 해당 시즌 소속 없음

### 20) 선수 검색 자동완성
- Method/Path: `GET /api/players/search?seasonId=&q=&teamId=&limit=`
- 요청: `seasonId` 필수, `q` 필수(1자 이상), `teamId` 옵션, `limit` 기본 20/최대 50
- 동작: `TEAM_PLAYER` 기반 이름 부분 검색. 공백 무시 비교 지원(`"강 대 건"` == `"강대건"`). 팀명 표기는 원문 유지, 검색은 정규화(공백 무시) 비교.
- 요청 예시
```
GET /api/players/search?seasonId=11&q=강대
GET /api/players/search?seasonId=11&q=강&teamId=34&limit=10
```
- 응답 예시
```json
[
  {
    "playerId": 42,
    "playerName": "강대건",
    "teamId": 34,
    "teamName": "연세대학교 EAGLES",
    "jerseyNumber": 27,
    "seasonId": 11
  }
]
```
- 에러: 400 seasonId/q 누락/형식오류, 404 season/team 미존재

### 21) 플레이오프 경기 목록 조회
- Method/Path: `GET /api/records/playoffs?seasonId=&view=games&tier=`
- 요청:
  - `seasonId` 필수
  - `view` 옵션: `games`(기본) | `teams`
  - `tier` 옵션: `ALL`(기본) | `EUTTEUM` | `BEOGEUM`
- 동작: `GAME.playoff_tier` / `GAME.playoff_round` 기반 조회. 라운드 순서: `ROUND_OF_16 → QUARTER_FINAL → SEMI_FINAL → FINAL`
- 요청 예시
```
GET /api/records/playoffs?seasonId=1
GET /api/records/playoffs?seasonId=1&tier=EUTTEUM
```
- 응답 예시
```json
[
  {
    "seasonId": 1,
    "seasonYear": 2017,
    "gameId": 42,
    "gameDate": "2017-10-01",
    "gameType": "포스트시즌",
    "playoffTier": "EUTTEUM",
    "playoffRound": "FINAL",
    "homeTeamId": 3,
    "homeTeamName": "연세대학교 EAGLES",
    "homeScore": 5,
    "awayTeamId": 7,
    "awayTeamName": "고려대학교 TIGERS",
    "awayScore": 3,
    "scope": "PLAYOFF"
  }
]
```
- 에러: 400 seasonId 누락/tier 오류, 404 season 미존재, 결과 없으면 200 + 빈 배열

### 22) 플레이오프 팀별 집계 조회
- Method/Path: `GET /api/records/playoffs?seasonId=&view=teams&tier=`
- 요청:
  - `seasonId` 필수
  - `view=teams`
  - `tier` 옵션: `ALL`(기본) | `EUTTEUM` | `BEOGEUM`
- 동작: 포스트시즌 경기 결과를 팀별로 집계. `bestRound`는 해당 팀이 진출한 가장 높은 라운드.
- 요청 예시
```
GET /api/records/playoffs?seasonId=1&view=teams
GET /api/records/playoffs?seasonId=1&view=teams&tier=BEOGEUM
```
- 응답 예시
```json
[
  {
    "seasonId": 1,
    "seasonYear": 2017,
    "teamId": 3,
    "teamName": "연세대학교 EAGLES",
    "playoffTier": "EUTTEUM",
    "bestRound": "FINAL",
    "wins": 4,
    "losses": 0,
    "runsScored": 22,
    "runsAllowed": 10,
    "scope": "PLAYOFF",
    "partCode": "2",
    "group": "B"
  },
  {
    "seasonId": 1,
    "seasonYear": 2017,
    "teamId": 7,
    "teamName": "고려대학교 TIGERS",
    "playoffTier": "EUTTEUM",
    "bestRound": "FINAL",
    "wins": 3,
    "losses": 1,
    "runsScored": 15,
    "runsAllowed": 12,
    "scope": "PLAYOFF",
    "partCode": "5",
    "group": "E"
  }
]
```
- 에러: 400 seasonId 누락/tier 오류, 404 season 미존재, 결과 없으면 200 + 빈 배열

### 23) 팀 활성/비활성 관리 (관리자 전용)
- Method/Path: `PATCH /api/admin/teams/{teamId}/active?active=`
- 인증: 관리자(`Authorization: Bearer Firebase ID token with admin=true`)
- 요청: `teamId` 필수(Path), `active` 필수(Query, `true` | `false`)
- 요청 예시
```
PATCH /api/admin/teams/15/active?active=false
Authorization: Bearer <Firebase Admin Token>
```
- 응답: `204 No Content`
- 에러
  - 400: `active` 누락
  - 403: 권한 없음
  - 404: team 미존재

### 24) 필터 옵션 조회
- Method/Path: `GET /api/records/filter-options?seasonId=`
- 요청: `seasonId` 필수
- 동작: `TEAM_PLAYER.part_code` DB 기반으로 해당 시즌의 실제 조 목록만 반환. 하드코딩 없음.
- 요청 예시
```
GET /api/records/filter-options?seasonId=11
```
- 응답 예시
```json
{
  "seasonId": 11,
  "groups": [
    { "partCode": "1", "group": "A", "label": "A조", "order": 1 },
    { "partCode": "2", "group": "B", "label": "B조", "order": 2 },
    { "partCode": "8", "group": "H", "label": "H조", "order": 8 }
  ],
  "scopes": ["LEAGUE", "PLAYOFF"],
  "playoffDivisions": ["EUTTEUM", "BEOGEUM"],
  "regulations": ["IN", "OUT"],
  "defaultRegulation": "IN",
  "batterSortOptions": ["battingAverage", "hits", "homeRuns", "rbi", "ops", "sluggingPct", "onBasePct", "gamesPlayed", "plateAppearance"],
  "pitcherSortOptions": ["era", "whip", "strikeouts", "wins", "saves", "inningsPitched", "walksAllowed", "gamesPlayed"]
}
```
- 에러: 400 seasonId 누락, 결과 없으면 `groups=[]`

### 25) 팀 순위 (standings)
- Method/Path: `GET /api/records/standings`
- Query: `seasonId` 필수, `scope`, `group`, `partCode`, `playoffDivision`, `division` 옵션
- 동작: `GET /api/records/teams`와 동일 로직. scope/group 필터 적용.
- 요청 예시
```
GET /api/records/standings?seasonId=1&scope=LEAGUE&group=A
GET /api/records/standings?seasonId=1&playoffDivision=EUTTEUM
```
- 응답 예시: `GET /api/records/teams`와 동일 형식

### 26) 파워랭킹 목록 조회
- Method/Path: `GET /api/records/power-ranking?rankingYear=&limit=`
- 요청: `rankingYear` 필수(int), `limit` 옵션(기본 0 = 전체)
- 동작: `POWER_RANKING` 캐시(최신 `calc_version`)에서 조회. `rebuild` 실행 전에는 빈 배열 반환. `weightedScore` 내림차순 정렬 후 `rank` 부여.
- 요청 예시
```
GET /api/records/power-ranking?rankingYear=2023
GET /api/records/power-ranking?rankingYear=2023&limit=10
```
- 응답 예시
```json
[
  {
    "rank": 1,
    "teamId": 3,
    "teamName": "한양대학교 WILDCAT",
    "weightedScore": 80.800,
    "y1Score": 32.000,
    "y2Score": 37.000,
    "y3Score": 49.000,
    "windowYears": [2021, 2022, 2023],
    "calcVersion": 1
  },
  {
    "rank": 2,
    "teamId": 7,
    "teamName": "연세대학교 EAGLES",
    "weightedScore": 65.400,
    "y1Score": 28.000,
    "y2Score": 30.000,
    "y3Score": 42.000,
    "windowYears": [2021, 2022, 2023],
    "calcVersion": 1
  }
]
```
- 에러: 400 rankingYear 누락, 결과 없으면 200 + 빈 배열

### 27) 팀별 연도별 파워랭킹 원점수 조회
- Method/Path: `GET /api/records/power-ranking/season-scores?teamId=&fromYear=&toYear=`
- 요청: `teamId` 필수(Long), `fromYear`/`toYear` 옵션(없으면 전체)
- 동작: 팀의 연도별 예선 원점수·환산점수·본선점수·합계 반환. `POWER_RANKING` 캐시가 있으면 캐시 우선, 없으면 실시간 계산.
- 요청 예시
```
GET /api/records/power-ranking/season-scores?teamId=3&fromYear=2021&toYear=2023
```
- 응답 예시
```json
[
  {
    "seasonYear": 2021,
    "prelimRaw": 9.000,
    "prelimNormalized": 12.000,
    "finalsPoints": 20.000,
    "total": 32.000
  },
  {
    "seasonYear": 2022,
    "prelimRaw": 12.000,
    "prelimNormalized": 12.000,
    "finalsPoints": 25.000,
    "total": 37.000
  },
  {
    "seasonYear": 2023,
    "prelimRaw": 24.000,
    "prelimNormalized": 24.000,
    "finalsPoints": 25.000,
    "total": 49.000
  }
]
```
- 에러: 400 teamId 누락, 404 team 미존재, 결과 없으면 200 + 빈 배열

### 28) 파워랭킹 재계산 (관리자 전용)
- Method/Path: `POST /api/admin/records/power-ranking/rebuild?fromYear=&toYear=`
- 인증: 관리자(`Authorization: Bearer Firebase ID token with admin=true`)
- 요청: `fromYear`/`toYear` 옵션(없으면 전체 연도)
- 동작: `fromYear`~`toYear` 범위의 각 `rankingYear`에 대해 파워랭킹을 재계산하고 `POWER_RANKING` 테이블에 새 `calc_version`으로 저장
  - 파워랭킹 공식: `총점 = (y1 × 0.3) + (y2 × 0.6) + (y3 × 1.0)`
  - `y1` = rankingYear-2 성적, `y2` = rankingYear-1, `y3` = rankingYear
  - 예선 환산 승점 = `(승×3 + 무×1) × (기준경기수 / 실제경기수)`
  - 본선 점수: 우승 25점, 준우승 20점, 4강 15점, 8강 10점, 16강 5점
- 요청 예시
```
POST /api/admin/records/power-ranking/rebuild?fromYear=2021&toYear=2023
Authorization: Bearer <Firebase Admin Token>
```
- 응답 예시
```json
{
  "runId": "a3f2c1d4-...",
  "startedAt": "2026-02-24T10:30:00",
  "status": "COMPLETED"
}
```
- 에러: 400 fromYear > toYear, 401/403 권한 없음

---

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

---

## 기록실 필터/랭킹 규격

### 공통 Query 파라미터
| 파라미터 | 설명 | 기본값 |
|---|---|---|
| `seasonId` | 시즌 ID (필수, >0) | - |
| `scope` | `ALL \| LEAGUE \| PLAYOFF` | `ALL` |
| `group` | `ALL \| A \| B \| ... \| H` | `ALL` |
| `partCode` | `1`~`8` (group alias, group 미지정 시 사용) | - |
| `playoffDivision` | `ALL \| EUTTEUM \| BEOGEUM` | `ALL` |
| `division` | `playoffDivision` legacy alias | - |
| `regulation` | `IN \| OUT \| ALL` | `ALL` (프론트 기본 요청: `IN`) |
| `sort` | 엔드포인트별 허용 키 | 엔드포인트별 기본값 |
| `sortOrder` | `asc \| desc` | 항목별 기본값 |
| `limit` | 상위 N개 제한 (0 = 전체) | `0` |

### 필터 적용 규칙
- `group` 우선, 없으면 `partCode` 사용. `group(A~H)` → `partCode(1~8)` 역매핑.
- `playoffDivision != ALL` → 내부적으로 `scope=PLAYOFF` 강제.
- **조 정보는 반드시 DB `TEAM_PLAYER.part_code` 기반. 팀명/정렬순 추론 금지.**
- 조 정렬: `partCode ASC` 고정 (A→H).
- `regulation=IN`: 유효 통계 보유 선수만, `OUT`: 미달 선수만, `ALL`: 전체.
- enum/sort 오류 → 400, 시즌 없음 → 404, 결과 없음 → 200 + 빈 배열.

### 응답 공통 메타 필드
각 row에 항상 포함:
```json
{
  "partCode": "1",
  "group": "A",
  "scope": "LEAGUE",
  "seasonType": null,
  "regulation": "IN"
}
```

### 조 매핑 규칙
- `partCode(1~8)` → `group(A~H)` 고정 매핑
  - `1:A, 2:B, 3:C, 4:D, 5:E, 6:F, 7:G, 8:H`
- 조 옵션 및 정렬은 반드시 `partCode ASC` (A→H). 팀명순 금지.

### 타자 sort 허용값
`battingAverage`(기본) · `hits` · `homeRuns` · `rbi` · `ops` · `sluggingPct` · `onBasePct` · `gamesPlayed` · `plateAppearance` · `stolenBases`

### 투수 sort 허용값
`era`(기본, asc) · `whip`(asc) · `strikeouts` · `wins` · `saves` · `inningsPitched` · `walksAllowed` · `gamesPlayed`

### 파워랭킹 계산 공식
- `총점 = (y1 × 0.3) + (y2 × 0.6) + (y3 × 1.0)`
  - `y1`: rankingYear-2 성적, `y2`: rankingYear-1, `y3`: rankingYear
- 예선 환산 승점 = `(승×3 + 무×1) × (기준경기수 / 실제경기수)`
- 본선 점수: 우승 25점, 준우승 20점, 4강 15점, 8강 10점, 16강 5점 · 예선탈락 0점
