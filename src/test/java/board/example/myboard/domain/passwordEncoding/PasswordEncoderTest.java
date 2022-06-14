package board.example.myboard.domain.passwordEncoding;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@Transactional
@SpringBootTest
public class PasswordEncoderTest {

    @Autowired
    PasswordEncoder passwordEncoder;


    @Test
    public void 패스워드_암호화() throws Exception {
        String password = "남아연skadkdus";

        String encode = passwordEncoder.encode(password);

        assertThat(encode).startsWith("{");
        assertThat(encode).contains("{bcrypt}");
        assertThat(encode).isNotEqualTo(password);
    }

    @Test
    public void 패스워드_랜덤_암호화() throws Exception {
        String password = "남아연skadkdus";
        String encode1 = passwordEncoder.encode(password);
        String encode2 = passwordEncoder.encode(password);

        assertThat(encode1).isNotEqualTo(encode2);

    }

    @Test
    public void 암호화된_비밀번호_매치() throws Exception {
        String password = "남아연skadkdus";
        String encode1 = passwordEncoder.encode(password);
    assertThat(passwordEncoder.matches(password,encode1)).isTrue();


    }
}
