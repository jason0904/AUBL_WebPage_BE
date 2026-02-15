package com.aubl.webpage.service;

import com.aubl.webpage.api.dto.SeasonCreateRequest;
import com.aubl.webpage.api.dto.SeasonResponse;
import com.aubl.webpage.domain.entity.Season;
import com.aubl.webpage.domain.repository.SeasonRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeasonService {

    private final SeasonRepository seasonRepository;

    public SeasonService(SeasonRepository seasonRepository) {
        this.seasonRepository = seasonRepository;
    }

    @Transactional
    public Season createSeason(SeasonCreateRequest request) {
        Season season = new Season();
        season.setYear(request.year());
        return seasonRepository.save(season);
    }

    @Transactional(readOnly = true)
    public List<SeasonResponse> getSeasons() {
        return seasonRepository.findAllByOrderByYearAsc().stream()
            .map(s -> new SeasonResponse(s.getId(), s.getYear()))
            .toList();
    }
}
