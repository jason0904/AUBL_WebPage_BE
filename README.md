# AUBL Webpage BE

## API 명세

기본 URL
- `http://localhost:8080`

공통 응답
- 200: 성공
- 400: 잘못된 요청 (필수 값 누락/유효성 오류)
- 404: 미존재 (FK 대상, match 미존재 등)

### 1) 시즌 생성
- Method/Path: `POST /api/seasons`
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
- 응답
```json
{
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

## Firestore Import 참고
- 팀 매칭: `TEAM.team_code`
- 선수 매칭: `(teamId, seasonId, jerseyNumber, playerName)`
- 시즌: 경기 연도 기준 자동 생성
- 경기는 사전 등록 필요(자동 생성 안 함)
