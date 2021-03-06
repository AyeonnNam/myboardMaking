package board.example.myboard.domain.member.dto;

import board.example.myboard.domain.member.Member;

public  record MemberSignUpDto(String username, String password, String name, String nickName, Integer age)  {



            public Member toEntity(){
                return Member.builder().username(username).password(password)
                        .name(name).nickname(nickName).age(age).build();
            }
    }

