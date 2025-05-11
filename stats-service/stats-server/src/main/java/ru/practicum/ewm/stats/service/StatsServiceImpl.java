package ru.practicum.ewm.stats.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.stats.EndpointHit;
import ru.practicum.ewm.dto.stats.ViewStats;
import ru.practicum.ewm.dto.stats.ViewStatsRequest;
import ru.practicum.ewm.stats.errorHandler.exceptions.ValidationException;
import ru.practicum.ewm.stats.repository.StatsRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsServiceImpl implements StatsService {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StatsRepository hitRepository;

    @Override
    public void recordHit(EndpointHit endpointHit) {
        hitRepository.saveHit(endpointHit);
    }

    @Override
    public List<ViewStats> calculateViews(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        ViewStatsRequest.ViewStatsRequestBuilder builder = ViewStatsRequest.builder()
                .unique(unique);
        builder.uris(uris);

        if (start.isAfter(end)) {
            throw new ValidationException("Дата начала находится после даты окончания.");
        }

        log.info("StatsService / calculateViews / start {}  end {} ", start, end);
        builder.start(start);
        builder.end(end);

        return hitRepository.getIntervalStats(builder.build());
    }
}
