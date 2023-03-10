package com.sweep.jaksim31.domain.members;

import com.sweep.jaksim31.domain.auth.Authority;
import com.sweep.jaksim31.domain.auth.MemberAuth;
import com.sweep.jaksim31.dto.diary.DiaryInfoResponse;
import com.sweep.jaksim31.dto.member.MemberUpdateRequest;
import com.sweep.jaksim31.exception.BizException;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * packageName :  com.sweep.jaksim31.entity.members
 * fileName : Members
 * author :  방근호
 * date : 2023-01-09
 * description : 사용자(Member) 객체
 * ===========================================================
 * DATE                 AUTHOR                NOTE
 * -----------------------------------------------------------
 * 2023-01-09           방근호             최초 생성
 * 2023-01-11           김주현             field 수정
 * 2023-01-12           김주현             profilePhoto -> profileImage
 * 2023-01-19           방근호             updateTime 수정(9시간 추가)
 * 2023-01-28           김주현             ""면 업데이트 하지 않도록 조건 추가
 * 2023-01-31           김주현             recentDiaries -> recentDiary(DiaryInfoResponse)
 */

@Getter
@Setter
@NoArgsConstructor
@ToString
@Document(collection = "member")
public class Members {

    @Id
    private String id;

    private String loginId; // 사용자 로그인 아이디
    private String password;
    private String username;

    private Boolean isSocial; // 소셜 로그인 사용자 여부
    private String profileImage;
    private DiaryInfoResponse recentDiary;
    private int diaryTotal; // 총 일기 수
    private char delYn;
    @CreatedDate
    private Instant registerDate;
    @LastModifiedDate
    private Instant updateDate;

    private Set<Authority> authorities = new HashSet<>();


    @Builder
    public Members(String username, String loginId, String password, Boolean isSocial, char delYn, DiaryInfoResponse recentDiary,
                   String profileImage, int diaryTotal,  Instant register_date, Instant update_date) {
        this.username = username;
        this.loginId = loginId;
        this.password = password;
        this.delYn = delYn;
        this.registerDate = register_date;
        this.updateDate = update_date;
        this.isSocial = isSocial;
        this.diaryTotal = diaryTotal;
        this.recentDiary = recentDiary;
        this.profileImage = profileImage;
        this.addAuthority(new Authority(username, MemberAuth.of("ROLE_USER")));
        System.out.println();
    }

    public void addAuthority(Authority authority) {
        this.getAuthorities().add(authority);
    }

    public void removeAuthority(Authority authority) {
        this.getAuthorities().remove(authority);
    }

    public String getAuthoritiesToString() {
        return this.authorities.stream()
                .map(Authority::getAuthorityName)
                .collect(Collectors.joining(","));
    }

    public void updateMember(MemberUpdateRequest dto) throws BizException {

        if(dto.getUsername() != null && !dto.getUsername().equals("")) this.username = dto.getUsername();
        if(dto.getProfileImage() != null && !dto.getProfileImage().equals("")) this.profileImage = dto.getProfileImage();

        this.updateDate = Instant.now().plus(9, ChronoUnit.HOURS);
    }


    public void remove(char delYn){
        this.delYn = delYn;
    }
}
