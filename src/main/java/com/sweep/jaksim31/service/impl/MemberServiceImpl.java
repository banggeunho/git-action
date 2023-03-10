package com.sweep.jaksim31.service.impl;

import com.sweep.jaksim31.adapter.cache.RefreshTokenCacheAdapter;
import com.sweep.jaksim31.auth.CustomLoginIdPasswordAuthToken;
import com.sweep.jaksim31.auth.CustomUserDetailsService;
import com.sweep.jaksim31.auth.TokenProvider;
import com.sweep.jaksim31.domain.diary.Diary;
import com.sweep.jaksim31.domain.diary.DiaryRepository;
import com.sweep.jaksim31.domain.members.MemberRepository;
import com.sweep.jaksim31.domain.members.Members;
import com.sweep.jaksim31.domain.token.RefreshToken;
import com.sweep.jaksim31.domain.token.RefreshTokenRepository;
import com.sweep.jaksim31.dto.login.LoginRequest;
import com.sweep.jaksim31.dto.member.*;
import com.sweep.jaksim31.enums.SuccessResponseType;
import com.sweep.jaksim31.exception.BizException;
import com.sweep.jaksim31.enums.JwtExceptionType;
import com.sweep.jaksim31.enums.MemberExceptionType;
import com.sweep.jaksim31.service.MemberService;
import com.sweep.jaksim31.utils.CookieUtil;
import com.sweep.jaksim31.utils.RedirectionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;

