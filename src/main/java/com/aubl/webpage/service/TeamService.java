package com.aubl.webpage.service;

import com.aubl.webpage.api.dto.TeamCreateRequest;
import com.aubl.webpage.domain.entity.Team;
import com.aubl.webpage.domain.entity.UserAccount;
import com.aubl.webpage.domain.repository.TeamRepository;
import com.aubl.webpage.domain.repository.UserAccountRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserAccountRepository userAccountRepository;

    public TeamService(TeamRepository teamRepository, UserAccountRepository userAccountRepository) {
        this.teamRepository = teamRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public Team createTeam(TeamCreateRequest request) {
        Team team = new Team();
        team.setTeamName(request.teamName());
        team.setTeamCode(request.teamCode());
        team.setManager(resolveManager(request.managerId()));
        return teamRepository.save(team);
    }

    @Transactional(readOnly = true)
    public List<Team> getTeams() {
        return teamRepository.findAll(Sort.by(Sort.Direction.ASC, "teamName", "id"));
    }

    private UserAccount resolveManager(Long managerId) {
        if (managerId == null) {
            return null;
        }
        return userAccountRepository.findById(managerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "manager not found"));
    }
}
