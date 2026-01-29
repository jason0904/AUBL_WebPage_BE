package com.aubl.webpage.service;

import com.aubl.webpage.api.dto.PlayerCreateRequest;
import com.aubl.webpage.domain.entity.Player;
import com.aubl.webpage.domain.entity.UserAccount;
import com.aubl.webpage.domain.repository.PlayerRepository;
import com.aubl.webpage.domain.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final UserAccountRepository userAccountRepository;

    public PlayerService(PlayerRepository playerRepository, UserAccountRepository userAccountRepository) {
        this.playerRepository = playerRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public Player createPlayer(PlayerCreateRequest request) {
        Player player = new Player();
        player.setUser(resolveUser(request.userId()));
        player.setPlayerName(request.playerName());
        player.setBirthDate(request.birthDate());
        player.setPosition(request.position());
        player.setHeight(request.height());
        player.setWeight(request.weight());
        player.setSchool(request.school());
        player.setIsPlayer(request.isPlayer());
        return playerRepository.save(player);
    }

    private UserAccount resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userAccountRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }
}
