CREATE DATABASE IF NOT EXISTS aubl;
USE aubl;

-- ============================================
-- 1. USER 테이블 (회원 정보 - OAuth2 지원)
-- ============================================
CREATE TABLE USER (
                      user_id INT AUTO_INCREMENT PRIMARY KEY,
                      email VARCHAR(255) NOT NULL UNIQUE,  -- 이메일 (로그인 ID)
                      password VARCHAR(255),  -- 비밀번호 (OAuth2 로그인 시 NULL 가능)
                      name VARCHAR(100) NOT NULL,  -- 이름
                      phone_number VARCHAR(20),  -- 전화번호

    -- OAuth2 관련
                      oauth_provider VARCHAR(50),  -- OAuth2 제공자 ('google', 'kakao', 'naver', NULL)
                      oauth_provider_id VARCHAR(255),  -- OAuth2 제공자의 사용자 ID

    -- 권한 및 상태
                      role VARCHAR(20) DEFAULT 'USER',  -- 권한 ('USER', 'PLAYER', 'TEAM_ADMIN', 'ADMIN')
                      is_active BOOLEAN DEFAULT TRUE,  -- 활성화 여부

    -- 타임스탬프
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 가입일
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  -- 수정일
                      last_login_at TIMESTAMP,  -- 마지막 로그인

    -- OAuth2 사용자 고유 식별 (provider + provider_id 조합)
                      UNIQUE KEY unique_oauth (oauth_provider, oauth_provider_id)
);

-- ============================================
-- 2. OAUTH_TOKEN 테이블 (OAuth2 토큰 관리)
-- ============================================
CREATE TABLE OAUTH_TOKEN (
                             token_id INT AUTO_INCREMENT PRIMARY KEY,
                             user_id INT NOT NULL,
                             access_token TEXT,
                             refresh_token TEXT,
                             expires_at TIMESTAMP,

                             FOREIGN KEY (user_id) REFERENCES USER(user_id)
);

-- ============================================
-- 3. SEASON 테이블
-- ============================================
CREATE TABLE SEASON (
                        season_id INT AUTO_INCREMENT PRIMARY KEY,
                        year INT NOT NULL UNIQUE  -- 연도 (예: 2024, 2025)
);

-- ============================================
-- 4. TEAM 테이블
-- ============================================
CREATE TABLE TEAM (
                      team_id INT AUTO_INCREMENT PRIMARY KEY,
                      team_name VARCHAR(100) NOT NULL,
                      team_code VARCHAR(20),  -- 팀 약어
                      manager_id INT,  -- 팀 매니저 (USER 테이블 참조, NULL 가능)

                      FOREIGN KEY (manager_id) REFERENCES USER(user_id)
);

-- ============================================
-- 5. PLAYER 테이블
-- ============================================
CREATE TABLE PLAYER (
                        player_id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT,  -- USER 테이블 참조 (NULL 가능 - 미가입 선수)
                        player_name VARCHAR(100) NOT NULL,
                        birth_date DATE,
                        position VARCHAR(20),  -- '투수', '타자', '양쪽'
                        height INT,  -- 키 (cm)
                        weight INT,  -- 몸무게 (kg)
                        school VARCHAR(200),  -- 출신학교
                        is_player BOOLEAN,  -- 학생야구 선수출신 여부 (중/고/대 야구부 출신)

                        FOREIGN KEY (user_id) REFERENCES USER(user_id),
                        UNIQUE KEY unique_user_player (user_id)  -- 한 유저는 하나의 선수 정보만
);

-- ============================================
-- 6. TEAM_PLAYER 테이블 (선수-팀-시즌 매핑)
-- ============================================
CREATE TABLE TEAM_PLAYER (
                             tp_id INT AUTO_INCREMENT PRIMARY KEY,
                             team_id INT NOT NULL,
                             player_id INT NOT NULL,
                             season_id INT NOT NULL,
                             jersey_number INT,  -- 등번호
                             part_code VARCHAR(2),  -- 조 구분(1~8)

                             FOREIGN KEY (team_id) REFERENCES TEAM(team_id),
                             FOREIGN KEY (player_id) REFERENCES PLAYER(player_id),
                             FOREIGN KEY (season_id) REFERENCES SEASON(season_id),

                             UNIQUE KEY unique_player_team_season (player_id, team_id, season_id)
);

