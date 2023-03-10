package com.sweep.jaksim31.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweep.jaksim31.Jaksim31Application;
import com.sweep.jaksim31.config.EmbeddedRedisConfig;
import com.sweep.jaksim31.domain.diary.Diary;
import com.sweep.jaksim31.domain.diary.DiaryRepository;
import com.sweep.jaksim31.domain.members.MemberRepository;
import com.sweep.jaksim31.domain.members.Members;
import com.sweep.jaksim31.dto.diary.DiaryAnalysisRequest;
import com.sweep.jaksim31.dto.diary.DiarySaveRequest;
import com.sweep.jaksim31.dto.login.LoginRequest;
import com.sweep.jaksim31.dto.member.MemberRemoveRequest;
import com.sweep.jaksim31.dto.member.MemberSaveRequest;
import com.sweep.jaksim31.enums.DiaryExceptionType;
import com.sweep.jaksim31.enums.MemberExceptionType;
import com.sweep.jaksim31.enums.SuccessResponseType;
import com.sweep.jaksim31.service.impl.MemberServiceImpl;
import com.sweep.jaksim31.utils.JsonUtil;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import javax.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.Arrays;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * packageName :  com.sweep.jaksim31.integration
 * fileName : IntegrationDiaryTest
 * author :  ?????????
 * date : 2023-01-31
 * description : Diary service ???????????????
 * ===========================================================
 * DATE                 AUTHOR                NOTE
 * -----------------------------------------------------------
 * 2023-01-31              ?????????             ?????? ??????
 * 2023-02-01              ?????????             ????????? ?????? recentDiary ????????? ??????
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ImportAutoConfiguration(EmbeddedRedisConfig.class)
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class IntegrationDiaryTest {
    private static final String LOGIN_ID = "kjh@test.com";
    private static final String PASSWORD = "password";
    private static final String USERNAME = "kjh";
    private static final String PROFILE_IMAGE = "profileImage";
    private static final String SENTENCE = "?????? ?????? ???, ??? ?????? ??????????????? ??????????????????. ??? ?????? ?????? ????????? ????????? ????????? ??? ???????????? ?????????. ?????? ?????????, ?????? ?????????, ????????? ????????? ????????? ?????????. ?????? ?????????, ???????????? ???????????????. ?????? ???????????? ???, ??????????????????. ??? ?????? ????????? ???????????? ????????? ?????? ???????????????. ??? ?????? ?????? ?????????. ?????? ?????? ?????? ??? ????????????. ????????? ?????? ???????????? ?????? ?????????. ??? ?????? ????????? ?????? ?????? ?????????. ?????????, ????????? ????????? ?????? ????????????.";
    private static final String NEW_EMOTION = "newEmotion";
    private static String userId = "";
    private static String accessToken = "";
    private static String refreshToken = "";
    private static Cookie atkCookie;
    private static Cookie rtkCookie;
    private static String diaryId;
    private static String recentDiaryId;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MemberServiceImpl memberService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    DiaryRepository diaryRepository;

    @BeforeAll
    public void init() {
        memberRepository.deleteAll();
        diaryRepository.deleteAll();
    }


    public DiarySaveRequest getDiaryRequest(int num, String userId) {
        return DiarySaveRequest.builder()
                .userId(userId)
                .content("content" + num)
                .date(LocalDate.of(2023, 1, num))
                .emotion(Integer.toString(num%9+1))
                .keywords(new String[]{"keyword" + num})
                .thumbnail("thumbnail" + num)
                .build();
    }

    @Nested
    @DisplayName("?????? ????????? 02. ????????????/????????? - DiaryService - ????????????")
    @TestMethodOrder(value = MethodOrderer.OrderAnnotation.class) // ????????? ?????? ??????
    class diaryTest{
        @Test
        @DisplayName("[??????] 1.?????? ??????")
        @Order(1)
        public void signUp() throws Exception {
            MemberSaveRequest memberSaveRequest = new MemberSaveRequest(LOGIN_ID, PASSWORD, USERNAME, PROFILE_IMAGE);
            String jsonRequest = JsonUtil.objectMapper.writeValueAsString(memberSaveRequest);
            // when
            MvcResult mvcResult = mockMvc.perform(post("/api/v0/members/register")
                            .content(jsonRequest)
                            .contentType(MediaType.APPLICATION_JSON)
                            .servletPath("/api/v0/members/register"))
                    //then
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType("text/plain;charset=UTF-8"))
                    .andExpect(content().string(SuccessResponseType.SIGNUP_SUCCESS.getMessage()))
                    .andDo(MockMvcResultHandlers.print(System.out))
                    .andReturn();
        }
        @Test
        @DisplayName("[??????] 2.?????????")
        @Order(2)
        public void logIn() throws Exception {
            LoginRequest loginRequest = new LoginRequest(LOGIN_ID, PASSWORD);
            String jsonRequest = JsonUtil.objectMapper.writeValueAsString(loginRequest);
            // when
            MvcResult mvcResult = mockMvc.perform(post("/api/v0/members/login")
                            .content(jsonRequest)
                            .contentType(MediaType.APPLICATION_JSON)
                            .servletPath("/api/v0/members/login"))
                    //then
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/plain;charset=UTF-8"))
                    .andDo(MockMvcResultHandlers.print(System.out))
                    .andReturn();

            // Cookie ?????? ??????
            MockHttpServletResponse response = mvcResult.getResponse();
            assertEquals(response.getCookie("isLogin").getValue(),"true");
            assertNotNull(response.getCookie("atk").getValue());
            assertNotNull(response.getCookie("rtk").getValue());

            // ?????? ???????????? ?????? static value ??????
            userId = mvcResult.getResponse().getCookie("userId").getValue();
            refreshToken = mvcResult.getResponse().getCookie("rtk").getValue();
            accessToken = mvcResult.getResponse().getCookie("atk").getValue();
            atkCookie = new Cookie("atk", accessToken);
            rtkCookie = new Cookie("rtk", refreshToken);

        }
        @Test
        @DisplayName("[??????] 3.????????? ?????? ??????")
        @Order(3)
        public void saveDiaries() throws Exception {
            // when
            Members members = Members.builder().build();
            // Default Diary setting
            for (int i = 1; i <= 20; i++) {
                DiarySaveRequest request = getDiaryRequest(i, userId);
                String jsonRequest = JsonUtil.objectMapper.writeValueAsString(request);

                mockMvc.perform(post("/api/v1/diaries")
                                .cookie(atkCookie,rtkCookie)
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON)
                                .servletPath("/api/v1/diaries"))
                        //then
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType("text/plain;charset=UTF-8"))
                        .andExpect(content().string(SuccessResponseType.DIARY_SAVE_SUCCESS.getMessage()))
                        .andDo(MockMvcResultHandlers.print(System.out));

                // Member DB ??????
                members = memberRepository.findById(userId).get();
                assertEquals(members.getDiaryTotal(), i);
                assertEquals(members.getRecentDiary().getDiaryDate(), LocalDate.of(2023, 1, i));
            }
            assertEquals(members.getRecentDiary().getDiaryDate(), LocalDate.of(2023, 1, 20));
            recentDiaryId = members.getRecentDiary().getDiaryId();
        }

        @Test
        @DisplayName("[??????] 4-1.????????? ?????? ??????_??????")
        @Order(4)
        public void findUserDiary_All() throws Exception {
            Members members = memberRepository.findById(userId).get();

            // when
            mockMvc.perform(get("/api/v1/diaries/"+userId)
                            .cookie(atkCookie,rtkCookie)
                            .contentType(MediaType.APPLICATION_JSON)
                            .servletPath("/api/v1/diaries/"+userId))
                    //then
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.size", Matchers.is(members.getDiaryTotal())))
                    .andDo(MockMvcResultHandlers.print(System.out))
                    .andReturn();

        }

        @Test
        @DisplayName("[??????] 4-2.????????? ?????? ??????_??????")
        @Order(4)
        public void findUserDiary_Some() throws Exception {
            Members members = memberRepository.findById(userId).get();

            // when
            MvcResult mvcResult = mockMvc.perform(get("/api/v1/diaries/"+userId)
                            .cookie(atkCookie,rtkCookie)
                            .param("emotion","2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .servletPath("/api/v1/diaries/"+userId))
                    //then
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content[1].emotion",Matchers.is("2")))
                    .andDo(MockMvcResultHandlers.print(System.out))
                    .andReturn();

            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(mvcResult.getResponse().getContentAsString());
            JSONArray contents = (JSONArray) parser.parse(jsonObject.get("content").toString());
            // ?????? ???????????? ?????? sample diaryId init
            diaryId = ((JSONObject) parser.parse(contents.get(1).toString())).get("diaryId").toString();
        }

        @Test
        @DisplayName("[??????] 5.?????? ?????? ??????")
        @Order(5)
        public void findDiary() throws Exception {
            Members members = memberRepository.findById(userId).get();

            // when
            MvcResult mvcResult = mockMvc.perform(get("/api/v1/diaries/"+userId+"/"+diaryId)
                            .cookie(atkCookie,rtkCookie)
                            .contentType(MediaType.APPLICATION_JSON)
                            .servletPath("/api/v1/diaries/"+userId+"/"+diaryId))
                    //then
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.userId", Matchers.is(userId)))
                    .andDo(MockMvcResultHandlers.print(System.out))
                    .andReturn();
        }

        @Test
        @DisplayName("[??????] 6-1.?????? ??????_??????x")
        @Order(6)
        public void updateDiary() throws Exception {
            DiarySaveRequest request = getDiaryRequest(10, userId);
            request.setContent(SENTENCE);
            String jsonRequest = JsonUtil.objectMapper.writeValueAsString(request);

            // when
            mockMvc.perform(put("/api/v1/diaries/"+diaryId)
                            .cookie(atkCookie,rtkCookie)
                            .content(jsonRequest)
                            .contentType(MediaType.APPLICATION_JSON)
                            .servletPath("/api/v1/diaries/"+diaryId))
                    //then
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/plain;charset=UTF-8"))
                    .andExpect(content().string(SuccessResponseType.DIARY_UPDATE_SUCCESS.getMessage()))
                    .andDo(MockMvcResultHandlers.print(System.out));
            // Diary DB ??????
            Diary diary = diaryRepository.findById(diaryId).get();
            assertEquals(diary.getContent(), SENTENCE);
            assertEquals(diary.getUserId(),userId);
            assertEquals(diary.getModifyDate(), LocalDate.now().atTime(9,0));
            assertEquals(diary.getDate(),LocalDate.of(2023,1,10).atTime(9,0));
            // ????????? DB ??????(RecentDiary ?????? ??????_?????? ?????? ??????)
            Members members = memberRepository.findById(userId).get();
            assertEquals(members.getRecentDiary().getEmotion(), Integer.toString(20%9+1));
        }

        @Test
        @DisplayName("[??????] 6-2.?????? ??????_??????")
        @Order(6)
        public void updateRecentDiary() throws Exception {

            DiarySaveRequest request = getDiaryRequest(20, userId);
            request.setContent(SENTENCE);
            request.setEmotion(NEW_EMOTION);
            String jsonRequest = JsonUtil.objectMapper.writeValueAsString(request);

            // when
            mockMvc.perform(put("/api/v1/diaries/"+recentDiaryId)
                            .cookie(atkCookie,rtkCookie)
                            .content(jsonRequest)
                            .contentType(MediaType.APPLICATION_JSON)
                            .servletPath("/api/v1/diaries/"+recentDiaryId))
                    //then
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/plain;charset=UTF-8"))
                    .andExpect(content().string(SuccessResponseType.DIARY_UPDATE_SUCCESS.getMessage()))
                    .andDo(MockMvcResultHandlers.print(System.out));
            // Diary DB ??????
            Diary diary = diaryRepository.findById(recentDiaryId).get();
            assertEquals(diary.getEmotion(), NEW_EMOTION);
            assertEquals(diary.getUserId(),userId);
            assertEquals(diary.getModifyDate(), LocalDate.now().atTime(9,0));
            assertEquals(diary.getDate(),LocalDate.of(2023,1,20).atTime(9,0));
            // ????????? DB ??????(RecentDiary ?????? ???)
            Members members = memberRepository.findById(userId).get();
            assertEquals(members.getRecentDiary().getEmotion(), NEW_EMOTION);
        }

        @Test
        @DisplayName("[??????] 7.?????? ??????")
        @Order(7)
        public void analyzeDiary() throws Exception {
            DiaryAnalysisRequest request = new DiaryAnalysisRequest();
            request.setSentences(Arrays.asList(SENTENCE));
            String jsonRequest = JsonUtil.objectMapper.writeValueAsString(request);

            // when
            mockMvc.perform(post("/api/v1/diaries/analyze")
                            .cookie(atkCookie,rtkCookie)
                            .content(jsonRequest)
                            .contentType(MediaType.APPLICATION_JSON)
                            .servletPath("/api/v1/diaries/analyze"))
                    //then
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andDo(MockMvcResultHandlers.print(System.out));
        }

        @Test
        @DisplayName("[??????] 8-1.?????? ??????_????????????")
        @Order(8)
        public void emotionStatistics_All() throws Exception {

            // when
            mockMvc.perform(get("/api/v1/diaries/"+userId+"/emotions")
                            .cookie(atkCookie,rtkCookie)
                            .servletPath("/api/v1/diaries/"+userId+"/emotions"))
                    //then
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andDo(MockMvcResultHandlers.print(System.out));
        }

        @Test
        @DisplayName("[??????] 8-2.?????? ??????_????????????")
        @Order(8)
        public void emotionStatistics_withDate() throws Exception {

            // when
            mockMvc.perform(get("/api/v1/diaries/"+userId+"/emotions")
                            .cookie(atkCookie,rtkCookie)
                            .param("startDate","2023-01-05")
                            .param("endDate","2023-01-15")
                            .servletPath("/api/v1/diaries/"+userId+"/emotions"))
                    //then
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andDo(MockMvcResultHandlers.print(System.out));
        }

        @Test
        @DisplayName("[??????] 9.???????????? ??????")
        @Order(9)
        public void deleteDiary() throws Exception {

            // when
            mockMvc.perform(delete("/api/v1/diaries/"+userId+"/"+diaryId)
                            .cookie(atkCookie,rtkCookie)
                            .servletPath("/api/v1/diaries/"+userId+"/"+diaryId))
                    //then
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/plain;charset=UTF-8"))
                    .andDo(MockMvcResultHandlers.print(System.out));

            // Diary DB ??????
            assertEquals(diaryRepository.findById(diaryId).isPresent(),false);
            // Member DB ??????
            assertEquals(memberRepository.findById(userId).get().getDiaryTotal(),19);
            // RecentDiary ?????? ??????_?????? ?????? ??????
            assertEquals(memberRepository.findById(userId).get().getRecentDiary().getEmotion(), NEW_EMOTION);
        }

        @Test
        @DisplayName("[??????] 10.?????? ???????????? ??????")
        @Order(10)
        public void deleteRecentDiary() throws Exception {

            // when
            mockMvc.perform(delete("/api/v1/diaries/"+userId+"/"+recentDiaryId)
                            .cookie(atkCookie,rtkCookie)
                            .servletPath("/api/v1/diaries/"+userId+"/"+recentDiaryId))
                    //then
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/plain;charset=UTF-8"))
                    .andExpect(content().string(SuccessResponseType.DIARY_REMOVE_SUCCESS.getMessage()))
                    .andDo(MockMvcResultHandlers.print(System.out));

            // Diary DB ??????
            assertEquals(diaryRepository.findById(recentDiaryId).isPresent(),false);
            // Member DB ??????
            assertEquals(memberRepository.findById(userId).get().getDiaryTotal(),18);
            // RecentDiary ?????? ??????.
            assertEquals(memberRepository.findById(userId).get().getRecentDiary().getDiaryDate(), LocalDate.of(2023, 1, 19));
        }

        @Test
        @DisplayName("[??????] 11.?????? ?????? ??????")
        @Order(11)
        public void saveTodayDiary() throws Exception {
            // when
            Members members = Members.builder().build();
            DiarySaveRequest request = DiarySaveRequest.builder()
                    .userId(userId)
                    .content("content")
                    .date(LocalDate.now())
                    .emotion("????????????")
                    .keywords(new String[]{"keyword"})
                    .thumbnail("thumbnail")
                    .build();

            String jsonRequest = JsonUtil.objectMapper.writeValueAsString(request);

            MvcResult mvcResult = mockMvc.perform(post("/api/v1/diaries")
                            .cookie(atkCookie,rtkCookie)
                            .content(jsonRequest)
                            .contentType(MediaType.APPLICATION_JSON)
                            .servletPath("/api/v1/diaries"))
                    //then
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType("text/plain;charset=UTF-8"))
                    .andExpect(content().string(SuccessResponseType.DIARY_SAVE_SUCCESS.getMessage()))
                    .andDo(MockMvcResultHandlers.print(System.out))
                    .andReturn();

            // Member DB ??????
            members = memberRepository.findById(userId).get();
            assertEquals(members.getDiaryTotal(), 19);

            assertEquals(members.getRecentDiary().getDiaryDate(), LocalDate.now());
            recentDiaryId = members.getRecentDiary().getDiaryId();

            // Cookie ?????? ??????
            MockHttpServletResponse response = mvcResult.getResponse();
            assertEquals(response.getCookie("todayDiaryId").getValue(),recentDiaryId);
        }

        @Test
        @DisplayName("[??????] 12.?????? ?????? ??????")
        @Order(12)
        public void deleteTodayDiary() throws Exception {

            // when
            MvcResult mvcResult = mockMvc.perform(delete("/api/v1/diaries/"+userId+"/"+recentDiaryId)
                            .cookie(atkCookie,rtkCookie)
                            .servletPath("/api/v1/diaries/"+userId+"/"+recentDiaryId))
                    //then
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/plain;charset=UTF-8"))
                    .andExpect(content().string(SuccessResponseType.DIARY_REMOVE_SUCCESS.getMessage()))
                    .andDo(MockMvcResultHandlers.print(System.out))
                    .andReturn();

            // Diary DB ??????
            assertEquals(diaryRepository.findById(recentDiaryId).isPresent(),false);
            // Member DB ??????
            assertEquals(memberRepository.findById(userId).get().getDiaryTotal(),18);

            // Cookie ?????? ??????
            MockHttpServletResponse response = mvcResult.getResponse();
            assertEquals(response.getCookie("todayDiaryId").getValue(),"");
        }

        @Test
        @DisplayName("[??????] 13-1.?????? ??????_????????? ????????????")
        @Order(13)
        public void invalidMemberRemove() throws Exception {
            MemberRemoveRequest request = new MemberRemoveRequest(userId, "wrongPassword");
            String jsonRequest = JsonUtil.objectMapper.writeValueAsString(request);
            // when
            mockMvc.perform(delete("/api/v1/members/"+userId)
                            .cookie(atkCookie,rtkCookie)
                            .content(jsonRequest)
                            .contentType(MediaType.APPLICATION_JSON)
                            .servletPath("/api/v1/members/"+userId))
                    //then
                    .andExpect(status().is4xxClientError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.errorCode", Matchers.is(MemberExceptionType.WRONG_PASSWORD.getErrorCode())))
                    .andExpect(jsonPath("$.errorMessage", Matchers.is(MemberExceptionType.WRONG_PASSWORD.getMessage())))
                    .andDo(MockMvcResultHandlers.print(System.out));

            // Member DB ??????_?????? ???????????? ??????
            assertEquals(memberRepository.findById(userId).get().getDelYn(),'N');
        }
        @Test
        @DisplayName("[??????] 13-2.?????? ??????")
        @Order(14)
        public void memberRemove() throws Exception {
            MemberRemoveRequest request = new MemberRemoveRequest(userId, PASSWORD);
            String jsonRequest = JsonUtil.objectMapper.writeValueAsString(request);
            // when
            mockMvc.perform(delete("/api/v1/members/"+userId)
                            .cookie(atkCookie,rtkCookie)
                            .content(jsonRequest)
                            .contentType(MediaType.APPLICATION_JSON)
                            .servletPath("/api/v1/members/"+userId))
                    //then
                    .andExpect(status().is3xxRedirection())
                    .andExpect(content().contentType("text/plain;charset=UTF-8"))
                    .andDo(MockMvcResultHandlers.print(System.out));

            // Member DB ??????
            assertEquals(memberRepository.findById(userId).isPresent(),true);
            assertEquals(memberRepository.findById(userId).get().getDelYn(),'Y');
        }
        @Test
        @DisplayName("[??????] 14.????????? ????????? ?????? ?????????")
        @Order(15)
        public void logInDeletedUser() throws Exception {
            LoginRequest loginRequest = new LoginRequest(LOGIN_ID, PASSWORD);
            String jsonRequest = JsonUtil.objectMapper.writeValueAsString(loginRequest);
            // when
            MvcResult mvcResult = mockMvc.perform(post("/api/v0/members/login")
                            .content(jsonRequest)
                            .contentType(MediaType.APPLICATION_JSON)
                            .servletPath("/api/v0/members/login"))
                    //then
                    .andExpect(status().is4xxClientError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.errorCode", Matchers.is(MemberExceptionType.DELETED_USER.getErrorCode())))
                    .andExpect(jsonPath("$.errorMessage", Matchers.is(MemberExceptionType.DELETED_USER.getMessage())))
                    .andDo(MockMvcResultHandlers.print(System.out))
                    .andReturn();

            // Cookie ?????? ??????
            MockHttpServletResponse response = mvcResult.getResponse();
            assertEquals(response.getCookies().length,0);


        }

    }
}
