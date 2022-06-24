package board.example.myboard.comment;

import board.example.myboard.domain.comment.repository.CommentRepository;
import board.example.myboard.domain.comment.service.Comment;
import board.example.myboard.domain.comment.service.CommentService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
public class CommentServiceTest {

    @Autowired
    CommentService commentService;

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    EntityManager em;

    private void clear(){
        em.flush();
        em.clear();
    }

    private Long saveComment() {
        Comment comment = Comment.builder().content("댓글").build();
        Long id = commentRepository.save(comment).getId();
        clear();
        return id;
    }

    private Long saveReComent(Long parentId) {
        Comment parent = commentRepository.findById(parentId).orElse(null);
        Comment comment = Comment.builder().content("댓글").parent(parent).build();

        Long id = commentRepository.save(comment).getId();
        clear();

        return id;
    }

    // 댓글을 삭제하는 경우
    // 대댓글이 남아있는 경우
    //DB의 화면에서는 지워지지 않고, "삭제된 댓글입니다"라고 표시

    @Test
    public void 댓글삭제_대댓글이_남아있는_경우() throws Exception {

        Long commentId = saveComment();
        Long reComment1Id = saveReComent(commentId);
        Long reComment2Id = saveReComent(commentId);
        Long reComment3Id = saveReComent(commentId);
        Long reComment4Id = saveReComent(commentId);

        assertThat(commentService.findById(commentId).getChildList().size()).isEqualTo(4);

        commentService.remove(reComment1Id);
        clear();

        commentService.remove(reComment2Id);
        clear();

        commentService.remove(reComment3Id);
        clear();

        commentService.remove(reComment4Id);
        clear();

        assertThat(commentService.findById(reComment1Id).isRemoved()).isTrue();

        assertThat(commentService.findById(reComment2Id).isRemoved()).isTrue();

        assertThat(commentService.findById(reComment3Id).isRemoved()).isTrue();

        assertThat(commentService.findById(reComment4Id).isRemoved()).isTrue();
        clear();

        commentService.remove(commentId);
        clear();

        LongStream.rangeClosed(commentId, reComment4Id).forEach(id -> {
            assertThat(assertThrows(Exception.class, () -> commentService.findById(id)).getMessage())
                    .isEqualTo("댓글이 없습니다.");
        });
    }


    //대댓글을 삭제하는 경우
    //부모 댓글이 삭제되지 않은 경우
    //내용만 삭제, DB에서는 삭제 X

    @Test
    public void 대댓글삭제_부모댓글이_남아있는_경우() throws Exception {

        Long commentId = saveComment();
        Long reCommentId = saveReComent(commentId);

        commentService.remove(reCommentId);
        clear();

        assertThat(commentService.findById(commentId)).isNotNull();
        assertThat(commentService.findById(reCommentId)).isNotNull();
        assertThat(commentService.findById(commentId).isRemoved()).isFalse();
        assertThat(commentService.findById(reCommentId).isRemoved()).isTrue();

    }

    //대댓글을 삭제하는 경우
    //부모 댓글이 삭제되어있고, 대댓글들도 모두 삭제된 경우
    //부모를 포함한 모든 대댓글을 DB에서 일괄 삭제, 화면상에서도 지움
    @Test
    public void 대댓글삭제_부모댓글이_삭제된_경우_모든_대댓글이_삭제된_경우() throws Exception {
        Long commentId = saveComment();
        Long reComment1Id = saveReComent(commentId);
        Long reComment2Id = saveReComent(commentId);
        Long reComment3Id = saveReComent(commentId);

        commentService.remove(reComment2Id);
        clear();

        commentService.remove(reComment3Id);
        clear();

        commentService.remove(commentId);
        clear();

        assertThat(commentService.findById(commentId)).isNotNull();

        assertThat(commentService.findById(commentId).getChildList().size()).isEqualTo(3);

        commentService.remove(reComment1Id);

        LongStream.rangeClosed(commentId, reComment3Id).forEach(id -> assertThat(assertThrows(Exception.class,
                () -> commentService.findById(id)).getMessage()).isEqualTo("댓글이 없습니다."));
    }


    //대댓글을 삭제하는 경우
    //부모 댓글이 삭제되어 있고, 다른 대댓글이 아직 삭제되지 않고 남아있는 경우
    //해당 대댓글만 삭제, 그러나 db에서 삭제되지는 않고, 화면상에서는 "삭제된 댓글입니다"라고 표시

    @Test
    public void 대댓글삭제_부모댓글이_삭제된_경우_다른_대댓글이_남아있는_경우() throws Exception {

        Long commentId = saveComment();
        Long reComment1Id = saveReComent(commentId);
        Long reComment2Id = saveReComent(commentId);
        Long reComment3Id = saveReComent(commentId);

        commentService.remove(reComment3Id);

        commentService.remove(commentId);

        clear();


        assertThat(commentService.findById(commentId)).isNotNull();

        assertThat(commentService.findById(commentId).getChildList().size()).isEqualTo(3);


        commentService.remove(reComment2Id);
        assertThat(commentService.findById(commentId)).isNotNull();

        assertThat(commentService.findById(reComment2Id)).isNotNull();
        assertThat(commentService.findById(reComment2Id).isRemoved()).isTrue();
        assertThat(commentService.findById(reComment1Id).getId()).isNotNull();
        assertThat(commentService.findById(reComment3Id).getId()).isNotNull();
        assertThat(commentService.findById(commentId).getId()).isNotNull();



    }
}
