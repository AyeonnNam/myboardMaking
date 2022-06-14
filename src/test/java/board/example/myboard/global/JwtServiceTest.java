package board.example.myboard.global;


import board.example.myboard.domain.member.Member;
import board.example.myboard.domain.member.Role;
import board.example.myboard.domain.repository.MemberRepository;
import board.example.myboard.global.jwt.service.JwtService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jdk.jfr.Threshold;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class JwtServiceTest {

    @Autowired
    JwtService jwtService;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    EntityManager em;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access.header}")
    private String accessHeader;

    @Value("${jwt.refresh.header}")
    private String refreshHeader;

    private static final String ACCESS_TOKEN_SUBJECT ="AccessToken";
    private static final String REFRESH_TOKEN_SUBJECT = "RefreshToken";
    private static final String USERNAME_CLAIM = "username";
    private static final String BEARER = "Bearer";

    private String username = "username";

    @BeforeEach
    public void init(){
        Member member = Member.builder().username(username).password("1234567889")
                .name("Member1")
                .nickname("Nickname1")
                .role(Role.USER)
                .age(22)
                .build();

        memberRepository.save(member);

        clear();
    }

    public void clear(){
        em.flush();
        em.clear();
    }

    private DecodedJWT getVerify(String token){
        return JWT.require(HMAC512(secret)).build().verify(token);
    }



    //AccessToken 발급 테스트
    @Test
    public void createAccessToken_AccessToken_발급() throws Exception{

        String accessToken = jwtService.createAccessToken(username);

        DecodedJWT verify = getVerify(accessToken);

        String subject = verify.getSubject();

        String findUsername = verify.getClaim(USERNAME_CLAIM).asString();

        assertThat(findUsername).isEqualTo(username);

        assertThat(subject).isEqualTo(ACCESS_TOKEN_SUBJECT);
    }

    //RefreshToken 발급 테스트
    @Test
    public void createRefreshToken_RefreshToken_발급() throws Exception {

        String refreshToken = jwtService.createRefreshToken();

        DecodedJWT verify = getVerify(refreshToken);

        String subject = verify.getSubject();

        String findUsername = verify.getClaim(USERNAME_CLAIM).asString();

        assertThat(subject).isEqualTo(REFRESH_TOKEN_SUBJECT);

        //assertThat(findUsername).isEqualTo(username);
        assertThat(findUsername).isNull();
    }

    //Refresh Update
    @Test
    public void updateRefreshToken_refreshToken_업데이트() throws Exception {

        String refreshToken = jwtService.createRefreshToken();
                jwtService.updateRefreshToken(username, refreshToken);

                clear();
                Thread.sleep(3000);

                String reIssuedRefreshToken = jwtService.createRefreshToken();
                jwtService.updateRefreshToken(username, reIssuedRefreshToken);
                clear();

                assertThrows(Exception.class,
                () -> memberRepository.findByRefreshToken(refreshToken).get());

                assertThat(memberRepository.findByRefreshToken
                        (reIssuedRefreshToken).get().getUsername()).isEqualTo(username);
    }

    //Refresh Destroy
    @Test
    public void destroyRefreshToken_refreshToken_재거() throws Exception {
        String refreshToken = jwtService.createRefreshToken();
        jwtService.updateRefreshToken(username, refreshToken);
        clear();

        jwtService.destroyRefreshToken(username);
        clear();

        assertThrows(Exception.class,() -> memberRepository.findByRefreshToken(refreshToken).get());

        Member member = memberRepository.findByUsername(username).get();
        assertThat(member.getRefreshToken()).isNull();
    }

    //토큰유효성 검사 테스트
    @Test
    public void 토큰_유효성_검사() throws Exception{
        String accessToken = jwtService.createAccessToken(username);
        String refreshToken = jwtService.createRefreshToken();

        assertThat(jwtService.isTokenValid(accessToken)).isTrue();
        assertThat(jwtService.isTokenValid(refreshToken)).isTrue();

    }


    //AccessToken, RefreshToken 헤더 설정 테스트
    @Test
    public void setAccessHeader_AccessToken_헤더_설정() throws Exception{
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        String accessToken = jwtService.createAccessToken(username);
        String refreshToken = jwtService.createRefreshToken();

        jwtService.setAccessTokenHeader(mockHttpServletResponse, accessToken);

        jwtService.sendAccessAndRefreshToken(mockHttpServletResponse, accessToken, refreshToken);

        String headerAccessToken = mockHttpServletResponse.getHeader(accessHeader);

        assertThat(headerAccessToken).isEqualTo(accessToken);
    }

    @Test
    public void setAccessHeader_RefreshToken_헤더_설정() throws Exception {
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        String accessToken = jwtService.createAccessToken(username);
        String refreshToken = jwtService.createRefreshToken();

        jwtService.setRefreshTokenHeader(mockHttpServletResponse, refreshToken);

        String headerRefreshToken = mockHttpServletResponse.getHeader(refreshHeader);

        assertThat(headerRefreshToken).isEqualTo(refreshToken);
    }

    //토큰 전송 테스트
    @Test
    public void sendAccessAndRefreshToken_토큰_전송() throws Exception {
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        String accessToken = jwtService.createAccessToken(username);
        String refreshToken = jwtService.createRefreshToken();

        jwtService.sendAccessAndRefreshToken(mockHttpServletResponse, accessToken, refreshToken);

        String headerAccessToken = mockHttpServletResponse.getHeader(accessHeader);
        String headerRefreshToken = mockHttpServletResponse.getHeader(refreshHeader);

        assertThat(headerAccessToken).isEqualTo(accessToken);
        assertThat(headerRefreshToken).isEqualTo(refreshToken);
    }

    
}
