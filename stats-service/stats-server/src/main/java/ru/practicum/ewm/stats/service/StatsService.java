package ru.practicum.ewm.stats.service;

import ru.practicum.ewm.dto.stats.EndpointHit;
import ru.practicum.ewm.dto.stats.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {

    void recordHit(EndpointHit endpointHit);

    List<ViewStats> calculateViews(LocalDateTime start,
                                   LocalDateTime end,
                                   List<String> uris,
                                   boolean unique);
}
