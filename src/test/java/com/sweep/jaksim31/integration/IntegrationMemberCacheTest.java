package com.sweep.jaksim31.integration;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweep.jaksim31.adapter.cache.MemberCacheAdapter;
import com.sweep.jaksim31.adapter.cache.RefreshTokenCacheAdapter;
import com.sweep.jaksim31.auth.TokenProvider;
import com.sweep.jaksim31.config.EmbeddedRedisConfig;
import com.sweep.jaksim31.domain.auth.Authority;
import com.sweep.jaksim31.domain.auth.MemberAuth;
import com.sweep.jaksim31.domain.members.MemberRepository;
import com.sweep.jaksim31.domain.members.Members;
import com.sweep.jaksim31.dto.login.LoginRequest;
import com.sweep.jaksim31.dto.member.MemberInfoResponse;
import com.sweep.jaksim31.dto.member.MemberRemoveRequest;
import com.sweep.jaksim31.dto.member.MemberSaveRequest;
import com.sweep.jaksim31.dto.member.MemberUpdateRequest;
import com.sweep.jaksim31.exception.BizException;
import com.sweep.jaksim31.service.impl.MemberServiceImpl;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.Cookie;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ImportAutoConfiguration(EmbeddedRedisConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
public class IntegrationMemberCacheTest {
    private static final String MEMBER_CACHE = "memberCache::";
    private static final String LOGIN_ID = "loginId";
    private static final String PASSWORD = "password";
    private static final String INVALID_PASSWORD = "asdasdsadadad";
    private static final String USERNAME = "username";
    private static final String PROFILE_IMAGE = "profileImage";
    private static final String INVALID_USER_ID = "adasdadasfa44";

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MemberServiceImpl memberService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MemberCacheAdapter memberCacheAdapter;
    @Autowired
    private RefreshTokenCacheAdapter refreshTokenCacheAdapter;

    private static String userId;

    private static String accessToken;
    private static String refreshToken;
    private static Members members;

    private static MemberUpdateRequest memberUpdateRequest;
    private static MemberRemoveRequest memberRemoveRequest;
    private static MemberInfoResponse memberInfoResponse;

    private static final MockHttpServletRequest request = new MockHttpServletRequest();
    private static final MockHttpServletResponse response = new MockHttpServletResponse();


    @BeforeAll
    public void setUp() {
        memberRepository.deleteAll();
        // ?????????????????? member entity ?????? ??? db??? ??????
        members = new MemberSaveRequest(LOGIN_ID, PASSWORD, USERNAME, PROFILE_IMAGE)
                .toMember(passwordEncoder, false);

        memberRepository.save(members);

        // db??? ????????? ???????????? ???????????? ???????????? ??????
        userId = Objects.requireNonNull(memberRepository.findByLoginId(LOGIN_ID).orElse(null)).getId();
        memberUpdateRequest = new MemberUpdateRequest("newUsername", "newProfileImage");
        memberInfoResponse = MemberInfoResponse.of(Objects.requireNonNull(memberRepository.findByLoginId(LOGIN_ID).orElse(null)));
        memberRemoveRequest = MemberRemoveRequest.builder().userId(userId).password(PASSWORD).build();

        // ???????????? token ??????
        Set<Authority> authoritySet = new HashSet<>();
        authoritySet.add(new Authority(LOGIN_ID, MemberAuth.of("ROLE_USER")));
        accessToken = tokenProvider.createAccessToken(LOGIN_ID, authoritySet);
        refreshToken = tokenProvider.createRefreshToken(LOGIN_ID, authoritySet);

        // ???????????? MockHttpServletRequest ??????
        request.setCookies(new Cookie("atk", accessToken), new Cookie("rtk", refreshToken));
    }

    @Nested
    @DisplayName("?????? ????????? 01. ?????? ?????? ?????? ?????????")
    @TestMethodOrder(value = MethodOrderer.OrderAnnotation.class) // ????????? ?????? ??????
    class memberCachingTest {

        @Test
        @DisplayName("[??????] ?????? ?????? ?????? ??? ?????? ")
        @Order(1)
        public void getMyInfoSearch() {

            // when
            MemberInfoResponse result = memberService.getMyInfo(userId, request);
            MemberInfoResponse cacheResult = memberCacheAdapter.get(MEMBER_CACHE + userId);

            //then
            assertNotNull(cacheResult);
            assertEquals(result.getUserId(), cacheResult.getUserId());
            assertEquals(result.getLoginId(), cacheResult.getLoginId());
        }

        @Test
        @DisplayName("[??????] ?????? ?????? ?????? ??? ?????? ???????????? ?????? X")
        @Order(2)
        public void failGetMyInfoSearch() {
            // when
            try {
                MemberInfoResponse result = memberService.getMyInfo(INVALID_USER_ID, request);
            } catch (BizException ex) {
                MemberInfoResponse cacheResult = memberCacheAdapter.get(MEMBER_CACHE + INVALID_USER_ID);
                //then
                assertNull(cacheResult);
            }
        }

        @Test
        @DisplayName("[??????] ?????? ?????? ?????? ??? ?????? ????????? ??????")
        @Order(3)
        public void updateUserInfo() {
            // when
            memberService.updateMemberInfo(userId, memberUpdateRequest, request);
            MemberInfoResponse cacheResult = memberCacheAdapter.get(MEMBER_CACHE + userId);

            //then
            assertNull(cacheResult);
        }

        @Test
        @DisplayName("[??????] ?????? ?????? ?????? ??? ?????? ???????????? ?????? ????????? ?????? X")
        @Order(4)
        public void failUpdateUserInfo() {
            // given
            memberCacheAdapter.put(MEMBER_CACHE + userId, memberInfoResponse);
            // when
            try {
                memberService.updateMemberInfo(INVALID_USER_ID, memberUpdateRequest, request);
            } catch (BizException ex) {
                MemberInfoResponse cacheResult = memberCacheAdapter.get(MEMBER_CACHE + userId);
                //then
                assertEquals(cacheResult.getUserId(), userId);
            }
        }

        @Test
        @DisplayName("[??????] ???????????? ??? ?????? ????????? ??????")
        @Order(5)
        public void deleteUserInfo() throws URISyntaxException {
            // when
            memberService.remove(userId, memberRemoveRequest, response, request);
            MemberInfoResponse cacheResult = memberCacheAdapter.get(MEMBER_CACHE + userId);

            //then
            assertNull(cacheResult);
        }

        @Test
        @DisplayName("[??????] ???????????? ??? ?????? ???????????? ?????? ????????? ?????? X")
        @Order(6)
        public void failDeleteUserInfo() {
            // given
            memberCacheAdapter.put(MEMBER_CACHE + userId, memberInfoResponse);
            // when
            try {
                memberService.remove(INVALID_USER_ID, memberRemoveRequest, response, request);
            } catch (BizException ex) {
                MemberInfoResponse cacheResult = memberCacheAdapter.get(MEMBER_CACHE + userId);
                //then
                assertEquals(cacheResult.getUserId(), userId);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    @DisplayName("?????? ????????? 02. Refresh Token ?????? ?????????")
    @TestMethodOrder(value = MethodOrderer.OrderAnnotation.class) // ????????? ?????? ??????
    class refreshTokenCachingTest{

        @Test
        @DisplayName("[??????] ????????? ??? ?????? ")
        @Order(1)
        public void successLogin() {
            // given
            memberRepository.save(members);
            LoginRequest loginRequest = LoginRequest.builder().loginId(LOGIN_ID).password(PASSWORD).build();

            // when
            memberService.login(loginRequest, response);
            String cacheResult = refreshTokenCacheAdapter.get(LOGIN_ID);

            //then
            assertNotNull(cacheResult);
        }

        @Test
        @DisplayName("[??????] ????????? ??? ?????? ???????????? ?????? X")
        @Order(2)
        public void failLogin() {
            //given
            refreshTokenCacheAdapter.delete(LOGIN_ID);
            LoginRequest invalidLoginRequest = LoginRequest.builder().loginId(LOGIN_ID).password(INVALID_PASSWORD).build();

            // when
            try {
                memberService.login(invalidLoginRequest, response);
            } catch (BizException ex) {
                String cacheResult = refreshTokenCacheAdapter.get(LOGIN_ID);
                //then
                assertNull(cacheResult);
            }
        }

        /**
         * TODO
         * ????????? ????????? ???????????? ????????? ?????? ?????????????????? ????????? ????????????;;
         */
//        @Test
//        @DisplayName("[??????] ?????? ????????? ??? ?????? ?????? ??????")
//        @Order(3)
//        public void successReissue() throws InterruptedException {
//
//            //given
//            LoginRequest loginRequest = LoginRequest.builder().loginId(LOGIN_ID).password(PASSWORD).build();
//            memberService.login(loginRequest, response);
//            Thread.sleep(500);
//
//            String oldToken = refreshTokenCacheAdapter.get(LOGIN_ID);
//            request.setCookies(new Cookie("atk", accessToken), new Cookie("rtk", oldToken));
//
//            // when
//            memberService.reissue(request, response);
//
//            // then
//            assertNotEquals(oldToken, refreshTokenCacheAdapter.get(LOGIN_ID));
//        }

        @Test
        @DisplayName("[??????] ???????????? ??? ?????? ????????? ??????")
        @Order(4)
        public void successLogout() {
            // when
            memberService.logout(request, response);
            String token = refreshTokenCacheAdapter.get(LOGIN_ID);

            // then
            assertNull(token);
        }

//        @Test
//        @DisplayName("[??????] 2.?????? ??????")
//        @Order(2)
//        public void signUp() throws Exception {
//            MemberSaveRequest memberSaveRequest = new MemberSaveRequest(loginId, password, username, profile);
//            String jsonRequest = JsonUtil.objectMapper.writeValueAsString(memberSaveRequest);
//            // when
//            MvcResult mvcResult = mockMvc.perform(post("/api/v0/members/register")
//                            .content(jsonRequest)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .servletPath("/api/v0/members/register"))
//                    //then
//                    .andExpect(status().isCreated())
//                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(jsonPath("$.userId", Matchers.is(notNullValue())))
//                    .andDo(MockMvcResultHandlers.print(System.out))
//                    .andReturn();
//            System.out.println("####### " + mvcResult.getResponse().getContentAsString());
//            JSONParser parser = new JSONParser();
//            JSONObject jsonObject = (JSONObject) parser.parse(mvcResult.getResponse().getContentAsString());
//
//            Members members = memberRepository.findById(jsonObject.getAsString("userId")).get();
//            assertEquals(members.getLoginId(), loginId);
//            assertEquals(members.getUsername(), username);
//            assertEquals(members.getProfileImage(), profile);
//        }

    }
}