-- ============================================
-- 7. GAME 테이블
-- ============================================
CREATE TABLE GAME (
                      game_id INT AUTO_INCREMENT PRIMARY KEY,
                      season_id INT NOT NULL,
                      game_date DATE NOT NULL,
                      game_number INT,  -- 더블헤더 등 구분
                      home_team INT NOT NULL,
                      away_team INT NOT NULL,
                      home_score INT,
                      away_score INT,
                      game_type VARCHAR(20),  -- '정규시즌', '포스트시즌'
                      -- 플레이오프 전용 (game_type='포스트시즌' 일 때만 유효)
                      playoff_tier VARCHAR(10),   -- 'EUTTEUM'(으뜸) | 'BEOGEUM'(버금) | NULL
                      playoff_round VARCHAR(20),  -- 'ROUND_OF_16' | 'QUARTER_FINAL' | 'SEMI_FINAL' | 'FINAL' | NULL
                      csv_file_path VARCHAR(500),  -- CSV 파일 경로

                      FOREIGN KEY (season_id) REFERENCES SEASON(season_id),
                      FOREIGN KEY (home_team) REFERENCES TEAM(team_id),
                      FOREIGN KEY (away_team) REFERENCES TEAM(team_id)
);

-- ============================================
-- 8. BATTER_STATS 테이블 (타자 시즌 누적 통계)
-- ============================================
CREATE TABLE BATTER_STATS (
                              batter_stat_id INT AUTO_INCREMENT PRIMARY KEY,
                              tp_id INT NOT NULL,
                              season_id INT NOT NULL,
                              season_type VARCHAR(20),  -- '정규시즌', '포스트시즌'

    -- 출전 정보
                              games_played INT DEFAULT 0,
                              plate_appearance INT DEFAULT 0,
                              at_bats INT DEFAULT 0,

    -- 타격 기록
                              hits INT DEFAULT 0,
                              doubles INT DEFAULT 0,
                              triples INT DEFAULT 0,
                              home_runs INT DEFAULT 0,
                              runs_batted_in INT DEFAULT 0,
                              runs_scored INT DEFAULT 0,
                              stolen_bases INT DEFAULT 0,
                              caught_stealing INT DEFAULT 0,
                              walks INT DEFAULT 0,
                              strikeouts INT DEFAULT 0,
                              hit_by_pitch INT DEFAULT 0,
                              sacrifice_hits INT DEFAULT 0,
                              sacrifice_flies INT DEFAULT 0,

    -- 계산 지표
                              batting_average DECIMAL(5,3),  -- 타율
                              on_base_pct DECIMAL(5,3),  -- 출루율
                              slugging_pct DECIMAL(5,3),  -- 장타율
                              ops DECIMAL(5,3),  -- OPS

                              FOREIGN KEY (tp_id) REFERENCES TEAM_PLAYER(tp_id),
                              FOREIGN KEY (season_id) REFERENCES SEASON(season_id)
);

-- ============================================
-- 9. PITCHER_STATS 테이블 (투수 시즌 누적 통계)
-- ============================================
CREATE TABLE PITCHER_STATS (
                               pitcher_stat_id INT AUTO_INCREMENT PRIMARY KEY,
                               tp_id INT NOT NULL,
                               season_id INT NOT NULL,
                               season_type VARCHAR(20),  -- '정규시즌', '포스트시즌'

    -- 출전 정보
                               games_played INT DEFAULT 0,
                               games_started INT DEFAULT 0,
                               complete_games INT DEFAULT 0,
                               shutouts INT DEFAULT 0,

    -- 투구 기록
                               innings_pitched DECIMAL(5,2) DEFAULT 0,
                               wins INT DEFAULT 0,
                               losses INT DEFAULT 0,
                               saves INT DEFAULT 0,
                               holds INT DEFAULT 0,
                               hits_allowed INT DEFAULT 0,
                               runs_allowed INT DEFAULT 0,
                               earned_runs INT DEFAULT 0,
                               home_runs_allow INT DEFAULT 0,
                               walks_allowed INT DEFAULT 0,
                               strikeouts INT DEFAULT 0,
                               hit_batters INT DEFAULT 0,
                               wild_pitches INT DEFAULT 0,
                               balks INT DEFAULT 0,

    -- 계산 지표
                               era DECIMAL(5,2),  -- 평균자책점
                               whip DECIMAL(5,3),  -- WHIP
                               k_per_9 DECIMAL(5,2),  -- 9이닝당 탈삼진
                               bb_per_9 DECIMAL(5,2),  -- 9이닝당 볼넷

                               FOREIGN KEY (tp_id) REFERENCES TEAM_PLAYER(tp_id),
                               FOREIGN KEY (season_id) REFERENCES SEASON(season_id)
);

