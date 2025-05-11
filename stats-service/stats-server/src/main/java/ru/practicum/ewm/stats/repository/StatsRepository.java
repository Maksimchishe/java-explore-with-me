package ru.practicum.ewm.stats.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.dto.stats.EndpointHit;
import ru.practicum.ewm.dto.stats.ViewStats;
import ru.practicum.ewm.dto.stats.ViewStatsRequest;

import java.util.List;

@Repository
@Slf4j
@RequiredArgsConstructor
public class StatsRepository {
    private final NamedParameterJdbcOperations jdbcTemplate;

    public void saveHit(EndpointHit endpointHit) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("app", endpointHit.getApp());
        params.addValue("ip", endpointHit.getIp());
        params.addValue("uri", endpointHit.getUri());
        params.addValue("created", endpointHit.getTimestamp());
        String sqlFilms = """
                INSERT INTO stats(app, ip, uri, created)
                VALUES (:app, :ip, :uri, :created)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sqlFilms, params, keyHolder, new String[]{"id"});

        log.debug("Добавлен EndpointHit c id {}", keyHolder.getKeyAs(Long.class));
    }

    public List<ViewStats> getIntervalStats(ViewStatsRequest vSR) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("start", vSR.getStart());
        params.addValue("end", vSR.getEnd());

        if (vSR.getUris() == null || vSR.getUris().isEmpty()) {
            if (vSR.isUnique()) {
                String sqlViewStats = """
                        select s.app, s.uri, count(distinct s.ip) as hits
                        from stats as s
                        where s.created between :start and :end
                        group by s.app, s.uri
                        order by count(distinct s.ip) desc
                        """;
                return jdbcTemplate.queryForList(sqlViewStats, params, ViewStats.class);
            } else {
                String sqlViewStats = """
                        select s.app, s.uri, count(s.ip) as hits
                        from stats as s
                        where s.created between :start and :end
                        group by s.app, s.uri
                        order by count(s.ip) desc
                        """;
                return jdbcTemplate.queryForList(sqlViewStats, params, ViewStats.class);
            }
        } else {
            params.addValue("uris", listToString(vSR.getUris()));
System.out.println(params);
            if (vSR.isUnique()) {
                String sqlViewStats = """
                        select s.app, s.uri, count(distinct s.ip) as hits
                        from stats as s
                        where s.created between :start and :end
                        and (:uris) like '%' || s.uri || ',%'
                        group by s.app, s.uri
                        order by count(distinct s.ip) desc
                        """;
                return jdbcTemplate.queryForList(sqlViewStats, params, ViewStats.class);
            } else {
                String sqlViewStats = """
                        select s.app, s.uri, count(s.ip) as hits
                        from stats as s
                        where s.created between :start and :end
                        and (:uris) like '%' || s.uri || ',%'
                        group by s.app, s.uri
                        order by count(s.ip) desc
                        """;
                return jdbcTemplate.queryForList(sqlViewStats, params, ViewStats.class);
            }
        }
    }

    private String listToString(List<String> stringList) {
        StringBuilder allUris = new StringBuilder();
        for (String currentUri : stringList) {
            allUris.append(currentUri).append(",");
        }
        return allUris.toString();
    }
}
