package board.example.myboard.domain.Member;


import board.example.myboard.domain.member.Member;
import board.example.myboard.domain.member.Role;
import board.example.myboard.domain.member.dto.MemberInfoDto;
import board.example.myboard.domain.member.dto.MemberSignUpDto;
import board.example.myboard.domain.member.dto.MemberUpdateDto;
import board.example.myboard.domain.repository.MemberRepository;
import board.example.myboard.domain.service.MemberService;
import com.zaxxer.hikari.SQLExceptionOverride;
import net.bytebuddy.asm.MemberSubstitution;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.persistence.EntityManager;
import javax.swing.plaf.metal.MetalMenuBarUI;
import javax.transaction.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class MemberServiceTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    MemberService memberService;

    @Autowired
    PasswordEncoder passwordEncoder;

    String PASSWORD = "password";

    private void clear(){
        em.flush();
        em.clear();
    }

    private MemberSignUpDto makeMemberSingUpDto(){
        return new MemberSignUpDto("username",PASSWORD,"name","nickName",22);
    }

    private MemberSignUpDto setMember() throws Exception {
        MemberSignUpDto memberSignUpDto = makeMemberSingUpDto();
        memberService.signUp(memberSignUpDto);
        clear();
        SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();

        emptyContext.setAuthentication(new UsernamePasswordAuthenticationToken(User.builder()
                .username(memberSignUpDto.username())
                .password(memberSignUpDto.password())
                .roles(Role.USER.name())
                .build(),
                null,null));

        SecurityContextHolder.setContext(emptyContext);
        return memberSignUpDto;
    }

    @AfterEach
    public void removeMember(){
        SecurityContextHolder.createEmptyContext().setAuthentication(null);

    }

    @Test
    public void 회원가입_성공() throws Exception{
        MemberSignUpDto memberSignUpDto = makeMemberSingUpDto();

        memberService.signUp(memberSignUpDto);
        clear();

        Member member = memberRepository.findByUsername(memberSignUpDto.username()).orElseThrow(() -> new Exception("회원이 없습니다"));

        assertThat(member.getId()).isNotNull();
        assertThat(member.getUsername()).isEqualTo(memberSignUpDto.username());
        assertThat(member.getName()).isEqualTo(memberSignUpDto.name());
        assertThat(member.getNickname()).isEqualTo(memberSignUpDto.nickName());
        assertThat(member.getAge()).isEqualTo(memberSignUpDto.age());
        assertThat(member.getRole()).isSameAs(Role.USER);



    }

    @Test
    public void 회원가입_실패_원인_아이디중복() throws Exception {
        MemberSignUpDto memberSignUpDto = makeMemberSingUpDto();
        memberService.signUp(memberSignUpDto);
        clear();

        assertThat(assertThrows(Exception.class, () -> memberService.signUp(memberSignUpDto))
                .getMessage()).isEqualTo("이미 존재하는 아이디 입니다.");


    }


    @Test
    public void 회원가입_실패_입력하지않은_필드가있으면_오류() throws Exception {
       // MemberSignUpDto("username",PASSWORD,"name","nickName",22);
        MemberSignUpDto memberSignUpDto1 = new MemberSignUpDto(null,passwordEncoder.encode(PASSWORD),"name","nickName",22);
        MemberSignUpDto memberSignUpDto2 = new MemberSignUpDto("usernmae",null,"name","nickName",22);
        MemberSignUpDto memberSignUpDto3 = new MemberSignUpDto("username",passwordEncoder.encode(PASSWORD),null,"NICKANS",22);
        MemberSignUpDto memberSignUpDto4 = new MemberSignUpDto("username",passwordEncoder.encode(PASSWORD),"AKSKDK",null,22);
        MemberSignUpDto memberSignUpDto5 = new MemberSignUpDto("username", passwordEncoder.encode(PASSWORD),"ALALA","KDKDK",null);

        assertThrows(Exception.class,()->memberService.signUp(memberSignUpDto1));
        assertThrows(Exception.class, () -> memberService.signUp(memberSignUpDto2));
        assertThrows(Exception.class, ()-> memberService.signUp(memberSignUpDto3));
        assertThrows(Exception.class, () -> memberService.signUp(memberSignUpDto4));
        assertThrows(Exception.class, () -> memberService.signUp(memberSignUpDto5));

    }

    @Test
    public void 회원수정_비밀번호수정_성공() throws Exception {
        MemberSignUpDto memberSignUpDto = setMember();

        String toBePassword = "12345667weoweiow";
        memberService.updatePassword(PASSWORD, toBePassword);
        clear();

        Member findMember = memberRepository.findByUsername(memberSignUpDto.username()).orElseThrow(() -> new Exception());
        assertThat(findMember.matchPassword(passwordEncoder, toBePassword)).isTrue();

    }

    @Test
    public void 회원수정_별명만수정() throws Exception {
        MemberSignUpDto memberSignUpDto = setMember();

        String updateNickName = "변경";

        memberService.update(new MemberUpdateDto(Optional.empty(), Optional.of(updateNickName), Optional.empty()));
        memberRepository.findByUsername(memberSignUpDto.username()).ifPresent((member -> {
            assertThat(member.getNickname()).isEqualTo(updateNickName);
            assertThat(member.getAge()).isEqualTo(memberSignUpDto.age());
            assertThat(member.getName()).isEqualTo(memberSignUpDto.name());

        }));

    }

    @Test
    public void 회원수정_나이만수정() throws Exception {
        MemberSignUpDto memberSignUpDto = setMember();

        Integer age = 30;

        memberService.update(new MemberUpdateDto(Optional.empty(), Optional.empty(), Optional.of(age)));

        memberRepository.findByUsername(memberSignUpDto.username()).ifPresent(
                member -> {
                    assertThat(member.getAge()).isEqualTo(age);
                    assertThat(member.getName()).isEqualTo(memberSignUpDto.name());
                    assertThat(member.getNickname()).isEqualTo(memberSignUpDto.nickName());
                }
        );

    }

    @Test
    public void 회원수정_이름별명수정() throws Exception {
        MemberSignUpDto memberSignUpDto = setMember();

        String updateNickName = "마우스소리미치겠네";
        String updateName = "마우스휠좀그렇게굴리지마미칠거같아";

        memberService.update(new MemberUpdateDto(Optional.of(updateName), Optional.of(updateNickName), Optional.empty()));
        memberRepository.findByUsername(memberSignUpDto.username()).ifPresent(member -> {

            assertThat(member.getNickname()).isEqualTo(updateNickName);
            assertThat(member.getName()).isEqualTo(updateName);
            assertThat(member.getAge()).isEqualTo(memberSignUpDto.age());
        });
    }

    @Test
    public void 회원탈퇴() throws Exception {
        MemberSignUpDto memberSignUpDto =  setMember();

        memberService.withdraw(PASSWORD);

        assertThat(assertThrows(Exception.class, () -> memberRepository
                .findByUsername(memberSignUpDto.username()).orElseThrow(()
                        -> new Exception("회원이 없습니다."))).getMessage()).isEqualTo("회원이 없습니다.");

    }

    @Test
    public void 회원탈퇴시_실패_비밀번호가_일치하지않음() throws Exception {

        MemberSignUpDto memberSignUpDto = setMember();

        assertThat(assertThrows(Exception.class, () -> memberService.withdraw(PASSWORD+ "1")).getMessage())
                .isEqualTo("비밀번호가 일치하지 않습니다");
    }

    @Test
    public void 회원정보조회() throws Exception {
        MemberSignUpDto memberSignUpDto = setMember();

        Member member = memberRepository
                .findByUsername(memberSignUpDto.username()).orElseThrow(() -> new Exception());

        MemberInfoDto info = memberService.getInfo(member.getId());

        assertThat(info.getUsername()).isEqualTo(memberSignUpDto.username());
        assertThat(info.getName()).isEqualTo(memberSignUpDto.name());
        assertThat(info.getAge()).isEqualTo(memberSignUpDto.age());
        assertThat(info.getNickName()).isEqualTo(memberSignUpDto.nickName());



    }

    @Test
    public void 내정보조회() throws Exception {
        MemberSignUpDto memberSignUpDto = setMember();

        MemberInfoDto myInfo = memberService.getMyInfo();

        assertThat(myInfo.getUsername()).isEqualTo(memberSignUpDto.username());
        assertThat(myInfo.getName()).isEqualTo(memberSignUpDto.name());
        assertThat(myInfo.getAge()).isEqualTo(memberSignUpDto.age());
        assertThat(myInfo.getNickName()).isEqualTo(memberSignUpDto.nickName());

    }



}
