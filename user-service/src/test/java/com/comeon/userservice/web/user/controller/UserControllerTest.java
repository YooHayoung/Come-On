package com.comeon.userservice.web.user.controller;

import com.comeon.userservice.common.exception.CustomException;
import com.comeon.userservice.common.exception.ErrorCode;
import com.comeon.userservice.docs.utils.RestDocsUtil;
import com.comeon.userservice.domain.common.exception.EntityNotFoundException;
import com.comeon.userservice.domain.user.entity.OAuthProvider;
import com.comeon.userservice.domain.user.entity.User;
import com.comeon.userservice.domain.user.entity.UserRole;
import com.comeon.userservice.domain.user.entity.UserStatus;
import com.comeon.userservice.domain.user.service.UserService;
import com.comeon.userservice.domain.user.service.dto.UserAccountDto;
import com.comeon.userservice.web.AbstractControllerTest;
import com.comeon.userservice.web.common.aop.ValidationAspect;
import com.comeon.userservice.web.common.response.ListResponse;
import com.comeon.userservice.web.feign.authservice.AuthFeignService;
import com.comeon.userservice.web.user.query.UserQueryService;
import com.comeon.userservice.web.user.request.UserModifyRequest;
import com.comeon.userservice.web.user.request.UserSaveRequest;
import com.comeon.userservice.web.user.response.UserDetailResponse;
import com.comeon.userservice.web.user.response.UserSimpleResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.snippet.Attributes.attributes;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@Import({
        AopAutoConfiguration.class,
        ValidationAspect.class
})
@WebMvcTest(UserController.class)
@MockBean(JpaMetamodelMappingContext.class)
public class UserControllerTest extends AbstractControllerTest {

    private static final String TOKEN_TYPE_BEARER = "Bearer ";

    @MockBean
    UserService userService;

    @MockBean
    UserQueryService userQueryService;

    @MockBean
    AuthFeignService authFeignService;

    @Nested
    @DisplayName("?????? ??????")
    class userSave {

        @Test
        @DisplayName("?????? ????????? ????????? ???????????? ?????? ????????? ????????????, ????????? ?????? ????????? ???????????? ????????????. ????????? ????????? ???????????? ????????? profileImg??? null ??????")
        void successWithNoProfileImg() throws Exception {
            // given
            String oauthId = "12345";
            String providerName = "kakao".toUpperCase();
            String name = "testName1";
            String email = "email1@email.com";
            UserSaveRequest userSaveRequest = new UserSaveRequest(
                    oauthId,
                    OAuthProvider.valueOf(providerName),
                    name,
                    email,
                    null
            );

            User user = setUser(oauthId, providerName, name, email, null);

            // mocking
            given(userService.saveUser(any(UserAccountDto.class)))
                    .willReturn(1L);
            given(userQueryService.getUserDetails(anyLong()))
                    .willReturn(new UserDetailResponse(user, null));

            // when
            ResultActions perform = mockMvc.perform(
                    post("/users")
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(userSaveRequest))
            );

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").isNotEmpty())
                    .andExpect(jsonPath("$.data.nickname").value(name))
                    .andExpect(jsonPath("$.data.role").value(UserRole.USER.getRoleValue()))
                    .andExpect(jsonPath("$.data.email").value(email))
                    .andExpect(jsonPath("$.data.name").value(name))
                    .andExpect(jsonPath("$.data.profileImg").isEmpty());

