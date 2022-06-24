package board.example.myboard.domain.comment.service;

import board.example.myboard.BaseTimeEntity;
import board.example.myboard.domain.member.Member;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name= "COMMENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "writer_id")
    private Member writer;


    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Lob
    @Column(nullable = false)
    private String content;

    private boolean isRemoved = false;

    @OneToMany(mappedBy = "parent")
    private List<Comment> childList = new ArrayList<>();

    //연관관계 편의 메서드//
    public void confirmWriter(Member writer){
        this.writer = writer;
        writer.addComment(this);
    }

    public void confirmPost(Post post){
        this.post = post;
        post.addComment(this);
    }

    public void confirmParent(Comment parent){
        this.parent = parent;
        parent.addChild(this);
    }
    public void addChild(Comment child) {
        childList.add(child);
    }

    //수정//
    public void updateContent(String content){
        this.content = content;
    }

    //삭제//
    public void remove() {
        this.isRemoved = true;
    }

    @Builder
    public Comment(Member writer, Post post, Comment parent, String content){
        this.writer =writer;
        this.post = post;
        this.parent = parent;
        this.content = content;
        this.isRemoved = false;
    }

    //비즈니스 로직//
    public List<Comment> findRemovableList(){
        List<Comment> result = new ArrayList<>();

        Optional.ofNullable(this.parent).ifPresentOrElse(

                parentComment -> {
                    if( parentComment.isRemoved()&& parentComment.isAllChildRemoved()){
                        result.addAll(parentComment.getChildList());
                        result.add(parentComment);
                    }
                },
                ()-> {
                    if(isAllChildRemoved()) {
                        result.add(this);
                        result.addAll(this.getChildList());
                    }
                }
        );

        return result;
    }



    private boolean isAllChildRemoved() {

        return getChildList().stream()
                .map(Comment::isRemoved)
                .filter(isRemove -> !isRemove)
                .findAny()
                .orElse(true);
    }
}
