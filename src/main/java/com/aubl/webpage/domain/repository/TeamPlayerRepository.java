package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.TeamPlayer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamPlayerRepository extends JpaRepository<TeamPlayer, Long> {
    java.util.Optional<TeamPlayer> findByTeamIdAndSeasonIdAndPlayerPlayerName(
        Long teamId,
        Long seasonId,
        String playerName
    );

    java.util.Optional<TeamPlayer> findByTeamIdAndSeasonIdAndJerseyNumberAndPlayerPlayerName(
        Long teamId,
        Long seasonId,
        Integer jerseyNumber,
        String playerName
    );

    @EntityGraph(attributePaths = {"team", "player", "season"})
    java.util.List<TeamPlayer> findByTeamIdAndSeasonId(Long teamId, Long seasonId);

    @EntityGraph(attributePaths = {"team", "season"})
    java.util.List<TeamPlayer> findDistinctBySeasonId(Long seasonId);

    /** 시즌에 실제 사용된 part_code 목록 (오름차순, null 제외) */
    @Query("SELECT DISTINCT tp.partCode FROM TeamPlayer tp WHERE tp.season.id = :seasonId "
        + "AND tp.partCode IS NOT NULL ORDER BY tp.partCode ASC")
    java.util.List<String> findDistinctPartCodesBySeasonId(@Param("seasonId") Long seasonId);

    /**
     * 시즌별 팀ID → partCode 매핑 조회.
     * 같은 팀에 여러 partCode가 있을 수 있으므로 DISTINCT + 첫 번째 값 사용.
     * 반환: Object[]{teamId, partCode}
     */
    @Query("SELECT DISTINCT tp.team.id, tp.partCode FROM TeamPlayer tp "
        + "WHERE tp.season.id = :seasonId AND tp.partCode IS NOT NULL")
    java.util.List<Object[]> findTeamPartCodesBySeasonId(@Param("seasonId") Long seasonId);

    @EntityGraph(attributePaths = {"team", "player", "season"})
    @Query("select tp from TeamPlayer tp join tp.player p join tp.team t join tp.season s " +
        "where s.id = :seasonId " +
        "and (:teamId is null or t.id = :teamId) " +
        "and (:cursorId is null or tp.id > :cursorId) " +
        "and (:normalized is null or lower(replace(p.playerName, ' ', '')) like concat('%', :normalized, '%')) " +
        "order by tp.id asc")
    java.util.List<TeamPlayer> findRoster(
        @Param("seasonId") Long seasonId,
        @Param("teamId") Long teamId,
        @Param("cursorId") Long cursorId,
        @Param("normalized") String normalized,
        org.springframework.data.domain.Pageable pageable
    );

    @Query("select count(tp) from TeamPlayer tp join tp.player p join tp.team t join tp.season s " +
        "where s.id = :seasonId " +
        "and (:teamId is null or t.id = :teamId) " +
        "and (:normalized is null or lower(replace(p.playerName, ' ', '')) like concat('%', :normalized, '%'))")
    long countRoster(
        @Param("seasonId") Long seasonId,
        @Param("teamId") Long teamId,
        @Param("normalized") String normalized
    );

    @EntityGraph(attributePaths = {"team", "player", "season"})
    java.util.Optional<TeamPlayer> findByPlayerIdAndSeasonId(Long playerId, Long seasonId);

    @EntityGraph(attributePaths = {"team", "player", "season"})
    java.util.Optional<TeamPlayer> findFirstByPlayerIdOrderBySeasonYearDesc(Long playerId);

    @EntityGraph(attributePaths = {"team", "player", "season"})
    @Query("select tp from TeamPlayer tp join tp.player p join tp.team t join tp.season s " +
        "where s.id = :seasonId " +
        "and (:teamId is null or t.id = :teamId) " +
        "and lower(replace(p.playerName, ' ', '')) like concat('%', :normalized, '%') " +
        "order by p.playerName asc")
    java.util.List<TeamPlayer> searchPlayers(
        @Param("seasonId") Long seasonId,
        @Param("teamId") Long teamId,
        @Param("normalized") String normalized,
        org.springframework.data.domain.Pageable pageable
    );
}