            // docs
            perform.andDo(
                    restDocs.document(
                            requestFields(
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("oauthId").type(JsonFieldType.STRING).description("?????? ????????? ?????????, ????????? ?????? ??????????????? ???????????? ?????? ID ???"),
                                    fieldWithPath("provider").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.OAUTH_PROVIDER)),
                                    fieldWithPath("email").type(JsonFieldType.STRING).description("?????? ????????? ??????"),
                                    fieldWithPath("name").type(JsonFieldType.STRING).description("?????? ?????? ?????? ????????? ??????"),
                                    fieldWithPath("profileImgUrl").type(JsonFieldType.STRING).description("?????? ????????? ????????? URL").optional()
                            ),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("userId").type(JsonFieldType.NUMBER).description("????????? ????????? ?????????"),
                                    fieldWithPath("nickname").type(JsonFieldType.STRING).description("????????? ????????? ?????????"),
                                    fieldWithPath("name").type(JsonFieldType.STRING).description("????????? ????????? ?????? ??????"),
                                    fieldWithPath("email").type(JsonFieldType.STRING).description("????????? ????????? ?????? ?????????"),
                                    fieldWithPath("role").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.USER_ROLE)),
                                    subsectionWithPath("profileImg").type(JsonFieldType.OBJECT).description("????????? ????????? ?????????").optional(),
                                    subsectionWithPath("profileImg.id").type(JsonFieldType.NUMBER).description("????????? ???????????? ?????????").optional(),
                                    subsectionWithPath("profileImg.imageUrl").type(JsonFieldType.STRING).description("????????? ???????????? URL").optional()
                            )
                    )
            );
        }

        @Test
        @DisplayName("?????? ????????? ????????? ???????????? ?????? ????????? ????????????, ????????? ?????? ????????? ???????????? ????????????. ????????? ???????????? ????????? ????????? ????????? ????????? ??????")
        void successWithProfileImg() throws Exception {
            // given
            String oauthId = "12345";
            String providerName = "kakao".toUpperCase();
            String name = "testName1";
            String email = "email1@email.com";
            UserSaveRequest userSaveRequest = new UserSaveRequest(
                    oauthId,
                    OAuthProvider.valueOf(providerName),
                    name,
                    email,
                    null
            );

            User user = setUser(oauthId, providerName, name, email, null);
            setProfileImg(user);

            // mocking
            given(userService.saveUser(any(UserAccountDto.class)))
                    .willReturn(1L);
            String fileUrl = fileManager.getFileUrl(user.getProfileImg().getStoredName(), dirName);
            given(userQueryService.getUserDetails(anyLong()))
                    .willReturn(new UserDetailResponse(user, fileUrl));

            // when
            ResultActions perform = mockMvc.perform(
                    post("/users")
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(userSaveRequest))
            );

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").isNotEmpty())
                    .andExpect(jsonPath("$.data.nickname").value(name))
                    .andExpect(jsonPath("$.data.role").value(UserRole.USER.getRoleValue()))
                    .andExpect(jsonPath("$.data.email").value(email))
                    .andExpect(jsonPath("$.data.name").value(name))
                    .andExpect(jsonPath("$.data.profileImg").isNotEmpty())
                    .andExpect(jsonPath("$.data.profileImg.id").isNotEmpty())
                    .andExpect(jsonPath("$.data.profileImg.imageUrl").isNotEmpty());

            // docs
            perform.andDo(
                    restDocs.document(
                            requestFields(
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("oauthId").type(JsonFieldType.STRING).description("?????? ????????? ?????????, ????????? ?????? ??????????????? ???????????? ?????? ID ???"),
                                    fieldWithPath("provider").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.OAUTH_PROVIDER)),
                                    fieldWithPath("email").type(JsonFieldType.STRING).description("?????? ????????? ??????"),
                                    fieldWithPath("name").type(JsonFieldType.STRING).description("?????? ?????? ?????? ????????? ??????"),
                                    fieldWithPath("profileImgUrl").type(JsonFieldType.STRING).description("?????? ????????? ????????? URL").optional()
                            ),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("userId").type(JsonFieldType.NUMBER).description("????????? ????????? ?????????"),
                                    fieldWithPath("nickname").type(JsonFieldType.STRING).description("????????? ????????? ?????????"),
                                    fieldWithPath("name").type(JsonFieldType.STRING).description("????????? ????????? ?????? ??????"),
                                    fieldWithPath("email").type(JsonFieldType.STRING).description("????????? ????????? ?????? ?????????"),
                                    fieldWithPath("role").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.USER_ROLE)),
                                    subsectionWithPath("profileImg").type(JsonFieldType.OBJECT).description("????????? ????????? ?????????").optional(),
                                    subsectionWithPath("profileImg.id").type(JsonFieldType.NUMBER).description("????????? ???????????? ?????????").optional(),
                                    subsectionWithPath("profileImg.imageUrl").type(JsonFieldType.STRING).description("????????? ???????????? URL").optional()
                            )
                    )
            );
        }

        @Test
        @DisplayName("?????? ????????? ????????? ???????????? http status 400 ????????????.")
        void validationFail() throws Exception {
            // given
            UserSaveRequest request = new UserSaveRequest();

            // when
            ResultActions perform = mockMvc.perform(
                    post("/users")
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.VALIDATION_FAIL.getCode()))
                    .andExpect(jsonPath("$.data.message").isNotEmpty());

            // docs
            perform.andDo(
                    restDocs.document(
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    subsectionWithPath("message").type(JsonFieldType.OBJECT).description("API ?????? ?????????")
                            )
                    )
            );
        }
    }

    @Nested
    @DisplayName("?????? ?????? ??????")
    class userDetails {

        @Test
        @DisplayName("???????????? ????????? ??????????????? ???????????? ?????? ????????? ACTIVATE ????????????, ????????? id, nickname, profileImgUrl, status ????????? ????????????.")
        void activateUser() throws Exception {
            // given
            User user = setUser();
            setProfileImg(user);

            Long userId = user.getId();

            String fileUrl = fileManager.getFileUrl(user.getProfileImg().getStoredName(), dirName);

            // mocking
            given(userQueryService.getUserSimple(userId))
                    .willReturn(new UserSimpleResponse(user, fileUrl));

            // when
            String path = "/users/{userId}";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path, userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(userId))
                    .andExpect(jsonPath("$.data.nickname").value(user.getNickname()))
                    .andExpect(jsonPath("$.data.profileImgUrl").isNotEmpty())
                    .andExpect(jsonPath("$.data.status").value(UserStatus.ACTIVATE.name()));

            // docs
            perform.andDo(
                    restDocs.document(
                            pathParameters(
                                    attributes(key("title").value(path)),
                                    parameterWithName("userId").description("????????? ????????? ?????????")
                            ),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("userId").type(JsonFieldType.NUMBER).description("?????? ?????????"),
                                    fieldWithPath("nickname").type(JsonFieldType.STRING).description("?????? ?????????").optional(),
                                    fieldWithPath("profileImgUrl").type(JsonFieldType.STRING).description("????????? ?????? ????????? ?????????").optional(),
                                    fieldWithPath("status").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.USER_STATUS))
                            )
                    )
            );
        }

        @Test
        @DisplayName("ACTIVATE ????????? ????????? ???????????? ?????? ??? ??????.")
        void activateUserWithNoProfileImg() throws Exception {
            // given
            User user = setUser();

            Long userId = user.getId();

            // mocking
            given(userQueryService.getUserSimple(userId))
                    .willReturn(new UserSimpleResponse(user, null));

            // when
            String path = "/users/{userId}";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path, userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(userId))
                    .andExpect(jsonPath("$.data.nickname").value(user.getNickname()))
                    .andExpect(jsonPath("$.data.profileImgUrl").isEmpty())
                    .andExpect(jsonPath("$.data.status").value(UserStatus.ACTIVATE.name()));

            // docs
            perform.andDo(
                    restDocs.document(
                            pathParameters(
                                    attributes(key("title").value(path)),
                                    parameterWithName("userId").description("????????? ????????? ?????????")
                            ),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("userId").type(JsonFieldType.NUMBER).description("?????? ?????????"),
                                    fieldWithPath("nickname").type(JsonFieldType.STRING).description("?????? ?????????").optional(),
                                    fieldWithPath("profileImgUrl").type(JsonFieldType.STRING).description("????????? ?????? ????????? ?????????").optional(),
                                    fieldWithPath("status").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.USER_STATUS))
                            )
                    )
            );
        }

        @Test
        @DisplayName("???????????? ????????? ??????????????? ???????????? ?????? ????????? WITHDRAW ????????????, ????????? id, status ????????? ????????????. nickname, profileImgUrl ????????? null")
        void withDrawnUser() throws Exception {
            // given
            User user = setUser();
            user.withdrawal();

            Long userId = user.getId();

            // mocking
            given(userQueryService.getUserSimple(userId))
                    .willReturn(new UserSimpleResponse(user));

            // when
            String path = "/users/{userId}";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path, userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(userId))
                    .andExpect(jsonPath("$.data.nickname").isEmpty())
                    .andExpect(jsonPath("$.data.profileImgUrl").isEmpty())
                    .andExpect(jsonPath("$.data.status").value(UserStatus.WITHDRAWN.name()));

            // docs
            perform.andDo(
                    restDocs.document(
                            pathParameters(
                                    attributes(key("title").value(path)),
                                    parameterWithName("userId").description("????????? ????????? ?????????")
                            ),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("userId").type(JsonFieldType.NUMBER).description("?????? ?????????"),
                                    fieldWithPath("nickname").type(JsonFieldType.STRING).description("?????? ?????????").optional(),
                                    fieldWithPath("profileImgUrl").type(JsonFieldType.STRING).description("????????? ?????? ????????? ?????????").optional(),
                                    fieldWithPath("status").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.USER_STATUS))
                            )
                    )
            );
        }

        @Test
        @DisplayName("???????????? ?????? ????????? ??????????????? ????????????, http status 400 ????????????.")
        void invalidUser() throws Exception {
            // give
            Long invaildUserId = 100L;

            // mocking
            given(userQueryService.getUserSimple(invaildUserId))
                    .willThrow(new EntityNotFoundException("?????? ???????????? ?????? User??? ????????????. ????????? User ????????? : " + invaildUserId));

            // when
            String path = "/users/{userId}";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path, invaildUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            // then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.ENTITY_NOT_FOUND.getCode()))
                    .andExpect(jsonPath("$.data.message").value(ErrorCode.ENTITY_NOT_FOUND.getMessage()));

            // docs
            perform.andDo(
                    restDocs.document(
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                            )
                    )
            );
        }
    }

    @Nested
    @DisplayName("?????? ????????? ??????")
    class userList {

        @Test
        @DisplayName("?????? ????????? ????????? ????????????.")
        void success() throws Exception {
            // given
            List<User> userList = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                User user = setUser();
                setProfileImg(user);

                if (i % 5 == 0) {
                    user.withdrawal();
                }
                userList.add(user);
            }

            List<Long> userIds = userList.stream()
                    .map(User::getId)
                    .collect(Collectors.toList());

            given(userQueryService.getUserList(userIds))
                    .will(invocation -> ListResponse.toListResponse(
                                    userList.stream()
                                            .map(user -> {
                                                        if (user.isActivateUser()) {
                                                            return UserSimpleResponse.activateUserResponseBuilder()
                                                                    .user(user)
                                                                    .profileImgUrl(fileManager.getFileUrl(user.getProfileImg().getStoredName(), dirName))
                                                                    .build();
                                                        }
                                                        return UserSimpleResponse.withdrawnUserResponseBuilder()
                                                                .user(user)
                                                                .build();
                                                    }
                                            ).collect(Collectors.toList())
                            )
                    );

            // when
            String params = userIds.stream().map(Object::toString).collect(Collectors.joining(","));
            ResultActions perform = mockMvc.perform(
                    MockMvcRequestBuilders.get("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .queryParam("userIds", params)
            );

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.count").value(userIds.size()))
                    .andExpect(jsonPath("$.data.contents").isNotEmpty());

            // docs
            perform.andDo(
                    restDocs.document(
                            requestParameters(
                                    attributes(key("title").value("?????? ????????????")),
                                    parameterWithName("userIds").description("????????? ?????? ????????? ?????????")
                            ),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("count").type(JsonFieldType.NUMBER).description("????????? ????????? ???"),
                                    subsectionWithPath("contents").type(JsonFieldType.ARRAY).description("????????? ?????? ?????? ?????????")
                            ),
                            responseFields(
                                    beneathPath("data.contents").withSubsectionId("contents"),
                                    attributes(key("title").value("contents ?????? ??????")),
                                    fieldWithPath("userId").type(JsonFieldType.NUMBER).description("????????? ?????????"),
                                    fieldWithPath("nickname").type(JsonFieldType.STRING).description("????????? ?????????").optional(),
                                    fieldWithPath("profileImgUrl").type(JsonFieldType.STRING).description("????????? ????????? ????????? URL").optional(),
                                    fieldWithPath("status").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.USER_STATUS))
                            )
                    )
            );
        }

        @Test
        @DisplayName("???????????? ?????? ????????? ???????????? ???????????? ????????????.")
        void ignoreNotExistUserIds() throws Exception {
            // given
            List<User> userList = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                User user = setUser();
                setProfileImg(user);

                if (i % 5 == 0) {
                    user.withdrawal();
                }
                userList.add(user);
            }

            List<Long> userIds = userList.stream()
                    .map(User::getId)
                    .collect(Collectors.toList());

            given(userQueryService.getUserList(userIds))
                    .will(invocation -> ListResponse.toListResponse(
                                    userList.stream()
                                            .map(user -> {
                                                        if (user.isActivateUser()) {
                                                            return UserSimpleResponse.activateUserResponseBuilder()
                                                                    .user(user)
                                                                    .profileImgUrl(fileManager.getFileUrl(user.getProfileImg().getStoredName(), dirName))
                                                                    .build();
                                                        }
                                                        return UserSimpleResponse.withdrawnUserResponseBuilder()
                                                                .user(user)
                                                                .build();
                                                    }
                                            ).collect(Collectors.toList())
                            )
                    );

            // when
            List<Long> invalidUserIds = List.of(100L, 200L);
            userIds.addAll(invalidUserIds);
            String params = userIds.stream().map(Object::toString).collect(Collectors.joining(","));
            ResultActions perform = mockMvc.perform(
                    MockMvcRequestBuilders.get("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .queryParam("userIds", params)
            );

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.count").value(userIds.size() - invalidUserIds.size()))
                    .andExpect(jsonPath("$.data.contents").isNotEmpty());

            // docs
            perform.andDo(
                    restDocs.document(
                            requestParameters(
                                    attributes(key("title").value("?????? ????????????")),
                                    parameterWithName("userIds").description("????????? ?????? ????????? ?????????")
                            ),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("count").type(JsonFieldType.NUMBER).description("????????? ????????? ???"),
                                    subsectionWithPath("contents").type(JsonFieldType.ARRAY).description("????????? ?????? ?????? ?????????")
                            ),
                            responseFields(
                                    beneathPath("data.contents").withSubsectionId("contents"),
                                    attributes(key("title").value("contents ?????? ??????")),
                                    fieldWithPath("userId").type(JsonFieldType.NUMBER).description("????????? ?????????"),
                                    fieldWithPath("nickname").type(JsonFieldType.STRING).description("????????? ?????????").optional(),
                                    fieldWithPath("profileImgUrl").type(JsonFieldType.STRING).description("????????? ????????? ????????? URL").optional(),
                                    fieldWithPath("status").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.USER_STATUS))
                            )
                    )
            );
        }

        @Test
        @DisplayName("??????????????? ???????????? ???????????? ????????? ????????? ???????????? http status 400 ????????????.")
        void noParamsError() throws Exception {
            // given

            // when
            ResultActions perform = mockMvc.perform(
                    MockMvcRequestBuilders.get("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            // then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.VALIDATION_FAIL.getCode()))
                    .andExpect(jsonPath("$.data.message").isNotEmpty());

            // docs
            perform.andDo(
                    document(
                            "{class-name}/{method-name}",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
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

    @Nested
    @DisplayName("??? ?????? ??????")
    class myDetails {

        @Test
        @DisplayName("?????? ????????? ????????? ?????? ????????? ?????? ?????? ????????? ????????????.")
        void success() throws Exception {
            // given
            User user = setUser();
            setProfileImg(user);

            Long userId = user.getId();

            String fileUrl = fileManager.getFileUrl(user.getProfileImg().getStoredName(), dirName);

            String accessToken = generateUserAccessToken(userId);

            // mocking
            given(userQueryService.getUserDetails(userId))
                    .willReturn(new UserDetailResponse(user, fileUrl));

            // when
            ResultActions perform = mockMvc.perform(
                    get("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .header(HttpHeaders.AUTHORIZATION, TOKEN_TYPE_BEARER + accessToken)
            );

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(userId))
                    .andExpect(jsonPath("$.data.nickname").value(user.getNickname()))
                    .andExpect(jsonPath("$.data.role").value(user.getRole().getRoleValue()))
                    .andExpect(jsonPath("$.data.email").value(user.getAccount().getEmail()))
                    .andExpect(jsonPath("$.data.name").value(user.getAccount().getName()))
                    .andExpect(jsonPath("$.data.profileImg").isNotEmpty());

            // docs
            perform.andDo(
                    restDocs.document(
                            requestHeaders(
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(HttpHeaders.AUTHORIZATION).description("????????? ??? ?????? ???????????? ?????? ???????????? Bearer AccessToken")
                            ),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("userId").type(JsonFieldType.NUMBER).description("????????? ????????? ?????????"),
                                    fieldWithPath("nickname").type(JsonFieldType.STRING).description("????????? ????????? ?????????"),
                                    fieldWithPath("name").type(JsonFieldType.STRING).description("????????? ????????? ?????? ??????"),
                                    fieldWithPath("email").type(JsonFieldType.STRING).description("????????? ????????? ?????? ?????????"),
                                    fieldWithPath("role").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.USER_ROLE)),
                                    subsectionWithPath("profileImg").type(JsonFieldType.OBJECT).description("????????? ????????? ?????????").optional(),
                                    subsectionWithPath("profileImg.id").type(JsonFieldType.NUMBER).description("????????? ???????????? ?????????").optional(),
                                    subsectionWithPath("profileImg.imageUrl").type(JsonFieldType.STRING).description("????????? ???????????? URL").optional()
                            )
                    )
            );
        }
    }


    @Nested
    @DisplayName("?????? ?????? ??????")
    class userModify {

        @Test
        @DisplayName("?????? ?????? ????????? ???????????? ?????? ?????? ???????????? ????????????.")
        void success() throws Exception {
            // given
            User user = setUser();

            Long userId = user.getId();

            String newNickname = "??????????????????";
            UserModifyRequest request = new UserModifyRequest(newNickname);

            String accessToken = generateUserAccessToken(userId);

            // mocking
            willDoNothing().given(userService).modifyUser(eq(userId), any());

            // when
            ResultActions perform = mockMvc.perform(
                    patch("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .header(HttpHeaders.AUTHORIZATION, TOKEN_TYPE_BEARER + accessToken)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.message").isNotEmpty());

            // docs
            perform.andDo(
                    restDocs.document(
                            requestHeaders(
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(HttpHeaders.AUTHORIZATION).description("????????? ??? ?????? ???????????? ?????? ???????????? Bearer AccessToken")
                            ),
                            requestFields(
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("nickname").type(JsonFieldType.STRING).description("????????? ?????????")
                            ),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????? ?????? ?????????")
                            )
                    )
            );
        }

        @Test
        @DisplayName("?????? ????????? ????????? ???????????? http status 400 ????????????.")
        void validationFail() throws Exception {
            // given
            User user = setUser();
            Long userId = user.getId();
            String accessToken = generateUserAccessToken(userId);

            UserModifyRequest request = new UserModifyRequest();

            // when
            ResultActions perform = mockMvc.perform(
                    patch("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .header(HttpHeaders.AUTHORIZATION, TOKEN_TYPE_BEARER + accessToken)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.message").isNotEmpty());

            // docs
            perform.andDo(
                    restDocs.document(
                            requestHeaders(
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(HttpHeaders.AUTHORIZATION).description("????????? ??? ?????? ???????????? ?????? ???????????? Bearer AccessToken")
                            ),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    subsectionWithPath("message").type(JsonFieldType.OBJECT).description("API ?????? ?????????")
                            )
                    )
            );
        }
    }

    @Nested
    @DisplayName("?????? ??????")
    class userWithdraw {

        @Test
        @DisplayName("?????? ?????? ????????? ??????????????? ????????????, ?????? ?????? ?????? ???????????? ????????????.")
        void success() throws Exception {
            // given
            User user = setUser();
            Long userId = user.getId();
            String accessToken = generateUserAccessToken(userId);

            // mocking
            given(userQueryService.getUserOauthId(userId))
                    .willReturn(Long.parseLong(user.getAccount().getOauthId()));
            willDoNothing().given(userService).withdrawUser(userId);
            willDoNothing().given(authFeignService)
                    .userUnlink(anyString(), anyLong());

            // when
            ResultActions perform = mockMvc.perform(
                    delete("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
            );

            // then
            perform.andExpect(status().isOk());

            // docs
            perform.andDo(
                    restDocs.document(
                            requestHeaders(
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(HttpHeaders.AUTHORIZATION).description("????????? ??? ?????? ???????????? ?????? ???????????? Bearer AccessToken")
                            ),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????? ?????????")
                            )
                    )
            );
        }
        
        @Test
        @DisplayName("auth-service ?????? ?????? ?????? ?????? ?????? ?????? ?????????????????? http status 500 ??????")
        void authServiceError() throws Exception {
            // given
            User user = setUser();
            Long userId = user.getId();
            String accessToken = generateUserAccessToken(userId);
            
            // mocking
            given(userQueryService.getUserOauthId(userId))
                    .willReturn(Long.parseLong(user.getAccount().getOauthId()));
            willThrow(new CustomException("error", ErrorCode.AUTH_SERVICE_ERROR))
                    .given(authFeignService)
                    .userUnlink(anyString(), anyLong());

            // when
            ResultActions perform = mockMvc.perform(
                    delete("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
            );

            // then
            perform.andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.AUTH_SERVICE_ERROR.getCode()))
                    .andExpect(jsonPath("$.data.message").value(ErrorCode.AUTH_SERVICE_ERROR.getMessage()));

            // docs
            perform.andDo(
                    document(
                            "{class-name}/{method-name}",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    subsectionWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                            )
                    )
            );
        }

        @Test
        @DisplayName("????????? API ?????? ????????? ????????? http status 500 ????????????.")
        void kakaoApiError() throws Exception {
            // given
            User user = setUser();
            Long userId = user.getId();
            String accessToken = generateUserAccessToken(userId);

            // mocking
            given(userQueryService.getUserOauthId(userId))
                    .willReturn(Long.parseLong(user.getAccount().getOauthId()));
            willThrow(new CustomException("error", ErrorCode.KAKAO_API_ERROR))
                    .given(authFeignService)
                    .userUnlink(anyString(), anyLong());

            // when
            ResultActions perform = mockMvc.perform(
                    delete("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
            );

            // then
            perform.andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.KAKAO_API_ERROR.getCode()))
                    .andExpect(jsonPath("$.data.message").value(ErrorCode.KAKAO_API_ERROR.getMessage()));

            // docs
            perform.andDo(
                    document(
                            "{class-name}/{method-name}",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    subsectionWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                            )
                    )
            );
        }

        @Test
        @DisplayName("oauthId ?????? ?????? ????????? ErrorCode.SERVER_ERROR")
        void userServiceError() throws Exception {
            // given
            User user = setUser();
            Long userId = user.getId();
            String accessToken = generateUserAccessToken(userId);

            // mocking
            given(userQueryService.getUserOauthId(userId))
                    .willReturn(Long.parseLong(user.getAccount().getOauthId()));
            willThrow(new CustomException("error", ErrorCode.SERVER_ERROR))
                    .given(authFeignService)
                    .userUnlink(anyString(), anyLong());

            // when
            ResultActions perform = mockMvc.perform(
                    delete("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
            );

            // then
            perform.andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.SERVER_ERROR.getCode()))
                    .andExpect(jsonPath("$.data.message").value(ErrorCode.SERVER_ERROR.getMessage()));

            // docs
            perform.andDo(
                    document(
                            "{class-name}/{method-name}",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    subsectionWithPath("message").type(JsonFieldType.STRING).description("API ?????? ?????????")
                            )
                    )
            );
        }
    }
}