-- ============================================
-- 10. BATTER_GAME_LOG 테이블 (타자 경기별 상세 기록)
-- ============================================
CREATE TABLE BATTER_GAME_LOG (
                                 batter_gl_id INT AUTO_INCREMENT PRIMARY KEY,
                                 game_idx INT NOT NULL,
                                 team_side VARCHAR(10),  -- 'home' or 'away'
                                 team_idx INT NOT NULL,
                                 player_idx INT,  -- NULL 가능
                                 batter_stat_id INT NOT NULL,  -- BATTER_STATS 참조
                                 player_name VARCHAR(100),
                                 player_position VARCHAR(10),  -- '좌', '우', '중', '1루', '2루', '3루', '유', '포', '지'
                                 player_bats INT,
                                 player_throws INT,

    -- 타격 기록
                                 at_bats INT,
                                 runs INT,
                                 hits INT,
                                 rbi INT,  -- 타점
                                 walks INT,  -- 볼넷
                                 strikeouts INT,  -- 삼진

                                 FOREIGN KEY (game_idx) REFERENCES GAME(game_id),
                                 FOREIGN KEY (team_idx) REFERENCES TEAM(team_id),
                                 FOREIGN KEY (player_idx) REFERENCES PLAYER(player_id),
                                 FOREIGN KEY (batter_stat_id) REFERENCES BATTER_STATS(batter_stat_id)
);

-- ============================================
-- 11. PITCHER_GAME_LOG 테이블 (투수 경기별 상세 기록)
-- ============================================
CREATE TABLE PITCHER_GAME_LOG (
                                  pitcher_gl_id INT AUTO_INCREMENT PRIMARY KEY,
                                  game_idx INT NOT NULL,
                                  team_side VARCHAR(10),  -- 'home' or 'away'
                                  team_idx INT NOT NULL,
                                  player_idx INT,  -- NULL 가능
                                  pitcher_stat_id INT NOT NULL,  -- PITCHER_STATS 참조
                                  player_name VARCHAR(100),
                                  player_position VARCHAR(10),  -- 'SP' (선발), 'RP' (릴리프), 'CP' (마무리)
                                  player_bats INT,  -- 상대한 타자수
                                  player_throws INT,  -- 투구수

    -- 투구 기록
                                  innings_pitched DECIMAL(5,2),  -- 예: 3.67
                                  hits_allowed INT,
                                  runs_allowed INT,
                                  earned_runs INT,
                                  walks INT,
                                  strikeouts INT,

                                  FOREIGN KEY (game_idx) REFERENCES GAME(game_id),
                                  FOREIGN KEY (team_idx) REFERENCES TEAM(team_id),
                                  FOREIGN KEY (player_idx) REFERENCES PLAYER(player_id),
                                  FOREIGN KEY (pitcher_stat_id) REFERENCES PITCHER_STATS(pitcher_stat_id)
);

-- ============================================
-- 12. TEAM_SEASON_RESULT 테이블 (팀별 시즌 예선 결과 — 파워랭킹 입력값)
-- ============================================
-- 파워랭킹 계산에 필요한 수동 입력 데이터.
-- 본선 성적은 GAME(playoff_tier, playoff_round)에서 자동 계산.
-- 수동 입력이 필요한 것: 예선 환산 기준 경기 수만.
CREATE TABLE TEAM_SEASON_RESULT (
    result_id INT AUTO_INCREMENT PRIMARY KEY,
    team_id INT NOT NULL,
    season_id INT NOT NULL,

    -- 예선 환산 기준 경기 수 (기본 4경기, 3경기 조는 3으로 입력)
    -- 환산 승점 = (승×3 + 무×1) × (prelim_games_standard / 실제경기수)
    -- 실제 승/무/패는 GAME 테이블에서 자동 집계
    prelim_games_standard INT NOT NULL DEFAULT 4,

    -- 메모 (수동 입력 시 참고용)
    note VARCHAR(200),

    FOREIGN KEY (team_id) REFERENCES TEAM(team_id),
    FOREIGN KEY (season_id) REFERENCES SEASON(season_id),
    UNIQUE KEY unique_team_season_result (team_id, season_id)
);

