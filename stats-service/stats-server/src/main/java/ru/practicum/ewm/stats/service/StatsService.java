package ru.practicum.ewm.stats.service;

import ru.practicum.ewm.dto.stats.EndpointHit;
import ru.practicum.ewm.dto.stats.ViewStats;

import java.util.List;

public interface StatsService {

    void recordHit(EndpointHit endpointHit);

    List<ViewStats> calculateViews(String start,
                                   String end,
                                   List<String> uris,
                                   boolean unique);
}