/**
 * packageName :  com.sweep.jaksim31.service.impl
 * fileName : MemberServiceImpl
 * author :  ?????????
 * date : 2023-01-09
 * description : ????????? ?????? ??? ????????? ?????? Services
 * ===========================================================
 * DATE                 AUTHOR                NOTE
 * -----------------------------------------------------------
 * 2023-01-09           ?????????             ?????? ??????
 * 2023-01-11           ?????????          Members ???????????? ?????? Service ?????? ??????
 * 2023-01-12           ?????????          ???????????? ??? ???????????? ???????????? ??????
 * 2023-01-15           ?????????          MemberSaveRequest ???????????? ?????? toMember ?????? ?????? ??????
 * 2023-01-16           ?????????          ????????? ??? ?????? ?????? id Set-Cookie
 * 2023-01-16           ?????????          ???????????? ????????? ??????
 * 2023-01-17           ?????????          ???????????? ????????? ????????? ?????? ??????
 * 2023-01-17           ?????????          ????????? ?????? ??????
 * 2023-01-18           ?????????          id data type ??????(ObjectId -> String)
 * 2023-01-18           ?????????          GetMyInfoByLoginId ????????? ??????
 * 2023-01-23           ?????????          ResponseEntity Wrapper class ??????
 * 2023-01-25           ?????????          getMyInfoByLoginId ??????
 * 2023-01-25           ?????????          getMyInfoByLoginId ??????
 * 2023-01-27           ?????????          ?????????/???????????? ??? userId ?????? ?????? ??? refresh token??? addSecureCookie??? ??????
 * 2023-01-30           ?????????          ?????? ?????? ???????????? ?????? ?????? ?????? ??????
 * 2023-01-31           ?????????,?????????    ???????????? ??? Cookie ??????
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss");
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    @Value("${jwt.refresh-token-expire-time}")
    private long rtkLive;
    @Value("${jwt.access-token-expire-time}")
    private long accExpTime;
    private final DiaryRepository diaryRepository;
    private final RedirectionUtil redirectionUtil;
    private final RefreshTokenCacheAdapter refreshTokenCacheAdapter;


    @Transactional
    public String signup(MemberSaveRequest memberRequestDto) {

        if(memberRepository.existsByLoginId(memberRequestDto.getLoginId()))
           throw new BizException(MemberExceptionType.DUPLICATE_USER);

        Members members = memberRequestDto.toMember(passwordEncoder, false);
        memberRepository.save(members);
        return SuccessResponseType.SIGNUP_SUCCESS.getMessage();
    }
    @Override
    @Transactional
    public String login(LoginRequest loginRequest, HttpServletResponse response) {

        // ?????? ??????
        CustomLoginIdPasswordAuthToken customLoginIdPasswordAuthToken = new CustomLoginIdPasswordAuthToken(loginRequest.getLoginId(), loginRequest.getPassword());
        Authentication authenticate = authenticationManager.authenticate(customLoginIdPasswordAuthToken);
        String loginId = authenticate.getName();
        Members members = customUserDetailsService.getMember(loginId);
        if(members.getDelYn()=='Y')
            throw new BizException(MemberExceptionType.DELETED_USER);

        // ?????? ??????
        String accessToken = tokenProvider.createAccessToken(loginId, members.getAuthorities());
        String refreshToken = tokenProvider.createRefreshToken(loginId, members.getAuthorities());

        // ????????? ?????? ?????? ??? ?????? ?????? ??????
        CookieUtil.addSecureCookie(response, "atk", accessToken, (int) rtkLive / 60);
        CookieUtil.addSecureCookie(response, "rtk", refreshToken, (int) rtkLive / 60);
        CookieUtil.addPublicCookie(response, "isLogin", "true", (int) rtkLive / 60);
        CookieUtil.addPublicCookie(response, "userId", members.getId(), (int) rtkLive / 60);
        CookieUtil.addPublicCookie(response, "isSocial", members.getIsSocial().toString(), (int) rtkLive / 60);

        // ???????????? ??????
        refreshTokenCacheAdapter.put(loginId, refreshToken, Duration.ofSeconds((int) rtkLive / 60));

        LocalDate today = LocalDate.now();
        Diary todayDiary = diaryRepository.findDiaryByUserIdAndDate(members.getId(), today.atTime(9,0)).orElse(null);
        // ?????? ????????? ?????? 23:59:59??? ??????
        long todayExpTime = LocalDateTime.of(today.plusDays(1), LocalTime.of(23, 59, 59,59)).toLocalTime().toSecondOfDay()
                - LocalDateTime.now().toLocalTime().toSecondOfDay() + (3600*9); // GMT??? ??????????????? 3600*9 ??????..

        CookieUtil.addCookie(response, "todayDiaryId", Objects.nonNull(todayDiary) ? todayDiary.getId() : "", todayExpTime);

        return SuccessResponseType.LOGIN_SUCCESS.getMessage();

    }

    @Transactional
    public HttpServletResponse reissue(HttpServletRequest request,
                                     HttpServletResponse response) {

        // cookie?????? refresh token ??????
        Cookie refreshTokenCookie = Arrays.stream(request.getCookies())
                .filter(req -> req.getName().equals("rtk"))
                .findAny()
                .orElseThrow(() -> new BizException(JwtExceptionType.EMPTY_TOKEN));

        String originRefreshToken = refreshTokenCookie.getValue();
        // refreshToken ??????
        int refreshTokenFlag = tokenProvider.validateToken(originRefreshToken);
        log.debug("refreshTokenFlag = {}", refreshTokenFlag);

        //refreshToken ???????????? ????????? ?????? ????????? ????????????.
        if (refreshTokenFlag == -1) {
            throw new BizException(JwtExceptionType.BAD_TOKEN); // ????????? ???????????? ??????
        } else if (refreshTokenFlag == 2) {
            throw new BizException(JwtExceptionType.REFRESH_TOKEN_EXPIRED); // ???????????? ?????? ??????
        }

        // 2. Access Token ?????? Member LoginId ????????????
        Authentication authentication = tokenProvider.getAuthentication(originRefreshToken);
        log.debug("Authentication = {}", authentication);

        // ????????? ???????????? ????????? ????????? ??????
        RefreshToken cachedRefreshToken = RefreshToken.builder()
                .loginId(authentication.getName())
                .value(refreshTokenCacheAdapter.get(authentication.getName()))
                .build();


        // ????????? ???????????? ????????? ?????? ?????? ???????????? ??????
        if (Objects.isNull(cachedRefreshToken.getValue())) {
            throw new BizException(MemberExceptionType.LOGOUT_MEMBER);
        } else {
            if (!cachedRefreshToken.getValue().equals(originRefreshToken)) {
                throw new BizException(JwtExceptionType.BAD_TOKEN);
            }
        }

        // 5. ????????? ?????? ??????
        String loginId = tokenProvider.getMemberLoginIdByToken(originRefreshToken);
        Members members = customUserDetailsService.getMember(loginId);
        String newAccessToken = tokenProvider.createAccessToken(loginId, members.getAuthorities());
        String newRefreshToken = tokenProvider.createRefreshToken(loginId, members.getAuthorities());

        // ????????? ?????? ????????????
        refreshTokenCacheAdapter.put(loginId, newRefreshToken, Duration.ofSeconds(rtkLive / 60));

        log.debug("refresh Origin = {}", originRefreshToken);
        log.debug("refresh New = {} ", newRefreshToken);

        // ????????? ?????? ??? ?????? ?????? ?????? Cookie ??????
        CookieUtil.addSecureCookie(response, "atk", newAccessToken, (int) rtkLive / 60);
        CookieUtil.addSecureCookie(response, "rtk", newRefreshToken, (int) rtkLive / 60);
        CookieUtil.addPublicCookie(response, "isLogin", "true", (int) rtkLive / 60);

        // ?????? ??????
        return response;
    }
    @Override
    @Transactional
    public String logout(HttpServletRequest request, HttpServletResponse response){

        Cookie refreshTokenCookie = Arrays.stream(request.getCookies())
                .filter(req -> req.getName().equals("atk"))
                .findAny()
                .orElseThrow(() -> new BizException(JwtExceptionType.EMPTY_TOKEN));

        String originAccessToken = refreshTokenCookie.getValue();

        // ???????????? ?????? ?????? ??????
        CookieUtil.resetDefaultCookies(response);

        Authentication authentication = tokenProvider.getAuthentication(originAccessToken);

        // ??????????????? ?????? ??????
        refreshTokenCacheAdapter.delete(authentication.getName());

        return SuccessResponseType.LOGOUT_SUCCESS.getMessage();
    }

    @Transactional
    public String isMember(MemberCheckLoginIdRequest memberRequestDto) {

        if (!memberRepository.existsByLoginId(memberRequestDto.getLoginId()))
            throw new BizException(MemberExceptionType.NOT_FOUND_USER);

        return memberRequestDto.getLoginId() + SuccessResponseType.IS_MEMBER_SUCCESS.getMessage();
    }

    @Transactional
    public String updatePassword(String loginId, MemberUpdatePasswordRequest dto) {
        Members members = memberRepository
                .findByLoginId(loginId)
                .orElseThrow(() -> new BizException(MemberExceptionType.NOT_FOUND_USER));

        members.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        // ???????????? ??? ?????? ??????
        memberRepository.save(members);
        return SuccessResponseType.USER_UPDATE_SUCCESS.getMessage();
    }


    /**
     *
     * @param userId ?????? ?????????
     * @return MemberInfoResponse
     */
    @Cacheable(
            value = "memberCache",
            key = "#userId"
    )
    @Transactional(readOnly = true)
    public MemberInfoResponse getMyInfo(String userId, HttpServletRequest request) {
        MemberInfoResponse members = memberRepository.findById(userId)
                .map(MemberInfoResponse::of)
                .orElseThrow(() -> new BizException(MemberExceptionType.NOT_FOUND_USER));
        // ????????? id??? ??????????????? ?????? id??? ???????????? ?????? ??????
        if(!tokenProvider.getMemberLoginIdByToken(CookieUtil.getAccessToken(request)).equals(members.getLoginId()))
            throw new BizException(MemberExceptionType.NO_PERMISSION);

        return members;
    }

    /**
     * DirtyChecking ??? ?????? ?????? ???????????? ( Login ID??? ???????????? ??? ??? ??????.)
     * @param userId
     * @param memberUpdateRequest member ?????? ?????? dto
     */

    @CacheEvict(
            value = "memberCache",
            key = "#userId"
    )
    @Transactional
    public String updateMemberInfo(String userId, MemberUpdateRequest memberUpdateRequest, HttpServletRequest request) {
        Members members = memberRepository
                .findById(userId)
                .orElseThrow(() -> new BizException(MemberExceptionType.NOT_FOUND_USER));
        // ????????? id??? ????????? ??????????????? ?????? id??? ???????????? ?????? ??????
        if(!tokenProvider.getMemberLoginIdByToken(CookieUtil.getAccessToken(request)).equals(members.getLoginId()))
            throw new BizException(MemberExceptionType.NO_PERMISSION);

        members.updateMember( memberUpdateRequest);
        memberRepository.save(members);
        return SuccessResponseType.USER_UPDATE_SUCCESS.getMessage();
    }

    @Transactional
    public String isMyPassword(String loginId, MemberCheckPasswordRequest dto){
        Members members = memberRepository
                .findByLoginId(loginId)
                .orElseThrow(() -> new BizException(MemberExceptionType.NOT_FOUND_USER));

        // ???????????? ??????
        if (!passwordEncoder.matches(dto.getPassword(), members.getPassword()))
            throw new BizException(MemberExceptionType.WRONG_PASSWORD);

        return SuccessResponseType.CHECK_PW_SUCCESS.getMessage();
    }

    @CacheEvict(
            value = "memberCache",
            key = "#userId"
    )
    @Transactional
    public String remove(String userId, MemberRemoveRequest dto, HttpServletResponse response, HttpServletRequest request) throws URISyntaxException {
        // ????????? ?????? ?????? 200 ?????? (???????????? ??????)
        Members members = memberRepository
                .findById(userId)
                .orElseThrow(() -> new BizException(MemberExceptionType.DELETE_NOT_FOUND_USER, redirectionUtil.getHomeUrl()));

        // ????????? id??? ????????? ??????????????? ?????? id??? ???????????? ?????? ??????
        if(!tokenProvider.getMemberLoginIdByToken(CookieUtil.getAccessToken(request)).equals(members.getLoginId()))
            throw new BizException(MemberExceptionType.NO_PERMISSION);

        // ??????????????? ????????? ??? ??????
        if (!passwordEncoder.matches(dto.getPassword(), members.getPassword())) {
            throw new BizException(MemberExceptionType.WRONG_PASSWORD);
        }

        // ?????? ???????????? delYn??? Yes??? ?????? ??? ?????? ??????
        members.remove('Y');
        memberRepository.save(members);

        // ??????????????? ?????? ??????
        refreshTokenCacheAdapter.delete(dto.getUserId());
        // ?????? ??????
        CookieUtil.resetDefaultCookies(response);

        return SuccessResponseType.USER_REMOVE_SUCCESS.getMessage();
    }
}
