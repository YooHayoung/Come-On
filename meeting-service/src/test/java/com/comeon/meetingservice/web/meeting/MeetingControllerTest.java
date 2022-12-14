package com.comeon.meetingservice.web.meeting;

import com.comeon.meetingservice.common.exception.CustomException;
import com.comeon.meetingservice.common.exception.ErrorCode;
import com.comeon.meetingservice.domain.meeting.dto.MeetingAddDto;
import com.comeon.meetingservice.domain.meeting.dto.MeetingModifyDto;
import com.comeon.meetingservice.domain.meeting.dto.MeetingRemoveDto;
import com.comeon.meetingservice.domain.meeting.entity.MeetingRole;
import com.comeon.meetingservice.domain.meetingdate.entity.DateStatus;
import com.comeon.meetingservice.domain.meetingplace.entity.PlaceCategory;
import com.comeon.meetingservice.web.ControllerTestBase;
import com.comeon.meetingservice.web.common.response.ApiResponseCode;
import com.comeon.meetingservice.web.common.response.SliceResponse;
import com.comeon.meetingservice.web.common.util.fileutils.UploadFileDto;
import com.comeon.meetingservice.web.meeting.query.MeetingCondition;
import com.comeon.meetingservice.web.meeting.response.*;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.JsonFieldType;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MeetingControllerTest extends ControllerTestBase {

    MockMultipartFile sampleFile;

    @Value("${meeting-file.dir}")
    String sampleDir;

    UploadFileDto uploadFileDto;

    @BeforeEach
    public void init() {
        String sampleFileOriName = "test.png";

        sampleFile = new MockMultipartFile(
                "image",
                sampleFileOriName,
                ContentType.IMAGE_PNG.getMimeType(),
                "test data".getBytes(StandardCharsets.UTF_8));

        uploadFileDto = UploadFileDto.builder()
                .storedFileName("storedName")
                .originalFileName(sampleFileOriName)
                .build();

        given(fileManager.upload(refEq(sampleFile), eq(sampleDir)))
                .willReturn(uploadFileDto);
    }

    @Nested
    @DisplayName("?????? ??????")
    class ???????????? {

        @Nested
        @DisplayName("????????????")
        class ???????????? {

            @Test
            @DisplayName("?????? ?????? ???????????? ????????? ?????? OK??? ????????????.")
            public void ??????_??????_????????????() throws Exception {

                Long addedUserId = mockedHostUserId;
                LocalDate addedStartDate = LocalDate.of(2022, 06, 10);
                LocalDate addedEndDate = LocalDate.of(2022, 06, 30);
                String addedTitle = "title";

                MeetingAddDto normalDto = MeetingAddDto.builder()
                        .userId(addedUserId)
                        .startDate(addedStartDate)
                        .endDate(addedEndDate)
                        .title(addedTitle)
                        .originalFileName(uploadFileDto.getOriginalFileName())
                        .storedFileName(uploadFileDto.getStoredFileName())
                        .meetingAddPlaceDtos(new ArrayList<>())
                        .build();

                Long createdMeetingId = 10L;
                given(meetingService.add(refEq(normalDto))).willReturn(createdMeetingId);

                String meetingCreatorToken = createToken(mockedHostUserId);

                mockMvc.perform(multipart("/meetings")
                                .file(sampleFile)
                                .param("title", addedTitle)
                                .param("startDate", addedStartDate.toString())
                                .param("endDate", addedEndDate.toString())
                                .header("Authorization", meetingCreatorToken)
                        )

                        .andExpect(status().isCreated())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))
                        .andExpect(jsonPath("$.data", equalTo(createdMeetingId), Long.class))

                        .andDo(document("meeting-create-normal",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                requestParameters(
                                        parameterWithName("title").description("?????? ??????"),
                                        parameterWithName("startDate").description("?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        parameterWithName("endDate").description("?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        parameterWithName("courseId").description("????????? ????????? ????????? ID").optional()
                                ),
                                requestParts(
                                        partWithName("image").description("?????? ?????????")
                                ))
                        )
                ;
            }

        }

        @Nested
        @DisplayName("??????")
        class ?????? {

            @Test
            @DisplayName("?????? ???????????? ???????????? ?????? ?????? Bad Request??? ????????????.")
            public void ??????_?????????() throws Exception {

                Long addedUserId = mockedHostUserId;
                LocalDate addedStartDate = LocalDate.of(2022, 06, 10);
                LocalDate addedEndDate = LocalDate.of(2022, 06, 30);
                String addedTitle = "title";

                MeetingAddDto normalDto = MeetingAddDto.builder()
                        .userId(addedUserId)
                        .startDate(addedStartDate)
                        .endDate(addedEndDate)
                        .title(addedTitle)
                        .originalFileName(uploadFileDto.getOriginalFileName())
                        .storedFileName(uploadFileDto.getStoredFileName())
                        .build();

                Long createdMeetingId = 10L;
                given(meetingService.add(refEq(normalDto))).willReturn(createdMeetingId);

                String meetingCreatorToken = createToken(mockedHostUserId);

                mockMvc.perform(multipart("/meetings")
                                .file(sampleFile)
                                .header("Authorization", meetingCreatorToken)
                        )

                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.BAD_PARAMETER.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.VALIDATION_FAIL.getCode())))

                        .andDo(document("meeting-create-error-param",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                requestParts(
                                        partWithName("image").description("?????? ?????????")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.OBJECT).description("?????? ??????????????? ????????? ??????????????? ??????"),
                                        fieldWithPath("message.title").type(JsonFieldType.ARRAY).description("????????? ????????? ??????"),
                                        fieldWithPath("message.endDate").type(JsonFieldType.ARRAY).description("????????? ????????? ??????"),
                                        fieldWithPath("message.startDate").type(JsonFieldType.ARRAY).description("????????? ????????? ??????")

                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ID??? ???????????? ??? ?????? ID??? ????????? ?????? Not Found??? ????????????.")
            public void ??????_???????????????() throws Exception {

                Long addedUserId = mockedHostUserId;
                LocalDate addedStartDate = LocalDate.of(2022, 06, 10);
                LocalDate addedEndDate = LocalDate.of(2022, 06, 30);
                String addedTitle = "title";
                Long nonexistentCourseId = 1000L;

                MeetingAddDto normalDto = MeetingAddDto.builder()
                        .userId(addedUserId)
                        .startDate(addedStartDate)
                        .endDate(addedEndDate)
                        .title(addedTitle)
                        .originalFileName(uploadFileDto.getOriginalFileName())
                        .storedFileName(uploadFileDto.getStoredFileName())
                        .build();

                Long createdMeetingId = 10L;
                given(meetingService.add(refEq(normalDto))).willReturn(createdMeetingId);

                willThrow(new CustomException("????????? ?????? ??? ??????", ErrorCode.COURSE_NOT_FOUND))
                        .given(courseFeignService).getCoursePlaceList(nonexistentCourseId);

                String meetingCreatorToken = createToken(mockedHostUserId);

                mockMvc.perform(multipart("/meetings")
                                .file(sampleFile)
                                .param("title", addedTitle)
                                .param("startDate", addedStartDate.toString())
                                .param("endDate", addedEndDate.toString())
                                .param("courseId", String.valueOf(nonexistentCourseId))
                                .header("Authorization", meetingCreatorToken)
                                .header("Authorization", meetingCreatorToken)
                        )

                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.COURSE_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.COURSE_NOT_FOUND.getMessage())))

                        .andDo(document("meeting-create-error-course-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                requestParameters(
                                        parameterWithName("title").description("?????? ??????"),
                                        parameterWithName("startDate").description("?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        parameterWithName("endDate").description("?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        parameterWithName("courseId").description("????????? ????????? ????????? ID").optional()
                                ),
                                requestParts(
                                        partWithName("image").description("?????? ?????????")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ID??? ???????????? ??? ?????? ????????? ?????? ???????????? Bad Request??? ????????????.")
            public void ??????_??????????????????() throws Exception {

                Long addedUserId = mockedHostUserId;
                LocalDate addedStartDate = LocalDate.of(2022, 06, 10);
                LocalDate addedEndDate = LocalDate.of(2022, 06, 30);
                String addedTitle = "title";
                Long unAvailableCourseId = 2000L;

                MeetingAddDto normalDto = MeetingAddDto.builder()
                        .userId(addedUserId)
                        .startDate(addedStartDate)
                        .endDate(addedEndDate)
                        .title(addedTitle)
                        .originalFileName(uploadFileDto.getOriginalFileName())
                        .storedFileName(uploadFileDto.getStoredFileName())
                        .build();

                Long createdMeetingId = 10L;
                given(meetingService.add(refEq(normalDto))).willReturn(createdMeetingId);

                willThrow(new CustomException("?????? ????????? ???????????? ??????", ErrorCode.COURSE_NOT_AVAILABLE))
                        .given(courseFeignService).getCoursePlaceList(unAvailableCourseId);

                String meetingCreatorToken = createToken(mockedHostUserId);

                mockMvc.perform(multipart("/meetings")
                                .file(sampleFile)
                                .param("title", addedTitle)
                                .param("startDate", addedStartDate.toString())
                                .param("endDate", addedEndDate.toString())
                                .param("courseId", String.valueOf(unAvailableCourseId))
                                .header("Authorization", meetingCreatorToken)
                                .header("Authorization", meetingCreatorToken)
                        )

                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.BAD_PARAMETER.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.COURSE_NOT_AVAILABLE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.COURSE_NOT_AVAILABLE.getMessage())))

                        .andDo(document("meeting-create-error-course-not-available",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                requestParameters(
                                        parameterWithName("title").description("?????? ??????"),
                                        parameterWithName("startDate").description("?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        parameterWithName("endDate").description("?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        parameterWithName("courseId").description("????????? ????????? ????????? ID").optional()
                                ),
                                requestParts(
                                        partWithName("image").description("?????? ?????????")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ID??? ???????????? ??? ?????? ???????????? ????????? ????????? Internal Server Error ??? ????????????.")
            public void ??????_???????????????() throws Exception {

                Long addedUserId = mockedHostUserId;
                LocalDate addedStartDate = LocalDate.of(2022, 06, 10);
                LocalDate addedEndDate = LocalDate.of(2022, 06, 30);
                String addedTitle = "title";
                Long normalCourseId = 500L;

                MeetingAddDto normalDto = MeetingAddDto.builder()
                        .userId(addedUserId)
                        .startDate(addedStartDate)
                        .endDate(addedEndDate)
                        .title(addedTitle)
                        .originalFileName(uploadFileDto.getOriginalFileName())
                        .storedFileName(uploadFileDto.getStoredFileName())
                        .build();

                Long createdMeetingId = 10L;
                given(meetingService.add(refEq(normalDto))).willReturn(createdMeetingId);

                willThrow(new CustomException("?????? ????????? ?????? ??????", ErrorCode.COURSE_SERVICE_ERROR))
                        .given(courseFeignService).getCoursePlaceList(normalCourseId);

                String meetingCreatorToken = createToken(mockedHostUserId);

                mockMvc.perform(multipart("/meetings")
                                .file(sampleFile)
                                .param("title", addedTitle)
                                .param("startDate", addedStartDate.toString())
                                .param("endDate", addedEndDate.toString())
                                .param("courseId", String.valueOf(normalCourseId))
                                .header("Authorization", meetingCreatorToken)
                                .header("Authorization", meetingCreatorToken)
                        )

                        .andExpect(status().isInternalServerError())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SERVER_ERROR.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.COURSE_SERVICE_ERROR.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.COURSE_SERVICE_ERROR.getMessage())))

                        .andDo(document("meeting-create-error-course-service",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                requestParameters(
                                        parameterWithName("title").description("?????? ??????"),
                                        parameterWithName("startDate").description("?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        parameterWithName("endDate").description("?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        parameterWithName("courseId").description("????????? ????????? ????????? ID").optional()
                                ),
                                requestParts(
                                        partWithName("image").description("?????? ?????????")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("????????? ????????? ?????? Internal Server Error??? ????????????.")
            public void ??????_????????????() throws Exception {

                String errorFileOriName = "error.png";

                MockMultipartFile uploadErrorFile = new MockMultipartFile(
                        "image",
                        errorFileOriName,
                        ContentType.IMAGE_PNG.getMimeType(),
                        "Upload Error".getBytes(StandardCharsets.UTF_8));

                willThrow(new CustomException("?????? ?????? ?????? ??????", ErrorCode.UPLOAD_FAIL))
                        .given(fileManager).upload(refEq(uploadErrorFile), eq(sampleDir));

                Long addedUserId = mockedHostUserId;
                LocalDate addedStartDate = LocalDate.of(2022, 06, 10);
                LocalDate addedEndDate = LocalDate.of(2022, 06, 30);
                String addedTitle = "title";

                MeetingAddDto normalDto = MeetingAddDto.builder()
                        .userId(addedUserId)
                        .startDate(addedStartDate)
                        .endDate(addedEndDate)
                        .title(addedTitle)
                        .originalFileName(uploadFileDto.getOriginalFileName())
                        .storedFileName(uploadFileDto.getStoredFileName())
                        .build();

                Long createdMeetingId = 10L;
                given(meetingService.add(refEq(normalDto))).willReturn(createdMeetingId);

                String meetingCreatorToken = createToken(mockedHostUserId);

                mockMvc.perform(multipart("/meetings")
                                .file(uploadErrorFile)
                                .param("title", addedTitle)
                                .param("startDate", addedStartDate.toString())
                                .param("endDate", addedEndDate.toString())
                                .header("Authorization", meetingCreatorToken)
                        )

                        .andExpect(status().isInternalServerError())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SERVER_ERROR.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.UPLOAD_FAIL.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.UPLOAD_FAIL.getMessage())))

                        .andDo(document("meeting-create-error-upload",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                requestParameters(
                                        parameterWithName("title").description("?????? ??????"),
                                        parameterWithName("startDate").description("?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        parameterWithName("endDate").description("?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        parameterWithName("courseId").description("????????? ????????? ????????? ID").optional()
                                ),
                                requestParts(
                                        partWithName("image").description("?????? ?????????")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ??????????????? ????????? ??????????????? ??????")
                                ))
                        )
                ;
            }
        }
    }


    @Nested
    @DisplayName("?????? ??????")
    class ???????????? {

        @Nested
        @DisplayName("????????????")
        class ???????????? {

            @Test
            @DisplayName("???????????? ???????????? ????????? ?????? ?????? ?????? ??? OK??? ????????????.")
            public void ????????????_???????????????() throws Exception {

                LocalDate modifiedStartDate = LocalDate.of(2022, 07, 10);
                LocalDate modifiedEndDate = LocalDate.of(2022, 07, 30);
                String modifiedTitle = "title";

                MeetingModifyDto imageIncludedDto = MeetingModifyDto.builder()
                        .id(mockedExistentMeetingId)
                        .startDate(modifiedStartDate)
                        .endDate(modifiedEndDate)
                        .title(modifiedTitle)
                        .originalFileName(uploadFileDto.getOriginalFileName())
                        .storedFileName(uploadFileDto.getStoredFileName())
                        .build();

                willDoNothing().given(meetingService).modify(refEq(imageIncludedDto));

                String hostUserToken = createToken(mockedHostUserId);

                mockMvc.perform(multipart("/meetings/{meetingId}", mockedExistentMeetingId)
                                .file(sampleFile)
                                .param("title", modifiedTitle)
                                .param("startDate", modifiedStartDate.toString())
                                .param("endDate", modifiedEndDate.toString())
                                .header("Authorization", hostUserToken)
                        )

                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))

                        .andDo(document("meeting-modify-normal-include-image",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????????????? ????????? ID")
                                ),
                                requestParameters(
                                        parameterWithName("title").description("????????? ?????? ??????"),
                                        parameterWithName("startDate").description("????????? ?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        parameterWithName("endDate").description("????????? ?????????").attributes(key("format").value("yyyy-MM-dd"))
                                ),
                                requestParts(
                                        partWithName("image").description("????????? ?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("???????????? ??????????????? ????????? ?????? ?????? ?????? ??? OK??? ????????????.")
            public void ????????????_??????????????????() throws Exception {

                LocalDate modifiedStartDate = LocalDate.of(2022, 07, 10);
                LocalDate modifiedEndDate = LocalDate.of(2022, 07, 30);
                String modifiedTitle = "title";

                MeetingModifyDto imageExcludedDto = MeetingModifyDto.builder()
                        .id(mockedExistentMeetingId)
                        .startDate(modifiedStartDate)
                        .endDate(modifiedEndDate)
                        .title(modifiedTitle)
                        .build();

                willDoNothing().given(meetingService).modify(refEq(imageExcludedDto));

                String hostUserToken = createToken(mockedHostUserId);

                mockMvc.perform(multipart("/meetings/{meetingId}", mockedExistentMeetingId)
                                .param("title", modifiedTitle)
                                .param("startDate", modifiedStartDate.toString())
                                .param("endDate", modifiedEndDate.toString())
                                .header("Authorization", hostUserToken)
                        )

                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))

                        .andDo(document("meeting-modify-normal-exclude-image",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????????????? ????????? ID")
                                ),
                                requestParameters(
                                        parameterWithName("title").description("????????? ?????? ??????"),
                                        parameterWithName("startDate").description("????????? ?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        parameterWithName("endDate").description("????????? ?????????").attributes(key("format").value("yyyy-MM-dd"))
                                ))
                        )
                ;
            }
        }

        @Nested
        @DisplayName("??????")
        class ?????? {

            @Test
            @DisplayName("?????? ???????????? ????????? ?????? ?????? Bad Request??? ????????????.")
            public void ?????????_??????() throws Exception {

                LocalDate modifiedStartDate = LocalDate.of(2022, 07, 10);
                LocalDate modifiedEndDate = LocalDate.of(2022, 07, 30);
                String modifiedTitle = "title";

                MeetingModifyDto normalDto = MeetingModifyDto.builder()
                        .id(mockedExistentMeetingId)
                        .startDate(modifiedStartDate)
                        .endDate(modifiedEndDate)
                        .title(modifiedTitle)
                        .build();

                willDoNothing().given(meetingService).modify(refEq(normalDto));

                String hostUserToken = createToken(mockedHostUserId);

                mockMvc.perform(multipart("/meetings/{meetingId}", mockedExistentMeetingId)
                                .param("endDate", modifiedEndDate.toString())
                                .header("Authorization", hostUserToken)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.BAD_PARAMETER.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.VALIDATION_FAIL.getCode())))

                        .andDo(document("meeting-modify-error-param",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????????????? ????????? ID")
                                ),
                                requestParameters(
                                        parameterWithName("endDate").description("????????? ?????????").attributes(key("format").value("yyyy-MM-dd"))
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.OBJECT).description("?????? ??????????????? ????????? ??????????????? ??????"),
                                        fieldWithPath("message.title").type(JsonFieldType.ARRAY).description("????????? ????????? ??????"),
                                        fieldWithPath("message.startDate").type(JsonFieldType.ARRAY).description("????????? ????????? ??????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("????????? ?????? ?????? ??? ????????? ????????? ?????? Internal Server Error??? ????????????.")
            public void ??????_????????????() throws Exception {

                String errorFileOriName = "error.png";

                MockMultipartFile uploadErrorFile = new MockMultipartFile(
                        "image",
                        errorFileOriName,
                        ContentType.IMAGE_PNG.getMimeType(),
                        "Upload Error".getBytes(StandardCharsets.UTF_8));

                willThrow(new CustomException("?????? ?????? ?????? ??????", ErrorCode.UPLOAD_FAIL))
                        .given(fileManager).upload(refEq(uploadErrorFile), eq(sampleDir));

                LocalDate modifiedStartDate = LocalDate.of(2022, 07, 10);
                LocalDate modifiedEndDate = LocalDate.of(2022, 07, 30);
                String modifiedTitle = "title";

                MeetingModifyDto normalDto = MeetingModifyDto.builder()
                        .id(mockedExistentMeetingId)
                        .startDate(modifiedStartDate)
                        .endDate(modifiedEndDate)
                        .title(modifiedTitle)
                        .build();

                willDoNothing().given(meetingService).modify(refEq(normalDto));

                String hostUserToken = createToken(mockedHostUserId);

                mockMvc.perform(multipart("/meetings")
                                .file(uploadErrorFile)
                                .param("title", modifiedTitle)
                                .param("startDate", modifiedStartDate.toString())
                                .param("endDate", modifiedEndDate.toString())
                                .header("Authorization", hostUserToken)
                        )

                        .andExpect(status().isInternalServerError())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SERVER_ERROR.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.UPLOAD_FAIL.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.UPLOAD_FAIL.getMessage())))

                        .andDo(document("meeting-modify-error-upload",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                requestParameters(
                                        parameterWithName("title").description("?????? ??????"),
                                        parameterWithName("startDate").description("?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        parameterWithName("endDate").description("?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        parameterWithName("courseId").description("????????? ????????? ????????? ID").optional()
                                ),
                                requestParts(
                                        partWithName("image").description("?????? ?????????")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ??????????????? ????????? ??????????????? ??????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ????????? ?????? ????????? ?????? ?????? Not Found??? ????????????.")
            public void ??????_???????????????() throws Exception {

                LocalDate modifiedStartDate = LocalDate.of(2022, 07, 10);
                LocalDate modifiedEndDate = LocalDate.of(2022, 07, 30);
                String modifiedTitle = "title";

                MeetingModifyDto normalDto = MeetingModifyDto.builder()
                        .id(mockedExistentMeetingId)
                        .startDate(modifiedStartDate)
                        .endDate(modifiedEndDate)
                        .title(modifiedTitle)
                        .build();

                willDoNothing().given(meetingService).modify(refEq(normalDto));

                String hostUserToken = createToken(mockedHostUserId);

                mockMvc.perform(multipart("/meetings/{meetingId}", mockedNonexistentMeetingId)
                                .param("title", modifiedTitle)
                                .param("startDate", modifiedStartDate.toString())
                                .param("endDate", modifiedEndDate.toString())
                                .header("Authorization", hostUserToken)
                        )

                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("meeting-modify-error-meeting-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????????????? ????????? ID")
                                ),
                                requestParameters(
                                        parameterWithName("title").description("????????? ?????? ??????"),
                                        parameterWithName("startDate").description("????????? ?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        parameterWithName("endDate").description("????????? ?????????").attributes(key("format").value("yyyy-MM-dd"))
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

                LocalDate modifiedStartDate = LocalDate.of(2022, 07, 10);
                LocalDate modifiedEndDate = LocalDate.of(2022, 07, 30);
                String modifiedTitle = "title";

                MeetingModifyDto normalDto = MeetingModifyDto.builder()
                        .id(mockedExistentMeetingId)
                        .startDate(modifiedStartDate)
                        .endDate(modifiedEndDate)
                        .title(modifiedTitle)
                        .build();

                willDoNothing().given(meetingService).modify(refEq(normalDto));

                String unJoinedUserToken = createToken(10L);

                mockMvc.perform(multipart("/meetings/{meetingId}", mockedExistentMeetingId)
                                .param("title", modifiedTitle)
                                .param("startDate", modifiedStartDate.toString())
                                .param("endDate", modifiedEndDate.toString())
                                .header("Authorization", unJoinedUserToken)
                        )

                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getMessage())))

                        .andDo(document("meeting-modify-error-not-joined",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????????????? ????????? ID")
                                ),
                                requestParameters(
                                        parameterWithName("title").description("????????? ?????? ??????"),
                                        parameterWithName("startDate").description("????????? ?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        parameterWithName("endDate").description("????????? ?????????").attributes(key("format").value("yyyy-MM-dd"))
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("HOST??? ?????? ????????? ????????? ?????? ?????? Forbidden??? ????????????.")
            public void ??????_????????????() throws Exception {

                LocalDate modifiedStartDate = LocalDate.of(2022, 07, 10);
                LocalDate modifiedEndDate = LocalDate.of(2022, 07, 30);
                String modifiedTitle = "title";

                MeetingModifyDto normalDto = MeetingModifyDto.builder()
                        .id(mockedExistentMeetingId)
                        .startDate(modifiedStartDate)
                        .endDate(modifiedEndDate)
                        .title(modifiedTitle)
                        .build();

                willDoNothing().given(meetingService).modify(refEq(normalDto));

                String editorUserToken = createToken(mockedEditorUserId);

                mockMvc.perform(multipart("/meetings/{meetingId}", mockedExistentMeetingId)
                                .param("title", modifiedTitle)
                                .param("startDate", modifiedStartDate.toString())
                                .param("endDate", modifiedEndDate.toString())
                                .header("Authorization", editorUserToken)
                        )

                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.AUTHORIZATION_FAIL.getCode())))

                        .andDo(document("meeting-modify-error-authorization",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? HOST??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????????????? ????????? ID")
                                ),
                                requestParameters(
                                        parameterWithName("title").description("????????? ?????? ??????"),
                                        parameterWithName("startDate").description("????????? ?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        parameterWithName("endDate").description("????????? ?????????").attributes(key("format").value("yyyy-MM-dd"))
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
    @DisplayName("?????? ??????")
    class ???????????? {

        @Nested
        @DisplayName("????????????")
        class ???????????? {

            @Test
            @DisplayName("??????????????? ????????? ?????? OK??? ????????????.")
            public void ??????_??????() throws Exception {

                MeetingRemoveDto normalDto = MeetingRemoveDto.builder()
                        .id(mockedExistentMeetingId)
                        .userId(mockedParticipantUserId)
                        .build();

                willDoNothing().given(meetingService).remove(refEq(normalDto));

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(delete("/meetings/{meetingId}", mockedExistentMeetingId)
                                .header("Authorization", participantUserToken)
                        )

                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))

                        .andDo(document("meeting-delete-normal",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????(??????)????????? ????????? ID")
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

                MeetingRemoveDto normalDto = MeetingRemoveDto.builder()
                        .id(mockedExistentMeetingId)
                        .userId(mockedParticipantUserId)
                        .build();

                willDoNothing().given(meetingService).remove(refEq(normalDto));

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(delete("/meetings/{meetingId}", mockedNonexistentMeetingId)
                                .header("Authorization", participantUserToken))

                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("meeting-delete-error-meeting-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????(??????)????????? ????????? ID")
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
                // given
                MeetingRemoveDto normalDto = MeetingRemoveDto.builder()
                        .id(mockedExistentMeetingId)
                        .userId(mockedParticipantUserId)
                        .build();

                willDoNothing().given(meetingService).remove(refEq(normalDto));

                String unJoinedUserToken = createToken(10L);

                mockMvc.perform(delete("/meetings/{meetingId}", mockedExistentMeetingId)
                                .header("Authorization", unJoinedUserToken))

                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getMessage())))

                        .andDo(document("meeting-delete-error-not-joined",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????(??????)????????? ????????? ID")
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
    @DisplayName("????????????-?????????")
    class ????????????????????? {

        @Test
        @DisplayName("??????????????? ????????? ?????? OK??? ?????? ????????? ???????????? ????????????.")
        public void ??????_??????() throws Exception {

            String sampleTitleCond = "title";
            LocalDate sampleStartDateCond = LocalDate.of(2022, 06, 10);
            LocalDate sampleEndDateCond = LocalDate.of(2022, 07, 30);
            int samplePage = 0;
            int sampleSize = 5;
            Pageable samplePageable = PageRequest.of(samplePage, sampleSize);

            MeetingCondition sampleCondition = MeetingCondition.builder()
                    .title(sampleTitleCond)
                    .startDate(sampleStartDateCond)
                    .endDate(sampleEndDateCond)
                    .build();

            Long responseMeetingId1 = 10L;
            String responseHostNickname1 = "host nickname1";
            Integer responseUserCount1 = 3;
            MeetingRole responseMeetingRole1 = MeetingRole.HOST;
            String responseTitle1 = "title1";
            LocalDate responseStartDate1 = LocalDate.of(2022, 06, 15);
            LocalDate responseEndDate1 = LocalDate.of(2022, 06, 20);
            Long responseMeetingCodeId1 = 100L;
            String responseImageLink1 = "https://link1";
            List<LocalDate> responseFixedDates1 = new ArrayList<>();
            responseFixedDates1.add(LocalDate.of(2022, 06, 16));
            responseFixedDates1.add(LocalDate.of(2022, 06, 17));
            MeetingStatus responseMeetingStatus1 = MeetingStatus.END;

            MeetingListResponse meetingListResponse1 = MeetingListResponse.builder()
                    .id(responseMeetingId1)
                    .hostNickname(responseHostNickname1)
                    .userCount(responseUserCount1)
                    .myMeetingRole(responseMeetingRole1)
                    .title(responseTitle1)
                    .startDate(responseStartDate1)
                    .endDate(responseEndDate1)
                    .meetingCodeId(responseMeetingCodeId1)
                    .imageLink(responseImageLink1)
                    .fixedDates(responseFixedDates1)
                    .meetingStatus(responseMeetingStatus1)
                    .build();

            Long responseMeetingId2 = 20L;
            String responseHostNickname2 = "host nickname2";
            Integer responseUserCount2 = 5;
            MeetingRole responseMeetingRole2 = MeetingRole.EDITOR;
            String responseTitle2 = "title2";
            LocalDate responseStartDate2 = LocalDate.of(2022, 07, 10);
            LocalDate responseEndDate2 = LocalDate.of(2022, 07, 25);
            Long responseMeetingCodeId2 = 200L;
            String responseImageLink2 = "https://link1";
            List<LocalDate> responseFixedDates2 = new ArrayList<>();
            MeetingStatus responseMeetingStatus2 = MeetingStatus.UNFIXED;

            MeetingListResponse meetingListResponse2 = MeetingListResponse.builder()
                    .id(responseMeetingId2)
                    .hostNickname(responseHostNickname2)
                    .userCount(responseUserCount2)
                    .myMeetingRole(responseMeetingRole2)
                    .title(responseTitle2)
                    .startDate(responseStartDate2)
                    .endDate(responseEndDate2)
                    .meetingCodeId(responseMeetingCodeId2)
                    .imageLink(responseImageLink2)
                    .fixedDates(responseFixedDates2)
                    .meetingStatus(responseMeetingStatus2)
                    .build();

            List<MeetingListResponse> responseContents = new ArrayList<>();
            responseContents.add(meetingListResponse1);
            responseContents.add(meetingListResponse2);

            boolean hasNext = false;
            SliceImpl sampleResultSlice = new SliceImpl(responseContents, samplePageable, hasNext);
            SliceResponse sampleResultResponse = SliceResponse.toSliceResponse(sampleResultSlice);

            given(meetingQueryService.getList(
                    eq(mockedHostUserId),
                    refEq(samplePageable),
                    refEq(sampleCondition))).willReturn(sampleResultResponse);

            String hostUserToken = createToken(mockedHostUserId);

            mockMvc.perform(get("/meetings")
                            .header("Authorization", hostUserToken)
                            .queryParam("page", String.valueOf(samplePage))
                            .queryParam("size", String.valueOf(sampleSize))
                            .queryParam("title", sampleTitleCond)
                            .queryParam("startDate", sampleStartDateCond.toString())
                            .queryParam("endDate", sampleEndDateCond.toString())
                    )

                    // SliceInfo
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))
                    .andExpect(jsonPath("$.data.currentSlice", equalTo(samplePage)))
                    .andExpect(jsonPath("$.data.sizePerSlice", equalTo(sampleSize)))
                    .andExpect(jsonPath("$.data.numberOfElements", equalTo(responseContents.size())))
                    .andExpect(jsonPath("$.data.hasPrevious", equalTo(sampleResultSlice.hasPrevious())))
                    .andExpect(jsonPath("$.data.hasNext", equalTo(sampleResultSlice.hasNext())))
                    .andExpect(jsonPath("$.data.first", equalTo(sampleResultResponse.isFirst())))
                    .andExpect(jsonPath("$.data.last", equalTo(sampleResultResponse.isLast())))

                    // MeetingData1
                    .andExpect(jsonPath("$.data.contents[0].id", equalTo(responseMeetingId1), Long.class))
                    .andExpect(jsonPath("$.data.contents[0].hostNickname", equalTo(responseHostNickname1)))
                    .andExpect(jsonPath("$.data.contents[0].userCount", equalTo(responseUserCount1)))
                    .andExpect(jsonPath("$.data.contents[0].myMeetingRole", equalTo(responseMeetingRole1.name())))
                    .andExpect(jsonPath("$.data.contents[0].title", containsString(sampleTitleCond)))
                    .andExpect(jsonPath("$.data.contents[0].startDate", greaterThanOrEqualTo(sampleStartDateCond.toString())))
                    .andExpect(jsonPath("$.data.contents[0].endDate", lessThanOrEqualTo(sampleEndDateCond.toString())))
                    .andExpect(jsonPath("$.data.contents[0].meetingCodeId", equalTo(responseMeetingCodeId1), Long.class))
                    .andExpect(jsonPath("$.data.contents[0].imageLink", equalTo(responseImageLink1)))
                    .andExpect(jsonPath("$.data.contents[0].fixedDates[0]", equalTo(meetingListResponse1.getFixedDates().get(0).toString())))
                    .andExpect(jsonPath("$.data.contents[0].fixedDates[1]", equalTo(meetingListResponse1.getFixedDates().get(1).toString())))
                    .andExpect(jsonPath("$.data.contents[0].meetingStatus", equalTo(responseMeetingStatus1.name())))

                    // MeetingData2
                    .andExpect(jsonPath("$.data.contents[1].id", equalTo(responseMeetingId2), Long.class))
                    .andExpect(jsonPath("$.data.contents[1].hostNickname", equalTo(responseHostNickname2)))
                    .andExpect(jsonPath("$.data.contents[1].userCount", equalTo(responseUserCount2)))
                    .andExpect(jsonPath("$.data.contents[1].myMeetingRole", equalTo(responseMeetingRole2.name())))
                    .andExpect(jsonPath("$.data.contents[1].title", containsString(sampleTitleCond)))
                    .andExpect(jsonPath("$.data.contents[1].startDate", greaterThanOrEqualTo(sampleStartDateCond.toString())))
                    .andExpect(jsonPath("$.data.contents[1].endDate", lessThanOrEqualTo(sampleEndDateCond.toString())))
                    .andExpect(jsonPath("$.data.contents[1].meetingCodeId", equalTo(responseMeetingCodeId2), Long.class))
                    .andExpect(jsonPath("$.data.contents[1].imageLink", equalTo(responseImageLink2)))
                    .andExpect(jsonPath("$.data.contents[1].fixedDates", empty()))
                    .andExpect(jsonPath("$.data.contents[1].meetingStatus", equalTo(responseMeetingStatus2.name())))

                    .andDo(document("meeting-list-normal",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("????????? Bearer ??????").attributes(key("format").value("Bearer somejwttokens..."))
                            ),
                            requestParameters(
                                    parameterWithName("page").description("????????? ?????????(????????????), ?????????: 0").optional(),
                                    parameterWithName("size").description("??? ?????????(????????????)??? ????????? ????????? ???, ?????????: 5").optional(),
                                    parameterWithName("title").description("????????? ?????? ??????, ?????? ????????? ???????????? ?????? ???????????? ??????").optional(),
                                    parameterWithName("startDate").description("????????? ?????????, ?????? ???????????? ?????? ????????? ????????? ?????? ??????????????? ??????").attributes(key("format").value("yyyy-MM-dd")).optional(),
                                    parameterWithName("endDate").description("????????? ?????????, ?????? ???????????? ?????? ????????? ????????? ?????? ??????????????? ??????").attributes(key("format").value("yyyy-MM-dd")).optional()
                            ),
                            responseFields(beneathPath("data.contents").withSubsectionId("contents"),
                                    fieldWithPath("id").type(JsonFieldType.NUMBER).description("????????? ID"),
                                    fieldWithPath("hostNickname").type(JsonFieldType.STRING).description("?????? ????????? HOST ?????? ?????????").optional(),
                                    fieldWithPath("userCount").type(JsonFieldType.NUMBER).description("?????? ????????? ??? ?????? ???").optional(),
                                    fieldWithPath("myMeetingRole").type(JsonFieldType.STRING).description("?????? ???????????? ????????? ?????? ????????? ??????").attributes(key("format").value("HOST, EDITOR, PARTICIPANT")),
                                    fieldWithPath("title").type(JsonFieldType.STRING).description("????????? ??????"),
                                    fieldWithPath("startDate").type(JsonFieldType.STRING).description("????????? ?????????").attributes(key("format").value("yyyy-MM-dd")),
                                    fieldWithPath("endDate").type(JsonFieldType.STRING).description("????????? ?????????").attributes(key("format").value("yyyy-MM-dd")),
                                    fieldWithPath("imageLink").type(JsonFieldType.STRING).description("????????? ????????? ??????"),
                                    fieldWithPath("meetingCodeId").type(JsonFieldType.NUMBER).description("????????? ???????????? ?????????"),
                                    fieldWithPath("fixedDates").type(JsonFieldType.ARRAY).description("????????? ?????? ?????????"),
                                    fieldWithPath("meetingStatus").type(JsonFieldType.STRING).description("????????? ??????, ????????? ????????? ??????/?????????/???").attributes(key("format").value("UNFIXED, PROCEEDING, END"))
                            ))
                    )
            ;
        }
    }

    @Nested
    @DisplayName("????????????-??????")
    class ?????????????????? {

        @Nested
        @DisplayName("????????????")
        class ???????????? {

            @Test
            @DisplayName("??????????????? ????????? ?????? OK??? ?????? ????????? ????????????.")
            public void ??????_??????() throws Exception {

                // === ?????? ?????? === //
                Long responseMeetingUserId = 11L;
                MeetingRole responseMeetingRole = MeetingRole.PARTICIPANT;
                String responseTitle = "title";
                LocalDate responseStartDate = LocalDate.of(2022, 06, 10);
                LocalDate responseEndDate = LocalDate.of(2022, 06, 30);

                // === ?????? ????????? === //
                List<MeetingDetailUserResponse> responseMeetingUsers = new ArrayList<>();

                Long responseUserId1 = 10L;
                String responseUserNickname1 = "nickname1";
                String responseUserImageLink1 = "http://link1";
                MeetingRole responseUserMeetingRole1 = MeetingRole.HOST;

                MeetingDetailUserResponse responseMeetingUser1 = MeetingDetailUserResponse.builder()
                        .id(responseUserId1)
                        .nickname(responseUserNickname1)
                        .imageLink(responseUserImageLink1)
                        .meetingRole(responseUserMeetingRole1)
                        .build();

                responseMeetingUsers.add(responseMeetingUser1);

                Long responseUserId2 = 10L;
                String responseUserNickname2 = "nickname1";
                String responseUserImageLink2 = "http://link1";
                MeetingRole responseUserMeetingRole2 = MeetingRole.HOST;

                MeetingDetailUserResponse responseMeetingUser2 = MeetingDetailUserResponse.builder()
                        .id(responseUserId2)
                        .nickname(responseUserNickname2)
                        .imageLink(responseUserImageLink2)
                        .meetingRole(responseUserMeetingRole2)
                        .build();

                responseMeetingUsers.add(responseMeetingUser2);

                // === ?????? ????????? === //
                List<MeetingDetailDateResponse> responseMeetingDates = new ArrayList<>();

                Long responseDateId1 = 10L;
                LocalDate responseDateDate1 = LocalDate.of(2022, 06, 15);
                Integer responseDateUserCount1 = 1;
                DateStatus responseDateDateStatus1 = DateStatus.UNFIXED;
                Boolean responseIsSelected1 = true;

                MeetingDetailDateResponse responseMeetingDate1 = MeetingDetailDateResponse.builder()
                        .id(responseDateId1)
                        .date(responseDateDate1)
                        .userCount(responseDateUserCount1)
                        .dateStatus(responseDateDateStatus1)
                        .isSelected(responseIsSelected1)
                        .build();

                responseMeetingDates.add(responseMeetingDate1);

                Long responseDateId2 = 11L;
                LocalDate responseDateDate2 = LocalDate.of(2022, 06, 25);
                Integer responseDateUserCount2 = 2;
                DateStatus responseDateDateStatus2 = DateStatus.FIXED;
                Boolean responseIsSelected2 = false;

                MeetingDetailDateResponse responseMeetingDate2 = MeetingDetailDateResponse.builder()
                        .id(responseDateId2)
                        .date(responseDateDate2)
                        .userCount(responseDateUserCount2)
                        .dateStatus(responseDateDateStatus2)
                        .isSelected(responseIsSelected2)
                        .build();

                responseMeetingDates.add(responseMeetingDate2);

                // === ?????? ????????? === //
                List<MeetingDetailPlaceResponse> responseMeetingPlaces = new ArrayList<>();

                Long responsePlaceId1 = 10L;
                Long responsePlaceApiId1 = 1000L;
                PlaceCategory responsePlaceCategory1 = PlaceCategory.BAR;
                String responsePlaceName1 = "place1";
                String responsePlaceAddress1 = "address1";
                String responsePlaceMemo1 = "memo1";
                Double responsePlaceLat1 = 10.1;
                Double responsePlaceLng1 = 20.1;
                Integer responsePlaceOrder1 = 1;

                MeetingDetailPlaceResponse responseMeetingPlace1 = MeetingDetailPlaceResponse.builder()
                        .id(responsePlaceId1)
                        .apiId(responsePlaceApiId1)
                        .category(responsePlaceCategory1)
                        .name(responsePlaceName1)
                        .address(responsePlaceAddress1)
                        .memo(responsePlaceMemo1)
                        .lat(responsePlaceLat1)
                        .lng(responsePlaceLng1)
                        .order(responsePlaceOrder1)
                        .build();

                responseMeetingPlaces.add(responseMeetingPlace1);

                Long responsePlaceId2 = 11L;
                Long responsePlaceApiId2 = 2000L;
                PlaceCategory responsePlaceCategory2 = PlaceCategory.CAFE;
                String responsePlaceName2 = "place2";
                String responsePlaceAddress2 = "address1";
                String responsePlaceMemo2 = "memo2";
                Double responsePlaceLat2 = 110.1;
                Double responsePlaceLng2 = 120.1;
                Integer responsePlaceOrder2 = 2;

                MeetingDetailPlaceResponse responseMeetingPlace2 = MeetingDetailPlaceResponse.builder()
                        .id(responsePlaceId2)
                        .apiId(responsePlaceApiId2)
                        .category(responsePlaceCategory2)
                        .name(responsePlaceName2)
                        .address(responsePlaceAddress2)
                        .memo(responsePlaceMemo2)
                        .lat(responsePlaceLat2)
                        .lng(responsePlaceLng2)
                        .order(responsePlaceOrder2)
                        .build();

                responseMeetingPlaces.add(responseMeetingPlace2);

                // === ?????? ?????? ????????? ?????? ??? ?????? === //
                MeetingDetailResponse resultResponse = MeetingDetailResponse.builder()
                        .id(mockedExistentMeetingId)
                        .myMeetingUserId(responseMeetingUserId)
                        .myMeetingRole(responseMeetingRole)
                        .title(responseTitle)
                        .startDate(responseStartDate)
                        .endDate(responseEndDate)
                        .meetingUsers(responseMeetingUsers)
                        .meetingDates(responseMeetingDates)
                        .meetingPlaces(responseMeetingPlaces)
                        .build();

                given(meetingQueryService.getDetail(mockedExistentMeetingId, mockedParticipantUserId))
                        .willReturn(resultResponse);

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(get("/meetings/{meetingId}", mockedExistentMeetingId)
                                .header("Authorization", participantUserToken)
                        )

                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))

                        // Meeting
                        .andExpect(jsonPath("$.data.id", equalTo(mockedExistentMeetingId), Long.class))
                        .andExpect(jsonPath("$.data.myMeetingUserId", equalTo(responseMeetingUserId), Long.class))
                        .andExpect(jsonPath("$.data.myMeetingRole", equalTo(responseMeetingRole.name())))
                        .andExpect(jsonPath("$.data.title", equalTo(responseTitle)))
                        .andExpect(jsonPath("$.data.startDate", equalTo(responseStartDate.toString())))
                        .andExpect(jsonPath("$.data.endDate", equalTo(responseEndDate.toString())))

                        // MeetingUsers
                        .andExpect(jsonPath("$.data.meetingUsers[0].id", equalTo(responseUserId1), Long.class))
                        .andExpect(jsonPath("$.data.meetingUsers[0].nickname", equalTo(responseUserNickname1)))
                        .andExpect(jsonPath("$.data.meetingUsers[0].imageLink", equalTo(responseUserImageLink1)))
                        .andExpect(jsonPath("$.data.meetingUsers[0].meetingRole", equalTo(responseUserMeetingRole1.name())))
                        .andExpect(jsonPath("$.data.meetingUsers[1].id", equalTo(responseUserId2), Long.class))
                        .andExpect(jsonPath("$.data.meetingUsers[1].nickname", equalTo(responseUserNickname2)))
                        .andExpect(jsonPath("$.data.meetingUsers[1].imageLink", equalTo(responseUserImageLink2)))
                        .andExpect(jsonPath("$.data.meetingUsers[1].meetingRole", equalTo(responseUserMeetingRole2.name())))

                        // MeetingDates
                        .andExpect(jsonPath("$.data.meetingDates[0].id", equalTo(responseDateId1), Long.class))
                        .andExpect(jsonPath("$.data.meetingDates[0].date", equalTo(responseDateDate1.toString())))
                        .andExpect(jsonPath("$.data.meetingDates[0].userCount", equalTo(responseDateUserCount1)))
                        .andExpect(jsonPath("$.data.meetingDates[0].dateStatus", equalTo(responseDateDateStatus1.name())))
                        .andExpect(jsonPath("$.data.meetingDates[0].isSelected", equalTo(responseIsSelected1)))
                        .andExpect(jsonPath("$.data.meetingDates[1].id", equalTo(responseDateId2), Long.class))
                        .andExpect(jsonPath("$.data.meetingDates[1].date", equalTo(responseDateDate2.toString())))
                        .andExpect(jsonPath("$.data.meetingDates[1].userCount", equalTo(responseDateUserCount2)))
                        .andExpect(jsonPath("$.data.meetingDates[1].dateStatus", equalTo(responseDateDateStatus2.name())))
                        .andExpect(jsonPath("$.data.meetingDates[1].isSelected", equalTo(responseIsSelected2)))

                        // MeetingPlaces
                        .andExpect(jsonPath("$.data.meetingPlaces[0].id", equalTo(responsePlaceId1), Long.class))
                        .andExpect(jsonPath("$.data.meetingPlaces[0].apiId", equalTo(responsePlaceApiId1), Long.class))
                        .andExpect(jsonPath("$.data.meetingPlaces[0].category", equalTo(responsePlaceCategory1.name())))
                        .andExpect(jsonPath("$.data.meetingPlaces[0].name", equalTo(responsePlaceName1)))
                        .andExpect(jsonPath("$.data.meetingPlaces[0].address", equalTo(responsePlaceAddress1)))
                        .andExpect(jsonPath("$.data.meetingPlaces[0].memo", equalTo(responsePlaceMemo1)))
                        .andExpect(jsonPath("$.data.meetingPlaces[0].lat", equalTo(responsePlaceLat1)))
                        .andExpect(jsonPath("$.data.meetingPlaces[0].lng", equalTo(responsePlaceLng1)))
                        .andExpect(jsonPath("$.data.meetingPlaces[0].order", equalTo(responsePlaceOrder1)))
                        .andExpect(jsonPath("$.data.meetingPlaces[1].id", equalTo(responsePlaceId2), Long.class))
                        .andExpect(jsonPath("$.data.meetingPlaces[1].apiId", equalTo(responsePlaceApiId2), Long.class))
                        .andExpect(jsonPath("$.data.meetingPlaces[1].category", equalTo(responsePlaceCategory2.name())))
                        .andExpect(jsonPath("$.data.meetingPlaces[1].name", equalTo(responsePlaceName2)))
                        .andExpect(jsonPath("$.data.meetingPlaces[1].address", equalTo(responsePlaceAddress2)))
                        .andExpect(jsonPath("$.data.meetingPlaces[1].memo", equalTo(responsePlaceMemo2)))
                        .andExpect(jsonPath("$.data.meetingPlaces[1].lat", equalTo(responsePlaceLat2)))
                        .andExpect(jsonPath("$.data.meetingPlaces[1].lng", equalTo(responsePlaceLng2)))
                        .andExpect(jsonPath("$.data.meetingPlaces[1].order", equalTo(responsePlaceOrder2)))

                        .andDo(document("meeting-detail-normal",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????????????? ????????? ID")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("id").type(JsonFieldType.NUMBER).description("????????? ID"),
                                        fieldWithPath("myMeetingUserId").type(JsonFieldType.NUMBER).description("?????? ???????????? ????????? ?????? ????????? ID"),
                                        fieldWithPath("myMeetingRole").type(JsonFieldType.STRING).description("?????? ???????????? ????????? ?????? ????????? ??????").attributes(key("format").value("HOST, EDITOR, PARTICIPANT")),
                                        fieldWithPath("title").type(JsonFieldType.STRING).description("????????? ??????"),
                                        fieldWithPath("startDate").type(JsonFieldType.STRING).description("????????? ?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        fieldWithPath("endDate").type(JsonFieldType.STRING).description("????????? ?????????").attributes(key("format").value("yyyy-MM-dd")),
                                        subsectionWithPath("meetingUsers").type(JsonFieldType.ARRAY).description("????????? ????????? ?????????"),
                                        subsectionWithPath("meetingDates").type(JsonFieldType.ARRAY).description("???????????? ????????? ?????????"),
                                        subsectionWithPath("meetingPlaces").type(JsonFieldType.ARRAY).description("????????? ?????? ?????????")
                                ),
                                responseFields(beneathPath("data.meetingUsers.[]").withSubsectionId("meeting-users"),
                                        fieldWithPath("id").type(JsonFieldType.NUMBER).description("?????? ????????? ID"),
                                        fieldWithPath("nickname").type(JsonFieldType.STRING).description("?????? ????????? ?????????").optional(),
                                        fieldWithPath("imageLink").type(JsonFieldType.STRING).description("?????? ????????? ????????? ????????? ??????").optional(),
                                        fieldWithPath("meetingRole").type(JsonFieldType.STRING).description("?????? ????????? ??????").attributes(key("format").value("HOST, EDITOR, PARTICIPANT"))
                                ),
                                responseFields(beneathPath("data.meetingDates.[]").withSubsectionId("meeting-dates"),
                                        fieldWithPath("id").type(JsonFieldType.NUMBER).description("?????? ????????? ID"),
                                        fieldWithPath("date").type(JsonFieldType.STRING).description("?????? ????????? ??????").attributes(key("format").value("yyyy-MM-dd")),
                                        fieldWithPath("userCount").type(JsonFieldType.NUMBER).description("?????? ?????? ????????? ????????? ?????? ???"),
                                        fieldWithPath("dateStatus").type(JsonFieldType.STRING).description("?????? ?????? ????????? ?????? ??????").attributes(key("format").value("FIXED, UNFIXED")),
                                        fieldWithPath("isSelected").type(JsonFieldType.BOOLEAN).description("?????? ????????? ?????? ?????? ?????? ??????")
                                ),
                                responseFields(beneathPath("data.meetingPlaces.[]").withSubsectionId("meeting-places"),
                                        fieldWithPath("id").type(JsonFieldType.NUMBER).description("?????? ????????? ID"),
                                        fieldWithPath("apiId").type(JsonFieldType.NUMBER).description("?????? ????????? ????????? ??? API ID"),
                                        fieldWithPath("category").type(JsonFieldType.STRING).description("?????? ????????? ????????????").attributes(key("format").value(categoryLink)),
                                        fieldWithPath("name").type(JsonFieldType.STRING).description("?????? ????????? ??????"),
                                        fieldWithPath("address").type(JsonFieldType.STRING).description("?????? ????????? ??????"),
                                        fieldWithPath("memo").type(JsonFieldType.STRING).description("?????? ????????? ??????"),
                                        fieldWithPath("lat").type(JsonFieldType.NUMBER).description("?????? ????????? ??????"),
                                        fieldWithPath("lng").type(JsonFieldType.NUMBER).description("?????? ????????? ??????"),
                                        fieldWithPath("order").type(JsonFieldType.NUMBER).description("?????? ????????? ??????")
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
                willThrow(new CustomException("?????? ID??? ???????????? ????????? ??????", ErrorCode.ENTITY_NOT_FOUND))
                        .given(meetingQueryService).getDetail(mockedNonexistentMeetingId, mockedParticipantUserId);

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(get("/meetings/{meetingId}", mockedNonexistentMeetingId)
                                .header("Authorization", participantUserToken)
                        )

                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("meeting-detail-error-meeting-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????????????? ????????? ID")
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

                Long unJoinedUserId = 20L;
                willThrow(new CustomException("?????? ID??? ???????????? ????????? ??????", ErrorCode.MEETING_USER_NOT_INCLUDE))
                        .given(meetingQueryService).getDetail(mockedExistentMeetingId, unJoinedUserId);

                String unJoinedUserToken = createToken(unJoinedUserId);

                mockMvc.perform(get("/meetings/{meetingId}", mockedExistentMeetingId)
                                .header("Authorization", unJoinedUserToken)
                        )

                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getMessage())))

                        .andDo(document("meeting-detail-error-not-joined",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("??????????????? ????????? ID")
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