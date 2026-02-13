CREATE DATABASE IF NOT EXISTS aubl;
USE aubl;

-- ============================================
-- 1. `USER` ?åÏù¥Î∏?(?åÏõê ?ïÎ≥¥ - OAuth2 ÏßÄ??
-- ============================================
CREATE TABLE `USER` (
                      user_id INT AUTO_INCREMENT PRIMARY KEY,
                      email VARCHAR(255) NOT NULL UNIQUE,  -- ?¥Î©î??(Î°úÍ∑∏??ID)
                      password VARCHAR(255),  -- ÎπÑÎ?Î≤àÌò∏ (OAuth2 Î°úÍ∑∏????NULL Í∞Ä??
                      name VARCHAR(100) NOT NULL,  -- ?¥Î¶Ñ
                      phone_number VARCHAR(20),  -- ?ÑÌôîÎ≤àÌò∏

    -- OAuth2 Í¥Ä??
                      oauth_provider VARCHAR(50),  -- OAuth2 ?úÍ≥µ??('google', 'kakao', 'naver', NULL)
                      oauth_provider_id VARCHAR(255),  -- OAuth2 ?úÍ≥µ?êÏùò ?¨Ïö©??ID

    -- Í∂åÌïú Î∞??ÅÌÉú
                      role VARCHAR(20) DEFAULT '`USER`',  -- Í∂åÌïú ('`USER`', '`PLAYER`', 'TEAM_ADMIN', 'ADMIN')
                      is_active BOOLEAN DEFAULT TRUE,  -- ?úÏÑ±???¨Î?

    -- ?Ä?ÑÏä§?¨ÌîÑ
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Í∞Ä?ÖÏùº
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  -- ?òÏ†ï??
                      last_login_at TIMESTAMP,  -- ÎßàÏ?Îß?Î°úÍ∑∏??

    -- OAuth2 ?¨Ïö©??Í≥†Ïú† ?ùÎ≥Ñ (provider + provider_id Ï°∞Ìï©)
                      UNIQUE KEY unique_oauth (oauth_provider, oauth_provider_id)
);

-- ============================================
-- 2. `OAUTH_TOKEN` ?åÏù¥Î∏?(OAuth2 ?†ÌÅ∞ Í¥ÄÎ¶?
-- ============================================
CREATE TABLE `OAUTH_TOKEN` (
                             token_id INT AUTO_INCREMENT PRIMARY KEY,
                             user_id INT NOT NULL,
                             access_token TEXT,
                             refresh_token TEXT,
                             expires_at TIMESTAMP,

                             FOREIGN KEY (user_id) REFERENCES `USER`(user_id)
);

-- ============================================
-- 3. `SEASON` ?åÏù¥Î∏?
-- ============================================
CREATE TABLE `SEASON` (
                        season_id INT AUTO_INCREMENT PRIMARY KEY,
                        year INT NOT NULL UNIQUE  -- ?∞ÎèÑ (?? 2024, 2025)
);

-- ============================================
-- 4. `TEAM` ?åÏù¥Î∏?
-- ============================================
CREATE TABLE `TEAM` (
                      team_id INT AUTO_INCREMENT PRIMARY KEY,
                      team_name VARCHAR(100) NOT NULL,
                      team_code VARCHAR(20),  -- ?Ä ?ΩÏñ¥
                      manager_id INT,  -- ?Ä Îß§Îãà?Ä (`USER` ?åÏù¥Î∏?Ï∞∏Ï°∞, NULL Í∞Ä??

                      FOREIGN KEY (manager_id) REFERENCES `USER`(user_id)
);

-- ============================================
-- 5. `PLAYER` ?åÏù¥Î∏?
-- ============================================
CREATE TABLE `PLAYER` (
                        player_id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT,  -- `USER` ?åÏù¥Î∏?Ï∞∏Ï°∞ (NULL Í∞Ä??- ÎØ∏Í????†Ïàò)
                        player_name VARCHAR(100) NOT NULL,
                        birth_date DATE,
                        position VARCHAR(20),  -- '?¨Ïàò', '?Ä??, '?ëÏ™Ω'
                        height INT,  -- ??(cm)
                        weight INT,  -- Î™∏Î¨¥Í≤?(kg)
                        school VARCHAR(200),  -- Ï∂úÏã†?ôÍµê
                        is_player BOOLEAN,  -- ?ôÏÉù?ºÍµ¨ ?†ÏàòÏ∂úÏã† ?¨Î? (Ï§?Í≥??Ä ?ºÍµ¨Î∂Ä Ï∂úÏã†)

                        FOREIGN KEY (user_id) REFERENCES `USER`(user_id),
                        UNIQUE KEY unique_user_player (user_id)  -- ???†Ï????òÎÇò???†Ïàò ?ïÎ≥¥Îß?
);

-- ============================================
-- 6. `TEAM_PLAYER` ?åÏù¥Î∏?(?†Ïàò-?Ä-?úÏ¶å Îß§Ìïë)
-- ============================================
CREATE TABLE `TEAM_PLAYER` (
                             tp_id INT AUTO_INCREMENT PRIMARY KEY,
                             team_id INT NOT NULL,
                             player_id INT NOT NULL,
                             season_id INT NOT NULL,
                             jersey_number INT,  -- ?±Î≤à??

                             FOREIGN KEY (team_id) REFERENCES `TEAM`(team_id),
                             FOREIGN KEY (player_id) REFERENCES `PLAYER`(player_id),
                             FOREIGN KEY (season_id) REFERENCES `SEASON`(season_id),

                             UNIQUE KEY unique_player_team_season (player_id, team_id, season_id)
);

-- ============================================
-- 7. `GAME` ?åÏù¥Î∏?
-- ============================================
CREATE TABLE `GAME` (
                      game_id INT AUTO_INCREMENT PRIMARY KEY,
                      season_id INT NOT NULL,
                      game_date DATE NOT NULL,
                      game_number INT,  -- ?îÎ∏î?§Îçî ??Íµ¨Î∂Ñ
                      home_team INT NOT NULL,
                      away_team INT NOT NULL,
                      home_score INT,
                      away_score INT,
                      game_type VARCHAR(20),  -- '?ïÍ∑ú?úÏ¶å', '?¨Ïä§?∏ÏãúÏ¶?
                      csv_file_path VARCHAR(500),  -- CSV ?åÏùº Í≤ΩÎ°ú

                      FOREIGN KEY (season_id) REFERENCES `SEASON`(season_id),
                      FOREIGN KEY (home_team) REFERENCES `TEAM`(team_id),
                      FOREIGN KEY (away_team) REFERENCES `TEAM`(team_id)
);

-- ============================================
-- 8. `BATTER_STATS` ?åÏù¥Î∏?(?Ä???úÏ¶å ?ÑÏ†Å ?µÍ≥Ñ)
-- ============================================
CREATE TABLE `BATTER_STATS` (
                              batter_stat_id INT AUTO_INCREMENT PRIMARY KEY,
                              tp_id INT NOT NULL,
                              season_id INT NOT NULL,
                              season_type VARCHAR(20),  -- '?ïÍ∑ú?úÏ¶å', '?¨Ïä§?∏ÏãúÏ¶?

    -- Ï∂úÏ†Ñ ?ïÎ≥¥
                              games_played INT DEFAULT 0,
                              plate_appearance INT DEFAULT 0,
                              at_bats INT DEFAULT 0,

    -- ?ÄÍ≤?Í∏∞Î°ù
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

    -- Í≥ÑÏÇ∞ ÏßÄ??
                              batting_average DECIMAL(5,3),  -- ?Ä??
                              on_base_pct DECIMAL(5,3),  -- Ï∂úÎ£®??
                              slugging_pct DECIMAL(5,3),  -- ?•Ì???
                              ops DECIMAL(5,3),  -- OPS

                              FOREIGN KEY (tp_id) REFERENCES `TEAM_PLAYER`(tp_id),
                              FOREIGN KEY (season_id) REFERENCES `SEASON`(season_id)
);

-- ============================================
-- 9. `PITCHER_STATS` ?åÏù¥Î∏?(?¨Ïàò ?úÏ¶å ?ÑÏ†Å ?µÍ≥Ñ)
-- ============================================
CREATE TABLE `PITCHER_STATS` (
                               pitcher_stat_id INT AUTO_INCREMENT PRIMARY KEY,
                               tp_id INT NOT NULL,
                               season_id INT NOT NULL,
                               season_type VARCHAR(20),  -- '?ïÍ∑ú?úÏ¶å', '?¨Ïä§?∏ÏãúÏ¶?

    -- Ï∂úÏ†Ñ ?ïÎ≥¥
                               games_played INT DEFAULT 0,
                               games_started INT DEFAULT 0,
                               complete_games INT DEFAULT 0,
                               shutouts INT DEFAULT 0,

    -- ?¨Íµ¨ Í∏∞Î°ù
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

    -- Í≥ÑÏÇ∞ ÏßÄ??
                               era DECIMAL(5,2),  -- ?âÍ∑†?êÏ±Ö??
                               whip DECIMAL(5,3),  -- WHIP
                               k_per_9 DECIMAL(5,2),  -- 9?¥Îãù???àÏÇºÏß?
                               bb_per_9 DECIMAL(5,2),  -- 9?¥Îãù??Î≥ºÎÑ∑

                               FOREIGN KEY (tp_id) REFERENCES `TEAM_PLAYER`(tp_id),
                               FOREIGN KEY (season_id) REFERENCES `SEASON`(season_id)
);

-- ============================================
-- 10. `BATTER_GAME_LOG` ?åÏù¥Î∏?(?Ä??Í≤ΩÍ∏∞Î≥??ÅÏÑ∏ Í∏∞Î°ù)
-- ============================================
CREATE TABLE `BATTER_GAME_LOG` (
                                 batter_gl_id INT AUTO_INCREMENT PRIMARY KEY,
                                 game_idx INT NOT NULL,
                                 team_side VARCHAR(10),  -- 'home' or 'away'
                                 team_idx INT NOT NULL,
                                 player_idx INT,  -- NULL Í∞Ä??
                                 batter_stat_id INT NOT NULL,  -- `BATTER_STATS` Ï∞∏Ï°∞
                                 player_name VARCHAR(100),
                                 player_position VARCHAR(10),  -- 'Ï¢?, '??, 'Ï§?, '1Î£?, '2Î£?, '3Î£?, '??, '??, 'ÏßÄ'
                                 player_bats INT,
                                 player_throws INT,

    -- ?ÄÍ≤?Í∏∞Î°ù
                                 at_bats INT,
                                 runs INT,
                                 hits INT,
                                 rbi INT,  -- ?Ä??
                                 walks INT,  -- Î≥ºÎÑ∑
                                 strikeouts INT,  -- ?ºÏßÑ

                                 FOREIGN KEY (game_idx) REFERENCES `GAME`(game_id),
                                 FOREIGN KEY (team_idx) REFERENCES `TEAM`(team_id),
                                 FOREIGN KEY (player_idx) REFERENCES `PLAYER`(player_id),
                                 FOREIGN KEY (batter_stat_id) REFERENCES `BATTER_STATS`(batter_stat_id)
);

-- ============================================
-- 11. `PITCHER_GAME_LOG` ?åÏù¥Î∏?(?¨Ïàò Í≤ΩÍ∏∞Î≥??ÅÏÑ∏ Í∏∞Î°ù)
-- ============================================
CREATE TABLE `PITCHER_GAME_LOG` (
                                  pitcher_gl_id INT AUTO_INCREMENT PRIMARY KEY,
                                  game_idx INT NOT NULL,
                                  team_side VARCHAR(10),  -- 'home' or 'away'
                                  team_idx INT NOT NULL,
                                  player_idx INT,  -- NULL Í∞Ä??
                                  pitcher_stat_id INT NOT NULL,  -- `PITCHER_STATS` Ï∞∏Ï°∞
                                  player_name VARCHAR(100),
                                  player_position VARCHAR(10),  -- 'SP' (?†Î∞ú), 'RP' (Î¶¥Î¶¨??, 'CP' (ÎßàÎ¨¥Î¶?
                                  player_bats INT,  -- ?ÅÎ????Ä?êÏàò
                                  player_throws INT,  -- ?¨Íµ¨??

    -- ?¨Íµ¨ Í∏∞Î°ù
                                  innings_pitched DECIMAL(5,2),  -- ?? 3.67
                                  hits_allowed INT,
                                  runs_allowed INT,
                                  earned_runs INT,
                                  walks INT,
                                  strikeouts INT,

                                  FOREIGN KEY (game_idx) REFERENCES `GAME`(game_id),
                                  FOREIGN KEY (team_idx) REFERENCES `TEAM`(team_id),
                                  FOREIGN KEY (player_idx) REFERENCES `PLAYER`(player_id),
                                  FOREIGN KEY (pitcher_stat_id) REFERENCES `PITCHER_STATS`(pitcher_stat_id)
);

-- ============================================
-- ?∏Îç±???ùÏÑ± (?±Îä• ÏµúÏ†Å??
-- ============================================

-- `USER` Ï°∞Ìöå ÏµúÏ†Å??
CREATE INDEX idx_user_email ON `USER`(email);
CREATE INDEX idx_user_oauth ON `USER`(oauth_provider, oauth_provider_id);

-- `OAUTH_TOKEN` Ï°∞Ìöå ÏµúÏ†Å??
CREATE INDEX idx_token_user ON `OAUTH_TOKEN`(user_id);

-- `TEAM` Ï°∞Ìöå ÏµúÏ†Å??
CREATE INDEX idx_team_manager ON `TEAM`(manager_id);

-- `PLAYER` Ï°∞Ìöå ÏµúÏ†Å??
CREATE INDEX idx_player_user ON `PLAYER`(user_id);

-- `TEAM_PLAYER` Ï°∞Ìöå ÏµúÏ†Å??
CREATE INDEX idx_team_player_season ON `TEAM_PLAYER`(season_id);
CREATE INDEX idx_team_player_team ON `TEAM_PLAYER`(team_id);
CREATE INDEX idx_team_player_player ON `TEAM_PLAYER`(player_id);

-- `GAME` Ï°∞Ìöå ÏµúÏ†Å??
CREATE INDEX idx_game_season ON `GAME`(season_id);
CREATE INDEX idx_game_date ON `GAME`(game_date);
CREATE INDEX idx_game_teams ON `GAME`(home_team, away_team);

-- STATS Ï°∞Ìöå ÏµúÏ†Å??
CREATE INDEX idx_batter_stats_season ON `BATTER_STATS`(season_id, season_type);
CREATE INDEX idx_batter_stats_tp ON `BATTER_STATS`(tp_id);
CREATE INDEX idx_pitcher_stats_season ON `PITCHER_STATS`(season_id, season_type);
CREATE INDEX idx_pitcher_stats_tp ON `PITCHER_STATS`(tp_id);

-- GAME_LOG Ï°∞Ìöå ÏµúÏ†Å??
CREATE INDEX idx_batter_log_game ON `BATTER_GAME_LOG`(game_idx);
CREATE INDEX idx_batter_log_player ON `BATTER_GAME_LOG`(player_idx);
CREATE INDEX idx_batter_log_stat ON `BATTER_GAME_LOG`(batter_stat_id);
CREATE INDEX idx_pitcher_log_game ON `PITCHER_GAME_LOG`(game_idx);
CREATE INDEX idx_pitcher_log_player ON `PITCHER_GAME_LOG`(player_idx);
CREATE INDEX idx_pitcher_log_stat ON `PITCHER_GAME_LOG`(pitcher_stat_id);

