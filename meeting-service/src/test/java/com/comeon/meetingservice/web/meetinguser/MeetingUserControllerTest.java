package com.comeon.meetingservice.web.meetinguser;

import com.comeon.meetingservice.common.exception.CustomException;
import com.comeon.meetingservice.common.exception.ErrorCode;
import com.comeon.meetingservice.domain.meeting.entity.MeetingRole;
import com.comeon.meetingservice.domain.meetinguser.dto.MeetingUserAddDto;
import com.comeon.meetingservice.domain.meetinguser.dto.MeetingUserModifyDto;
import com.comeon.meetingservice.domain.meetinguser.service.MeetingUserService;
import com.comeon.meetingservice.web.ControllerTestBase;
import com.comeon.meetingservice.web.common.response.ApiResponseCode;
import com.comeon.meetingservice.web.meetinguser.request.MeetingUserAddRequest;
import com.comeon.meetingservice.web.meetinguser.request.MeetingUserModifyRequest;
import com.comeon.meetingservice.web.meetinguser.response.MeetingUserAddResponse;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.beneathPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class MeetingUserControllerTest extends ControllerTestBase {

    @Nested
    @DisplayName("???????????? ??????")
    class ?????????????????? {

        @Nested
        @DisplayName("????????????")
        class ???????????? {

            @Test
            @DisplayName("????????? ????????? ????????? ????????? ????????? ?????? Created??? ????????????.")
            public void ????????????() throws Exception {

                String unexpiredInviteCode = "AAAAAA";
                Long unJoinedUserId = 10L;
                String unJoinedUserToken = createToken(unJoinedUserId);

                // ????????? ?????? ???????????? ??????
                MeetingUserAddDto meetingUserAddDto = MeetingUserAddDto.builder()
                        .inviteCode(unexpiredInviteCode)
                        .userId(unJoinedUserId)
                        .build();

                Long meetingId = 15L;
                Long createdMeetingUserId = 20L;
                given(meetingUserService.add(refEq(meetingUserAddDto))).willReturn(createdMeetingUserId);
                given(meetingUserQueryService.getMeetingIdAndUserId(createdMeetingUserId))
                        .willReturn(
                                MeetingUserAddResponse.builder()
                                        .meetingId(meetingId)
                                        .meetingUserId(createdMeetingUserId)
                                        .build()
                        );

                // ?????? ?????????
                MeetingUserAddRequest meetingUserAddRequest =
                        MeetingUserAddRequest.builder()
                                .inviteCode(unexpiredInviteCode)
                                .build();

                mockMvc.perform(post("/meetings/users")
                                .header("Authorization", unJoinedUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingUserAddRequest))
                        )

                        .andExpect(status().isCreated())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))
                        .andExpect(jsonPath("$.data.meetingId", equalTo(meetingId), Long.class))
                        .andExpect(jsonPath("$.data.meetingUserId", equalTo(createdMeetingUserId), Long.class))

                        .andDo(document("user-create-normal",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                requestFields(
                                        fieldWithPath("inviteCode").description("????????? ?????? ??????").attributes(key("format").value("[?????? ?????????], [??????], [?????? ????????? + ?????? ??????] ????????? 6??????"))
                                ))
                        )
                ;
            }
        }

        @Nested
        @DisplayName("??????")
        class ?????? {

            @Test
            @DisplayName("??????????????? ?????? ????????? ??????Bad Request??? ????????????.")
            public void ??????_????????????() throws Exception {

                String expiredInviteCode = "BBBBBB";
                Long unJoinedUserId = 10L;
                String unJoinedUserToken = createToken(unJoinedUserId);

                // ????????? ???????????? ?????? ???????????? ??????
                MeetingUserAddDto meetingUserAddDto = MeetingUserAddDto.builder()
                        .inviteCode(expiredInviteCode)
                        .userId(unJoinedUserId)
                        .build();

                willThrow(new CustomException("???????????? ??????", ErrorCode.EXPIRED_CODE))
                        .given(meetingUserService).add(refEq(meetingUserAddDto));

                MeetingUserAddRequest meetingUserAddRequest =
                        MeetingUserAddRequest.builder()
                                .inviteCode(expiredInviteCode)
                                .build();

                mockMvc.perform(post("/meetings/users")
                                .header("Authorization", unJoinedUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingUserAddRequest))
                        )

                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.BAD_PARAMETER.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.EXPIRED_CODE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.EXPIRED_CODE.getMessage())))

                        .andDo(document("user-create-error-expired-code",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                requestFields(
                                        fieldWithPath("inviteCode").description("????????? ?????? ??????")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }


            @Test
            @DisplayName("?????? ????????? ?????? ????????? ?????? ??????Bad Request??? ????????????.")
            public void ??????_??????????????????() throws Exception {

                String nonexistentCode = "CCCCCC";
                Long unJoinedUserId = 10L;
                String unJoinedUserToken = createToken(unJoinedUserId);

                // ????????? ???????????? ?????? ???????????? ??????
                MeetingUserAddDto meetingUserAddDto = MeetingUserAddDto.builder()
                        .inviteCode(nonexistentCode)
                        .userId(unJoinedUserId)
                        .build();

                willThrow(new CustomException("?????? ????????? ?????? ????????? ??????", ErrorCode.NONEXISTENT_CODE))
                        .given(meetingUserService).add(refEq(meetingUserAddDto));

                MeetingUserAddRequest meetingUserAddRequest =
                        MeetingUserAddRequest.builder()
                                .inviteCode(nonexistentCode)
                                .build();

                mockMvc.perform(post("/meetings/users")
                                .header("Authorization", unJoinedUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingUserAddRequest))
                        )

                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.BAD_PARAMETER.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.NONEXISTENT_CODE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.NONEXISTENT_CODE.getMessage())))

                        .andDo(document("user-create-error-nonexistent-code",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                requestFields(
                                        fieldWithPath("inviteCode").description("????????? ?????? ??????")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }


            @Test
            @DisplayName("?????? ????????? ????????? ????????? ??????Bad Request??? ????????????.")
            public void ??????_?????????????????????() throws Exception {

                String unexpiredInviteCode = "AAAAAA";
                Long joinedUserId = 20L;
                String joinedUserToken = createToken(joinedUserId);

                // ????????? ???????????? ?????? ???????????? ??????
                MeetingUserAddDto meetingUserAddDto = MeetingUserAddDto.builder()
                        .inviteCode(unexpiredInviteCode)
                        .userId(joinedUserId)
                        .build();

                willThrow(new CustomException("?????? ????????? ????????? ?????????", ErrorCode.USER_ALREADY_PARTICIPATE))
                        .given(meetingUserService).add(refEq(meetingUserAddDto));

                MeetingUserAddRequest meetingUserAddRequest =
                        MeetingUserAddRequest.builder()
                                .inviteCode(unexpiredInviteCode)
                                .build();

                mockMvc.perform(post("/meetings/users")
                                .header("Authorization", joinedUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingUserAddRequest))
                        )

                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.BAD_PARAMETER.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.USER_ALREADY_PARTICIPATE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.USER_ALREADY_PARTICIPATE.getMessage())))

                        .andDo(document("user-create-error-already-participate",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                requestFields(
                                        fieldWithPath("inviteCode").description("????????? ?????? ??????")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ?????? ????????? ?????? ???????????? ????????? ????????? ?????? ?????????Bad Request??? ????????????.")
            public void ??????_???????????????() throws Exception {

                String invalidInviteCode = "AAA";
                Long unJoinedUserId = 20L;
                String unJoinedUserToken = createToken(unJoinedUserId);

                MeetingUserAddRequest meetingUserAddRequest =
                        MeetingUserAddRequest.builder()
                                .inviteCode(invalidInviteCode)
                                .build();

                mockMvc.perform(post("/meetings/users")
                                .header("Authorization", unJoinedUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingUserAddRequest))
                        )

                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.BAD_PARAMETER.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.VALIDATION_FAIL.getCode())))

                        .andDo(document("user-create-error-param",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                requestFields(
                                        fieldWithPath("inviteCode").description("????????? ?????? ??????")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.OBJECT).description("?????? ?????????"),
                                        fieldWithPath("message.inviteCode").type(JsonFieldType.ARRAY).description("????????? ????????? ??????")
                                ))
                        )
                ;
            }
        }
    }

    @Nested
    @DisplayName("???????????? ??????")
    class ?????????????????? {

        @Nested
        @DisplayName("????????????")
        class ???????????? {

            @Test
            @DisplayName("?????? ID??? ?????? ?????? ID??? ??????????????? OK??? ????????????.")
            public void ????????????() throws Exception {

                MeetingRole modifyingRole = MeetingRole.EDITOR;
                MeetingUserModifyDto meetingUserModifyDto = MeetingUserModifyDto.builder()
                        .id(mockedParticipantUserId)
                        .meetingId(mockedExistentMeetingId)
                        .meetingRole(modifyingRole)
                        .build();

                // ????????? ?????? ???????????? ?????? (?????? ???????????? ????????? ??????)
                willDoNothing().given(meetingUserService).modify(refEq(meetingUserModifyDto));

                // ?????? ?????????
                MeetingUserModifyRequest meetingUserModifyRequest = MeetingUserModifyRequest.builder()
                        .meetingRole(modifyingRole)
                        .build();

                String hostUserToken = createToken(mockedHostUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/users/{userId}", mockedExistentMeetingId, mockedParticipantUserId)
                                .header("Authorization", hostUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingUserModifyRequest))
                        )

                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))

                        .andDo(document("user-modify-normal",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????????????? ?????? ????????? ???????????? ????????? ID"),
                                        parameterWithName("userId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("meetingRole").description("????????? ????????? ??????").attributes(key("format").value("EDITOR, PARTICIPANT"))
                                ))
                        )
                ;
            }
        }

        @Nested
        @DisplayName("??????")
        class ?????? {

            @Test
            @DisplayName("??????????????? ????????? HOST??? ??????Bad Request??? ????????????.")
            public void ??????_HOST?????????() throws Exception {

                MeetingRole modifyingRole = MeetingRole.HOST;
                MeetingUserModifyDto meetingUserModifyDto = MeetingUserModifyDto.builder()
                        .id(mockedParticipantUserId)
                        .meetingId(mockedExistentMeetingId)
                        .meetingRole(modifyingRole)
                        .build();

                // ????????? ?????? ???????????? ??????
                willThrow(new CustomException("HOST??? ????????? ???????????? ??????", ErrorCode.MODIFY_HOST_NOT_SUPPORT))
                        .given(meetingUserService).modify(refEq(meetingUserModifyDto));

                // ?????? ?????????
                MeetingUserModifyRequest meetingUserModifyRequest = MeetingUserModifyRequest.builder()
                        .meetingRole(modifyingRole)
                        .build();

                String hostUserToken = createToken(mockedHostUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/users/{userId}", mockedExistentMeetingId, mockedParticipantUserId)
                                .header("Authorization", hostUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingUserModifyRequest))
                        )

                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.BAD_PARAMETER.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.MODIFY_HOST_NOT_SUPPORT.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.MODIFY_HOST_NOT_SUPPORT.getMessage())))

                        .andDo(document("user-modify-error-modifying-host",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????????????? ?????? ????????? ???????????? ????????? ID"),
                                        parameterWithName("userId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("meetingRole").description("????????? ????????? ??????").attributes(key("format").value("EDITOR, PARTICIPANT"))
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("HOST??? ????????? ????????? ??????Bad Request??? ????????????.")
            public void ??????_HOST?????????() throws Exception {

                MeetingRole modifyingRole = MeetingRole.PARTICIPANT;
                MeetingUserModifyDto meetingUserModifyDto = MeetingUserModifyDto.builder()
                        .id(mockedHostUserId)
                        .meetingId(mockedExistentMeetingId)
                        .meetingRole(modifyingRole)
                        .build();

                // ????????? ?????? ???????????? ??????
                willThrow(new CustomException("HOST????????? ????????? ?????? ?????????", ErrorCode.MODIFY_HOST_IMPOSSIBLE))
                        .given(meetingUserService).modify(refEq(meetingUserModifyDto));

                // ?????? ?????????
                MeetingUserModifyRequest meetingUserModifyRequest = MeetingUserModifyRequest.builder()
                        .meetingRole(modifyingRole)
                        .build();

                String hostUserToken = createToken(mockedHostUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/users/{userId}", mockedExistentMeetingId, mockedHostUserId)
                                .header("Authorization", hostUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingUserModifyRequest))
                        )

                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.BAD_PARAMETER.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.MODIFY_HOST_IMPOSSIBLE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.MODIFY_HOST_IMPOSSIBLE.getMessage())))

                        .andDo(document("user-modify-error-host-modified",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????????????? ?????? ????????? ???????????? ????????? ID"),
                                        parameterWithName("userId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("meetingRole").description("????????? ????????? ??????").attributes(key("format").value("EDITOR, PARTICIPANT"))
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ????????? ?????? ????????????Bad Request??? ????????????.")
            public void ??????_?????????????????????() throws Exception {

                MeetingRole modifyingRole = MeetingRole.EDITOR;
                Long unJoinedUserId = 10L;
                MeetingUserModifyDto meetingUserModifyDto = MeetingUserModifyDto.builder()
                        .id(unJoinedUserId)
                        .meetingId(mockedExistentMeetingId)
                        .meetingRole(modifyingRole)
                        .build();

                // ????????? ?????? ???????????? ??????
                willThrow(new CustomException("?????? ?????? ???????????? ?????? ??? ??????", ErrorCode.ENTITY_NOT_FOUND))
                        .given(meetingUserService).modify(refEq(meetingUserModifyDto));

                // ?????? ?????????
                MeetingUserModifyRequest meetingUserModifyRequest = MeetingUserModifyRequest.builder()
                        .meetingRole(modifyingRole)
                        .build();

                String hostUserToken = createToken(mockedHostUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/users/{userId}", mockedExistentMeetingId, unJoinedUserId)
                                .header("Authorization", hostUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingUserModifyRequest))
                        )

                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("user-modify-error-user-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????????????? ?????? ????????? ???????????? ????????? ID"),
                                        parameterWithName("userId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("meetingRole").description("????????? ????????? ??????").attributes(key("format").value("EDITOR, PARTICIPANT"))
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("????????? ?????? ?????? ????????? ????????? Not Found??? ????????????.")
            public void ??????_???????????????() throws Exception {

                MeetingRole modifyingRole = MeetingRole.EDITOR;
                MeetingUserModifyDto meetingUserModifyDto = MeetingUserModifyDto.builder()
                        .id(mockedParticipantUserId)
                        .meetingId(mockedExistentMeetingId)
                        .meetingRole(modifyingRole)
                        .build();

                // ????????? ?????? ???????????? ??????
                willDoNothing().given(meetingUserService).modify(refEq(meetingUserModifyDto));

                // ?????? ?????????
                MeetingUserModifyRequest meetingUserModifyRequest = MeetingUserModifyRequest.builder()
                        .meetingRole(modifyingRole)
                        .build();

                String hostUserToken = createToken(mockedHostUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/users/{userId}", mockedNonexistentMeetingId, mockedParticipantUserId)
                                .header("Authorization", hostUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingUserModifyRequest))
                        )

                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("user-modify-error-meeting-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????????????? ?????? ????????? ???????????? ????????? ID"),
                                        parameterWithName("userId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("meetingRole").description("????????? ????????? ??????").attributes(key("format").value("EDITOR, PARTICIPANT"))
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("????????? ?????? ????????? ????????? ?????????????????? ????????? Forbidden??? ????????????.")
            public void ??????_???????????????() throws Exception {

                MeetingRole modifyingRole = MeetingRole.EDITOR;
                MeetingUserModifyDto meetingUserModifyDto = MeetingUserModifyDto.builder()
                        .id(mockedParticipantUserId)
                        .meetingId(mockedExistentMeetingId)
                        .meetingRole(modifyingRole)
                        .build();

                // ????????? ?????? ???????????? ??????
                willDoNothing().given(meetingUserService).modify(refEq(meetingUserModifyDto));

                // ?????? ?????????
                MeetingUserModifyRequest meetingUserModifyRequest = MeetingUserModifyRequest.builder()
                        .meetingRole(modifyingRole)
                        .build();

                String unJoinedUserToken = createToken(10L);

                mockMvc.perform(patch("/meetings/{meetingId}/users/{userId}", mockedExistentMeetingId, mockedParticipantUserId)
                                .header("Authorization", unJoinedUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingUserModifyRequest))
                        )

                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getMessage())))

                        .andDo(document("user-modify-error-not-joined",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????????????? ?????? ????????? ???????????? ????????? ID"),
                                        parameterWithName("userId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("meetingRole").description("????????? ????????? ??????").attributes(key("format").value("EDITOR, PARTICIPANT"))
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("????????? ?????? ????????? HOST??? ???????????? Forbidden??? ????????????.")
            public void ??????_????????????() throws Exception {

                MeetingRole modifyingRole = MeetingRole.EDITOR;
                MeetingUserModifyDto meetingUserModifyDto = MeetingUserModifyDto.builder()
                        .id(mockedParticipantUserId)
                        .meetingId(mockedExistentMeetingId)
                        .meetingRole(modifyingRole)
                        .build();

                // ????????? ?????? ???????????? ??????
                willDoNothing().given(meetingUserService).modify(refEq(meetingUserModifyDto));

                // ?????? ?????????
                MeetingUserModifyRequest meetingUserModifyRequest = MeetingUserModifyRequest.builder()
                        .meetingRole(modifyingRole)
                        .build();

                String editorUserToken = createToken(mockedEditorUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/users/{userId}", mockedExistentMeetingId, mockedParticipantUserId)
                                .header("Authorization", editorUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingUserModifyRequest))
                        )

                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.AUTHORIZATION_FAIL.getCode())))

                        .andDo(document("user-modify-error-authorization",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????????????? ?????? ????????? ???????????? ????????? ID"),
                                        parameterWithName("userId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("meetingRole").description("????????? ????????? ??????").attributes(key("format").value("EDITOR, PARTICIPANT"))
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }
        }
    }

}