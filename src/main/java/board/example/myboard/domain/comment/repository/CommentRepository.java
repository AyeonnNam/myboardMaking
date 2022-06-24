package board.example.myboard.domain.comment.repository;

import board.example.myboard.domain.comment.service.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
