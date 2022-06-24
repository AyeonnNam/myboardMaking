package board.example.myboard.domain.Member;

import board.example.myboard.domain.member.Member;
import board.example.myboard.domain.member.Role;
import board.example.myboard.domain.repository.MemberRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Entity;
import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberRepositoryTest {


    @Autowired
    MemberRepository memberRepository;

   @Autowired EntityManager em;



    @AfterEach
    private void after(){
        em.clear();
    }

    private void clear(){
        em.flush();;
        em.clear();
        }


    @Test
    public void 회원저장_성공() throws Exception {
        Member member = Member.builder().username("username").password("!23455ee5").name("Member1").nickname("nickname").role(Role.USER).age(22).build();

        Member saveMember = memberRepository.save(member);

        Member findMember = memberRepository.findById(saveMember.getId()).orElseThrow(() -> new RuntimeException("저장된 회원이 없습니다."));

        assertThat(findMember).isSameAs(saveMember);

        assertThat(findMember).isSameAs(member);


    }

    @Test
    public void 오류_회원가입시_아이디가_없음() throws Exception {
        Member member = Member.builder().password("!23455ee5").name("Member1").nickname("nickname").role(Role.USER).age(22).build();

        assertThrows(Exception.class, () -> memberRepository.save(member));
    }

    @Test
    public void 오류_이름없이_가입시() throws Exception {
        Member member = Member.builder().username("username").password("!23455ee5").nickname("nickname").role(Role.USER).age(22).build();

        assertThrows(Exception.class, () -> memberRepository.save(member));
    }

    @Test
    public void 오류_닉네임없이_가입시() throws Exception {
        Member member = Member.builder().username("username").password("!23455ee5").name("Member1").role(Role.USER).age(22).build();

        assertThrows(Exception.class, () -> memberRepository.save(member));
    }

    @Test
    public void 오류_나이없이_가입시() throws Exception {
        Member member = Member.builder().username("username").password("!23455ee5").name("Member1").nickname("nickname").role(Role.USER).build();

        assertThrows(Exception.class, () -> memberRepository.save(member));
    }

    @Test
    public void 오류_회원가입시_중복된_아이디가_있음() throws Exception{
        Member member1 = Member.builder().username("username").password("!23455ee5").name("Member1").nickname("nickname").role(Role.USER).age(22).build();
        Member member2 = Member.builder().username("username").password("efasdfdsf").name("Member2").nickname("닉네임").role(Role.USER).age(23).build();

        memberRepository.save(member1);
        clear();

        assertThrows(Exception.class, () -> memberRepository.save(member2));

    }

    @Test
    public void 성공_회원수정() throws Exception {

        Member member = Member.builder().username("username")
                .password("!23455ee5").name("Member1").nickname("nickname").role(Role.USER).age(22).build();
        memberRepository.save(member);

        em.flush();
        em.clear();

        String updatePassword = "updatePassword";
        String updateName = "updateName";
        String updateNickname = "updateNickname";
        int updateAge = 33;

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        Member findMember = memberRepository.findById(member.getId()).orElseThrow(() -> new Exception());

        findMember.updateAge(updateAge);
        findMember.updateName(updateName);
        findMember.updateNickname(updateNickname);
        findMember.updatePassword(passwordEncoder,updatePassword);
        em.flush();

        Member updateMember = memberRepository.findById(findMember.getId()).orElseThrow(() -> new Exception());

        assertThat(updateMember).isSameAs(findMember);
        assertThat(passwordEncoder.matches(updatePassword,findMember.getPassword())).isTrue();
        assertThat(updateMember.getName()).isEqualTo(updateName);
        assertThat(updateMember.getName()).isNotEqualTo(member.getName());


    }

    @Test
    public void 성공_회원삭제() throws Exception {
        Member member = Member.builder().username("username").password("!23455ee5").name("Member1").nickname("nickname").role(Role.USER).age(22).build();

        memberRepository.save(member);
        clear();

        memberRepository.delete(member);
        clear();

        assertThrows(Exception.class, () -> memberRepository.findById(member.getId()).orElseThrow(() -> new Exception()));

    }

    @Test
    void findByUsername_정상작동() throws Exception {
        String username ="username";
        Member member = Member.builder().username(username).password("!23455ee5").name("Member1").nickname("nickname").role(Role.USER).age(22).build();
        memberRepository.save(member);
        clear();

      assertThat(memberRepository.findByUsername(username).get().getUsername()).isEqualTo(member.getUsername());
      assertThat(memberRepository.findByUsername(username).get().getName()).isEqualTo(member.getName());
      assertThat(memberRepository.findByUsername(username).get().getId()).isEqualTo(member.getId());
      assertThrows(Exception.class, () -> memberRepository.findByUsername(username+"123").orElseThrow(() -> new Exception()));

    }

    @Test
    void existsByUsername_정상작동() throws Exception {
        String username = "username";
        Member member = Member.builder().username("username").password("!23455ee5").name("Member1").nickname("nickname").role(Role.USER).age(22).build();
        memberRepository.save(member);
        clear();

        assertThat(memberRepository.existsByUsername(username)).isTrue();
        assertThat(memberRepository.existsByUsername(username+"123")).isFalse();

    }

    @Test
    void 회원가입시_생성시간_등록() throws Exception {
        Member member = Member.builder().username("username").password("!23455ee5").name("Member1").nickname("nickname").role(Role.USER).age(22).build();
        memberRepository.save(member);
        clear();

        Member findMember = memberRepository.findById(member.getId()).orElseThrow(() -> new Exception());

        assertThat(findMember.getCreatedDate()).isNotNull();
        assertThat(findMember.getLastModifiedDate()).isNotNull();

    }

}