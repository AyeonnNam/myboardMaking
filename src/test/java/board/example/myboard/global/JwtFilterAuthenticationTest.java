package board.example.myboard.global;

import board.example.myboard.domain.member.Member;
import board.example.myboard.domain.member.Role;
import board.example.myboard.domain.repository.MemberRepository;
import board.example.myboard.global.jwt.service.JwtService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.h2.security.auth.impl.StaticUserCredentialsValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.persistence.EntityManager;
import javax.persistence.SecondaryTable;
import javax.swing.tree.ExpandVetoException;
import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class JwtFilterAuthenticationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    EntityManager em;

    @Autowired
    JwtService jwtService;

    PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.access.header}")
    private String accessHeader;
    @Value("${jwt.refresh.header}")
    private String refreshHeader;

    private static String KEY_USERNAME = "username";
    private static String KEY_PASSWORD = "password";
    private static String USERNAME = "username";
    private static String PASSWORD = "12345677";

    private static String LOGIN_URL = "/login";

    private static final String ACCESS_TOKEN_SUBJECT = "AccessToken";
    private static final String BEARER ="Bearer";

    private ObjectMapper objectMapper = new ObjectMapper();

    private void clear(){
        em.flush();
        em.clear();
    }

    @BeforeEach
    private void init(){
        memberRepository.save(Member.builder().username(USERNAME).password(passwordEncoder.encode(PASSWORD))
                .name("MEMBER1").nickname("Nicknamae1").role(Role.USER).age(30).build());

        clear();
    }

    private Map getUsernamePasswordMap(String username, String password){
        Map<String, String> map = new HashMap<>();
        map.put(KEY_USERNAME, username);
    map.put(KEY_PASSWORD,password);
    return map;
    }

    private Map getAccessAndRefreshToken() throws Exception {
        Map<String, String> map = getUsernamePasswordMap(USERNAME, PASSWORD);

        MvcResult result = mockMvc.perform(
                post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map))
        ).andReturn();

        String accessToken = result.getResponse().getHeader(accessHeader);
        String refreshToken = result.getResponse().getHeader(refreshHeader);

        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put(accessHeader, accessToken);
        tokenMap.put(refreshHeader, refreshToken);

        return tokenMap;
    }


    //AccessToken, RefreshToken 모두 존재하지 않은 경우
    @Test
    public void Access_Token_모두_존재_X() throws Exception {
        mockMvc.perform(get(LOGIN_URL+"123")) //로그인이 아닌 다른 임의의 주소
                .andExpect(status().isForbidden());
    }

    //AccessToken만 존재하지 않은 경우
    @Test
    public void AccessToken만_보내서_인증() throws Exception {
        Map accessAndRefreshToken = getAccessAndRefreshToken();

        String accessToken = (String) accessAndRefreshToken.get(accessHeader);
        mockMvc.perform(get(LOGIN_URL+"123").header(accessHeader, BEARER+ accessToken))
                .andExpectAll(status().isNotFound());

    }

    //AccessToken만 존재하는 경우 -> 유효하지 않은 토큰일 때
    @Test
    public void 안유효한AccessToken만_보내서_인증x_상태코드는_403() throws Exception {
        Map accessAndRefreshToken = getAccessAndRefreshToken();
        String accessToken = (String) accessAndRefreshToken.get(accessHeader);

        mockMvc.perform(get(LOGIN_URL+"123").header(accessHeader,accessToken+"1"))
                .andExpectAll(status().isForbidden());
    }

    //RefreshToken만 존재하는 경우 -> 유효한 토큰일 때
    @Test
    public void 유효한RefreshToekn만_보내서_AccessToken_재발급_200() throws Exception {
        Map accessAndRefreshToken = getAccessAndRefreshToken();
        String refreshToken =(String) accessAndRefreshToken.get(refreshHeader);

        MvcResult result = mockMvc.perform(get(LOGIN_URL + "123").header(refreshHeader, BEARER+refreshToken))
                .andExpect(status().isOk()).andReturn();

        String accessToken = result.getResponse().getHeader(accessHeader);

        String subject = JWT.require(Algorithm.HMAC512(secret)).build().verify(accessToken).getSubject();

        assertThat(subject).isEqualTo(ACCESS_TOKEN_SUBJECT);


    }

    //RefreshToken만 존재하는 경우 -> 유효하지 않은 경우
    @Test
    public void 안유효한RefreshToken만_보내면_403() throws Exception {
        Map accessAndRefreshToken = getAccessAndRefreshToken();
        String refreshToken = (String) accessAndRefreshToken.get(refreshHeader);

        mockMvc.perform(get(LOGIN_URL + "123")
                .header(refreshHeader, refreshToken)).andExpect(status().isForbidden());

        mockMvc.perform(get(LOGIN_URL + "123").header(refreshHeader, BEARER + refreshToken +"!23"))
                .andExpect(status().isForbidden());
    }

    //AccessToken,RefreshToken 모두 유효한 경우 -AccessToken 재발급
    @Test
    public void 유효한RefreshAToken이랑_유효한AccessToken_같이보냈을때_AccessToken_재발급_200()throws Exception {
        Map accessAndRefreshToken = getAccessAndRefreshToken();
       String accessToken = (String) accessAndRefreshToken.get(accessHeader);
       String refreshToken = (String) accessAndRefreshToken.get(refreshHeader);

       MvcResult result = mockMvc.perform(get(LOGIN_URL + "123")
               .header(refreshHeader, BEARER + refreshToken)
               .header(accessHeader, BEARER + accessToken))
               .andExpect(status().isOk())
               .andReturn();

        String responseAccessToken = result.getResponse().getHeader(accessHeader);
        String responseRefreshToken = result.getResponse().getHeader(refreshHeader);

        String subject = JWT.require(Algorithm.HMAC512(secret)).build().verify(responseAccessToken).getSubject();

        assertThat(responseRefreshToken).isNull();

    }

    //RefreshToken은 유효하고, AccessToken은 유효하지 않은 경우 - AccessToken 재발급
    @Test
    public void 유효한RefreshToken이랑_안유효한AccessToken_같이보냈을때_AccessToken_재발급_200() throws Exception {
        Map accessAndRefreshToken = getAccessAndRefreshToken();
        String accessToken = (String) accessAndRefreshToken.get(accessHeader);
        String refreshToken = (String) accessAndRefreshToken.get(refreshHeader);

        MvcResult result = mockMvc.perform(get(LOGIN_URL +"!23")
                .header(refreshHeader, BEARER + refreshToken)
                .header(accessHeader, BEARER +accessToken + 1))
                .andExpect(status().isOk())
                .andReturn();


        String responseAccessToken = result.getResponse().getHeader(accessHeader);
        String responseRefreshToken = result.getResponse().getHeader(refreshHeader);

        String subject = JWT.require(Algorithm.HMAC512(secret)).build().verify(responseAccessToken).getSubject();

        assertThat(subject).isEqualTo(ACCESS_TOKEN_SUBJECT);
        assertThat(responseRefreshToken).isNull();

    }


    //AccessToken은 유효하고, RefreshToken은 유효하지 않은 경우 - 인증은 되나 아무것도 재발급되지 않음
    @Test
    public void 안유효한RefreshToken이랑_유효한RefreshToken_같이보냈을때_상태코드200_혹은404_RefreshToken은_AccessToken모두_재발급되지_않음()
            throws Exception {

        Map accessAndRefreshToken = getAccessAndRefreshToken();
        String accessToken = (String) accessAndRefreshToken.get(accessHeader);
        String refreshToken = (String) accessAndRefreshToken.get(refreshHeader);

        MvcResult result = mockMvc.perform(get(LOGIN_URL + "!23")
                .header(refreshHeader, BEARER + refreshToken +1)
                .header(accessHeader, BEARER + accessToken))
                .andExpect(status().isNotFound())
                .andReturn();

        String responseAccessToken = result.getResponse().getHeader(accessHeader);
        String responseRefreshToken = result.getResponse().getHeader(refreshHeader);

        assertThat(responseRefreshToken).isNull();
        assertThat(responseAccessToken).isNull();

    }

    //AccessToken, RefreshToken 모두 유효하지 않은 경우
    @Test
    public void 안유효한RefreshToken이랑_안유효한AccessToken_같이보냈을때_403() throws Exception {
        Map accessAndRefreshToken = getAccessAndRefreshToken();
        String accessToken = (String) accessAndRefreshToken.get(accessHeader);
        String refreshToken = (String) accessAndRefreshToken.get(refreshHeader);

        MvcResult result = mockMvc.perform(get(LOGIN_URL + "123")
                .header(refreshHeader, BEARER + refreshToken +1)
                .header(accessHeader, BEARER + accessToken+1))
                .andExpect(status().isForbidden())
                .andReturn();

        String responseAccessToken = result.getResponse().getHeader(accessHeader);
        String responseRefreshToken = result.getResponse().getHeader(refreshHeader);

        assertThat(responseAccessToken).isNull();
        assertThat(responseRefreshToken).isNull();


    }

 //로그인 주소로 보내면 필터 작동 X
    @Test
    public void 로그인_주소로_보내면_필터작동_x() throws Exception{
        Map accessAndRefreshToken = getAccessAndRefreshToken();

        String accessToken = (String) accessAndRefreshToken.get(accessHeader);
        String refreshToken = (String) accessAndRefreshToken.get(refreshHeader);

        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .header(refreshHeader, BEARER + refreshToken)
                        .header(accessHeader, BEARER + accessToken)
                        )
                .andExpect(status().isBadRequest())
                .andReturn();

    }



}
