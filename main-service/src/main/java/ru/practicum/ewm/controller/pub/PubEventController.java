package ru.practicum.ewm.controller.pub;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.service.comment.CommentService;
import ru.practicum.ewm.service.event.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/events")
@RequiredArgsConstructor
@Slf4j
public class PubEventController {
    private final EventService eventService;
    private final CommentService commentService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getEventList(@RequestParam(required = false) String text,
                                            @RequestParam(required = false) List<Long> categories,
                                            @RequestParam(required = false) Boolean paid,
                                            @RequestParam(required = false)
                                            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") String rangeStart,
                                            @RequestParam(required = false)
                                            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") String rangeEnd,
                                            @RequestParam(required = false, defaultValue = "false") Boolean onlyAvailable,
                                            @RequestParam(required = false) String sort,
                                            @RequestParam(required = false, defaultValue = "0") @PositiveOrZero Integer from,
                                            @RequestParam(required = false, defaultValue = "10") @Positive Integer size,
                                            HttpServletRequest request) {
        log.info("PubEventController / getEventList: получение событий с возможностью фильтрации " +
                 text + categories + paid + rangeStart + rangeEnd + onlyAvailable + sort + from + size +
                 request.getRemoteAddr() + request.getRequestURI());
        return eventService.getEventList(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from,
                size, request.getRemoteAddr(), request.getRequestURI());
    }

    @GetMapping("/{id}")
    public EventFullDto getEvent(@PathVariable Long id, HttpServletRequest request) {
        log.info("PubEventController / getEvent: получение подробной инфо о событии по его id {}", id);
        return eventService.getEvent(id, request.getRemoteAddr(), request.getRequestURI());
    }

    @GetMapping("/{id}/comments")
    public List<CommentDto> getAllCommentsToEvent(@PathVariable Long id,
                                                  @RequestParam(required = false, defaultValue = "0") @PositiveOrZero Integer from,
                                                  @RequestParam(required = false, defaultValue = "10") @Positive Integer size) {
        log.info("PubEventController / getAllCommentsToEvent: получение комментариев к событию по его id {}", id);
        return commentService.getAllCommentsToEvent(id, PageRequest.of(from, size));
    }
}