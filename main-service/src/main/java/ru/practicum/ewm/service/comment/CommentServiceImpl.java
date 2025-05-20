package ru.practicum.ewm.service.comment;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.errorHandler.exceptions.NotFoundException;
import ru.practicum.ewm.errorHandler.exceptions.ValidationException;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        Comment commentToSave = commentMapper.toComment(newCommentDto);
        commentToSave.setCreated(LocalDateTime.now());
        commentToSave.setCommentator(userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("User does not exist " + userId)));
        commentToSave.setEvent(eventRepository.findById(eventId).orElseThrow(() ->
                new NotFoundException("Event does not exist " + eventId)));

        commentRepository.save(commentToSave);

        return commentMapper.toCommentDto(commentToSave);
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        if (userId.equals(commentRepository.findById(commentId).orElseThrow(() ->
                new NotFoundException("Comment does not exist " + commentId)).getCommentator().getId())) {
            commentRepository.deleteById(commentId);
        } else {
            throw new ValidationException("User with id is not commentator" + userId);
        }
    }

    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto newCommentDto) {
        Comment commentToUpdate = commentRepository.findById(commentId).orElseThrow(() ->
                new NotFoundException("Comment does not exist " + commentId));

        if (!userId.equals(commentToUpdate.getCommentator().getId())) {
            throw new ValidationException("User with id is not commentator" + userId);
        }

        if (!newCommentDto.getCommentText().isEmpty() && newCommentDto.getCommentText() != null) {
            commentToUpdate.setCommentText(newCommentDto.getCommentText());
            commentRepository.save(commentToUpdate);
        }

        return commentMapper.toCommentDto(commentToUpdate);
    }

    @Override
    public List<CommentDto> getAllCommentsToEvent(Long eventId, Pageable pageable) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Event does not exist " + eventId);
        }

        List<Comment> commentList = commentRepository.findAllByEvent_IdOrderByCreatedDesc(eventId, pageable);
        List<CommentDto> commentDtoList = new ArrayList<>();

        for (Comment comment : commentList) {
            commentDtoList.add(commentMapper.toCommentDto(comment));
        }

        return commentDtoList;
    }

    @Override
    public void deleteCommentByAdmin(Long commentId) {
        if (commentRepository.existsById(commentId)) {
            commentRepository.deleteById(commentId);
        } else {
            throw new NotFoundException("Comment does not exist " + commentId);
        }
    }
}