-- ============================================
-- 13. POWER_RANKING 테이블 (파워랭킹 집계 캐시)
-- ============================================
-- rebuild API 호출 시 갱신. 직접 수정 금지.
-- 총점 = (y1_score × 0.3) + (y2_score × 0.6) + (y3_score × 1.0)
-- 단, 해당 연도 데이터가 없으면 0으로 처리.
CREATE TABLE POWER_RANKING (
    ranking_id INT AUTO_INCREMENT PRIMARY KEY,
    ranking_year INT NOT NULL,          -- 랭킹 기준 연도 (최근 연도, 예: 2024)
    team_id INT NOT NULL,

    -- 3개년 원점수 (각 연도의 환산승점 + 본선점수 합계)
    y1_score    DECIMAL(8,3) NOT NULL DEFAULT 0,  -- ranking_year - 2 성적
    y2_score    DECIMAL(8,3) NOT NULL DEFAULT 0,  -- ranking_year - 1 성적
    y3_score    DECIMAL(8,3) NOT NULL DEFAULT 0,  -- ranking_year 성적

    -- 최종 가중치 합산 점수
    weighted_score DECIMAL(8,3) NOT NULL DEFAULT 0,  -- y1×0.3 + y2×0.6 + y3×1.0

    -- 어떤 연도 데이터가 반영됐는지 (JSON 배열, 예: "[2021,2022,2023]")
    window_years VARCHAR(50),

    -- 집계 버전 (rebuild 할 때마다 증가, 동일 ranking_year 내 최신값만 사용)
    calc_version INT NOT NULL DEFAULT 1,

    -- 집계 시각
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (team_id) REFERENCES TEAM(team_id),
    UNIQUE KEY unique_power_ranking (ranking_year, team_id, calc_version)
);


-- ============================================
-- 인덱스 생성 (성능 최적화)
-- ============================================

-- USER 조회 최적화
CREATE INDEX idx_user_email ON USER(email);
CREATE INDEX idx_user_oauth ON USER(oauth_provider, oauth_provider_id);

-- OAUTH_TOKEN 조회 최적화
CREATE INDEX idx_token_user ON OAUTH_TOKEN(user_id);

-- TEAM 조회 최적화
CREATE INDEX idx_team_manager ON TEAM(manager_id);

-- PLAYER 조회 최적화
CREATE INDEX idx_player_user ON PLAYER(user_id);

-- TEAM_PLAYER 조회 최적화
CREATE INDEX idx_team_player_season ON TEAM_PLAYER(season_id);
CREATE INDEX idx_team_player_team ON TEAM_PLAYER(team_id);
CREATE INDEX idx_team_player_player ON TEAM_PLAYER(player_id);
CREATE INDEX idx_team_player_part ON TEAM_PLAYER(part_code, season_id);

-- GAME 조회 최적화
CREATE INDEX idx_game_season ON GAME(season_id);
CREATE INDEX idx_game_date ON GAME(game_date);
CREATE INDEX idx_game_teams ON GAME(home_team, away_team);

-- STATS 조회 최적화
CREATE INDEX idx_batter_stats_season ON BATTER_STATS(season_id, season_type);
CREATE INDEX idx_batter_stats_tp ON BATTER_STATS(tp_id);
CREATE INDEX idx_pitcher_stats_season ON PITCHER_STATS(season_id, season_type);
CREATE INDEX idx_pitcher_stats_tp ON PITCHER_STATS(tp_id);

-- GAME_LOG 조회 최적화
CREATE INDEX idx_batter_log_game ON BATTER_GAME_LOG(game_idx);
CREATE INDEX idx_batter_log_player ON BATTER_GAME_LOG(player_idx);
CREATE INDEX idx_batter_log_stat ON BATTER_GAME_LOG(batter_stat_id);
CREATE INDEX idx_pitcher_log_game ON PITCHER_GAME_LOG(game_idx);
CREATE INDEX idx_pitcher_log_player ON PITCHER_GAME_LOG(player_idx);
CREATE INDEX idx_pitcher_log_stat ON PITCHER_GAME_LOG(pitcher_stat_id);

-- TEAM_SEASON_RESULT 조회 최적화
CREATE INDEX idx_team_season_result_season ON TEAM_SEASON_RESULT(season_id);
CREATE INDEX idx_team_season_result_team ON TEAM_SEASON_RESULT(team_id);

-- POWER_RANKING 조회 최적화
CREATE INDEX idx_power_ranking_year ON POWER_RANKING(ranking_year, calc_version);
CREATE INDEX idx_power_ranking_team ON POWER_RANKING(team_id);

