package com.sweep.jaksim31.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sweep.jaksim31.domain.auth.AuthorityRepository;
import com.sweep.jaksim31.domain.diary.Diary;
import com.sweep.jaksim31.domain.diary.DiaryRepository;
import com.sweep.jaksim31.domain.members.MemberRepository;
import com.sweep.jaksim31.domain.token.RefreshTokenRepository;
import com.sweep.jaksim31.dto.login.KakaoLoginRequest;
import com.sweep.jaksim31.dto.login.KakaoProfile;
import com.sweep.jaksim31.dto.login.LoginRequest;
import com.sweep.jaksim31.dto.member.*;
import com.sweep.jaksim31.dto.token.TokenRequest;
import com.sweep.jaksim31.dto.token.TokenResponse;
import com.sweep.jaksim31.exception.BizException;
import com.sweep.jaksim31.exception.type.JwtExceptionType;
import com.sweep.jaksim31.exception.type.MemberExceptionType;
import com.sweep.jaksim31.service.impl.KaKaoMemberServiceImpl;
import com.sweep.jaksim31.service.impl.MemberServiceImpl;
import com.sweep.jaksim31.utils.JsonUtil;
import com.sweep.jaksim31.utils.RedirectionUtil;
import io.swagger.v3.core.util.Json;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Nested;
import org.junit.platform.engine.support.discovery.SelectorResolver;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.annotation.BeforeTestMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MembersApiController.class)
@ExtendWith(MockitoExtension.class)
@WithMockUser // 401 에러 방지
class MembersApiControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private MemberServiceImpl memberService;

    @MockBean
    private KaKaoMemberServiceImpl kaKaoMemberService;

    @MockBean
    private MemberRepository memberRepository;

    @MockBean
    private AuthorityRepository authorityRepository;
    @MockBean
    private RefreshTokenRepository refreshTokenRepository;
    @MockBean
    private DiaryRepository diaryRepository;

    @MockBean
    private RedirectionUtil redirectionUtil;





    @Test
    @DisplayName("회원가입 컨트롤러")
    public void singup() throws Exception{
        //given
        given(memberService.signup(any()))
                .willReturn(ResponseEntity.ok(MemberSaveResponse.builder()
                        .userId("userId")
                        .loginId("loginId")
                        .username("geunho")
                        .build()));

        //when
        MemberSaveRequest memberSaveRequest = new MemberSaveRequest("loginId", "password", "geunho", "profileImage");
        String jsonRequest = JsonUtil.objectMapper.writeValueAsString(memberSaveRequest);

        mockMvc.perform(post("/v0/members/register")
                        .with(csrf()) //403 에러 방지
                .content(jsonRequest)
                .contentType(MediaType.APPLICATION_JSON))

                //then
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId", Matchers.is("userId")))
                .andExpect(jsonPath("$.loginId", Matchers.is("loginId")))
                .andExpect(jsonPath("$.username", Matchers.is("geunho")))
                .andDo(MockMvcResultHandlers.print(System.out));

    }


    @Test
    @DisplayName("로그인 컨트롤러")
    void login() throws Exception {
        //given
        given(memberService.login(any(), any()))
                .willReturn(ResponseEntity.ok(TokenResponse.builder()
                                .loginId("loginId")
                                .grantType("USER_ROLE")
                                .accessToken("accessToken")
                                .refreshToken("refreshToken")
                                .expTime("1000")
                                .build()));

        // when
        LoginRequest loginRequest = new LoginRequest("loginId", "password");
        String jsonRequest = JsonUtil.objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/v0/members/login")
                        .with(csrf()) //403 에러 방지
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))

                //then
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.grantType", Matchers.is("USER_ROLE")))
                .andExpect(jsonPath("$.loginId", Matchers.is("loginId")))
                .andExpect(jsonPath("$.accessToken", Matchers.is("accessToken")))
                .andExpect(jsonPath("$.refreshToken", Matchers.is("refreshToken")))
                .andExpect(jsonPath("$.expTime", Matchers.is("1000")))
                .andDo(MockMvcResultHandlers.print(System.out));
    }

    @Test
    @DisplayName("로그인 컨트롤러[예외]")
    void invalidLogin() throws Exception {
        //given
        given(memberService.login(any(), any()))
                .willThrow(new BizException(MemberExceptionType.WRONG_PASSWORD));

        // when
        LoginRequest loginRequest = new LoginRequest("loginId", "password");
        String jsonRequest = JsonUtil.objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/v0/members/login")
                        .with(csrf()) //403 에러 방지
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                //then
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode", Matchers.is("WRONG_PASSWORD")))
                .andExpect(jsonPath("$.errorMessage", Matchers.is("비밀번호를 잘못 입력하였습니다.")))
                .andDo(MockMvcResultHandlers.print(System.out));
    }

    @Test
    @DisplayName("Reissue 컨트롤러")
    void reissue() throws Exception {
        //given
        given(memberService.reissue(any(), any()))
                .willReturn(ResponseEntity.ok(TokenResponse.builder()
                        .loginId("reissueTest")
                        .grantType("USER_ROLE")
                        .accessToken("accessToken")
                        .refreshToken("refreshToken")
                        .expTime("2000")
                        .build()));
        //when
        TokenRequest tokenRequest = new TokenRequest("accessToken", "refreshToken");
        String jsonRequest = JsonUtil.objectMapper.writeValueAsString(tokenRequest);

        mockMvc.perform(post("/v0/members/geunho/reissue")
                        .with(csrf()) //403 에러 방지
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                //then
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.grantType", Matchers.is("USER_ROLE")))
                .andExpect(jsonPath("$.loginId", Matchers.is("reissueTest")))
                .andExpect(jsonPath("$.accessToken", Matchers.is("accessToken")))
                .andExpect(jsonPath("$.refreshToken", Matchers.is("refreshToken")))
                .andExpect(jsonPath("$.expTime", Matchers.is("2000")))
                .andDo(MockMvcResultHandlers.print(System.out));

    }

    @Test
    @DisplayName("회원가입여부 컨트롤러")
    void isMember() throws Exception {
        //given
        given(memberService.isMember(any()))
                .willReturn(ResponseEntity.ok("test ok"));
        //when
        MemberCheckLoginIdRequest memberCheckLoginIdRequest = new MemberCheckLoginIdRequest("string");
        String jsonRequest = JsonUtil.objectMapper.writeValueAsString(memberCheckLoginIdRequest);

        mockMvc.perform(post("/v0/members")
                        .with(csrf()) //403 에러 방지
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                //then
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string("test ok"))
                .andDo(MockMvcResultHandlers.print(System.out));
    }

    @Test
    @DisplayName("회원가입여부 컨트롤러[예외]")
    void invalidIsMember() throws Exception {
        //given
        given(memberService.isMember(any()))
                .willThrow(new BizException(MemberExceptionType.NOT_FOUND_USER));
        //when
        MemberCheckLoginIdRequest memberCheckLoginIdRequest = new MemberCheckLoginIdRequest("string");
        String jsonRequest = JsonUtil.objectMapper.writeValueAsString(memberCheckLoginIdRequest);

        mockMvc.perform(post("/v0/members")
                        .with(csrf()) //403 에러 방지
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                //then
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode", Matchers.is(MemberExceptionType.NOT_FOUND_USER.getErrorCode())))
                .andExpect(jsonPath("$.errorMessage", Matchers.is(MemberExceptionType.NOT_FOUND_USER.getMessage())))
                .andDo(MockMvcResultHandlers.print(System.out));
    }

    @DisplayName("비밀번호 재설정 컨트롤러")
    @Test
    void changePw() throws Exception {

        given(memberService.updatePassword(any(), any()))
                .willReturn(new ResponseEntity<>("회원 정보가 정상적으로 변경되었습니다.", HttpStatus.OK));

        //when
        MemberUpdatePasswordRequest memberUpdatePasswordRequest = new MemberUpdatePasswordRequest("string");
        String jsonRequest = JsonUtil.objectMapper.writeValueAsString(memberUpdatePasswordRequest);

        mockMvc.perform(put("/v0/members/string/password")
                        .with(csrf()) //403 에러 방지
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                //then
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string("회원 정보가 정상적으로 변경되었습니다."))
                .andDo(MockMvcResultHandlers.print(System.out));

    }

    @DisplayName("비밀번호 재설정 컨트롤러[예외]")
    @Test
    void invalidChangePw() throws Exception {

        given(memberService.updatePassword(any(), any()))
                .willThrow(new BizException(MemberExceptionType.NOT_FOUND_USER));

        //when
        MemberUpdatePasswordRequest memberUpdatePasswordRequest = new MemberUpdatePasswordRequest("string");
        String jsonRequest = JsonUtil.objectMapper.writeValueAsString(memberUpdatePasswordRequest);

        mockMvc.perform(put("/v0/members/string/password")
                        .with(csrf()) //403 에러 방지
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                //then
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode", Matchers.is(MemberExceptionType.NOT_FOUND_USER.getErrorCode())))
                .andExpect(jsonPath("$.errorMessage", Matchers.is(MemberExceptionType.NOT_FOUND_USER.getMessage())))
                .andDo(MockMvcResultHandlers.print(System.out));

    }

    @DisplayName("내 정보 컨트롤러[Login]")
    @Test
    void getMyInfoByLoginId() throws Exception {

        //given
        given(memberService.getMyInfoByLoginId(any()))
                .willReturn(ResponseEntity.ok(MemberInfoResponse.builder()
                        .loginId("loginId")
                        .userId("userId")
                        .username("username")
                        .profileImage("profileImage")
                        .diaryTotal(10)
                        .build()));

        //when
        mockMvc.perform(get("/v0/members/")
                        .with(csrf()) //403 에러 방지
                        .param("loginId", "loginId"))
//                        .contentType(MediaType.APPLICATION_JSON))
                //then
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId", Matchers.is("userId")))
                .andExpect(jsonPath("$.loginId", Matchers.is("loginId")))
                .andExpect(jsonPath("$.username", Matchers.is("username")))
                .andExpect(jsonPath("$.profileImage", Matchers.is("profileImage")))
                .andExpect(jsonPath("$.diaryTotal", Matchers.is(10)))
                .andExpect(jsonPath("$.recentDiaries", Matchers.nullValue()))
                .andDo(MockMvcResultHandlers.print(System.out));
    }

    @DisplayName("내 정보 컨트롤러[Login][예외]")
    @Test
    void invalidGetMyInfoByLoginId() throws Exception {

        given(memberService.getMyInfoByLoginId(any()))
                .willThrow(new BizException(MemberExceptionType.NOT_FOUND_USER));

        //when
        mockMvc.perform(get("/v0/members")
                        .with(csrf()) //403 에러 방지
                        .param("loginId","loginId"))

                //then
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode", Matchers.is(MemberExceptionType.NOT_FOUND_USER.getErrorCode())))
                .andExpect(jsonPath("$.errorMessage", Matchers.is(MemberExceptionType.NOT_FOUND_USER.getMessage())))
                .andDo(MockMvcResultHandlers.print(System.out));

    }

    @DisplayName("내 정보 컨트롤러[User]")
    @Test
    void getMyInfoByUserId() throws Exception {
        given(memberService.getMyInfo(any()))
                .willReturn(ResponseEntity.ok(MemberInfoResponse.builder()
                        .loginId("loginId")
                        .userId("userId")
                        .username("username")
                        .profileImage("profileImage")
                        .diaryTotal(10)
                        .build()));

        //when
        mockMvc.perform(get("/v0/members/geunho")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId", Matchers.is("userId")))
                .andExpect(jsonPath("$.loginId", Matchers.is("loginId")))
                .andExpect(jsonPath("$.username", Matchers.is("username")))
                .andExpect(jsonPath("$.profileImage", Matchers.is("profileImage")))
                .andExpect(jsonPath("$.diaryTotal", Matchers.is(10)))
                .andExpect(jsonPath("$.recentDiaries", Matchers.nullValue()))
                .andDo(MockMvcResultHandlers.print(System.out));

    }

    @DisplayName("내 정보 컨트롤러[User][예외]")
    @Test
    void invalidGetMyInfoByUserId() throws Exception {

        given(memberService.getMyInfo(any()))
                .willThrow(new BizException(MemberExceptionType.NOT_FOUND_USER));

        //when
        mockMvc.perform(get("/v0/members/geunho")
                        .with(csrf())) //403 에러 방지

                //then
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode", Matchers.is(MemberExceptionType.NOT_FOUND_USER.getErrorCode())))
                .andExpect(jsonPath("$.errorMessage", Matchers.is(MemberExceptionType.NOT_FOUND_USER.getMessage())))
                .andDo(MockMvcResultHandlers.print(System.out));

    }

    @DisplayName("정보 업데이트 컨트롤러")
    @Test
    void updateMember() throws Exception {
        given(memberService.updateMemberInfo(any(), any()))
                .willReturn(ResponseEntity.ok("회원 정보가 변경 되었습니다."));

        //when
        MemberUpdateRequest memberUpdateRequest = new MemberUpdateRequest("방근호", "프로필이미지");
        String jsonString = JsonUtil.objectMapper.writeValueAsString(memberUpdateRequest);

        mockMvc.perform(patch("/v0/members/geunho")
                .with(csrf())
                .content(jsonString)
                .contentType(MediaType.APPLICATION_JSON))

                //then
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string("회원 정보가 변경 되었습니다."))
                .andDo(MockMvcResultHandlers.print(System.out));
    }

    @DisplayName("정보 업데이트 컨트롤러[예외]")
    @Test
    void invalidUpdateMember() throws Exception {
        given(memberService.updateMemberInfo(any(), any()))
                .willThrow(new BizException(MemberExceptionType.NOT_FOUND_USER));

        //when
        MemberUpdateRequest memberUpdateRequest = new MemberUpdateRequest("방근호", "프로필이미지");
        String jsonString = JsonUtil.objectMapper.writeValueAsString(memberUpdateRequest);

        mockMvc.perform(patch("/v0/members/geunho")
                        .with(csrf())
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))

                //then
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorMessage", Matchers.is(MemberExceptionType.NOT_FOUND_USER.getMessage())))
                .andExpect(jsonPath("$.errorCode", Matchers.is(MemberExceptionType.NOT_FOUND_USER.getErrorCode())))
                .andDo(MockMvcResultHandlers.print(System.out));
    }

    @DisplayName("삭제 컨트롤러")
    @Test
    void remove() throws Exception {

        URI redirectUri = new URI("test");
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(redirectUri);

        given(memberService.remove(any(), any()))
                .willReturn(new ResponseEntity<>("삭제되었습니다.", httpHeaders, HttpStatus.SEE_OTHER));

        MemberRemoveRequest memberRemoveRequest = new MemberRemoveRequest("geunho", "geunho");
        String jsonString = JsonUtil.objectMapper.writeValueAsString(memberRemoveRequest);

        mockMvc.perform(delete("/v0/members/geunho")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonString))

                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "test"))
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string("삭제되었습니다."))
                .andDo(MockMvcResultHandlers.print(System.out));
    }

        @Nested
        @DisplayName("삭제 컨트롤러 - 유저 정보 X")
        @Test
        void invalidRemove2xx() throws Exception{

            given(memberService.remove(any(), any()))
                    .willThrow(new BizException(MemberExceptionType.DELETE_NOT_FOUND_USER, "test"));

            MemberRemoveRequest memberRemoveRequest = new MemberRemoveRequest("geunho", "geunho");
            String jsonString = JsonUtil.objectMapper.writeValueAsString(memberRemoveRequest);

            mockMvc.perform(delete("/v0/members/geunho")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonString))

                    .andExpect(status().is3xxRedirection())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(header().string("Location", "test"))
                    .andExpect(jsonPath("$.errorMessage", Matchers.is(MemberExceptionType.DELETE_NOT_FOUND_USER.getMessage())))
                    .andExpect(jsonPath("$.errorCode", Matchers.is(MemberExceptionType.DELETE_NOT_FOUND_USER.getErrorCode())))
                    .andDo(MockMvcResultHandlers.print(System.out));
        }

    @Nested
    @DisplayName("삭제 컨트롤러 - 비밀번호 불일치")
    @Test
    void invalidRemove4xx() throws Exception{

        given(memberService.remove(any(), any()))
                .willThrow(new BizException(MemberExceptionType.WRONG_PASSWORD));

        MemberRemoveRequest memberRemoveRequest = new MemberRemoveRequest("geunho", "geunho");
        String jsonString = JsonUtil.objectMapper.writeValueAsString(memberRemoveRequest);

        mockMvc.perform(delete("/v0/members/geunho")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString))

                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorMessage", Matchers.is(MemberExceptionType.WRONG_PASSWORD.getMessage())))
                .andExpect(jsonPath("$.errorCode", Matchers.is(MemberExceptionType.WRONG_PASSWORD.getErrorCode())))
                .andDo(MockMvcResultHandlers.print(System.out));
    }

    @DisplayName("비밀번호 검증 컨트롤러")
    @Test
    void isMyPw() throws Exception {

        given(memberService.isMyPassword(any(), any()))
                .willReturn(ResponseEntity.ok("비밀번호가 일치합니다."));

        MemberCheckPasswordRequest memberCheckPasswordRequest = new MemberCheckPasswordRequest("password");
        String jsonString = JsonUtil.objectMapper.writeValueAsString(memberCheckPasswordRequest);

        mockMvc.perform(post("/v0/members/guneho/password")
                .with(csrf())
                .content(jsonString)
                .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string("비밀번호가 일치합니다."))
                .andDo(MockMvcResultHandlers.print(System.out));
    }

    @Nested
    @DisplayName("비밀번호 검증 컨트롤러 - 유저 정보 X")
    @Test
    void invalidUserIsMyPassword() throws Exception{

        given(memberService.isMyPassword(any(), any()))
                .willThrow(new BizException(MemberExceptionType.NOT_FOUND_USER));

        MemberCheckPasswordRequest memberRemoveRequest = new MemberCheckPasswordRequest("password");
        String jsonString = JsonUtil.objectMapper.writeValueAsString(memberRemoveRequest);

        mockMvc.perform(post("/v0/members/geunho/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString))

                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorMessage", Matchers.is(MemberExceptionType.NOT_FOUND_USER.getMessage())))
                .andExpect(jsonPath("$.errorCode", Matchers.is(MemberExceptionType.NOT_FOUND_USER.getErrorCode())))
                .andDo(MockMvcResultHandlers.print(System.out));
    }

    @Nested
    @DisplayName("비밀번호 검증 컨트롤러 - 비밀번호 불일치")
    @Test
    void invalidPasswordIsMyPassword() throws Exception{

        given(memberService.isMyPassword(any(), any()))
                .willThrow(new BizException(MemberExceptionType.WRONG_PASSWORD));

        MemberCheckPasswordRequest memberRemoveRequest = new MemberCheckPasswordRequest("password");
        String jsonString = JsonUtil.objectMapper.writeValueAsString(memberRemoveRequest);

        mockMvc.perform(post("/v0/members/geunho/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString))

                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorMessage", Matchers.is(MemberExceptionType.WRONG_PASSWORD.getMessage())))
                .andExpect(jsonPath("$.errorCode", Matchers.is(MemberExceptionType.WRONG_PASSWORD.getErrorCode())))
                .andDo(MockMvcResultHandlers.print(System.out));
    }

    @Test
    @DisplayName("로그아웃 컨트롤러")
    void logout() throws Exception{

        given(memberService.logout(any(), any()))
                .willReturn(ResponseEntity.ok("로그아웃 되었습니다."));

        mockMvc.perform(post("/v0/members/logout")
                .with(csrf()))

                .andExpect(status().isOk())
                .andExpect(content().string("로그아웃 되었습니다."))
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andDo(MockMvcResultHandlers.print(System.out));

    }

    @Nested
    @DisplayName("로그아웃 컨트롤러 - Empty token")
    @Test
    void invalidLogout() throws Exception{

        given(memberService.logout(any(), any()))
                .willThrow(new BizException(JwtExceptionType.LOGOUT_EMPTY_TOKEN, "test"));

        mockMvc.perform(post("/v0/members/logout")
                .with(csrf()))

                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "test"))
                .andExpect(jsonPath("$.errorCode", Matchers.is(JwtExceptionType.LOGOUT_EMPTY_TOKEN.getErrorCode())))
                .andExpect(jsonPath("$.errorMessage", Matchers.is(JwtExceptionType.LOGOUT_EMPTY_TOKEN.getMessage())))
                .andDo(MockMvcResultHandlers.print(System.out));
    }

    @Test
    @DisplayName("카카오 로그인 컨트롤러")
    void kakaoLogin() throws Exception{
        //given
        // Redirect 주소 설정
        URI redirectUri = new URI("test");
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("accessToken", "accessToken");
        httpHeaders.setLocation(redirectUri);

        given(kaKaoMemberService.getAccessToken(any()))
                .willReturn("code");

        given(kaKaoMemberService.getKakaoUserInfo(any()))
                .willReturn(KakaoProfile.builder()
                        .id("geunho")
                        .properties(new KakaoProfile.Properties("geunho", "profileImage", "thumbnailImage"))
                        .connectedAt("geunho")
                        .kakaoAccount(null)
                        .build());

        given(kaKaoMemberService.login(any(), any()))
                .willReturn(new ResponseEntity<>(TokenResponse.builder()
                        .accessToken("accessToken")
                        .refreshToken("refreshToken")
                        .grantType("USER_ROLE")
                        .expTime("100000")
                        .build(), httpHeaders, HttpStatus.SEE_OTHER));

        //when
        mockMvc.perform(get("/v0/members/kakao-login")
                .with(csrf())
                .param("code", "code"))

                //then
                .andExpect(status().is3xxRedirection())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken", Matchers.is("accessToken")))
                .andExpect(jsonPath("$.refreshToken", Matchers.is("refreshToken")))
                .andExpect(jsonPath("$.grantType", Matchers.is("USER_ROLE")))
                .andExpect(jsonPath("$.expTime", Matchers.is("100000")))
                .andExpect(header().string("accessToken", "accessToken"))
                .andExpect(header().string("location", "test"))
                .andDo(MockMvcResultHandlers.print(System.out));
    }

    @Test
    @DisplayName("카카오 로그아웃 컨트롤러")
    void kakaoLogout() throws Exception {

        URI redirectUri = new URI("test");
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(redirectUri);

        //given
        given(kaKaoMemberService.logout(any(), any()))
                .willReturn(new ResponseEntity<>("로그아웃 되었습니다.", httpHeaders,  HttpStatus.SEE_OTHER));

        //when
        mockMvc.perform(get("/v0/members/kakao-logout")
                .with(csrf()))
        //then
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "test"))
                .andExpect(content().string("로그아웃 되었습니다."))
                .andDo(MockMvcResultHandlers.print(System.out));
    }

    @Test
    @DisplayName("카카오 로그아웃 컨트롤러 - 이미 로그아웃된 사용자")
    void alreadyKakaoLogout() throws Exception {

        //given
        given(kaKaoMemberService.logout(any(), any()))
                .willThrow(new BizException(JwtExceptionType.LOGOUT_EMPTY_TOKEN, "test"));

        //when
        mockMvc.perform(get("/v0/members/kakao-logout")
                        .with(csrf()))
                //then
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "test"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode",Matchers.is(JwtExceptionType.LOGOUT_EMPTY_TOKEN.getErrorCode())))
                .andExpect(jsonPath("$.errorMessage", Matchers.is(JwtExceptionType.LOGOUT_EMPTY_TOKEN.getMessage())))
                .andDo(MockMvcResultHandlers.print(System.out));
    }











}