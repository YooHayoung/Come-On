package com.comeon.meetingservice.web.meetingdate;

import com.comeon.meetingservice.common.exception.CustomException;
import com.comeon.meetingservice.common.exception.ErrorCode;
import com.comeon.meetingservice.domain.meeting.entity.MeetingRole;
import com.comeon.meetingservice.domain.meetingdate.dto.MeetingDateAddDto;
import com.comeon.meetingservice.domain.meetingdate.dto.MeetingDateModifyDto;
import com.comeon.meetingservice.domain.meetingdate.dto.MeetingDateRemoveDto;
import com.comeon.meetingservice.domain.meetingdate.entity.DateStatus;
import com.comeon.meetingservice.domain.meetingdate.service.MeetingDateService;
import com.comeon.meetingservice.web.ControllerTestBase;
import com.comeon.meetingservice.web.common.response.ApiResponseCode;
import com.comeon.meetingservice.web.meetingdate.query.MeetingDateQueryService;
import com.comeon.meetingservice.web.meetingdate.request.MeetingDateAddRequest;
import com.comeon.meetingservice.web.meetingdate.request.MeetingDateModifyRequest;
import com.comeon.meetingservice.web.meetingdate.response.MeetingDateDetailResponse;
import com.comeon.meetingservice.web.meetingdate.response.MeetingDateDetailUserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Attributes;

