package ru.practicum.ewm.service.comment;

import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;

import java.util.List;

public interface CommentService {

    CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    void deleteComment(Long userId, Long commentId);

    CommentDto updateComment(Long userId, Long commentId, NewCommentDto newCommentDto);

    List<CommentDto> getAllCommentsToEvent(Long eventId, Pageable pageable);

    void deleteCommentByAdmin(Long commentId);
}
