package ru.practicum.ewm.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.client.stats.StatsClient;
import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.dto.stats.ViewStats;
import ru.practicum.ewm.dto.stats.ViewStatsRequest;
import ru.practicum.ewm.enums.*;
import ru.practicum.ewm.errorHandler.exceptions.AlreadyExistsException;
import ru.practicum.ewm.errorHandler.exceptions.NotFoundException;
import ru.practicum.ewm.errorHandler.exceptions.ValidationException;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final RequestRepository requestRepository;

    private final StatsClient statsClient;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<EventShortDto> getEventsByInitiator(Long userId, Pageable pageable) {
        List<Event> eventList = eventRepository.findAllByInitiatorId(userId, pageable).toList();
        List<EventShortDto> eventShortDtoList = new ArrayList<>();
        for (Event currentEvent : eventList) {
            EventShortDto eventShortDto = eventMapper.toEventShortDto(currentEvent);
            eventShortDtoList.add(addShortConfirmedRequestsAndViews(eventShortDto));
        }
        return eventShortDtoList;
    }

    @Override
    public EventFullDto addEvent(Long userId, NewEventDto newEventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User does not exist " + userId));
        if (newEventDto.getDescription().isBlank()) {
            throw new ValidationException("Поле Description не может быть пустым.");
        }
        if (newEventDto.getAnnotation().isBlank()) {
            throw new ValidationException("Поле Annotation не может быть пустым.");
        }
        if (newEventDto.getParticipantLimit() < 0) {
            throw new ValidationException("Поле ParticipantLimit не может быть отрицательным.");
        }
        if (newEventDto.getEventDate().minusHours(2).isBefore(LocalDateTime.now())) {
            throw new ValidationException("дата и время на которые намечено событие не может быть раньше, " +
                                          "чем через два часа от текущего момента");
        }
        Event eventToSave = eventMapper.toEvent(newEventDto);
        eventToSave.setState(EventState.PENDING);
        eventToSave.setCreatedOn(LocalDateTime.now());
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category does not exist"));
        eventToSave.setCategory(category);
        eventToSave.setInitiator(user);
        eventRepository.save(eventToSave);
        return addConfirmedRequestsAndViews(eventMapper.toEventFullDto(eventToSave));
    }

    @Override
    public EventFullDto getEventByInitiator(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event does not exist"));
        return addConfirmedRequestsAndViews(eventMapper.toEventFullDto(event));
    }

    @Override
    public EventFullDto updateEventByInitiator(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        Event eventToUpdate = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event does not exist " + eventId));
        if (updateEventUserRequest.getParticipantLimit() < 0) {
            throw new ValidationException("Поле ParticipantLimit не может быть отрицательным.");
        }
        if (eventToUpdate.getState().equals(EventState.CANCELED) || eventToUpdate.getState().equals(EventState.PENDING)) {
            if (updateEventUserRequest.getEventDate() != null
                && updateEventUserRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Дата и время на которые намечено событие не может быть раньше, " +
                                              "чем через два часа от текущего момента ");
            }
            if (StateActionUser.SEND_TO_REVIEW == updateEventUserRequest.getStateAction()) {
                eventToUpdate.setState(EventState.PENDING);
            }
            if (StateActionUser.CANCEL_REVIEW == updateEventUserRequest.getStateAction()) {
                eventToUpdate.setState(EventState.CANCELED);
            }
        } else {
            throw new AlreadyExistsException("State of event should be CANCELED or PENDING" + eventToUpdate.getState());
        }
        updateEventEntity(updateEventUserRequest, eventToUpdate);
        eventRepository.save(eventToUpdate);
        return addConfirmedRequestsAndViews(eventMapper.toEventFullDto(eventToUpdate));
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> userIdList, List<String> states, List<Long> categories,
                                               String rangeStart, String rangeEnd, Integer from, Integer size) {
        final PageRequest pageRequest = PageRequest.of(from / size, size);

        List<EventFullDto> eventFullDtoList = new ArrayList<>();
        if (states == null & rangeStart == null & rangeEnd == null) {
            List<Event> eventList = eventRepository.findAll(pageRequest).toList();
            for (Event currentEvent : eventList) {
                EventFullDto eventFullDto = eventMapper.toEventFullDto(currentEvent);
                eventFullDtoList.add(addConfirmedRequestsAndViews(eventFullDto));
            }
            return eventFullDtoList;
        }
        LocalDateTime start;
        if (rangeStart != null && !rangeStart.isEmpty()) {
            start = LocalDateTime.parse(rangeStart, DTF);
        } else {
            start = LocalDateTime.now().plusYears(5);
        }
        LocalDateTime end;
        if (rangeEnd != null && !rangeEnd.isEmpty()) {
            end = LocalDateTime.parse(rangeEnd, DTF);
        } else {
            end = LocalDateTime.now().plusYears(5);
        }
        List<Event> eventList = eventRepository.findEvents(userIdList, states, categories, start, end, pageRequest).toList();
        for (Event currentEvent : eventList) {
            EventFullDto eventFullDto = eventMapper.toEventFullDto(currentEvent);
            eventFullDtoList.add(addConfirmedRequestsAndViews(eventFullDto));
        }
        return eventFullDtoList;
    }

    @Override
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {
        Event eventToUpdate = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event does not exist " + eventId));

        if (updateEventAdminRequest.getEventDate() != null) {
            if (updateEventAdminRequest.getEventDate().minusHours(1).isBefore(LocalDateTime.now())) {
                throw new ValidationException("Дата начала события должна быть не ранее чем за час от даты публикации");
            }
        }
        if (updateEventAdminRequest.getStateAction() != null) {
            if (updateEventAdminRequest.getStateAction() == StateActionAdmin.PUBLISH_EVENT) {
                if (eventToUpdate.getState().equals(EventState.PENDING)) {
                    eventToUpdate.setState(EventState.PUBLISHED);
                    eventToUpdate.setPublishedOn(LocalDateTime.now());
                } else {
                    throw new AlreadyExistsException("Event should be PENDING in order to be PUBLISHED" +
                                                     updateEventAdminRequest.getStateAction());
                }
            }
            if (updateEventAdminRequest.getStateAction() == StateActionAdmin.REJECT_EVENT) {
                if (eventToUpdate.getState().equals(EventState.PUBLISHED)) {
                    throw new AlreadyExistsException("Event should be PENDING in order to reject it " +
                                                     updateEventAdminRequest.getStateAction());
                }
                eventToUpdate.setState(EventState.CANCELED);
            }
        }
        updateEventEntity(updateEventAdminRequest, eventToUpdate);
        eventRepository.save(eventToUpdate);
        return addConfirmedRequestsAndViews(eventMapper.toEventFullDto(eventToUpdate));
    }

    @Override
    public List<EventShortDto> getEventList(String text, List<Long> categoryIdList, Boolean paid, String rangeStart,
                                            String rangeEnd, Boolean onlyAvailable, String sort, Integer from,
                                            Integer size, String userIp, String requestUri) {
        LocalDateTime start = null;
        LocalDateTime end = null;

        statsClient.hit(userIp, requestUri);

        if (rangeStart != null && rangeEnd != null) {
            start = LocalDateTime.parse(rangeStart, DTF);
            end = LocalDateTime.parse(rangeEnd, DTF);
            if (start.isAfter(end)) {
                throw new ValidationException("Wrong dates");
            }
        } else {
            if (rangeStart == null && rangeEnd == null) {
                start = LocalDateTime.now();
                end = LocalDateTime.now().plusYears(10);
            } else {
                if (rangeStart == null) {
                    start = LocalDateTime.now();
                }
            }
        }
        final PageRequest pageRequest = PageRequest.of(from / size, size);
        List<Event> eventList = eventRepository.searchPublishedEvents(text, categoryIdList, paid, start, end, pageRequest)
                .getContent();
        List<EventShortDto> eventShortDtoList = new ArrayList<>();
        if (eventList.isEmpty()) {
            return eventShortDtoList;
        }
        for (Event currentEvent : eventList) {
            eventShortDtoList.add(addShortConfirmedRequestsAndViews(eventMapper.toEventShortDto(currentEvent)));
        }
        if (sort != null) {
            switch (SortValue.valueOf(sort)) {
                case EVENT_DATE:
                    eventShortDtoList.sort(Comparator.comparing(EventShortDto::getEventDate));
                    break;

                case VIEWS:
                    eventShortDtoList.sort(Comparator.comparing(EventShortDto::getViews));
                    break;
                default:
                    throw new ValidationException("Parameter sort is not valid");
            }
        }
        return eventShortDtoList;
    }

    @Override
    public EventFullDto getEvent(Long eventId, String userIp, String requestUri) {
        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event does not exist " + eventId));

        statsClient.hit(userIp, requestUri);

        return addConfirmedRequestsAndViews(eventMapper.toEventFullDto(event));
    }

    private void updateEventEntity(UpdateEventUserRequest event, Event eventToUpdate) {
        eventToUpdate.setAnnotation(Objects.requireNonNullElse(event.getAnnotation(), eventToUpdate.getAnnotation()));
        eventToUpdate.setCategory(event.getCategory() == null
                ? eventToUpdate.getCategory()
                : categoryRepository.findById(event.getCategory()).orElseThrow(() -> new NotFoundException("Category not fount")));
        eventToUpdate.setDescription(Objects.requireNonNullElse(event.getDescription(), eventToUpdate.getDescription()));
        eventToUpdate.setEventDate(Objects.requireNonNullElse(event.getEventDate(), eventToUpdate.getEventDate()));
        eventToUpdate.setLocation(event.getLocation() == null
                ? eventToUpdate.getLocation()
                : locationRepository.findByLatAndLon(event.getLocation().getLat(), event.getLocation().getLon())
                .orElse(new Location(null, event.getLocation().getLat(), event.getLocation().getLon())));
        eventToUpdate.setPaid(Objects.requireNonNullElse(event.getPaid(), eventToUpdate.getPaid()));
        eventToUpdate.setParticipantLimit(Objects.requireNonNullElse(event.getParticipantLimit(), eventToUpdate.getParticipantLimit()));
        eventToUpdate.setRequestModeration(Objects.requireNonNullElse(event.getRequestModeration(), eventToUpdate.getRequestModeration()));
        eventToUpdate.setTitle(Objects.requireNonNullElse(event.getTitle(), eventToUpdate.getTitle()));
    }

    private void updateEventEntity(UpdateEventAdminRequest event, Event eventToUpdate) {
        eventToUpdate.setAnnotation(Objects.requireNonNullElse(event.getAnnotation(), eventToUpdate.getAnnotation()));
        eventToUpdate.setCategory(event.getCategory() == null
                ? eventToUpdate.getCategory()
                : categoryRepository.findById(event.getCategory()).orElseThrow(() -> new NotFoundException("Category not fount")));
        eventToUpdate.setDescription(Objects.requireNonNullElse(event.getDescription(), eventToUpdate.getDescription()));
        eventToUpdate.setEventDate(Objects.requireNonNullElse(event.getEventDate(), eventToUpdate.getEventDate()));
        eventToUpdate.setLocation(event.getLocation() == null
                ? eventToUpdate.getLocation()
                : locationRepository.findByLatAndLon(event.getLocation().getLat(), event.getLocation().getLon())
                .orElse(new Location(null, event.getLocation().getLat(), event.getLocation().getLon())));
        eventToUpdate.setPaid(Objects.requireNonNullElse(event.getPaid(), eventToUpdate.getPaid()));
        eventToUpdate.setParticipantLimit(Objects.requireNonNullElse(event.getParticipantLimit(), eventToUpdate.getParticipantLimit()));
        eventToUpdate.setRequestModeration(Objects.requireNonNullElse(event.getRequestModeration(), eventToUpdate.getRequestModeration()));
        eventToUpdate.setTitle(Objects.requireNonNullElse(event.getTitle(), eventToUpdate.getTitle()));
    }

    private EventFullDto addConfirmedRequestsAndViews(EventFullDto eventFullDto) {
        eventFullDto.setConfirmedRequests(
                requestRepository.countByEventIdAndStatus(eventFullDto.getId(), RequestStatus.CONFIRMED));
        List<String> uris = new ArrayList<>();
        uris.add("/events/" + eventFullDto.getId());
        ViewStatsRequest viewStatsRequest = new ViewStatsRequest(
                LocalDateTime.now().minusYears(100),
                LocalDateTime.now(),
                uris,
                true);
        List<ViewStats> viewStatsList = statsClient.getStats(viewStatsRequest);
        if (viewStatsList.isEmpty()) {
            eventFullDto.setViews(0L);
        } else {
            eventFullDto.setViews(viewStatsList.get(0).getHits());
        }
        return eventFullDto;
    }

    private EventShortDto addShortConfirmedRequestsAndViews(EventShortDto eventShortDto) {
        eventShortDto.setConfirmedRequests(
                requestRepository.countByEventIdAndStatus(eventShortDto.getId(), RequestStatus.CONFIRMED));
        List<String> uris = new ArrayList<>();
        uris.add("/events/" + eventShortDto.getId());
        ViewStatsRequest viewStatsRequest = new ViewStatsRequest(
                LocalDateTime.now().minusYears(100),
                LocalDateTime.now(),
                uris,
                true);
        List<ViewStats> viewStatsList = statsClient.getStats(viewStatsRequest);
        if (viewStatsList.isEmpty()) {
            log.info("addShortConfirmedRequestsAndViews: просмотров нет у этого эндпоинта " +
                     viewStatsRequest.getUris());
            eventShortDto.setViews(0L);
        } else {
            eventShortDto.setViews(viewStatsList.get(0).getHits());
        }
        return eventShortDto;
    }
}