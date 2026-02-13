package com.aubl.webpage.service;

import com.aubl.webpage.api.dto.SeasonCreateRequest;
import com.aubl.webpage.domain.entity.Season;
import com.aubl.webpage.domain.repository.SeasonRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
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
    public List<Season> getSeasons() {
        return seasonRepository.findAll(Sort.by(Sort.Direction.DESC, "year", "id"));
    }
}