import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.beneathPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MeetingDateControllerTest extends ControllerTestBase {

    @Nested
    @DisplayName("???????????? ??????")
    class ?????????????????? {

        @Nested
        @DisplayName("????????????")
        class ???????????? {

            @Test
            @DisplayName("?????? ?????? ???????????? ????????? ?????? Created????????? ?????????, ?????? ????????? ?????? ID??? ????????????.")
            public void ??????_??????() throws Exception {

                LocalDate addedDate = LocalDate.of(2022, 06, 10);

                MeetingDateAddDto meetingDateAddDto = MeetingDateAddDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .userId(mockedParticipantUserId)
                        .date(addedDate)
                        .build();

                Long createdDateId = 10L;
                given(meetingDateService.add(refEq(meetingDateAddDto))).willReturn(createdDateId);

                MeetingDateAddRequest meetingDateAddRequest =
                        MeetingDateAddRequest.builder()
                                .date(addedDate)
                                .build();

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(post("/meetings/{meetingId}/dates", mockedExistentMeetingId)
                                .header("Authorization", participantUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingDateAddRequest))
                        )

                        .andExpect(status().isCreated())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))
                        .andExpect(jsonPath("$.data", equalTo(createdDateId), Long.class))

                        .andDo(document("date-create-normal",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("?????? ????????? ??????????????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("date").description("????????? ??????").attributes(key("format").value("yyyy-MM-dd"))
                                ))
                        )
                ;
            }
        }

        @Nested
        @DisplayName("??????")
        class ?????? {

            @Test
            @DisplayName("????????? ?????? ???????????? ????????? Bad Request??? ????????????.")
            public void ??????_????????????????????????() throws Exception {

                LocalDate notWithinPeriodDate = LocalDate.of(2022, 05, 10);

                MeetingDateAddDto meetingDateAddDto = MeetingDateAddDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .userId(mockedParticipantUserId)
                        .date(notWithinPeriodDate)
                        .build();

                willThrow(new CustomException("????????? ?????? ?????? ?????? ???????????? ??????", ErrorCode.DATE_NOT_WITHIN_PERIOD))
                        .given(meetingDateService).add(refEq(meetingDateAddDto));

                MeetingDateAddRequest meetingDateAddRequest =
                        MeetingDateAddRequest.builder()
                                .date(notWithinPeriodDate)
                                .build();

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(post("/meetings/{meetingId}/dates", mockedExistentMeetingId)
                                .header("Authorization", participantUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingDateAddRequest))
                        )

                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.BAD_PARAMETER.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.DATE_NOT_WITHIN_PERIOD.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.DATE_NOT_WITHIN_PERIOD.getMessage())))

                        .andDo(document("date-create-error-not-within",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("?????? ????????? ??????????????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("date").description("????????? ??????").attributes(key("format").value("yyyy-MM-dd"))
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }


            @Test
            @DisplayName("?????? ?????? ????????? ?????? ????????? ??????????????? Bad Request??? ????????????.")
            public void ??????_?????????????????????() throws Exception {

                LocalDate alreadySelectedDate = LocalDate.of(2022, 06, 30);

                MeetingDateAddDto meetingDateAddDto = MeetingDateAddDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .userId(mockedParticipantUserId)
                        .date(alreadySelectedDate)
                        .build();


                willThrow(new CustomException("?????? ????????? ????????? ??????", ErrorCode.USER_ALREADY_SELECT))
                        .given(meetingDateService).add(refEq(meetingDateAddDto));

                MeetingDateAddRequest meetingDateAddRequest =
                        MeetingDateAddRequest.builder()
                                .date(alreadySelectedDate)
                                .build();

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(post("/meetings/{meetingId}/dates", mockedExistentMeetingId)
                                .header("Authorization", participantUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingDateAddRequest))
                        )

                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.BAD_PARAMETER.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.USER_ALREADY_SELECT.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.USER_ALREADY_SELECT.getMessage())))

                        .andDo(document("date-create-error-already-select",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("?????? ????????? ??????????????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("date").description("????????? ??????").attributes(new Attributes.Attribute("format", "yyyy-MM-dd"))
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }


            @Test
            @DisplayName("?????? ???????????? ????????? ????????? ???????????? Bad Request??? ????????????.")
            public void ??????_?????????() throws Exception {

                LocalDate addedDate = LocalDate.of(2022, 06, 10);

                MeetingDateAddDto meetingDateAddDto = MeetingDateAddDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .userId(mockedParticipantUserId)
                        .date(addedDate)
                        .build();

                Long createdDateId = 10L;
                given(meetingDateService.add(refEq(meetingDateAddDto))).willReturn(createdDateId);

                Map<String, String> invalidFormatRequest = new HashMap<>();
                invalidFormatRequest.put("date", "123456");

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(post("/meetings/{meetingId}/dates", mockedExistentMeetingId)
                                .header("Authorization", participantUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(invalidFormatRequest))
                        )

                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.BAD_PARAMETER.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.HTTP_MESSAGE_NOT_READABLE.getCode())))

                        .andDo(document("date-create-error-param",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("?????? ????????? ??????????????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("date").description("????????? ??????").attributes(new Attributes.Attribute("format", "yyyy-MM-dd"))
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ????????? ?????? ????????? ?????? ?????? Not Found??? ????????????.")
            public void ??????_???????????????() throws Exception {

                LocalDate addedDate = LocalDate.of(2022, 06, 10);

                MeetingDateAddDto meetingDateAddDto = MeetingDateAddDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .userId(mockedParticipantUserId)
                        .date(addedDate)
                        .build();

                Long createdDateId = 10L;
                given(meetingDateService.add(refEq(meetingDateAddDto))).willReturn(createdDateId);

                MeetingDateAddRequest meetingDateAddRequest =
                        MeetingDateAddRequest.builder()
                                .date(addedDate)
                                .build();

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(post("/meetings/{meetingId}/dates", mockedNonexistentMeetingId)
                                .header("Authorization", participantUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingDateAddRequest))
                        )

                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("date-create-error-meeting-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("?????? ????????? ??????????????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("date").description("????????? ??????").attributes(new Attributes.Attribute("format", "yyyy-MM-dd"))
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ????????? ????????? ?????????????????? ????????? Forbidden??? ????????????.")
            public void ??????_???????????????() throws Exception {

                LocalDate addedDate = LocalDate.of(2022, 06, 10);

                MeetingDateAddDto meetingDateAddDto = MeetingDateAddDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .userId(mockedParticipantUserId)
                        .date(addedDate)
                        .build();

                Long createdDateId = 10L;
                given(meetingDateService.add(refEq(meetingDateAddDto))).willReturn(createdDateId);

                MeetingDateAddRequest meetingDateAddRequest =
                        MeetingDateAddRequest.builder()
                                .date(addedDate)
                                .build();

                String unJoinedUserToken = createToken(10L);

                mockMvc.perform(post("/meetings/{meetingId}/dates", mockedExistentMeetingId)
                                .header("Authorization", unJoinedUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingDateAddRequest))
                        )

                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getMessage())))

                        .andDo(document("date-create-error-not-joined",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("?????? ????????? ??????????????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("date").description("????????? ??????").attributes(new Attributes.Attribute("format", "yyyy-MM-dd"))
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

    @Nested
    @DisplayName("???????????? ??????")
    class ?????????????????? {

        @Nested
        @DisplayName("????????????")
        class ???????????? {


            @Test
            @DisplayName("?????? ?????? ???????????? ???????????? ????????? ????????? OK??? ????????????.")
            public void ??????_??????() throws Exception {

                Long existentDateId = 10L;
                DateStatus modifiedStatus = DateStatus.FIXED;

                MeetingDateModifyDto normalDto = MeetingDateModifyDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentDateId)
                        .dateStatus(modifiedStatus)
                        .build();

                willDoNothing().given(meetingDateService).modify(refEq(normalDto));

                MeetingDateModifyRequest meetingDateModifyRequest =
                        MeetingDateModifyRequest.builder()
                                .dateStatus(modifiedStatus)
                                .build();

                String hostUserToken = createToken(mockedHostUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/dates/{dateId}", mockedExistentMeetingId, existentDateId)
                                .header("Authorization", hostUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingDateModifyRequest))
                        )

                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))

                        .andDo(document("date-modify-normal",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ?????? ????????? ID"),
                                        parameterWithName("dateId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("dateStatus").description("????????? ????????? ??????").attributes(key("format").value("FIXED, UNFIXED"))
                                ))
                        )
                ;
            }

        }

        @Nested
        @DisplayName("??????")
        class ?????? {

            @Test
            @DisplayName("DateStatus ????????? ?????? ????????? Bad Request??? ????????????.")
            public void ??????_??????() throws Exception {

                Long existentDateId = 10L;
                DateStatus modifiedStatus = DateStatus.FIXED;

                MeetingDateModifyDto normalDto = MeetingDateModifyDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentDateId)
                        .dateStatus(modifiedStatus)
                        .build();

                willDoNothing().given(meetingDateService).modify(refEq(normalDto));

                Map<String, String> invalidFormatRequest = new HashMap<>();
                invalidFormatRequest.put("dateStatus", "xxx");

                String hostUserToken = createToken(mockedHostUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/dates/{dateId}", mockedExistentMeetingId, existentDateId)
                                .header("Authorization", hostUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(invalidFormatRequest))
                        )

                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.BAD_PARAMETER.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.HTTP_MESSAGE_NOT_READABLE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.HTTP_MESSAGE_NOT_READABLE.getMessage())))

                        .andDo(document("date-modify-error-format",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ?????? ????????? ID"),
                                        parameterWithName("dateId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("dateStatus").description("????????? ????????? ??????").attributes(key("format").value("FIXED, UNFIXED"))
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ????????? ?????? ????????? ?????? ?????? Not Found??? ????????????.")
            public void ??????_???????????????() throws Exception {

                Long existentDateId = 10L;
                DateStatus modifiedStatus = DateStatus.FIXED;

                MeetingDateModifyDto normalDto = MeetingDateModifyDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentDateId)
                        .dateStatus(modifiedStatus)
                        .build();

                willDoNothing().given(meetingDateService).modify(refEq(normalDto));

                MeetingDateModifyRequest meetingDateModifyRequest =
                        MeetingDateModifyRequest.builder()
                                .dateStatus(modifiedStatus)
                                .build();

                String hostUserToken = createToken(mockedHostUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/dates/{dateId}", mockedNonexistentMeetingId, existentDateId)
                                .header("Authorization", hostUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingDateModifyRequest))
                        )

                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("date-modify-error-meeting-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ?????? ????????? ID"),
                                        parameterWithName("dateId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("dateStatus").description("????????? ????????? ??????").attributes(key("format").value("FIXED, UNFIXED"))
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ????????? ?????? ????????? ?????? ?????? Not Found??? ????????????.")
            public void ??????_???????????????() throws Exception {

                Long nonExistentDateId = 20L;
                DateStatus modifiedStatus = DateStatus.FIXED;

                MeetingDateModifyDto nonExistentDto = MeetingDateModifyDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(nonExistentDateId)
                        .dateStatus(modifiedStatus)
                        .build();

                willThrow(new CustomException("?????? ID??? ???????????? ????????? ??????", ErrorCode.ENTITY_NOT_FOUND))
                        .given(meetingDateService).modify(refEq(nonExistentDto));

                MeetingDateModifyRequest meetingDateModifyRequest =
                        MeetingDateModifyRequest.builder()
                                .dateStatus(modifiedStatus)
                                .build();

                String hostUserToken = createToken(mockedHostUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/dates/{dateId}", mockedExistentMeetingId, nonExistentDateId)
                                .header("Authorization", hostUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingDateModifyRequest))
                        )

                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("date-modify-error-date-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ?????? ????????? ID"),
                                        parameterWithName("dateId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("dateStatus").description("????????? ????????? ??????").attributes(key("format").value("FIXED, UNFIXED"))
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ????????? ????????? ?????????????????? ????????? Forbidden??? ????????????.")
            public void ??????_???????????????() throws Exception {

                Long existentDateId = 10L;
                DateStatus modifiedStatus = DateStatus.FIXED;

                MeetingDateModifyDto normalDto = MeetingDateModifyDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentDateId)
                        .dateStatus(modifiedStatus)
                        .build();

                willDoNothing().given(meetingDateService).modify(refEq(normalDto));

                MeetingDateModifyRequest meetingDateModifyRequest =
                        MeetingDateModifyRequest.builder()
                                .dateStatus(modifiedStatus)
                                .build();

                String unJoinedUserToken = createToken(10L);

                mockMvc.perform(patch("/meetings/{meetingId}/dates/{dateId}", mockedExistentMeetingId, existentDateId)
                                .header("Authorization", unJoinedUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingDateModifyRequest))
                        )

                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getMessage())))

                        .andDo(document("date-modify-error-not-joined",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ?????? ????????? ID"),
                                        parameterWithName("dateId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("dateStatus").description("????????? ????????? ??????").attributes(key("format").value("FIXED, UNFIXED"))
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("HOST ????????? ?????? ????????? ????????? ?????? ?????? Forbidden??? ????????????.")
            public void ??????_????????????() throws Exception {

                Long existentDateId = 10L;
                DateStatus modifiedStatus = DateStatus.FIXED;

                MeetingDateModifyDto normalDto = MeetingDateModifyDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentDateId)
                        .dateStatus(modifiedStatus)
                        .build();

                willDoNothing().given(meetingDateService).modify(refEq(normalDto));

                MeetingDateModifyRequest meetingDateModifyRequest =
                        MeetingDateModifyRequest.builder()
                                .dateStatus(modifiedStatus)
                                .build();

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/dates/{dateId}", mockedExistentMeetingId, existentDateId)
                                .header("Authorization", participantUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingDateModifyRequest))
                        )

                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.AUTHORIZATION_FAIL.getCode())))

                        .andDo(document("date-modify-error-authorization",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ?????? ????????? ID"),
                                        parameterWithName("dateId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("dateStatus").description("????????? ????????? ??????").attributes(key("format").value("FIXED, UNFIXED"))
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

    @Nested
    @DisplayName("???????????? ??????")
    class ?????????????????? {

        @Nested
        @DisplayName("????????????")
        class ???????????? {

            @Test
            @DisplayName("??????????????? ?????? ????????? ??????????????? OK??? ????????????.")
            public void ??????_??????() throws Exception {

                Long existentDateId = 10L;

                MeetingDateRemoveDto normalDto = MeetingDateRemoveDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentDateId)
                        .userId(mockedParticipantUserId)
                        .build();

                willDoNothing().given(meetingDateService).remove(refEq(normalDto));

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(delete("/meetings/{meetingId}/dates/{dateId}", mockedExistentMeetingId, existentDateId)
                                .header("Authorization", participantUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                        )

                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))

                        .andDo(document("date-delete-normal",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ?????? ????????? ID"),
                                        parameterWithName("dateId").description("??????????????? ?????? ????????? ID")
                                )
                        ))
                ;
            }
        }

        @Nested
        @DisplayName("??????")
        class ?????? {

            @Test
            @DisplayName("????????? ?????? ????????? ???????????? ?????? ?????? Bad Request??? ????????????.")
            public void ??????_???????????????() throws Exception {

                Long existentDateId = 10L;
                Long notSelectedUserId = mockedParticipantUserId;

                MeetingDateRemoveDto notSelectedDto = MeetingDateRemoveDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentDateId)
                        .userId(notSelectedUserId)
                        .build();

                willThrow(new CustomException("????????? ????????? ???????????? ??????", ErrorCode.USER_NOT_SELECT_DATE))
                        .given(meetingDateService).remove(refEq(notSelectedDto));

                String notSelectedUserToken = createToken(notSelectedUserId);

                mockMvc.perform(delete("/meetings/{meetingId}/dates/{dateId}", mockedExistentMeetingId, existentDateId)
                                .header("Authorization", notSelectedUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                        )

                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.BAD_PARAMETER.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.USER_NOT_SELECT_DATE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.USER_NOT_SELECT_DATE.getMessage())))

                        .andDo(document("date-delete-error-not-selected",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ?????? ????????? ID"),
                                        parameterWithName("dateId").description("??????????????? ?????? ????????? ID")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ????????? ?????? ????????? ?????? ?????? Not Found??? ????????????.")
            public void ??????_???????????????() throws Exception {

                Long existentDateId = 10L;

                MeetingDateRemoveDto normalDto = MeetingDateRemoveDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentDateId)
                        .userId(mockedParticipantUserId)
                        .build();

                willDoNothing().given(meetingDateService).remove(refEq(normalDto));

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(delete("/meetings/{meetingId}/dates/{dateId}", mockedNonexistentMeetingId, existentDateId)
                                .header("Authorization", participantUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                        )

                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("date-delete-error-meeting-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ?????? ????????? ID"),
                                        parameterWithName("dateId").description("??????????????? ?????? ????????? ID")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ????????? ?????? ????????? ?????? ?????? Not Found??? ????????????.")
            public void ??????_???????????????() throws Exception {

                Long nonExistentDateId = 20L;

                MeetingDateRemoveDto nonexistentDto = MeetingDateRemoveDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(nonExistentDateId)
                        .userId(mockedParticipantUserId)
                        .build();

                willThrow(new CustomException("?????? ID??? ???????????? ????????? ??????", ErrorCode.ENTITY_NOT_FOUND))
                        .given(meetingDateService).remove(refEq(nonexistentDto));

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(delete("/meetings/{meetingId}/dates/{dateId}", mockedExistentMeetingId, nonExistentDateId)
                                .header("Authorization", participantUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                        )

                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("date-delete-error-date-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ?????? ????????? ID"),
                                        parameterWithName("dateId").description("??????????????? ?????? ????????? ID")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ????????? ????????? ?????????????????? ????????? Forbidden??? ????????????.")
            public void ??????_???????????????() throws Exception {

                Long existentDateId = 10L;

                MeetingDateRemoveDto normalDto = MeetingDateRemoveDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentDateId)
                        .userId(mockedParticipantUserId)
                        .build();

                willDoNothing().given(meetingDateService).remove(refEq(normalDto));

                String unJoinedUserToken = createToken(10L);

                mockMvc.perform(delete("/meetings/{meetingId}/dates/{dateId}", mockedExistentMeetingId, existentDateId)
                                .header("Authorization", unJoinedUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                        )

                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getMessage())))

                        .andDo(document("date-delete-error-not-joined",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ?????? ????????? ID"),
                                        parameterWithName("dateId").description("??????????????? ?????? ????????? ID")
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


    @Nested
    @DisplayName("?????????????????? - ??????")
    class ???????????????????????? {

        @Nested
        @DisplayName("????????????")
        class ???????????? {


            @Test
            @DisplayName("?????? ID??? ?????????????????? ????????? ????????? ????????? ?????? ???????????? ????????????.")
            public void ??????_??????() throws Exception {

                Long existentDateId = 10L;

                LocalDate resultDate = LocalDate.of(2022, 06, 30);
                Integer resultUserCount = 2;
                DateStatus resultDateStatus = DateStatus.UNFIXED;
                List<MeetingDateDetailUserResponse> returnDateUsers = new ArrayList<>();

                Long existentDateUserId1 = 10L;
                String returnImageLink1 = "imageLink1";
                String returnNickname1 = "nickname1";
                MeetingRole returnMeetingRole1 = MeetingRole.HOST;

                MeetingDateDetailUserResponse returnDateUser1 = MeetingDateDetailUserResponse.builder()
                        .id(existentDateUserId1)
                        .imageLink(returnImageLink1)
                        .nickname(returnNickname1)
                        .meetingRole(returnMeetingRole1)
                        .build();

                Long existentDateUserId2 = 20L;
                String returnImageLink2 = "imageLink2";
                String returnNickname2 = "nickname2";
                MeetingRole returnMeetingRole2 = MeetingRole.EDITOR;

                MeetingDateDetailUserResponse returnDateUser2 = MeetingDateDetailUserResponse.builder()
                        .id(existentDateUserId2)
                        .imageLink(returnImageLink2)
                        .nickname(returnNickname2)
                        .meetingRole(returnMeetingRole2)
                        .build();

                returnDateUsers.add(returnDateUser1);
                returnDateUsers.add(returnDateUser2);

                MeetingDateDetailResponse resultResponse = MeetingDateDetailResponse.builder()
                        .id(existentDateId)
                        .date(resultDate)
                        .userCount(resultUserCount)
                        .dateStatus(resultDateStatus)
                        .dateUsers(returnDateUsers)
                        .build();

                given(meetingDateQueryService.getDetail(mockedExistentMeetingId, existentDateId))
                        .willReturn(resultResponse);

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(get("/meetings/{meetingId}/dates/{dateId}", mockedExistentMeetingId, existentDateId)
                                .header("Authorization", participantUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                        )

                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))
                        .andExpect(jsonPath("$.data.id", equalTo(existentDateId), Long.class))
                        .andExpect(jsonPath("$.data.date", equalTo(resultDate.toString())))
                        .andExpect(jsonPath("$.data.userCount", equalTo(resultUserCount)))
                        .andExpect(jsonPath("$.data.dateStatus", equalTo(resultDateStatus.name())))
                        .andExpect(jsonPath("$.data.dateUsers[0].id", equalTo(existentDateUserId1), Long.class))
                        .andExpect(jsonPath("$.data.dateUsers[0].imageLink", equalTo(returnImageLink1)))
                        .andExpect(jsonPath("$.data.dateUsers[0].nickname", equalTo(returnNickname1)))
                        .andExpect(jsonPath("$.data.dateUsers[0].meetingRole", equalTo(returnMeetingRole1.name())))
                        .andExpect(jsonPath("$.data.dateUsers[1].id", equalTo(existentDateUserId2), Long.class))
                        .andExpect(jsonPath("$.data.dateUsers[1].imageLink", equalTo(returnImageLink2)))
                        .andExpect(jsonPath("$.data.dateUsers[1].nickname", equalTo(returnNickname2)))
                        .andExpect(jsonPath("$.data.dateUsers[1].meetingRole", equalTo(returnMeetingRole2.name())))

                        .andDo(document("date-detail-normal",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ?????? ????????? ID"),
                                        parameterWithName("dateId").description("??????????????? ?????? ????????? ID")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("id").type(JsonFieldType.NUMBER).description("????????? ?????? ????????? ID"),
                                        fieldWithPath("date").type(JsonFieldType.STRING).description("?????? ????????? ??????").attributes(new Attributes.Attribute("format", "yyyy-MM-dd")),
                                        fieldWithPath("userCount").type(JsonFieldType.NUMBER).description("?????? ????????? ????????? ?????? ???"),
                                        fieldWithPath("dateStatus").type(JsonFieldType.STRING).description("?????? ????????? ?????? ??????").attributes(new Attributes.Attribute("format", "FIXED, UNFIXED")),
                                        subsectionWithPath("dateUsers").type(JsonFieldType.ARRAY).description("?????? ????????? ????????? ???????????? ??????")
                                ),
                                responseFields(beneathPath("data.dateUsers.[]").withSubsectionId("date-users"),
                                        fieldWithPath("id").type(JsonFieldType.NUMBER).description("?????? ????????? ????????? ????????? ?????? ?????? ID"),
                                        fieldWithPath("nickname").type(JsonFieldType.STRING).description("?????? ?????? ????????? ?????????").optional(),
                                        fieldWithPath("imageLink").type(JsonFieldType.STRING).description("?????? ?????? ????????? ????????? ????????? ??????").optional(),
                                        fieldWithPath("meetingRole").type(JsonFieldType.STRING).description("?????? ?????? ????????? ??????").attributes(key("format").value("HOST, EDITOR, PARTICIPANT"))
                                ))
                        )
                ;
            }

        }

        @Nested
        @DisplayName("??????")
        class ?????? {

            @Test
            @DisplayName("?????? ????????? ?????? ????????? ?????? ?????? Not Found??? ????????????.")
            public void ??????_???????????????() throws Exception {

                Long existentDateId = 10L;

                LocalDate resultDate = LocalDate.of(2022, 06, 30);
                Integer resultUserCount = 2;
                DateStatus resultDateStatus = DateStatus.UNFIXED;
                List<MeetingDateDetailUserResponse> returnDateUsers = new ArrayList<>();

                Long existentDateUserId1 = 10L;
                String returnImageLink1 = "imageLink1";
                String returnNickname1 = "nickname1";
                MeetingRole returnMeetingRole1 = MeetingRole.HOST;

                MeetingDateDetailUserResponse returnDateUser1 = MeetingDateDetailUserResponse.builder()
                        .id(existentDateUserId1)
                        .imageLink(returnImageLink1)
                        .nickname(returnNickname1)
                        .meetingRole(returnMeetingRole1)
                        .build();

                Long existentDateUserId2 = 20L;
                String returnImageLink2 = "imageLink2";
                String returnNickname2 = "nickname2";
                MeetingRole returnMeetingRole2 = MeetingRole.EDITOR;

                MeetingDateDetailUserResponse returnDateUser2 = MeetingDateDetailUserResponse.builder()
                        .id(existentDateUserId2)
                        .imageLink(returnImageLink2)
                        .nickname(returnNickname2)
                        .meetingRole(returnMeetingRole2)
                        .build();

                returnDateUsers.add(returnDateUser1);
                returnDateUsers.add(returnDateUser2);

                MeetingDateDetailResponse resultResponse = MeetingDateDetailResponse.builder()
                        .id(existentDateId)
                        .date(resultDate)
                        .userCount(resultUserCount)
                        .dateStatus(resultDateStatus)
                        .dateUsers(returnDateUsers)
                        .build();

                given(meetingDateQueryService.getDetail(mockedExistentMeetingId, existentDateId))
                        .willReturn(resultResponse);

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(get("/meetings/{meetingId}/dates/{dateId}", mockedNonexistentMeetingId, existentDateId)
                                .header("Authorization", participantUserToken)
                        )

                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("date-detail-error-meeting-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ?????? ????????? ID"),
                                        parameterWithName("dateId").description("??????????????? ?????? ????????? ID")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ????????? ?????? ????????? ?????? ?????? Not Found??? ????????????.")
            public void ??????_???????????????() throws Exception {

                Long nonExistentDateId = 20L;

                willThrow(new CustomException("?????? ID??? ???????????? ????????? ??????", ErrorCode.ENTITY_NOT_FOUND))
                        .given(meetingDateQueryService).getDetail(mockedExistentMeetingId, nonExistentDateId);

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(get("/meetings/{meetingId}/dates/{dateId}", mockedExistentMeetingId, nonExistentDateId)
                                .header("Authorization", participantUserToken)
                        )

                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("date-detail-error-date-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ?????? ????????? ID"),
                                        parameterWithName("dateId").description("??????????????? ?????? ????????? ID")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("????????? ???????????? ?????? ????????? ????????? ?????? ?????? Forbidden??? ????????????.")
            public void ??????_???????????????() throws Exception {

                Long existentDateId = 10L;

                LocalDate resultDate = LocalDate.of(2022, 06, 30);
                Integer resultUserCount = 2;
                DateStatus resultDateStatus = DateStatus.UNFIXED;
                List<MeetingDateDetailUserResponse> returnDateUsers = new ArrayList<>();

                Long existentDateUserId1 = 10L;
                String returnImageLink1 = "imageLink1";
                String returnNickname1 = "nickname1";
                MeetingRole returnMeetingRole1 = MeetingRole.HOST;

                MeetingDateDetailUserResponse returnDateUser1 = MeetingDateDetailUserResponse.builder()
                        .id(existentDateUserId1)
                        .imageLink(returnImageLink1)
                        .nickname(returnNickname1)
                        .meetingRole(returnMeetingRole1)
                        .build();

                Long existentDateUserId2 = 20L;
                String returnImageLink2 = "imageLink2";
                String returnNickname2 = "nickname2";
                MeetingRole returnMeetingRole2 = MeetingRole.EDITOR;

                MeetingDateDetailUserResponse returnDateUser2 = MeetingDateDetailUserResponse.builder()
                        .id(existentDateUserId2)
                        .imageLink(returnImageLink2)
                        .nickname(returnNickname2)
                        .meetingRole(returnMeetingRole2)
                        .build();

                returnDateUsers.add(returnDateUser1);
                returnDateUsers.add(returnDateUser2);

                MeetingDateDetailResponse resultResponse = MeetingDateDetailResponse.builder()
                        .id(existentDateId)
                        .date(resultDate)
                        .userCount(resultUserCount)
                        .dateStatus(resultDateStatus)
                        .dateUsers(returnDateUsers)
                        .build();

                given(meetingDateQueryService.getDetail(mockedExistentMeetingId, existentDateId))
                        .willReturn(resultResponse);

                String unJoinedUserToken = createToken(10L);

                mockMvc.perform(get("/meetings/{meetingId}/dates/{dateId}", mockedExistentMeetingId, existentDateId)
                                .header("Authorization", unJoinedUserToken)
                        )

                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getMessage())))

                        .andDo(document("date-detail-error-not-joined",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ?????? ????????? ID"),
                                        parameterWithName("dateId").description("??????????????? ?????? ????????? ID")
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
