package com.comeon.authservice.web.auth.controller;

import com.comeon.authservice.common.jwt.RedisRepository;
import com.comeon.authservice.common.exception.ErrorCode;
import com.comeon.authservice.common.jwt.JwtTokenInfo;
import com.comeon.authservice.common.utils.CookieUtil;
import com.comeon.authservice.config.security.handler.UserLogoutRequest;
import com.comeon.authservice.feign.kakao.KakaoApiFeignClient;
import com.comeon.authservice.feign.kakao.response.UserUnlinkResponse;
import com.comeon.authservice.web.AbstractControllerTest;
import com.comeon.authservice.web.docs.utils.RestDocsUtil;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockCookie;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;

import javax.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;

import static com.comeon.authservice.common.utils.CookieUtil.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.snippet.Attributes.attributes;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AuthControllerTest extends AbstractControllerTest {

    static String TOKEN_TYPE_BEARER = "Bearer ";

    @Autowired
    AuthController authController;

    @Autowired
    RedisRepository redisRepository;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @MockBean
    KakaoApiFeignClient kakaoApiFeignClient;

    @AfterEach
    void deleteData() {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.flushAll();
            return null;
        });
    }

    @Nested
    @DisplayName("?????????")
    class login {

        @Test
        @DisplayName("?????????")
        void request() throws Exception {
            String path = "/oauth2/authorize/{providerName}";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path, "kakao")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("redirect_uri", "http://localhost:3000/front/redirect-page")
            );

            perform.andDo(
                    document(
                            "{class-name}/{method-name}",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            pathParameters(
                                    attributes(key("title").value(path)),
                                    parameterWithName("providerName").description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.OAUTH_PROVIDER_CODE))
                            ),
                            requestParameters(
                                    attributes(key("title").value("?????? ????????????")),
                                    parameterWithName("redirect_uri").description("????????? ?????????, ????????? ????????????, ???????????? ??????????????? URL")
                            )
                    )
            );
        }
    }

    @Nested
    @DisplayName("JwtAuthenticationFilter ??????")
    class jwtAuthenticationFilterRequests {

        @Nested
        @DisplayName("?????? ??????")
        class validateMe {

            @Test
            @DisplayName("????????? ????????? ???????????? ????????? ????????????, ?????? ?????? ?????? ???????????? ?????????.")
            void success() throws Exception {
                // given
                Long userId = 1L;
                String userRole = "ROLE_USER";
                JwtTokenInfo accessTokenInfo = generateAccessToken(userId, userRole, Instant.now(), Instant.now().plusSeconds(300));

                // when
                String requestAccessToken = accessTokenInfo.getValue();
                ResultActions perform = mockMvc.perform(
                        get("/auth/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, TOKEN_TYPE_BEARER + requestAccessToken)
                );

                // then
                perform.andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.userId").value(userId));

                // docs
                perform.andDo(
                        restDocs.document(
                                requestHeaders(
                                        attributes(key("title").value("?????? ??????")),
                                        headerWithName(org.springframework.http.HttpHeaders.AUTHORIZATION).description("Bearer ????????? ????????? AccessToken")
                                ),
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("userId").type(JsonFieldType.NUMBER).description("?????? ???????????? ????????? ?????????")
                                )
                        )
                );
            }

            @Test
            @DisplayName("????????? ????????? ??????, http status 401 ??????. ErrorCode.INVALID_ACCESS_TOKEN")
            void expiredAccessToken() throws Exception {
                // given
                Long userId = 1L;
                String userRole = "ROLE_USER";
                Instant expired = Instant.now().minusSeconds(300);
                JwtTokenInfo expiredAccessTokenInfo = generateAccessToken(userId, userRole, expired, expired);

                // when
                String invalidAccessToken = expiredAccessTokenInfo.getValue();
                ResultActions perform = mockMvc.perform(
                        get("/auth/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, TOKEN_TYPE_BEARER + invalidAccessToken)
                );

                // then
                perform.andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.INVALID_ACCESS_TOKEN.getCode()))
                        .andExpect(jsonPath("$.data.message").value(ErrorCode.INVALID_ACCESS_TOKEN.getMessage()));

                // docs
                perform.andDo(
                        restDocs.document(
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                                )
                        )
                );
            }

            @Test
            @DisplayName("????????? ?????? ?????? ??????????????? ????????? ????????? ??????, 401 error ??????. ErrorCode.INVALID_ACCESS_TOKEN")
            void invalidAccessToken() throws Exception {
                // given
                Long userId = 1L;
                String userRole = "ROLE_USER";
                JwtTokenInfo accessToken = generateAccessToken(userId, userRole, Instant.now(), Instant.now().plusSeconds(300));

                // when
                String invalidAccessTokenValue = accessToken.getValue() + "asd";
                ResultActions perform = mockMvc.perform(
                        get("/auth/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, TOKEN_TYPE_BEARER + invalidAccessTokenValue)
                );

                // then
                perform.andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.INVALID_ACCESS_TOKEN.getCode()))
                        .andExpect(jsonPath("$.data.message").value(ErrorCode.INVALID_ACCESS_TOKEN.getMessage()));

                // docs
                perform.andDo(
                        restDocs.document(
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                                )
                        )
                );
            }

            @Test
            @DisplayName("Authorization ????????? ?????? ??????, http status 401 ??????. ErrorCode.NO_AUTHORIZATION_HEADER")
            void noAuthorizationHeader() throws Exception {
                // when
                ResultActions perform = mockMvc.perform(
                        get("/auth/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                );

                // then
                perform.andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.NO_AUTHORIZATION_HEADER.getCode()))
                        .andExpect(jsonPath("$.data.message").value(ErrorCode.NO_AUTHORIZATION_HEADER.getMessage()));

                // docs
                perform.andDo(
                        restDocs.document(
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                                )
                        )
                );
            }

            @Test
            @DisplayName("Authorization ????????? ????????? ?????? ?????? ??????, http status 401 ??????. ErrorCode.NO_AUTHORIZATION_HEADER")
            void noAccessToken() throws Exception {
                // when
                ResultActions perform = mockMvc.perform(
                        get("/auth/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "")
                );

                // then
                perform.andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.NO_AUTHORIZATION_HEADER.getCode()))
                        .andExpect(jsonPath("$.data.message").value(ErrorCode.NO_AUTHORIZATION_HEADER.getMessage()));

                // docs
                perform.andDo(
                        restDocs.document(
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                                )
                        )
                );
            }

            @Test
            @DisplayName("Authorization ????????? ????????? 'Bearer '??? ???????????? ?????? ??????, http status 401 ??????. ErrorCode.NOT_SUPPORTED_TOKEN_TYPE")
            void accessTokenIsNotBearerType() throws Exception {
                // given
                Long userId = 1L;
                String userRole = "ROLE_USER";
                JwtTokenInfo accessToken = generateAccessToken(userId, userRole, Instant.now(), Instant.now().plusSeconds(300));

                // when
                String accessTokenValue = accessToken.getValue();
                ResultActions perform = mockMvc.perform(
                        get("/auth/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, accessTokenValue)
                );

                // then
                perform.andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.NOT_SUPPORTED_TOKEN_TYPE.getCode()))
                        .andExpect(jsonPath("$.data.message").value(ErrorCode.NOT_SUPPORTED_TOKEN_TYPE.getMessage()));

                // docs
                perform.andDo(
                        restDocs.document(
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                                )
                        )
                );
            }

            @Test
            @DisplayName("AccessToken??? ????????? ???????????? ?????????????????? ?????? ??????, http status 401 ??????. ErrorCode.INVALID_ACCESS_TOKEN")
            void includeBlackList() throws Exception {
                // given
                Long userId = 1L;
                String userRole = "ROLE_USER";
                JwtTokenInfo accessToken = generateAccessToken(userId, userRole, Instant.now(), Instant.now().plusSeconds(300));

                String accessTokenValue = accessToken.getValue();
                // ?????????????????? AccessToken ??????
                redisRepository.addBlackList(accessTokenValue, Duration.between(Instant.now(), accessToken.getExpiry()));

                // when
                ResultActions perform = mockMvc.perform(
                        get("/auth/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, TOKEN_TYPE_BEARER + accessTokenValue)
                );

                // then
                perform.andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.INVALID_ACCESS_TOKEN.getCode()))
                        .andExpect(jsonPath("$.data.message").value(ErrorCode.INVALID_ACCESS_TOKEN.getMessage()));

                // docs
                perform.andDo(
                        restDocs.document(
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                                )
                        )
                );
            }
        }

        @Nested
        @DisplayName("???????????? ??? ????????? ?????? ??????")
        class unlink {

            void mockingKakaoApiFeignClient(Long oauthId) {
                given(kakaoApiFeignClient.userUnlink(anyString(), eq(oauthId), anyString()))
                        .willReturn(new UserUnlinkResponse(oauthId));
            }

            @Test
            @DisplayName("????????? ????????? userOauthId??? ???????????? ???????????? ????????? ?????? ?????? ??? ??????????????? ????????????.")
            void success() throws Exception {
                // given
                Long userId = 1L;
                String userRole = "ROLE_USER";
                JwtTokenInfo accessTokenInfo = generateAccessToken(userId, userRole, Instant.now(), Instant.now().plusSeconds(300));

                Long oauthId = 10000L;

                JwtTokenInfo refreshTokenInfo = generateRefreshToken(Instant.now(), Instant.now().plusSeconds(600));
                redisRepository.addRefreshToken(
                        String.valueOf(userId),
                        refreshTokenInfo.getValue(),
                        Duration.between(
                                Instant.now(),
                                Instant.now().plusSeconds(600)
                        )
                );
                ResponseCookie refreshTokenCookie = ResponseCookie.from(COOKIE_NAME_REFRESH_TOKEN, refreshTokenInfo.getValue())
                        .path("/")
                        .domain(SERVER_DOMAIN)
                        .maxAge(60)
                        .httpOnly(true)
                        .secure(true)
                        .sameSite(org.springframework.boot.web.server.Cookie.SameSite.NONE.attributeValue())
                        .build();

                // mocking
                mockingKakaoApiFeignClient(oauthId);

                // when
                String requestAccessToken = accessTokenInfo.getValue();
                ResultActions perform = mockMvc.perform(
                        post("/auth/unlink")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, TOKEN_TYPE_BEARER + requestAccessToken)
                                .param("userOauthId", String.valueOf(oauthId))
                                .cookie(MockCookie.parse(refreshTokenCookie.toString()))
                );

                // then
                perform.andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.message").exists())
                        .andExpect(cookie().value(COOKIE_NAME_REFRESH_TOKEN, ""))
                        .andExpect(cookie().maxAge(COOKIE_NAME_REFRESH_TOKEN, 0));

                assertThat(redisRepository.findRefreshTokenByUserId(String.valueOf(userId)))
                        .isNotPresent();
                assertThat(redisRepository.findBlackList(requestAccessToken))
                        .isPresent();

                // docs
                perform.andDo(
                        restDocs.document(
                                requestHeaders(
                                        attributes(key("title").value("?????? ??????")),
                                        headerWithName(org.springframework.http.HttpHeaders.AUTHORIZATION).description("Bearer ????????? ????????? AccessToken")
                                ),
                                requestParameters(
                                        attributes(key("title").value("?????? ????????????")),
                                        parameterWithName("userOauthId").description("????????? ?????? ????????? ID")
                                ),
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("????????? ?????? ?????? ??? ???????????? ?????? ?????? ?????????")
                                ),
                                RestDocsUtil.customResponseHeaders(
                                        "cookie-response",
                                        attributes(key("title").value("?????? ??????")),
                                        headerWithName(org.springframework.http.HttpHeaders.SET_COOKIE)
                                                .description("???????????? ?????? ?????? ??????")
                                                .attributes(
                                                        key("HttpOnly").value(true),
                                                        key("cookie").value(COOKIE_NAME_REFRESH_TOKEN),
                                                        key("Secure").value(true),
                                                        key("SameSite").value("NONE")
                                                )
                                )
                        )
                );
            }

            @Test
            @DisplayName("???????????? ?????? ????????? ???????????? http status 401 ????????????.")
            void invalidAccessToken() throws Exception {
                // given
                Long userId = 1L;
                String userRole = "ROLE_USER";
                JwtTokenInfo accessToken = generateAccessToken(userId, userRole, Instant.now(), Instant.now().plusSeconds(300));

                Long oauthId = 10000L;

                // when
                String invalidAccessTokenValue = accessToken.getValue() + "asd";
                ResultActions perform = mockMvc.perform(
                        post("/auth/unlink")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, TOKEN_TYPE_BEARER + invalidAccessTokenValue)
                                .param("userOauthId", String.valueOf(oauthId))
                );

                // then
                perform.andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.INVALID_ACCESS_TOKEN.getCode()))
                        .andExpect(jsonPath("$.data.message").value(ErrorCode.INVALID_ACCESS_TOKEN.getMessage()));

                // docs
                perform.andDo(
                        restDocs.document(
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                                )
                        )
                );
            }

            @Test
            @DisplayName("?????? ??????????????? ???????????? ????????? http status 400 ????????????.")
            void noRequestParam() throws Exception {
                // given
                Long userId = 1L;
                String userRole = "ROLE_USER";
                JwtTokenInfo accessTokenInfo = generateAccessToken(userId, userRole, Instant.now(), Instant.now().plusSeconds(300));

                JwtTokenInfo refreshTokenInfo = generateRefreshToken(Instant.now(), Instant.now().plusSeconds(600));
                redisRepository.addRefreshToken(
                        String.valueOf(userId),
                        refreshTokenInfo.getValue(),
                        Duration.between(
                                Instant.now(),
                                Instant.now().plusSeconds(600)
                        )
                );
                ResponseCookie refreshTokenCookie = ResponseCookie.from(COOKIE_NAME_REFRESH_TOKEN, refreshTokenInfo.getValue())
                        .path("/")
                        .domain(SERVER_DOMAIN)
                        .maxAge(60)
                        .httpOnly(true)
                        .secure(true)
                        .sameSite(org.springframework.boot.web.server.Cookie.SameSite.NONE.attributeValue())
                        .build();

                // when
                String requestAccessToken = accessTokenInfo.getValue();
                ResultActions perform = mockMvc.perform(
                        post("/auth/unlink")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, TOKEN_TYPE_BEARER + requestAccessToken)
                                .cookie(MockCookie.parse(refreshTokenCookie.toString()))
                );

                // then
                perform.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.VALIDATION_FAIL.getCode()))
                        .andExpect(jsonPath("$.data.message").exists());

                // docs
                perform.andDo(
                        restDocs.document(
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                        subsectionWithPath("message").type(JsonFieldType.OBJECT).description("API ?????? ?????????")
                                )
                        )
                );
            }
        }
    }

    @Nested
    @DisplayName("ReissueAuthenticationFilter ??????")
    class reissueAuthenticationFilterRequests {

        @Nested
        @DisplayName("?????? ?????????")
        class reissueTokens {

            private Cookie generateRefreshTokenCookie(String refreshTokenValue) {
                Cookie refreshTokenCookie = new Cookie("refreshToken", refreshTokenValue);
                refreshTokenCookie.setPath("/");
                refreshTokenCookie.setHttpOnly(true);
                refreshTokenCookie.setMaxAge(300);
                return refreshTokenCookie;
            }

            @Test
            @DisplayName("AccessToken ??????, RefreshToken??? ????????????, RefreshToken ???????????? 7??? ?????? ????????????, ??? ??? ????????? ??????.")
            void reissueAllTokens() throws Exception {
                // given
                Long userId = 1L;
                String userRole = "ROLE_USER";
                // accessToken ?????? ??????
                Instant accessTokenExpiredAt = Instant.now().minusSeconds(300);
                JwtTokenInfo accessTokenInfo = generateAccessToken(userId, userRole, accessTokenExpiredAt, accessTokenExpiredAt);

                // RefreshToken ?????? ?????? - ???????????? 7??? ?????? ??????
                Instant refreshTokenIssuedAt = Instant.now();
                Instant refreshTokenExpiredAt = refreshTokenIssuedAt.plusSeconds(60 * 60 * 24 * 7);
                JwtTokenInfo refreshTokenInfo = generateRefreshToken(refreshTokenIssuedAt, refreshTokenExpiredAt);
                // ???????????? refreshToken ??????
                redisRepository.addRefreshToken(
                        userId.toString(),
                        refreshTokenInfo.getValue(),
                        Duration.between(refreshTokenIssuedAt, refreshTokenExpiredAt)
                );

                // when
                String accessTokenValue = accessTokenInfo.getValue();
                Cookie refreshTokenCookie = generateRefreshTokenCookie(refreshTokenInfo.getValue());
                ResultActions perform = mockMvc.perform(
                        post("/auth/reissue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, TOKEN_TYPE_BEARER + accessTokenValue)
                                .cookie(refreshTokenCookie)
                );

                // then
                perform.andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                        .andExpect(jsonPath("$.data.expiry").isNotEmpty())
                        .andExpect(jsonPath("$.data.userId").isNotEmpty())
                        .andExpect(cookie().exists("refreshToken"));

                // docs
                perform.andDo(
                        restDocs.document(
                                requestHeaders(
                                        attributes(key("title").value("?????? ??????")),
                                        headerWithName(org.springframework.http.HttpHeaders.AUTHORIZATION).description("Bearer ????????? ????????? AccessToken")
                                ),
                                RestDocsUtil.customRequestHeaders(
                                        "cookie-request",
                                        attributes(
                                                key("title").value("?????? ??????"),
                                                key("name").value("Cookie"),
                                                key("cookie").value("refreshToken"),
                                                key("description").value("????????? RefreshToken")
                                        )
                                ),
                                RestDocsUtil.customResponseHeaders(
                                        "cookie-response",
                                        attributes(key("title").value("?????? ??????")),
                                        headerWithName(org.springframework.http.HttpHeaders.SET_COOKIE)
                                                .description("?????? RefreshToken ?????? ????????? 7??? ???????????? ???????????? ??????????????? ???????????????.")
                                                .optional()
                                                .attributes(
                                                        key("HttpOnly").value(true),
                                                        key("cookie").value("refreshToken"),
                                                        key("Secure").value(true),
                                                        key("SameSite").value("NONE")
                                                )
                                ),
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("accessToken").type(JsonFieldType.STRING).description("???????????? Access Token"),
                                        fieldWithPath("expiry").type(JsonFieldType.NUMBER).description("???????????? Access Token??? ????????? - UNIX TIME"),
                                        fieldWithPath("userId").type(JsonFieldType.NUMBER).description("????????? ????????? ????????? ?????????")
                                )
                        )
                );
            }

            @Test
            @DisplayName("AccessToken ??????, RefreshToken 7??? ?????? ????????? AccessToken??? ????????? ??????.")
            void reissueAccessTokenOnly() throws Exception {
                // given
                Long userId = 1L;
                String userRole = "ROLE_USER";
                // AccessToken ?????? ??????
                Instant accessTokenExpiredAt = Instant.now().minusSeconds(300);
                JwtTokenInfo accessTokenInfo = generateAccessToken(userId, userRole, accessTokenExpiredAt, accessTokenExpiredAt);

                // RefreshToken ?????? ?????? - ???????????? 7??? ?????? ??????
                Instant refreshTokenIssuedAt = Instant.now();
                Instant refreshTokenExpiredAt = refreshTokenIssuedAt.plusSeconds(60 * 60 * 24 * 7 + 10);
                JwtTokenInfo refreshTokenInfo = generateRefreshToken(refreshTokenIssuedAt, refreshTokenExpiredAt);
                redisRepository.addRefreshToken(
                        userId.toString(),
                        refreshTokenInfo.getValue(),
                        Duration.between(refreshTokenIssuedAt, refreshTokenExpiredAt)
                );

                // when
                String accessTokenValue = accessTokenInfo.getValue();
                Cookie refreshTokenCookie = generateRefreshTokenCookie(refreshTokenInfo.getValue());
                ResultActions perform = mockMvc.perform(
                        post("/auth/reissue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, TOKEN_TYPE_BEARER + accessTokenValue)
                                .cookie(refreshTokenCookie)
                );

                // then
                perform.andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                        .andExpect(jsonPath("$.data.expiry").isNotEmpty())
                        .andExpect(jsonPath("$.data.userId").isNotEmpty())
                        .andExpect(cookie().doesNotExist("refreshToken"));

                // docs
                perform.andDo(
                        restDocs.document(
                                requestHeaders(
                                        attributes(key("title").value("?????? ??????")),
                                        headerWithName(org.springframework.http.HttpHeaders.AUTHORIZATION).description("Bearer ????????? ????????? AccessToken")
                                ),
                                RestDocsUtil.customRequestHeaders(
                                        "cookie-request",
                                        attributes(
                                                key("title").value("?????? ??????"),
                                                key("name").value("Cookie"),
                                                key("cookie").value("refreshToken"),
                                                key("description").value("????????? RefreshToken")
                                        )
                                ),
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("accessToken").type(JsonFieldType.STRING).description("???????????? Access Token"),
                                        fieldWithPath("expiry").type(JsonFieldType.NUMBER).description("???????????? Access Token??? ????????? - UNIX TIME"),
                                        fieldWithPath("userId").type(JsonFieldType.NUMBER).description("????????? ????????? ????????? ?????????")
                                )
                        )
                );
            }

            @Test
            @DisplayName("AccessToken ????????? ?????? ????????? ?????? ?????? ????????? ????????????, http status 401 ??????. ErrorCode.INVALID_ACCESS_TOKEN")
            void invalidAccessToken() throws Exception {
                // given
                Long userId = 1L;
                String userRole = "ROLE_USER";
                // AccessToken ?????? ??????
                Instant accessTokenExpiredAt = Instant.now().minusSeconds(300);
                JwtTokenInfo accessTokenInfo = generateAccessToken(userId, userRole, accessTokenExpiredAt, accessTokenExpiredAt);

                // RefreshToken ?????? ?????? - ???????????? 7??? ?????? ??????
                Instant refreshTokenIssuedAt = Instant.now();
                Instant refreshTokenExpiredAt = refreshTokenIssuedAt.plusSeconds(60 * 60 * 24 * 7 + 10);
                JwtTokenInfo refreshTokenInfo = generateRefreshToken(refreshTokenIssuedAt, refreshTokenExpiredAt);
                redisRepository.addRefreshToken(
                        userId.toString(),
                        refreshTokenInfo.getValue(),
                        Duration.between(refreshTokenIssuedAt, refreshTokenExpiredAt)
                );

                // when
                String invalidAccessToken = accessTokenInfo.getValue() + "asd";
                Cookie refreshTokenCookie = generateRefreshTokenCookie(refreshTokenInfo.getValue());
                ResultActions perform = mockMvc.perform(
                        post("/auth/reissue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, TOKEN_TYPE_BEARER + invalidAccessToken)
                                .cookie(refreshTokenCookie)
                );

                // then
                perform.andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.INVALID_ACCESS_TOKEN.getCode()))
                        .andExpect(jsonPath("$.data.message").value(ErrorCode.INVALID_ACCESS_TOKEN.getMessage()));

                // docs
                perform.andDo(
                        restDocs.document(
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                                )
                        )
                );
            }

            @Test
            @DisplayName("AccessToken??? ???????????? ????????? ???????????? Http Status 400 ????????????. ErrorCode.NOT_EXPIRED_ACCESS_TOKEN")
            void notExpiredAccessToken() throws Exception {
                // given
                Long userId = 1L;
                String userRole = "ROLE_USER";
                // AccessToken ?????? ??????
                Instant accessTokenIssuedAt = Instant.now();
                Instant accessTokenExpiredAt = accessTokenIssuedAt.plusSeconds(300);
                JwtTokenInfo accessTokenInfo = generateAccessToken(userId, userRole, accessTokenIssuedAt, accessTokenExpiredAt);

                // RefreshToken ?????? ?????? - ???????????? 7??? ?????? ??????
                Instant refreshTokenIssuedAt = Instant.now();
                Instant refreshTokenExpiredAt = refreshTokenIssuedAt.plusSeconds(60 * 60 * 24 * 7 + 10);
                JwtTokenInfo refreshTokenInfo = generateRefreshToken(refreshTokenIssuedAt, refreshTokenExpiredAt);
                redisRepository.addRefreshToken(
                        userId.toString(),
                        refreshTokenInfo.getValue(),
                        Duration.between(refreshTokenIssuedAt, refreshTokenExpiredAt)
                );

                // when
                String accessTokenValue = accessTokenInfo.getValue();
                Cookie refreshTokenCookie = generateRefreshTokenCookie(refreshTokenInfo.getValue());
                ResultActions perform = mockMvc.perform(
                        post("/auth/reissue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, TOKEN_TYPE_BEARER + accessTokenValue)
                                .cookie(refreshTokenCookie)
                );

                // then
                perform.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.NOT_EXPIRED_ACCESS_TOKEN.getCode()))
                        .andExpect(jsonPath("$.data.message").value(ErrorCode.NOT_EXPIRED_ACCESS_TOKEN.getMessage()));

                // docs
                perform.andDo(
                        restDocs.document(
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                                )
                        )
                );
            }

            @Test
            @DisplayName("AccessToken ??????, refreshToken??? ?????? ????????? ?????? ??????, ????????? ???????????? http status 401 ????????????. ErrorCode.NO_REFRESH_TOKEN")
            void noRefreshToken() throws Exception {
                // given
                Long userId = 1L;
                String userRole = "ROLE_USER";
                // AccessToken ?????? ??????
                Instant accessTokenExpiredAt = Instant.now().minusSeconds(300);
                JwtTokenInfo accessTokenInfo = generateAccessToken(userId, userRole, accessTokenExpiredAt, accessTokenExpiredAt);

                // when
                String accessTokenValue = accessTokenInfo.getValue();
                ResultActions perform = mockMvc.perform(
                        post("/auth/reissue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, TOKEN_TYPE_BEARER + accessTokenValue)
                );

                // then
                perform.andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.NO_REFRESH_TOKEN.getCode()))
                        .andExpect(jsonPath("$.data.message").value(ErrorCode.NO_REFRESH_TOKEN.getMessage()));

                // docs
                perform.andDo(
                        restDocs.document(
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                                )
                        )
                );
            }


            @Test
            @DisplayName("AccessToken ??????, RefreshToken ????????? ???????????? ????????? ???????????? Http Status 401 ????????????. ErrorCode.INVALID_REFRESH_TOKEN")
            void invalidRefreshToken() throws Exception {
                // given
                Long userId = 1L;
                String userRole = "ROLE_USER";
                // AccessToken ?????? ??????
                Instant accessTokenExpiredAt = Instant.now().minusSeconds(300);
                JwtTokenInfo accessTokenInfo = generateAccessToken(userId, userRole, accessTokenExpiredAt, accessTokenExpiredAt);

                // RefreshToken ?????? ?????? - ???????????? 7??? ?????? ??????
                Instant refreshTokenIssuedAt = Instant.now();
                Instant refreshTokenExpiredAt = refreshTokenIssuedAt.plusSeconds(60 * 60 * 24 * 7 + 10);
                JwtTokenInfo refreshTokenInfo = generateRefreshToken(refreshTokenIssuedAt, refreshTokenExpiredAt);
                redisRepository.addRefreshToken(
                        userId.toString(),
                        refreshTokenInfo.getValue(),
                        Duration.between(refreshTokenIssuedAt, refreshTokenExpiredAt)
                );

                // when
                String accessTokenValue = accessTokenInfo.getValue();
                Cookie refreshTokenCookie = generateRefreshTokenCookie(refreshTokenInfo.getValue() + "asd");
                ResultActions perform = mockMvc.perform(
                        post("/auth/reissue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, TOKEN_TYPE_BEARER + accessTokenValue)
                                .cookie(refreshTokenCookie)
                );

                // then
                perform.andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.INVALID_REFRESH_TOKEN.getCode()))
                        .andExpect(jsonPath("$.data.message").value(ErrorCode.INVALID_REFRESH_TOKEN.getMessage()));

                // docs
                perform.andDo(
                        restDocs.document(
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                                )
                        )
                );
            }

            @Test
            @DisplayName("AccessToken ?????????????????? RefreshToken??? redis??? ???????????? ????????? ????????? ???????????? Http Status 401 ????????????. ErrorCode.INVALID_REFRESH_TOKEN")
            void refreshTokenDoesNotMatchRedis() throws Exception {
                // given
                Long userId = 1L;
                String userRole = "ROLE_USER";
                // AccessToken ?????? ??????
                Instant accessTokenExpiredAt = Instant.now().minusSeconds(300);
                JwtTokenInfo accessTokenInfo = generateAccessToken(userId, userRole, accessTokenExpiredAt, accessTokenExpiredAt);

                // RefreshToken ?????? ?????? - ???????????? 7??? ?????? ??????
                Instant refreshTokenIssuedAt = Instant.now();
                Instant refreshTokenExpiredAt = refreshTokenIssuedAt.plusSeconds(60 * 60 * 24 * 7 + 10);
                JwtTokenInfo refreshTokenInfo = generateRefreshToken(refreshTokenIssuedAt, refreshTokenExpiredAt);
                // RefreshToken redis??? ???????????? ?????? -> ?????? ?????? ?????? ??????!

                // when
                String accessTokenValue = accessTokenInfo.getValue();
                Cookie refreshTokenCookie = generateRefreshTokenCookie(refreshTokenInfo.getValue());
                ResultActions perform = mockMvc.perform(
                        post("/auth/reissue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, TOKEN_TYPE_BEARER + accessTokenValue)
                                .cookie(refreshTokenCookie)
                );

                // then
                perform.andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.INVALID_REFRESH_TOKEN.getCode()))
                        .andExpect(jsonPath("$.data.message").value(ErrorCode.INVALID_REFRESH_TOKEN.getMessage()));

                // docs
                perform.andDo(
                        restDocs.document(
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                                )
                        )
                );
            }

            @Test
            @DisplayName("Authorization ????????? ????????? Http Status 401 ????????????. ErrorCode.NO_AUTHORIZATION_HEADER")
            void noAuthorizationHeader() throws Exception {
                // when
                ResultActions perform = mockMvc.perform(
                        post("/auth/reissue")
                                .contentType(MediaType.APPLICATION_JSON)
                );

                // then
                perform.andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.NO_AUTHORIZATION_HEADER.getCode()))
                        .andExpect(jsonPath("$.data.message").value(ErrorCode.NO_AUTHORIZATION_HEADER.getMessage()));

                // docs
                perform.andDo(
                        restDocs.document(
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                                )
                        )
                );
            }

            @Test
            @DisplayName("Authorization ????????? ?????????, ?????? ??????????????? Http Status 401 ????????????. ErrorCode.NO_AUTHORIZATION_HEADER")
            void emptyAuthorizationHeader() throws Exception {
                // when
                ResultActions perform = mockMvc.perform(
                        post("/auth/reissue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "")
                );

                // then
                perform.andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.NO_AUTHORIZATION_HEADER.getCode()))
                        .andExpect(jsonPath("$.data.message").value(ErrorCode.NO_AUTHORIZATION_HEADER.getMessage()));

                // docs
                perform.andDo(
                        restDocs.document(
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                                )
                        )
                );
            }

            @Test
            @DisplayName("Authorization ????????? 'Bearer '??? ???????????? ?????????, http status 401 ??????. ErrorCode.NOT_SUPPORTED_TOKEN_TYPE")
            void notSupportedTokenType() throws Exception {
                // given
                Long userId = 1L;
                String userRole = "ROLE_USER";
                // AccessToken ?????? ??????
                Instant accessTokenExpiredAt = Instant.now().minusSeconds(300);
                JwtTokenInfo accessTokenInfo = generateAccessToken(userId, userRole, accessTokenExpiredAt, accessTokenExpiredAt);

                // when
                String accessTokenValue = accessTokenInfo.getValue();
                ResultActions perform = mockMvc.perform(
                        post("/auth/reissue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, accessTokenValue)
                );

                // then
                perform.andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.NOT_SUPPORTED_TOKEN_TYPE.getCode()))
                        .andExpect(jsonPath("$.data.message").value(ErrorCode.NOT_SUPPORTED_TOKEN_TYPE.getMessage()));

                // docs
                perform.andDo(
                        restDocs.document(
                                responseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        attributes(key("title").value("?????? ??????")),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                                )
                        )
                );
            }
        }
    }

    @Nested
    @DisplayName("????????????")
    class logout {

        @Test
        @DisplayName("?????? ???????????? ????????? ???????????????")
        void redirectSocialLogout() throws Exception {
            // given
            JwtTokenInfo accessTokenInfo = generateAccessToken(1L, "USER_ROLE", Instant.now(), Instant.now().plusSeconds(300));
            String accessToken = accessTokenInfo.getValue();
            String redirectUri = "http://localhost:3000";

            // when
            ResultActions perform = mockMvc.perform(
                    post("/oauth2/logout")
                            .param("token", accessToken)
                            .param("redirect_uri", redirectUri)
            );

            // then
            perform.andExpect(status().is3xxRedirection())
                    .andExpect(cookie().exists(CookieUtil.COOKIE_NAME_USER_LOGOUT_REQUEST));

            // docs
            perform.andDo(
                    restDocs.document(
                            requestParameters(
                                    attributes(key("title").value("?????? ????????????")),
                                    parameterWithName("token").description("????????? ?????? ???????????? ?????? ???????????? ????????? ????????? ?????????"),
                                    parameterWithName("redirect_uri").description("???????????? ????????? ???????????? ??????????????? ??? ??????")
                            ),
                            RestDocsUtil.customResponseHeaders(
                                    "cookie-response",
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(org.springframework.http.HttpHeaders.SET_COOKIE)
                                            .description("?????? ???????????? ??????, ?????? ?????????????????? ?????? ??? ????????? ???????????? ?????? ??????. ?????? ??????????????? ????????? token, redirect_uri ??????.")
                                            .attributes(
                                                    key("HttpOnly").value(true),
                                                    key("cookie").value("logoutRequest"),
                                                    key("Secure").value(true),
                                                    key("SameSite").value("NONE")
                                            )
                            )
                    )
            );
        }

        @Test
        @DisplayName("?????? ???????????? ?????? ??????")
        void serverLogoutSuccess() throws Exception {
            // given
            JwtTokenInfo accessTokenInfo = generateAccessToken(1L, "USER_ROLE", Instant.now(), Instant.now().plusSeconds(300));
            String accessToken = accessTokenInfo.getValue();
            String redirectUri = "http://localhost:3000";

            UserLogoutRequest userLogoutRequest = new UserLogoutRequest(accessToken, redirectUri);
            ResponseCookie responseCookie = ResponseCookie.from(COOKIE_NAME_USER_LOGOUT_REQUEST, CookieUtil.serialize(userLogoutRequest))
                    .path("/")
                    .domain(SERVER_DOMAIN)
                    .maxAge(60)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite(org.springframework.boot.web.server.Cookie.SameSite.NONE.attributeValue())
                    .build();

            // when
            ResultActions perform = mockMvc.perform(
                    post("/auth/logout")
                            .cookie(MockCookie.parse(responseCookie.toString()))
            );

            // then
            perform.andExpect(status().is3xxRedirection())
                    .andExpect(cookie().value(COOKIE_NAME_USER_LOGOUT_REQUEST, ""))
                    .andExpect(cookie().maxAge(COOKIE_NAME_USER_LOGOUT_REQUEST, 0))
                    .andExpect(cookie().doesNotExist(COOKIE_NAME_REFRESH_TOKEN));

            // docs
            perform.andDo(
                    restDocs.document(
                            RestDocsUtil.customRequestHeaders(
                                    "cookie-request",
                                    attributes(
                                            key("title").value("?????? ??????"),
                                            key("name").value(org.springframework.http.HttpHeaders.COOKIE),
                                            key("cookie").value("logoutRequest"),
                                            key("description").value("?????????????????? ?????? ??? ????????? ???????????? ?????? ??????. ?????? ???????????? API ?????????, ?????? ??????????????? ????????? token, redirect_uri ???????????? ?????? ??????.")
                                    )
                            ),
                            RestDocsUtil.customResponseHeaders(
                                    "cookie-response",
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(org.springframework.http.HttpHeaders.SET_COOKIE)
                                            .description("???????????? ?????? ?????? ??????")
                                            .attributes(
                                                    key("HttpOnly").value(true),
                                                    key("cookie").value("logoutRequest"),
                                                    key("Secure").value(true),
                                                    key("SameSite").value("NONE")
                                            )
                            )
                    )
            );
        }

        @Test
        @DisplayName("?????? ???????????? ?????? ??????. ???????????? ?????? ????????? ????????? ???????????? ?????? ????????? ????????????. ?????????????????? ?????????. ????????? ????????? ?????????????????? ????????????.")
        void removeRefreshTokenCookie() throws Exception {
            // given
            long userId = 1L;
            JwtTokenInfo accessTokenInfo = generateAccessToken(userId, "USER_ROLE", Instant.now(), Instant.now().plusSeconds(300));
            String accessToken = accessTokenInfo.getValue();
            String redirectUri = "http://localhost:3000";

            UserLogoutRequest userLogoutRequest = new UserLogoutRequest(accessToken, redirectUri);
            ResponseCookie logoutRequestCookie = ResponseCookie.from(COOKIE_NAME_USER_LOGOUT_REQUEST, CookieUtil.serialize(userLogoutRequest))
                    .path("/")
                    .domain(SERVER_DOMAIN)
                    .maxAge(60)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite(org.springframework.boot.web.server.Cookie.SameSite.NONE.attributeValue())
                    .build();

            JwtTokenInfo refreshTokenInfo = generateRefreshToken(Instant.now(), Instant.now().plusSeconds(600));
            redisRepository.addRefreshToken(
                    String.valueOf(userId),
                    refreshTokenInfo.getValue(),
                    Duration.between(
                            Instant.now(),
                            Instant.now().plusSeconds(600)
                    )
            );
            ResponseCookie refreshTokenCookie = ResponseCookie.from(COOKIE_NAME_REFRESH_TOKEN, refreshTokenInfo.getValue())
                    .path("/")
                    .domain(SERVER_DOMAIN)
                    .maxAge(60)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite(org.springframework.boot.web.server.Cookie.SameSite.NONE.attributeValue())
                    .build();

            // when
            ResultActions perform = mockMvc.perform(
                    post("/auth/logout")
                            .cookie(
                                    MockCookie.parse(logoutRequestCookie.toString()),
                                    MockCookie.parse(refreshTokenCookie.toString())
                            )
            );

            // then
            perform.andExpect(status().is3xxRedirection())
                    .andExpect(cookie().value(COOKIE_NAME_USER_LOGOUT_REQUEST, ""))
                    .andExpect(cookie().maxAge(COOKIE_NAME_USER_LOGOUT_REQUEST, 0))
                    .andExpect(cookie().value(COOKIE_NAME_REFRESH_TOKEN, ""))
                    .andExpect(cookie().maxAge(COOKIE_NAME_REFRESH_TOKEN, 0));

            assertThat(redisRepository.findRefreshTokenByUserId(String.valueOf(userId)))
                    .isNotPresent();
            assertThat(redisRepository.findBlackList(accessToken))
                    .isPresent();

            // docs
            perform.andDo(
                    restDocs.document(
                            RestDocsUtil.customRequestHeaders(
                                    "cookie-request",
                                    attributes(
                                            key("title").value("?????? ??????"),
                                            key("name").value(org.springframework.http.HttpHeaders.COOKIE),
                                            key("cookie").value(COOKIE_NAME_USER_LOGOUT_REQUEST),
                                            key("description").value("?????????????????? ?????? ??? ????????? ???????????? ?????? ??????. ?????? ???????????? API ?????????, ?????? ??????????????? ????????? token, redirect_uri ???????????? ?????? ??????.")
                                    )
                            ),
                            RestDocsUtil.customResponseHeaders(
                                    "cookie-response",
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(org.springframework.http.HttpHeaders.SET_COOKIE)
                                            .description("???????????? ?????? ?????? ??????")
                                            .attributes(
                                                    key("HttpOnly").value(true),
                                                    key("cookie").value(COOKIE_NAME_USER_LOGOUT_REQUEST),
                                                    key("Secure").value(true),
                                                    key("SameSite").value("NONE")
                                            ),
                                    headerWithName(org.springframework.http.HttpHeaders.SET_COOKIE)
                                            .description("???????????? ?????? ?????? ??????")
                                            .attributes(
                                                    key("HttpOnly").value(true),
                                                    key("cookie").value(COOKIE_NAME_REFRESH_TOKEN),
                                                    key("Secure").value(true),
                                                    key("SameSite").value("NONE")
                                            )
                            )
                    )
            );
        }

        @Test
        @DisplayName("?????? ???????????? ?????? ??????. logoutRequest ????????? ????????? ?????? ??????????????? ?????? logoutRequest??? ????????????.")
        void logoutRequestInParameter() throws Exception {
            // given
            long userId = 1L;
            JwtTokenInfo accessTokenInfo = generateAccessToken(userId, "USER_ROLE", Instant.now(), Instant.now().plusSeconds(300));
            String accessToken = accessTokenInfo.getValue();
            String redirectUri = "http://localhost:3000";

            JwtTokenInfo refreshTokenInfo = generateRefreshToken(Instant.now(), Instant.now().plusSeconds(600));
            redisRepository.addRefreshToken(
                    String.valueOf(userId),
                    refreshTokenInfo.getValue(),
                    Duration.between(
                            Instant.now(),
                            Instant.now().plusSeconds(600)
                    )
            );
            ResponseCookie refreshTokenCookie = ResponseCookie.from(COOKIE_NAME_REFRESH_TOKEN, refreshTokenInfo.getValue())
                    .path("/")
                    .domain(SERVER_DOMAIN)
                    .maxAge(60)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite(org.springframework.boot.web.server.Cookie.SameSite.NONE.attributeValue())
                    .build();

            // when
            ResultActions perform = mockMvc.perform(
                    post("/auth/logout")
                            .param("token", accessToken)
                            .param("redirect_uri", redirectUri)
                            .cookie(
                                    MockCookie.parse(refreshTokenCookie.toString())
                            )
            );

            // then
            perform.andExpect(status().is3xxRedirection())
                    .andExpect(cookie().doesNotExist(COOKIE_NAME_USER_LOGOUT_REQUEST))
                    .andExpect(cookie().value(COOKIE_NAME_REFRESH_TOKEN, ""))
                    .andExpect(cookie().maxAge(COOKIE_NAME_REFRESH_TOKEN, 0));

            assertThat(redisRepository.findRefreshTokenByUserId(String.valueOf(userId)))
                    .isNotPresent();
            assertThat(redisRepository.findBlackList(accessToken))
                    .isPresent();

            // docs
            perform.andDo(
                    restDocs.document(
                            requestParameters(
                                    attributes(key("title").value("?????? ????????????")),
                                    parameterWithName("token").description("????????? ?????? ???????????? ?????? ???????????? ????????? ????????? ?????????"),
                                    parameterWithName("redirect_uri").description("???????????? ????????? ???????????? ??????????????? ??? ??????")
                            ),
                            RestDocsUtil.customResponseHeaders(
                                    "cookie-response",
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(org.springframework.http.HttpHeaders.SET_COOKIE)
                                            .description("???????????? ?????? ?????? ??????")
                                            .attributes(
                                                    key("HttpOnly").value(true),
                                                    key("cookie").value(COOKIE_NAME_REFRESH_TOKEN),
                                                    key("Secure").value(true),
                                                    key("SameSite").value("NONE")
                                            )
                            )
                    )
            );
        }

        @Test
        @DisplayName("????????? ?????? ???????????? ?????? logoutRequest??? ????????? 401 ????????????. ErrorCode.NO_PARAM_TOKEN")
        void noLogoutRequest() throws Exception {
            // when
            ResultActions perform = mockMvc.perform(
                    post("/auth/logout")
            );

            // then
            perform.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.NO_PARAM_TOKEN.getCode()))
                    .andExpect(jsonPath("$.data.message").value(ErrorCode.NO_PARAM_TOKEN.getMessage()));

            // docs
            perform.andDo(
                    restDocs.document(
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                            )
                    )
            );
        }

        @Test
        @DisplayName("param-logoutRequest??? ????????? ????????? ????????? 401 ????????????. ErrorCode.NO_PARAM_TOKEN")
        void noAccessTokenParam() throws Exception {
            // given
            String redirectUri = "http://localhost:3000";

            // when
            ResultActions perform = mockMvc.perform(
                    post("/auth/logout")
                            .param("redirect_uri", redirectUri)
            );

            // then
            perform.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.NO_PARAM_TOKEN.getCode()))
                    .andExpect(jsonPath("$.data.message").value(ErrorCode.NO_PARAM_TOKEN.getMessage()));

            // docs
            perform.andDo(
                    restDocs.document(
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                            )
                    )
            );
        }

        @Test
        @DisplayName("param-logoutRequest??? redirectUri ????????? 400 ????????????. ErrorCode.NO_PARAM_REDIRECT_URI")
        void noRedirectUriParam() throws Exception {
            // given
            long userId = 1L;
            JwtTokenInfo accessTokenInfo = generateAccessToken(userId, "USER_ROLE", Instant.now(), Instant.now().plusSeconds(300));
            String accessToken = accessTokenInfo.getValue();

            // when
            ResultActions perform = mockMvc.perform(
                    post("/auth/logout")
                            .param("token", accessToken)
            );

            // then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.NO_PARAM_REDIRECT_URI.getCode()))
                    .andExpect(jsonPath("$.data.message").value(ErrorCode.NO_PARAM_REDIRECT_URI.getMessage()));

            // docs
            perform.andDo(
                    restDocs.document(
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                            )
                    )
            );
        }

        @Test
        @DisplayName("cookie-logoutRequest??? redirectUri ????????? 400 ????????????. ErrorCode.NO_PARAM_REDIRECT_URI")
        void noRedirectUriInCookie() throws Exception {
            // given
            long userId = 1L;
            JwtTokenInfo accessTokenInfo = generateAccessToken(userId, "USER_ROLE", Instant.now(), Instant.now().plusSeconds(300));
            String accessToken = accessTokenInfo.getValue();

            UserLogoutRequest userLogoutRequest = new UserLogoutRequest(accessToken, null);
            ResponseCookie logoutRequestCookie = ResponseCookie.from(COOKIE_NAME_USER_LOGOUT_REQUEST, CookieUtil.serialize(userLogoutRequest))
                    .path("/")
                    .domain(SERVER_DOMAIN)
                    .maxAge(60)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite(org.springframework.boot.web.server.Cookie.SameSite.NONE.attributeValue())
                    .build();

            // when
            ResultActions perform = mockMvc.perform(
                    post("/auth/logout")
                            .cookie(
                                    MockCookie.parse(logoutRequestCookie.toString())
                            )
            );

            // then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.NO_PARAM_REDIRECT_URI.getCode()))
                    .andExpect(jsonPath("$.data.message").value(ErrorCode.NO_PARAM_REDIRECT_URI.getMessage()));

            // docs
            perform.andDo(
                    restDocs.document(
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                            )
                    )
            );
        }

        @Test
        @DisplayName("cookie-logoutRequest??? ????????? ????????? ????????? 401 ????????????. ErrorCode.NO_PARAM_TOKEN")
        void noAccessTokenInCookie() throws Exception {
            // given
            String redirectUri = "http://localhost:3000";

            UserLogoutRequest userLogoutRequest = new UserLogoutRequest(null, redirectUri);
            ResponseCookie logoutRequestCookie = ResponseCookie.from(COOKIE_NAME_USER_LOGOUT_REQUEST, CookieUtil.serialize(userLogoutRequest))
                    .path("/")
                    .domain(SERVER_DOMAIN)
                    .maxAge(60)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite(org.springframework.boot.web.server.Cookie.SameSite.NONE.attributeValue())
                    .build();

            // when
            ResultActions perform = mockMvc.perform(
                    post("/auth/logout")
                            .cookie(
                                    MockCookie.parse(logoutRequestCookie.toString())
                            )
            );

            // then
            perform.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.NO_PARAM_TOKEN.getCode()))
                    .andExpect(jsonPath("$.data.message").value(ErrorCode.NO_PARAM_TOKEN.getMessage()));

            // docs
            perform.andDo(
                    restDocs.document(
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                            )
                    )
            );
        }

        @Test
        @DisplayName("accessToken??? ?????????????????? ????????? 401 ????????????. ErrorCode.INVALID_ACCESS_TOKEN")
        void accessTokenInBlackList() throws Exception {
            // given
            long userId = 1L;
            JwtTokenInfo accessTokenInfo = generateAccessToken(userId, "USER_ROLE", Instant.now(), Instant.now().plusSeconds(300));
            String accessToken = accessTokenInfo.getValue();
            String redirectUri = "http://localhost:3000";

            UserLogoutRequest userLogoutRequest = new UserLogoutRequest(accessToken, redirectUri);
            ResponseCookie logoutRequestCookie = ResponseCookie.from(COOKIE_NAME_USER_LOGOUT_REQUEST, CookieUtil.serialize(userLogoutRequest))
                    .path("/")
                    .domain(SERVER_DOMAIN)
                    .maxAge(60)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite(org.springframework.boot.web.server.Cookie.SameSite.NONE.attributeValue())
                    .build();

            redisRepository.addBlackList(accessToken, Duration.between(Instant.now(), Instant.now().plusSeconds(60)));

            // when
            ResultActions perform = mockMvc.perform(
                    post("/auth/logout")
                            .cookie(
                                    MockCookie.parse(logoutRequestCookie.toString())
                            )
            );

            // then
            perform.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.INVALID_ACCESS_TOKEN.getCode()))
                    .andExpect(jsonPath("$.data.message").value(ErrorCode.INVALID_ACCESS_TOKEN.getMessage()));

            // docs
            perform.andDo(
                    restDocs.document(
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                            )
                    )
            );
        }

        @Test
        @DisplayName("accessToken ????????? ???????????? 401 ????????????. ErrorCode.INVALID_ACCESS_TOKEN")
        void validFailAccessToken() throws Exception {
            // given
            long userId = 1L;
            String accessToken = "aewhfbajwefbajewhbfawekjfhbaejkwfhbajkehfbk";
            String redirectUri = "http://localhost:3000";

            UserLogoutRequest userLogoutRequest = new UserLogoutRequest(accessToken, redirectUri);
            ResponseCookie logoutRequestCookie = ResponseCookie.from(COOKIE_NAME_USER_LOGOUT_REQUEST, CookieUtil.serialize(userLogoutRequest))
                    .path("/")
                    .domain(SERVER_DOMAIN)
                    .maxAge(60)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite(org.springframework.boot.web.server.Cookie.SameSite.NONE.attributeValue())
                    .build();

            // when
            ResultActions perform = mockMvc.perform(
                    post("/auth/logout")
                            .cookie(
                                    MockCookie.parse(logoutRequestCookie.toString())
                            )
            );

            // then
            perform.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.INVALID_ACCESS_TOKEN.getCode()))
                    .andExpect(jsonPath("$.data.message").value(ErrorCode.INVALID_ACCESS_TOKEN.getMessage()));

            // docs
            perform.andDo(
                    restDocs.document(
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                            )
                    )
            );
        }
    }
}