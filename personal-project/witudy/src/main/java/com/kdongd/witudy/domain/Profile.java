package com.kdongd.witudy.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Member 생성자에서 new Profile()로 빈 프로필을 만들기 때문에, nullable = false로 설정해두면
    // 처음에는 자기소개가 비어 있어서(null) 필수로 막으면 회원가입 시점에 저장이 실패하는 문제 발생.
    @Column(nullable = true, length = 500)
    private String introduction;


    public void updateIntroduction(String introduction) {
        this.introduction = introduction;
    }

}
