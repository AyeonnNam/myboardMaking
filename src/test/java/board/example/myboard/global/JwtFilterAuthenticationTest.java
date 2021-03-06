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


    //AccessToken, RefreshToken ?????? ???????????? ?????? ??????
    @Test
    public void Access_Token_??????_??????_X() throws Exception {
        mockMvc.perform(get(LOGIN_URL+"123")) //???????????? ?????? ?????? ????????? ??????
                .andExpect(status().isForbidden());
    }

    //AccessToken??? ???????????? ?????? ??????
    @Test
    public void AccessToken???_?????????_??????() throws Exception {
        Map accessAndRefreshToken = getAccessAndRefreshToken();

        String accessToken = (String) accessAndRefreshToken.get(accessHeader);
        mockMvc.perform(get(LOGIN_URL+"123").header(accessHeader, BEARER+ accessToken))
                .andExpectAll(status().isNotFound());

    }

    //AccessToken??? ???????????? ?????? -> ???????????? ?????? ????????? ???
    @Test
    public void ????????????AccessToken???_?????????_??????x_???????????????_403() throws Exception {
        Map accessAndRefreshToken = getAccessAndRefreshToken();
        String accessToken = (String) accessAndRefreshToken.get(accessHeader);

        mockMvc.perform(get(LOGIN_URL+"123").header(accessHeader,accessToken+"1"))
                .andExpectAll(status().isForbidden());
    }

    //RefreshToken??? ???????????? ?????? -> ????????? ????????? ???
    @Test
    public void ?????????RefreshToekn???_?????????_AccessToken_?????????_200() throws Exception {
        Map accessAndRefreshToken = getAccessAndRefreshToken();
        String refreshToken =(String) accessAndRefreshToken.get(refreshHeader);

        MvcResult result = mockMvc.perform(get(LOGIN_URL + "123").header(refreshHeader, BEARER+refreshToken))
                .andExpect(status().isOk()).andReturn();

        String accessToken = result.getResponse().getHeader(accessHeader);

        String subject = JWT.require(Algorithm.HMAC512(secret)).build().verify(accessToken).getSubject();

        assertThat(subject).isEqualTo(ACCESS_TOKEN_SUBJECT);


    }

    //RefreshToken??? ???????????? ?????? -> ???????????? ?????? ??????
    @Test
    public void ????????????RefreshToken???_?????????_403() throws Exception {
        Map accessAndRefreshToken = getAccessAndRefreshToken();
        String refreshToken = (String) accessAndRefreshToken.get(refreshHeader);

        mockMvc.perform(get(LOGIN_URL + "123")
                .header(refreshHeader, refreshToken)).andExpect(status().isForbidden());

        mockMvc.perform(get(LOGIN_URL + "123").header(refreshHeader, BEARER + refreshToken +"!23"))
                .andExpect(status().isForbidden());
    }

    //AccessToken,RefreshToken ?????? ????????? ?????? -AccessToken ?????????
    @Test
    public void ?????????RefreshAToken??????_?????????AccessToken_??????????????????_AccessToken_?????????_200()throws Exception {
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

    //RefreshToken??? ????????????, AccessToken??? ???????????? ?????? ?????? - AccessToken ?????????
    @Test
    public void ?????????RefreshToken??????_????????????AccessToken_??????????????????_AccessToken_?????????_200() throws Exception {
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


    //AccessToken??? ????????????, RefreshToken??? ???????????? ?????? ?????? - ????????? ?????? ???????????? ??????????????? ??????
    @Test
    public void ????????????RefreshToken??????_?????????RefreshToken_??????????????????_????????????200_??????404_RefreshToken???_AccessToken??????_???????????????_??????()
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

    //AccessToken, RefreshToken ?????? ???????????? ?????? ??????
    @Test
    public void ????????????RefreshToken??????_????????????AccessToken_??????????????????_403() throws Exception {
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

 //????????? ????????? ????????? ?????? ?????? X
    @Test
    public void ?????????_?????????_?????????_????????????_x() throws Exception{
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
