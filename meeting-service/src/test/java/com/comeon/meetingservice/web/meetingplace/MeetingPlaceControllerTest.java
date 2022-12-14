package com.comeon.meetingservice.web.meetingplace;

import com.comeon.meetingservice.common.exception.CustomException;
import com.comeon.meetingservice.common.exception.ErrorCode;
import com.comeon.meetingservice.domain.meetingplace.dto.MeetingPlaceAddDto;
import com.comeon.meetingservice.domain.meetingplace.dto.MeetingPlaceModifyDto;
import com.comeon.meetingservice.domain.meetingplace.dto.MeetingPlaceRemoveDto;
import com.comeon.meetingservice.domain.meetingplace.entity.PlaceCategory;
import com.comeon.meetingservice.web.ControllerTestBase;
import com.comeon.meetingservice.web.common.response.ApiResponseCode;
import com.comeon.meetingservice.web.common.response.ListResponse;
import com.comeon.meetingservice.web.meetingplace.request.MeetingPlaceModifyRequest;
import com.comeon.meetingservice.web.meetingplace.request.MeetingPlaceAddRequest;
import com.comeon.meetingservice.web.meetingplace.response.MeetingPlaceDetailResponse;
import com.comeon.meetingservice.web.meetingplace.response.MeetingPlaceListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.ArrayList;
import java.util.List;

import static com.comeon.meetingservice.common.exception.ErrorCode.ENTITY_NOT_FOUND;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class MeetingPlaceControllerTest extends ControllerTestBase {

    @Nested
    @DisplayName("???????????? ??????")
    class ?????????????????? {

        @Nested
        @DisplayName("????????????")
        class ???????????? {

            @Test
            @DisplayName("?????? ?????? ???????????? ????????? ?????? Created??? ????????? ID??? ????????????.")
            public void ??????_??????() throws Exception {

                Long addedApiId = 500L;
                Double addedLat = 10.1;
                Double addedLng = 20.1;
                String addedName = "name";
                String addedAddress = "address";
                PlaceCategory addedCategory = PlaceCategory.BAR;
                String addedMemo = "memo";

                MeetingPlaceAddDto normalDto = MeetingPlaceAddDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .apiId(addedApiId)
                        .lat(addedLat)
                        .lng(addedLng)
                        .name(addedName)
                        .address(addedAddress)
                        .category(addedCategory)
                        .memo(addedMemo)
                        .build();

                Long createPlaceId = 10L;

                given(meetingPlaceService.add(refEq(normalDto))).willReturn(createPlaceId);

                MeetingPlaceAddRequest meetingPlaceAddRequest =
                        MeetingPlaceAddRequest.builder()
                                .name(addedName)
                                .lat(addedLat)
                                .lng(addedLng)
                                .apiId(addedApiId)
                                .category(addedCategory)
                                .address(addedAddress)
                                .memo(addedMemo)
                                .build();

                String editorUserToken = createToken(mockedEditorUserId);

                mockMvc.perform(post("/meetings/{meetingId}/places", mockedExistentMeetingId)
                                .header("Authorization", editorUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingPlaceAddRequest))
                        )

                        .andExpect(status().isCreated())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))
                        .andExpect(jsonPath("$.data", equalTo(createPlaceId), Long.class))

                        .andDo(document("place-create-normal",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("?????? ????????? ??????????????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("apiId").description("????????? ????????? ????????? API ID"),
                                        fieldWithPath("name").description("????????? ????????? ??????"),
                                        fieldWithPath("address").description("????????? ????????? ??????"),
                                        fieldWithPath("lat").description("????????? ????????? ??????"),
                                        fieldWithPath("lng").description("????????? ????????? ??????"),
                                        fieldWithPath("category").description("????????? ????????? ????????????").attributes(key("format").value(categoryLink)),
                                        fieldWithPath("memo").description("????????? ????????? ??????").optional()
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

                Long addedApiId = 500L;
                Double addedLat = 10.1;
                Double addedLng = 20.1;
                String addedName = "name";
                String addedAddress = "address";
                PlaceCategory addedCategory = PlaceCategory.BAR;
                String addedMemo = "memo";

                MeetingPlaceAddDto nonexistentDto = MeetingPlaceAddDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .apiId(addedApiId)
                        .lat(addedLat)
                        .lng(addedLng)
                        .name(addedName)
                        .address(addedAddress)
                        .category(addedCategory)
                        .memo(addedMemo)
                        .build();

                willThrow(new CustomException("?????? ID??? ???????????? ????????? ?????? ??? ??????", ErrorCode.ENTITY_NOT_FOUND))
                        .given(meetingPlaceService).add(refEq(nonexistentDto));

                MeetingPlaceAddRequest meetingPlaceAddRequest =
                        MeetingPlaceAddRequest.builder()
                                .name(addedName)
                                .address(addedAddress)
                                .lat(addedLat)
                                .lng(addedLng)
                                .apiId(addedApiId)
                                .category(addedCategory)
                                .memo(addedMemo)
                                .build();

                String editorUserToken = createToken(mockedEditorUserId);

                mockMvc.perform(post("/meetings/{meetingId}/places", mockedNonexistentMeetingId)
                                .header("Authorization", editorUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingPlaceAddRequest))
                        )

                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))


                        .andDo(document("place-create-error-meeting-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("?????? ????????? ??????????????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("apiId").description("????????? ????????? ????????? API ID"),
                                        fieldWithPath("name").description("????????? ????????? ??????"),
                                        fieldWithPath("address").description("????????? ????????? ??????"),
                                        fieldWithPath("lat").description("????????? ????????? ??????"),
                                        fieldWithPath("lng").description("????????? ????????? ??????"),
                                        fieldWithPath("category").description("????????? ????????? ????????????").attributes(key("format").value(categoryLink)),
                                        fieldWithPath("memo").description("????????? ????????? ??????").optional()
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ???????????? ????????? ?????? ?????? Bad Request??? ????????????.")
            public void ??????_???????????????() throws Exception {

                Long addedApiId = 500L;
                Double addedLat = 10.1;
                Double addedLng = 20.1;
                String addedName = "name";
                String addedAddress = "address";
                PlaceCategory addedCategory = PlaceCategory.BAR;
                String addedMemo = "memo";

                MeetingPlaceAddDto normalDto = MeetingPlaceAddDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .apiId(addedApiId)
                        .lat(addedLat)
                        .lng(addedLng)
                        .name(addedName)
                        .address(addedAddress)
                        .category(addedCategory)
                        .memo(addedMemo)
                        .build();

                Long createPlaceId = 10L;

                given(meetingPlaceService.add(refEq(normalDto))).willReturn(createPlaceId);

                MeetingPlaceAddRequest meetingPlaceAddRequest =
                        MeetingPlaceAddRequest.builder()
                                .name(addedName)
                                .lat(addedLat)
                                .lng(addedLng)
                                .apiId(addedApiId)
                                .address(addedAddress)
                                .build();

                String editorUserToken = createToken(mockedEditorUserId);

                mockMvc.perform(post("/meetings/{meetingId}/places", mockedExistentMeetingId)
                                .header("Authorization", editorUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingPlaceAddRequest))
                        )

                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.BAD_PARAMETER.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.VALIDATION_FAIL.getCode())))

                        .andDo(document("place-create-error-param",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("?????? ????????? ??????????????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("apiId").description("????????? ????????? ????????? API ID"),
                                        fieldWithPath("name").description("????????? ????????? ??????"),
                                        fieldWithPath("address").description("????????? ????????? ??????"),
                                        fieldWithPath("lat").description("????????? ????????? ??????"),
                                        fieldWithPath("lng").description("????????? ????????? ??????"),
                                        fieldWithPath("category").description("????????? ????????? ????????????").attributes(key("format").value(categoryLink)),
                                        fieldWithPath("memo").description("????????? ????????? ??????").optional()
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.OBJECT).description("?????? ?????????"),
                                        fieldWithPath("message.category").type(JsonFieldType.ARRAY).description("????????? ????????? ??????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("????????? ???????????? ?????? ????????? ????????? ?????? ?????? Forbidden??? ????????????.")
            public void ??????_???????????????() throws Exception {

                Long addedApiId = 500L;
                Double addedLat = 10.1;
                Double addedLng = 20.1;
                String addedName = "name";
                String addedAddress = "address";
                PlaceCategory addedCategory = PlaceCategory.BAR;
                String addedMemo = "memo";

                MeetingPlaceAddDto normalDto = MeetingPlaceAddDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .apiId(addedApiId)
                        .lat(addedLat)
                        .lng(addedLng)
                        .name(addedName)
                        .address(addedAddress)
                        .category(addedCategory)
                        .memo(addedMemo)
                        .build();

                Long createPlaceId = 10L;

                given(meetingPlaceService.add(refEq(normalDto))).willReturn(createPlaceId);

                MeetingPlaceAddRequest meetingPlaceAddRequest =
                        MeetingPlaceAddRequest.builder()
                                .name(addedName)
                                .address(addedAddress)
                                .lat(addedLat)
                                .lng(addedLng)
                                .apiId(addedApiId)
                                .category(addedCategory)
                                .memo(addedMemo)
                                .build();

                String unJoinedUserToken = createToken(10L);

                mockMvc.perform(post("/meetings/{meetingId}/places", mockedExistentMeetingId)
                                .header("Authorization", unJoinedUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingPlaceAddRequest))
                        )

                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getMessage())))

                        .andDo(document("place-create-error-not-joined",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("?????? ????????? ??????????????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("apiId").description("????????? ????????? ????????? API ID"),
                                        fieldWithPath("name").description("????????? ????????? ??????"),
                                        fieldWithPath("address").description("????????? ????????? ??????"),
                                        fieldWithPath("lat").description("????????? ????????? ??????"),
                                        fieldWithPath("lng").description("????????? ????????? ??????"),
                                        fieldWithPath("category").description("????????? ????????? ????????????").attributes(key("format").value(categoryLink)),
                                        fieldWithPath("memo").description("????????? ????????? ??????").optional()
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("HOST, EDITOR??? ?????? ????????? ????????? ?????? ?????? Forbidden??? ????????????.")
            public void ??????_????????????() throws Exception {

                Long addedApiId = 500L;
                Double addedLat = 10.1;
                Double addedLng = 20.1;
                String addedName = "name";
                String addedAddress = "address";
                PlaceCategory addedCategory = PlaceCategory.BAR;
                String addedMemo = "memo";

                MeetingPlaceAddDto normalDto = MeetingPlaceAddDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .apiId(addedApiId)
                        .lat(addedLat)
                        .lng(addedLng)
                        .name(addedName)
                        .address(addedAddress)
                        .category(addedCategory)
                        .memo(addedMemo)
                        .build();

                Long createPlaceId = 10L;

                given(meetingPlaceService.add(refEq(normalDto))).willReturn(createPlaceId);

                MeetingPlaceAddRequest meetingPlaceAddRequest =
                        MeetingPlaceAddRequest.builder()
                                .name(addedName)
                                .address(addedAddress)
                                .lat(addedLat)
                                .lng(addedLng)
                                .apiId(addedApiId)
                                .category(addedCategory)
                                .memo(addedMemo)
                                .build();

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(post("/meetings/{meetingId}/places", mockedExistentMeetingId)
                                .header("Authorization", participantUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingPlaceAddRequest))
                        )

                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.AUTHORIZATION_FAIL.getCode())))

                        .andDo(document("place-create-error-authorization",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("?????? ????????? ??????????????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("apiId").description("????????? ????????? ????????? API ID"),
                                        fieldWithPath("name").description("????????? ????????? ??????"),
                                        fieldWithPath("address").description("????????? ????????? ??????"),
                                        fieldWithPath("lat").description("????????? ????????? ??????"),
                                        fieldWithPath("lng").description("????????? ????????? ??????"),
                                        fieldWithPath("category").description("????????? ????????? ????????????").attributes(key("format").value(categoryLink)),
                                        fieldWithPath("memo").description("????????? ????????? ??????").optional()
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
        @DisplayName("?????? ??????")
        class ???????????? {

            @Test
            @DisplayName("?????? ?????? ????????? ????????? ?????? apiId, name, lat, lng, category ????????? ????????? OK??? ????????????.")
            public void ??????_????????????() throws Exception {

                Long modifiedApiId = 1000L;
                Double modifiedLat = 10.1;
                Double modifiedLng = 20.1;
                String modifiedName = "name";
                String modifiedAddress = "address";
                PlaceCategory modifiedCategory = PlaceCategory.CAFE;
                Long existentPlaceId = 10L;

                MeetingPlaceModifyDto modifyInfoDto = MeetingPlaceModifyDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentPlaceId)
                        .apiId(modifiedApiId)
                        .lat(modifiedLat)
                        .lng(modifiedLng)
                        .name(modifiedName)
                        .address(modifiedAddress)
                        .category(modifiedCategory)
                        .build();

                willDoNothing().given(meetingPlaceService).modify(refEq(modifyInfoDto));

                MeetingPlaceModifyRequest meetingPlaceModifyRequest =
                        MeetingPlaceModifyRequest.builder()
                                .apiId(modifiedApiId)
                                .name(modifiedName)
                                .lat(modifiedLat)
                                .lng(modifiedLng)
                                .category(modifiedCategory)
                                .address(modifiedAddress)
                                .build();

                String editorUserToken = createToken(mockedEditorUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/places/{placeId}", mockedExistentMeetingId, existentPlaceId)
                                .header("Authorization", editorUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingPlaceModifyRequest))
                        )

                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))

                        .andDo(document("place-modify-normal-info",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID"),
                                        parameterWithName("placeId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("apiId").description("????????? ????????? ????????? ?????? API ID"),
                                        fieldWithPath("name").description("????????? ????????? ??????"),
                                        fieldWithPath("address").description("????????? ????????? ??????"),
                                        fieldWithPath("lat").description("????????? ????????? ??????"),
                                        fieldWithPath("lng").description("????????? ????????? ??????"),
                                        fieldWithPath("category").description("????????? ????????? ????????????").attributes(key("format").value(categoryLink)),
                                        fieldWithPath("memo").description("????????? ????????? ??????").optional(),
                                        fieldWithPath("order").description("????????? ????????? ??????").optional()
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ????????? ????????? ?????? memo ????????? ????????? OK??? ????????????.")
            public void ??????_????????????() throws Exception {

                String modifiedMemo = "memo";
                Long existentPlaceId = 10L;

                MeetingPlaceModifyDto modifyMemoDto = MeetingPlaceModifyDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentPlaceId)
                        .memo(modifiedMemo)
                        .build();

                willDoNothing().given(meetingPlaceService).modify(refEq(modifyMemoDto));

                MeetingPlaceModifyRequest meetingPlaceModifyRequest =
                        MeetingPlaceModifyRequest.builder()
                                .memo(modifiedMemo)
                                .build();

                String editorUserToken = createToken(mockedEditorUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/places/{placeId}", mockedExistentMeetingId, existentPlaceId)
                                .header("Authorization", editorUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingPlaceModifyRequest))
                        )

                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))

                        .andDo(document("place-modify-normal-memo",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID"),
                                        parameterWithName("placeId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("apiId").description("????????? ????????? ????????? ?????? API ID").optional(),
                                        fieldWithPath("name").description("????????? ????????? ??????").optional(),
                                        fieldWithPath("address").description("????????? ????????? ??????").optional(),
                                        fieldWithPath("lat").description("????????? ????????? ??????").optional(),
                                        fieldWithPath("lng").description("????????? ????????? ??????").optional(),
                                        fieldWithPath("category").description("????????? ????????? ????????????").attributes(key("format").value(categoryLink)).optional(),
                                        fieldWithPath("memo").description("????????? ????????? ??????"),
                                        fieldWithPath("order").description("????????? ????????? ??????").optional()
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ????????? ????????? ?????? order ????????? ????????? OK??? ????????????.")
            public void ??????_????????????() throws Exception {

                Integer modifiedOrder = 5;
                Long existentPlaceId = 10L;

                MeetingPlaceModifyDto modifyOrderDto = MeetingPlaceModifyDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentPlaceId)
                        .order(modifiedOrder)
                        .build();

                willDoNothing().given(meetingPlaceService).modify(refEq(modifyOrderDto));

                MeetingPlaceModifyRequest meetingPlaceModifyRequest =
                        MeetingPlaceModifyRequest.builder()
                                .order(modifiedOrder)
                                .build();

                String editorUserToken = createToken(mockedEditorUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/places/{placeId}", mockedExistentMeetingId, existentPlaceId)
                                .header("Authorization", editorUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingPlaceModifyRequest))
                        )

                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))

                        .andDo(document("place-modify-normal-order",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID"),
                                        parameterWithName("placeId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("apiId").description("????????? ????????? ????????? ?????? API ID").optional(),
                                        fieldWithPath("name").description("????????? ????????? ??????").optional(),
                                        fieldWithPath("address").description("????????? ????????? ??????").optional(),
                                        fieldWithPath("lat").description("????????? ????????? ??????").optional(),
                                        fieldWithPath("lng").description("????????? ????????? ??????").optional(),
                                        fieldWithPath("category").description("????????? ????????? ????????????").attributes(key("format").value(categoryLink)).optional(),
                                        fieldWithPath("memo").description("????????? ????????? ??????").optional(),
                                        fieldWithPath("order").description("????????? ????????? ??????")
                                ))
                        )
                ;
            }
        }

        @Nested
        @DisplayName("??????")
        class ?????? {

            @Test
            @DisplayName("?????? ?????? ????????? ????????? ?????? apiId, name, lat, lng ?????? ??? ???????????? ????????? Bad Request??? ????????????.")
            public void ??????_???????????????????????????() throws Exception {

                Long modifiedApiId = 1000L;
                Double modifiedLat = 10.1;
                Double modifiedLng = 20.1;
                String modifiedName = "name";
                String modifiedAddress = "address";
                Long existentPlaceId = 10L;

                MeetingPlaceModifyDto invalidModifyInfoDto = MeetingPlaceModifyDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentPlaceId)
                        .apiId(modifiedApiId)
                        .lat(modifiedLat)
                        .lng(modifiedLng)
                        .name(modifiedName)
                        .address(modifiedAddress)
                        .build();

                willDoNothing().given(meetingPlaceService).modify(refEq(invalidModifyInfoDto));

                MeetingPlaceModifyRequest meetingPlaceModifyRequest =
                        MeetingPlaceModifyRequest.builder()
                                .name(modifiedName)
                                .lng(modifiedLng)
                                .build();

                String editorUserToken = createToken(mockedEditorUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/places/{placeId}", mockedExistentMeetingId, existentPlaceId)
                                .header("Authorization", editorUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingPlaceModifyRequest))
                        )

                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.BAD_PARAMETER.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.VALIDATION_FAIL.getCode())))

                        .andDo(document("place-modify-error-info",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID"),
                                        parameterWithName("placeId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("apiId").description("????????? ????????? ????????? ?????? API ID"),
                                        fieldWithPath("name").description("????????? ????????? ??????"),
                                        fieldWithPath("address").description("????????? ????????? ??????"),
                                        fieldWithPath("lat").description("????????? ????????? ??????"),
                                        fieldWithPath("lng").description("????????? ????????? ??????"),
                                        fieldWithPath("category").description("????????? ????????? ????????????").attributes(key("format").value(categoryLink)),
                                        fieldWithPath("memo").description("????????? ????????? ??????").optional(),
                                        fieldWithPath("order").description("????????? ????????? ??????").optional()
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.OBJECT).description("?????? ?????????"),
                                        fieldWithPath("message.objectError").type(JsonFieldType.ARRAY).description("????????? ????????? ??????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("?????? ??????????????? ?????? ????????? ?????? ?????? Not Found??? ????????????.")
            public void ??????_???????????????() throws Exception {

                Long modifiedApiId = 1000L;
                Double modifiedLat = 10.1;
                Double modifiedLng = 20.1;
                String modifiedName = "name";
                String modifiedAddress = "address";
                PlaceCategory modifiedCategory = PlaceCategory.CAFE;
                Long nonexistentPlaceId = 20L;

                MeetingPlaceModifyDto nonexistentDto = MeetingPlaceModifyDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(nonexistentPlaceId)
                        .apiId(modifiedApiId)
                        .lat(modifiedLat)
                        .lng(modifiedLng)
                        .name(modifiedName)
                        .address(modifiedAddress)
                        .category(modifiedCategory)
                        .build();

                willThrow(new CustomException("?????? ID??? ???????????? ????????? ?????? ??? ??????", ErrorCode.ENTITY_NOT_FOUND))
                        .given(meetingPlaceService).modify(refEq(nonexistentDto));

                MeetingPlaceModifyRequest meetingPlaceModifyRequest =
                        MeetingPlaceModifyRequest.builder()
                                .apiId(modifiedApiId)
                                .name(modifiedName)
                                .address(modifiedAddress)
                                .lat(modifiedLat)
                                .lng(modifiedLng)
                                .category(modifiedCategory)
                                .build();

                String editorUserToken = createToken(mockedEditorUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/places/{placeId}", mockedExistentMeetingId, nonexistentPlaceId)
                                .header("Authorization", editorUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingPlaceModifyRequest))
                        )
                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("place-modify-error-place-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID"),
                                        parameterWithName("placeId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("apiId").description("????????? ????????? ????????? ?????? API ID"),
                                        fieldWithPath("name").description("????????? ????????? ??????"),
                                        fieldWithPath("address").description("????????? ????????? ??????"),
                                        fieldWithPath("lat").description("????????? ????????? ??????"),
                                        fieldWithPath("lng").description("????????? ????????? ??????"),
                                        fieldWithPath("category").description("????????? ????????? ????????????").attributes(key("format").value(categoryLink)),
                                        fieldWithPath("memo").description("????????? ????????? ??????").optional(),
                                        fieldWithPath("order").description("????????? ????????? ??????").optional()
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

                Long modifiedApiId = 1000L;
                Double modifiedLat = 10.1;
                Double modifiedLng = 20.1;
                String modifiedName = "name";
                String modifiedAddress = "address";
                PlaceCategory modifiedCategory = PlaceCategory.CAFE;
                Long existentPlaceId = 10L;

                MeetingPlaceModifyDto modifyInfoDto = MeetingPlaceModifyDto.builder()
                        .meetingId(mockedNonexistentMeetingId)
                        .id(existentPlaceId)
                        .apiId(modifiedApiId)
                        .lat(modifiedLat)
                        .lng(modifiedLng)
                        .name(modifiedName)
                        .address(modifiedAddress)
                        .category(modifiedCategory)
                        .build();

                willThrow(new CustomException("?????? ID??? ???????????? ???????????? ??????", ENTITY_NOT_FOUND))
                        .given(meetingPlaceService).modify(refEq(modifyInfoDto));

                MeetingPlaceModifyRequest meetingPlaceModifyRequest =
                        MeetingPlaceModifyRequest.builder()
                                .apiId(modifiedApiId)
                                .name(modifiedName)
                                .lat(modifiedLat)
                                .lng(modifiedLng)
                                .category(modifiedCategory)
                                .address(modifiedAddress)
                                .build();

                String editorUserToken = createToken(mockedEditorUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/places/{placeId}", mockedNonexistentMeetingId, existentPlaceId)
                                .header("Authorization", editorUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingPlaceModifyRequest))
                        )
                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("place-modify-error-meeting-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID"),
                                        parameterWithName("placeId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("apiId").description("????????? ????????? ????????? ?????? API ID"),
                                        fieldWithPath("name").description("????????? ????????? ??????"),
                                        fieldWithPath("address").description("????????? ????????? ??????"),
                                        fieldWithPath("lat").description("????????? ????????? ??????"),
                                        fieldWithPath("lng").description("????????? ????????? ??????"),
                                        fieldWithPath("category").description("????????? ????????? ????????????").attributes(key("format").value(categoryLink)),
                                        fieldWithPath("memo").description("????????? ????????? ??????").optional(),
                                        fieldWithPath("order").description("????????? ????????? ??????").optional()
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("????????? ????????? ????????? ???????????? Forbidden??? ????????????.")
            public void ??????_???????????????() throws Exception {

                Long modifiedApiId = 1000L;
                Double modifiedLat = 10.1;
                Double modifiedLng = 20.1;
                String modifiedName = "name";
                String modifiedAddress = "address";
                PlaceCategory modifiedCategory = PlaceCategory.CAFE;
                Long existentPlaceId = 10L;

                MeetingPlaceModifyDto modifyInfoDto = MeetingPlaceModifyDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentPlaceId)
                        .apiId(modifiedApiId)
                        .lat(modifiedLat)
                        .lng(modifiedLng)
                        .name(modifiedName)
                        .address(modifiedAddress)
                        .category(modifiedCategory)
                        .build();

                willDoNothing().given(meetingPlaceService).modify(refEq(modifyInfoDto));

                MeetingPlaceModifyRequest meetingPlaceModifyRequest =
                        MeetingPlaceModifyRequest.builder()
                                .apiId(modifiedApiId)
                                .name(modifiedName)
                                .lat(modifiedLat)
                                .lng(modifiedLng)
                                .category(modifiedCategory)
                                .address(modifiedAddress)
                                .build();

                String unJoinedUserToken = createToken(10L);

                mockMvc.perform(patch("/meetings/{meetingId}/places/{placeId}", mockedExistentMeetingId, existentPlaceId)
                                .header("Authorization", unJoinedUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingPlaceModifyRequest))
                        )
                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getMessage())))

                        .andDo(document("place-modify-error-not-joined",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID"),
                                        parameterWithName("placeId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("apiId").description("????????? ????????? ????????? ?????? API ID"),
                                        fieldWithPath("name").description("????????? ????????? ??????"),
                                        fieldWithPath("address").description("????????? ????????? ??????"),
                                        fieldWithPath("lat").description("????????? ????????? ??????"),
                                        fieldWithPath("lng").description("????????? ????????? ??????"),
                                        fieldWithPath("category").description("????????? ????????? ????????????").attributes(key("format").value(categoryLink)),
                                        fieldWithPath("memo").description("????????? ????????? ??????").optional(),
                                        fieldWithPath("order").description("????????? ????????? ??????").optional()
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("HOST, EDITOR??? ?????? ????????? ????????? ?????? ?????? Forbidden??? ????????????.")
            public void ??????_????????????() throws Exception {

                Long modifiedApiId = 1000L;
                Double modifiedLat = 10.1;
                Double modifiedLng = 20.1;
                String modifiedName = "name";
                String modifiedAddress = "address";
                PlaceCategory modifiedCategory = PlaceCategory.CAFE;
                Long existentPlaceId = 10L;

                MeetingPlaceModifyDto modifyInfoDto = MeetingPlaceModifyDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentPlaceId)
                        .apiId(modifiedApiId)
                        .lat(modifiedLat)
                        .lng(modifiedLng)
                        .name(modifiedName)
                        .address(modifiedAddress)
                        .category(modifiedCategory)
                        .build();

                willDoNothing().given(meetingPlaceService).modify(refEq(modifyInfoDto));

                MeetingPlaceModifyRequest meetingPlaceModifyRequest =
                        MeetingPlaceModifyRequest.builder()
                                .apiId(modifiedApiId)
                                .name(modifiedName)
                                .address(modifiedAddress)
                                .lat(modifiedLat)
                                .lng(modifiedLng)
                                .category(modifiedCategory)
                                .build();

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(patch("/meetings/{meetingId}/places/{placeId}", mockedExistentMeetingId, existentPlaceId)
                                .header("Authorization", participantUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson(meetingPlaceModifyRequest))
                        )
                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.AUTHORIZATION_FAIL.getCode())))

                        .andDo(document("place-modify-error-authorization",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID"),
                                        parameterWithName("placeId").description("??????????????? ?????? ????????? ID")
                                ),
                                requestFields(
                                        fieldWithPath("apiId").description("????????? ????????? ????????? ?????? API ID"),
                                        fieldWithPath("name").description("????????? ????????? ??????"),
                                        fieldWithPath("address").description("????????? ????????? ??????"),
                                        fieldWithPath("lat").description("????????? ????????? ??????"),
                                        fieldWithPath("lng").description("????????? ????????? ??????"),
                                        fieldWithPath("category").description("????????? ????????? ????????????").attributes(key("format").value(categoryLink)),
                                        fieldWithPath("memo").description("????????? ????????? ??????").optional(),
                                        fieldWithPath("order").description("????????? ????????? ??????").optional()
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
            @DisplayName("?????? ????????? HOST?????? ?????? ???????????? ????????? OK??? ????????????.")
            public void ??????_??????() throws Exception {
                Long existentPlaceId = 10L;
                MeetingPlaceRemoveDto meetingPlaceRemoveDto = MeetingPlaceRemoveDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentPlaceId)
                        .build();

                willDoNothing().given(meetingPlaceService).remove(refEq(meetingPlaceRemoveDto));

                String editorUserToken = createToken(mockedEditorUserId);

                mockMvc.perform(delete("/meetings/{meetingId}/places/{placeId}", mockedExistentMeetingId, existentPlaceId)
                                .header("Authorization", editorUserToken)
                        )

                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))

                        .andDo(document("place-delete-normal",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID"),
                                        parameterWithName("placeId").description("??????????????? ?????? ????????? ID")
                                ))
                        )
                ;
            }

        }

        @Nested
        @DisplayName("??????")
        class ?????? {

            @Test
            @DisplayName("?????? ??????????????? ?????? ????????? ?????? ?????? Not Found??? ????????????.")
            public void ??????_???????????????() throws Exception {

                Long nonexistentPlaceId = 20L;
                MeetingPlaceRemoveDto meetingPlaceRemoveDto = MeetingPlaceRemoveDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(nonexistentPlaceId)
                        .build();

                willThrow(new CustomException("?????? ID??? ???????????? ????????? ?????? ??? ??????", ErrorCode.ENTITY_NOT_FOUND))
                        .given(meetingPlaceService).remove(refEq(meetingPlaceRemoveDto));

                String editorUserToken = createToken(mockedEditorUserId);

                mockMvc.perform(delete("/meetings/{meetingId}/places/{placeId}", mockedExistentMeetingId, nonexistentPlaceId)
                                .header("Authorization", editorUserToken)
                        )
                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("place-delete-error-place-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID"),
                                        parameterWithName("placeId").description("??????????????? ?????? ????????? ID")
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

                Long existentPlaceId = 10L;
                MeetingPlaceRemoveDto meetingPlaceRemoveDto = MeetingPlaceRemoveDto.builder()
                        .meetingId(mockedNonexistentMeetingId)
                        .id(existentPlaceId)
                        .build();

                willThrow(new CustomException("?????? ID??? ???????????? ???????????? ?????? ??? ??????", ErrorCode.ENTITY_NOT_FOUND))
                        .given(meetingPlaceService).remove(refEq(meetingPlaceRemoveDto));

                String editorUserToken = createToken(mockedEditorUserId);

                mockMvc.perform(delete("/meetings/{meetingId}/places/{placeId}", mockedNonexistentMeetingId, existentPlaceId)
                                .header("Authorization", editorUserToken)
                        )
                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("place-delete-error-meeting-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID"),
                                        parameterWithName("placeId").description("??????????????? ?????? ????????? ID")
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

                Long existentPlaceId = 10L;
                MeetingPlaceRemoveDto meetingPlaceRemoveDto = MeetingPlaceRemoveDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentPlaceId)
                        .build();

                willDoNothing().given(meetingPlaceService).remove(refEq(meetingPlaceRemoveDto));

                String unJoinedUserToken = createToken(10L);

                mockMvc.perform(delete("/meetings/{meetingId}/places/{placeId}", mockedExistentMeetingId, existentPlaceId)
                                .header("Authorization", unJoinedUserToken)
                        )
                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getMessage())))

                        .andDo(document("place-delete-error-not-joined",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID"),
                                        parameterWithName("placeId").description("??????????????? ?????? ????????? ID")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("HOST, EDITOR??? ?????? ????????? ????????? ?????? ?????? Forbidden??? ????????????.")
            public void ??????_????????????() throws Exception {

                Long existentPlaceId = 10L;
                MeetingPlaceRemoveDto meetingPlaceRemoveDto = MeetingPlaceRemoveDto.builder()
                        .meetingId(mockedExistentMeetingId)
                        .id(existentPlaceId)
                        .build();

                willDoNothing().given(meetingPlaceService).remove(refEq(meetingPlaceRemoveDto));

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(delete("/meetings/{meetingId}/places/{placeId}", mockedExistentMeetingId, existentPlaceId)
                                .header("Authorization", participantUserToken)
                        )
                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.AUTHORIZATION_FAIL.getCode())))

                        .andDo(document("place-delete-error-authorization",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? EDITOR, HOST ??? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID"),
                                        parameterWithName("placeId").description("??????????????? ?????? ????????? ID")
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
    @DisplayName("???????????? ?????? - ??????")
    class ???????????????????????? {

        @Nested
        @DisplayName("????????????")
        class ???????????? {

            @Test
            @DisplayName("??????????????? ????????? ?????? OK??? ?????? ????????? ????????????.")
            public void ??????_??????() throws Exception {

                Long existentPlaceId = 10L;
                Long returnApiId = 1000L;
                PlaceCategory returnCategory = PlaceCategory.CAFE;
                String returnMemo = "memo";
                String returnName = "place name";
                String returnAddress = "address";
                Double returnLat = 10.1;
                Double returnLng = 20.1;

                MeetingPlaceDetailResponse returnResponse = MeetingPlaceDetailResponse.builder()
                        .id(existentPlaceId)
                        .apiId(returnApiId)
                        .category(returnCategory)
                        .memo(returnMemo)
                        .name(returnName)
                        .address(returnAddress)
                        .lat(returnLat)
                        .lng(returnLng)
                        .build();

                given(meetingPlaceQueryService.getDetail(eq(mockedExistentMeetingId), eq(existentPlaceId)))
                        .willReturn(returnResponse);

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(get("/meetings/{meetingId}/places/{placeId}", mockedExistentMeetingId, existentPlaceId)
                                .header("Authorization", participantUserToken)
                        )
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))
                        .andExpect(jsonPath("$.data.name", equalTo(returnName)))
                        .andExpect(jsonPath("$.data.id", equalTo(existentPlaceId), Long.class))
                        .andExpect(jsonPath("$.data.apiId", equalTo(returnApiId), Long.class))
                        .andExpect(jsonPath("$.data.category", equalTo(returnCategory.name())))
                        .andExpect(jsonPath("$.data.address", equalTo(returnAddress)))
                        .andExpect(jsonPath("$.data.memo", equalTo(returnMemo)))
                        .andExpect(jsonPath("$.data.lat", equalTo(returnLat)))
                        .andExpect(jsonPath("$.data.lng", equalTo(returnLng)))

                        .andDo(document("place-detail-normal",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID"),
                                        parameterWithName("placeId").description("??????????????? ?????? ????????? ID")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("id").type(JsonFieldType.NUMBER).description("?????? ????????? ID"),
                                        fieldWithPath("apiId").type(JsonFieldType.NUMBER).description("?????? ????????? ????????? API ID"),
                                        fieldWithPath("category").type(JsonFieldType.STRING).description("?????? ????????? ????????????").attributes(key("format").value(categoryLink)),
                                        fieldWithPath("memo").type(JsonFieldType.STRING).description("?????? ????????? ??????").optional(),
                                        fieldWithPath("name").type(JsonFieldType.STRING).description("?????? ????????? ??????"),
                                        fieldWithPath("address").type(JsonFieldType.STRING).description("?????? ????????? ??????"),
                                        fieldWithPath("lat").type(JsonFieldType.NUMBER).description("?????? ????????? ??????"),
                                        fieldWithPath("lng").type(JsonFieldType.NUMBER).description("?????? ????????? ??????")
                                ))
                        )
                ;
            }
        }

        @Nested
        @DisplayName("??????")
        class ?????? {

            @Test
            @DisplayName("?????? ?????? ????????? ?????? ????????? ?????? ?????? Not Found??? ????????????")
            public void ??????_???????????????() throws Exception {

                Long nonexistentPlaceId = 20L;

                willThrow(new CustomException("?????? ID??? ???????????? ?????? ????????? ?????? ??? ??????", ErrorCode.ENTITY_NOT_FOUND))
                        .given(meetingPlaceQueryService).getDetail(eq(mockedExistentMeetingId), eq(nonexistentPlaceId));

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(get("/meetings/{meetingId}/places/{placeId}", mockedExistentMeetingId, nonexistentPlaceId)
                                .header("Authorization", participantUserToken)
                        )

                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("place-detail-error-place-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID"),
                                        parameterWithName("placeId").description("??????????????? ?????? ????????? ID")
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

                Long existentPlaceId = 10L;
                Long returnApiId = 1000L;
                PlaceCategory returnCategory = PlaceCategory.CAFE;
                String returnMemo = "memo";
                String returnName = "place name";
                String returnAddress = "address";
                Double returnLat = 10.1;
                Double returnLng = 20.1;

                MeetingPlaceDetailResponse returnResponse = MeetingPlaceDetailResponse.builder()
                        .id(existentPlaceId)
                        .apiId(returnApiId)
                        .category(returnCategory)
                        .memo(returnMemo)
                        .name(returnName)
                        .address(returnAddress)
                        .lat(returnLat)
                        .lng(returnLng)
                        .build();

                given(meetingPlaceQueryService.getDetail(eq(mockedExistentMeetingId), eq(existentPlaceId)))
                        .willReturn(returnResponse);

                willThrow(new CustomException("?????? ID??? ???????????? ???????????? ?????? ??? ??????", ErrorCode.ENTITY_NOT_FOUND))
                        .given(meetingPlaceQueryService).getDetail(eq(mockedNonexistentMeetingId), eq(existentPlaceId));

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(get("/meetings/{meetingId}/places/{placeId}", mockedNonexistentMeetingId, existentPlaceId)
                                .header("Authorization", participantUserToken)
                        )

                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("place-detail-error-meeting-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID"),
                                        parameterWithName("placeId").description("??????????????? ?????? ????????? ID")
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

                Long existentPlaceId = 10L;
                Long returnApiId = 1000L;
                PlaceCategory returnCategory = PlaceCategory.CAFE;
                String returnMemo = "memo";
                String returnName = "place name";
                String returnAddress = "address";
                Double returnLat = 10.1;
                Double returnLng = 20.1;

                MeetingPlaceDetailResponse returnResponse = MeetingPlaceDetailResponse.builder()
                        .id(existentPlaceId)
                        .apiId(returnApiId)
                        .category(returnCategory)
                        .memo(returnMemo)
                        .name(returnName)
                        .address(returnAddress)
                        .lat(returnLat)
                        .lng(returnLng)
                        .build();

                given(meetingPlaceQueryService.getDetail(eq(mockedExistentMeetingId), eq(existentPlaceId)))
                        .willReturn(returnResponse);

                String unJoinedToken = createToken(10L);

                mockMvc.perform(get("/meetings/{meetingId}/places/{placeId}", mockedExistentMeetingId, existentPlaceId)
                                .header("Authorization", unJoinedToken)
                        )
                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getMessage())))

                        .andDo(document("place-detail-error-not-joined",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID"),
                                        parameterWithName("placeId").description("??????????????? ?????? ????????? ID")
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
    @DisplayName("???????????? ?????? - ?????????")
    class ???????????????????????????{

        @Nested
        @DisplayName("????????????")
        class ???????????? {

            @Test
            @DisplayName("??????????????? ????????? ?????? OK??? ?????? ?????? ???????????? ????????????.")
            public void ??????_??????() throws Exception {

                Long returnId1 = 10L;
                Long returnApiId1 = 1000L;
                PlaceCategory returnCategory1 = PlaceCategory.CAFE;
                String returnMemo1 = "memo1";
                String returnName1 = "place name1";
                String returnAddress1 = "address1";
                Double returnLat1 = 10.1;
                Double returnLng1 = 20.1;
                Integer returnOrder1 = 1;

                MeetingPlaceListResponse returnPlaceResponse1 = MeetingPlaceListResponse.builder()
                        .id(returnId1)
                        .apiId(returnApiId1)
                        .category(returnCategory1)
                        .name(returnName1)
                        .address(returnAddress1)
                        .lat(returnLat1)
                        .lng(returnLng1)
                        .memo(returnMemo1)
                        .order(returnOrder1)
                        .build();

                Long returnId2 = 11L;
                Long returnApiId2 = 2000L;
                PlaceCategory returnCategory2 = PlaceCategory.ACCOMMODATION;
                String returnMemo2 = "memo2";
                String returnName2 = "place name2";
                String returnAddress2 = "address2";
                Double returnLat2 = 110.1;
                Double returnLng2 = 90.1;
                Integer returnOrder2 = 2;

                MeetingPlaceListResponse returnPlaceResponse2 = MeetingPlaceListResponse.builder()
                        .id(returnId2)
                        .apiId(returnApiId2)
                        .category(returnCategory2)
                        .name(returnName2)
                        .address(returnAddress2)
                        .lat(returnLat2)
                        .lng(returnLng2)
                        .memo(returnMemo2)
                        .order(returnOrder2)
                        .build();

                Long returnId3 = 12L;
                Long returnApiId3 = 3000L;
                PlaceCategory returnCategory3 = PlaceCategory.ACCOMMODATION;
                String returnMemo3 = "memo3";
                String returnName3 = "place name3";
                String returnAddress3 = "address3";
                Double returnLat3 = 50.1;
                Double returnLng3 = 20.1;
                Integer returnOrder3 = 3;

                MeetingPlaceListResponse returnPlaceResponse3 = MeetingPlaceListResponse.builder()
                        .id(returnId3)
                        .apiId(returnApiId3)
                        .category(returnCategory3)
                        .name(returnName3)
                        .address(returnAddress3)
                        .lat(returnLat3)
                        .lng(returnLng3)
                        .memo(returnMemo3)
                        .order(returnOrder3)
                        .build();

                List<MeetingPlaceListResponse> returnPlaceReponses = new ArrayList<>();
                returnPlaceReponses.add(returnPlaceResponse1);
                returnPlaceReponses.add(returnPlaceResponse2);
                returnPlaceReponses.add(returnPlaceResponse3);

                ListResponse<MeetingPlaceListResponse> returnResponse
                        = ListResponse.createListResponse(returnPlaceReponses);

                given(meetingPlaceQueryService.getList(eq(mockedExistentMeetingId)))
                        .willReturn(returnResponse);

                String hostUserToken = createToken(mockedHostUserId);

                mockMvc.perform(get("/meetings/{meetingId}/places", mockedExistentMeetingId)
                                .header("Authorization", hostUserToken)
                        )
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.SUCCESS.name())))
                        .andExpect(jsonPath("$.data.count", equalTo(returnPlaceReponses.size())))

                        .andExpect(jsonPath("$.data.contents[0].id", equalTo(returnId1), Long.class))
                        .andExpect(jsonPath("$.data.contents[0].apiId", equalTo(returnApiId1), Long.class))
                        .andExpect(jsonPath("$.data.contents[0].category", equalTo(returnCategory1.name())))
                        .andExpect(jsonPath("$.data.contents[0].name", equalTo(returnName1)))
                        .andExpect(jsonPath("$.data.contents[0].address", equalTo(returnAddress1)))
                        .andExpect(jsonPath("$.data.contents[0].lat", equalTo(returnLat1)))
                        .andExpect(jsonPath("$.data.contents[0].lng", equalTo(returnLng1)))
                        .andExpect(jsonPath("$.data.contents[0].memo", equalTo(returnMemo1)))
                        .andExpect(jsonPath("$.data.contents[0].order", equalTo(returnOrder1)))

                        .andExpect(jsonPath("$.data.contents[1].id", equalTo(returnId2), Long.class))
                        .andExpect(jsonPath("$.data.contents[1].apiId", equalTo(returnApiId2), Long.class))
                        .andExpect(jsonPath("$.data.contents[1].category", equalTo(returnCategory2.name())))
                        .andExpect(jsonPath("$.data.contents[1].name", equalTo(returnName2)))
                        .andExpect(jsonPath("$.data.contents[1].address", equalTo(returnAddress2)))
                        .andExpect(jsonPath("$.data.contents[1].lat", equalTo(returnLat2)))
                        .andExpect(jsonPath("$.data.contents[1].lng", equalTo(returnLng2)))
                        .andExpect(jsonPath("$.data.contents[1].memo", equalTo(returnMemo2)))
                        .andExpect(jsonPath("$.data.contents[1].order", equalTo(returnOrder2)))

                        .andExpect(jsonPath("$.data.contents[2].id", equalTo(returnId3), Long.class))
                        .andExpect(jsonPath("$.data.contents[2].apiId", equalTo(returnApiId3), Long.class))
                        .andExpect(jsonPath("$.data.contents[2].category", equalTo(returnCategory3.name())))
                        .andExpect(jsonPath("$.data.contents[2].name", equalTo(returnName3)))
                        .andExpect(jsonPath("$.data.contents[2].address", equalTo(returnAddress3)))
                        .andExpect(jsonPath("$.data.contents[2].lat", equalTo(returnLat3)))
                        .andExpect(jsonPath("$.data.contents[2].lng", equalTo(returnLng3)))
                        .andExpect(jsonPath("$.data.contents[2].memo", equalTo(returnMemo3)))
                        .andExpect(jsonPath("$.data.contents[2].order", equalTo(returnOrder3)))

                        .andDo(document("place-list-normal",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("count").type(JsonFieldType.NUMBER).description("??? ????????? ???????????? ?????? ???"),
                                        subsectionWithPath("contents").type(JsonFieldType.ARRAY).description("?????? ?????????")
                                ),
                                responseFields(beneathPath("data.contents.[]").withSubsectionId("contents"),
                                        fieldWithPath("id").type(JsonFieldType.NUMBER).description("?????? ????????? ID"),
                                        fieldWithPath("apiId").type(JsonFieldType.NUMBER).description("?????? ????????? ????????? ??? API ID"),
                                        fieldWithPath("category").type(JsonFieldType.STRING).description("?????? ????????? ????????????").attributes(key("format").value(categoryLink)),
                                        fieldWithPath("name").type(JsonFieldType.STRING).description("?????? ????????? ??????"),
                                        fieldWithPath("address").type(JsonFieldType.STRING).description("?????? ????????? ??????"),
                                        fieldWithPath("lat").type(JsonFieldType.NUMBER).description("?????? ????????? ??????"),
                                        fieldWithPath("lng").type(JsonFieldType.NUMBER).description("?????? ????????? ??????"),
                                        fieldWithPath("memo").type(JsonFieldType.STRING).description("?????? ????????? ??????"),
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

                ListResponse emptyResponse = ListResponse.createListResponse(new ArrayList<>());

                given(meetingPlaceQueryService.getList(eq(mockedNonexistentMeetingId)))
                        .willReturn(emptyResponse);

                String hostUserToken = createToken(mockedHostUserId);

                mockMvc.perform(get("/meetings/{meetingId}/places", mockedNonexistentMeetingId)
                                .header("Authorization", hostUserToken)
                        )
                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.NOT_FOUND.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))

                        .andDo(document("place-list-error-meeting-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID")
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

                Long returnId1 = 10L;
                Long returnApiId1 = 1000L;
                PlaceCategory returnCategory1 = PlaceCategory.CAFE;
                String returnMemo1 = "memo1";
                String returnName1 = "place name1";
                String returnAddress1 = "address1";
                Double returnLat1 = 10.1;
                Double returnLng1 = 20.1;
                Integer returnOrder1 = 1;

                MeetingPlaceListResponse returnPlaceResponse1 = MeetingPlaceListResponse.builder()
                        .id(returnId1)
                        .apiId(returnApiId1)
                        .category(returnCategory1)
                        .name(returnName1)
                        .address(returnAddress1)
                        .lat(returnLat1)
                        .lng(returnLng1)
                        .memo(returnMemo1)
                        .order(returnOrder1)
                        .build();

                Long returnId2 = 11L;
                Long returnApiId2 = 2000L;
                PlaceCategory returnCategory2 = PlaceCategory.ACCOMMODATION;
                String returnMemo2 = "memo2";
                String returnName2 = "place name2";
                String returnAddress2 = "address2";
                Double returnLat2 = 110.1;
                Double returnLng2 = 90.1;
                Integer returnOrder2 = 2;

                MeetingPlaceListResponse returnPlaceResponse2 = MeetingPlaceListResponse.builder()
                        .id(returnId2)
                        .apiId(returnApiId2)
                        .category(returnCategory2)
                        .name(returnName2)
                        .address(returnAddress2)
                        .lat(returnLat2)
                        .lng(returnLng2)
                        .memo(returnMemo2)
                        .order(returnOrder2)
                        .build();

                Long returnId3 = 12L;
                Long returnApiId3 = 3000L;
                PlaceCategory returnCategory3 = PlaceCategory.ACCOMMODATION;
                String returnMemo3 = "memo3";
                String returnName3 = "place name3";
                String returnAddress3 = "address3";
                Double returnLat3 = 50.1;
                Double returnLng3 = 20.1;
                Integer returnOrder3 = 3;

                MeetingPlaceListResponse returnPlaceResponse3 = MeetingPlaceListResponse.builder()
                        .id(returnId3)
                        .apiId(returnApiId3)
                        .category(returnCategory3)
                        .name(returnName3)
                        .address(returnAddress3)
                        .lat(returnLat3)
                        .lng(returnLng3)
                        .memo(returnMemo3)
                        .order(returnOrder3)
                        .build();

                List<MeetingPlaceListResponse> returnPlaceReponses = new ArrayList<>();
                returnPlaceReponses.add(returnPlaceResponse1);
                returnPlaceReponses.add(returnPlaceResponse2);
                returnPlaceReponses.add(returnPlaceResponse3);

                ListResponse<MeetingPlaceListResponse> returnResponse
                        = ListResponse.createListResponse(returnPlaceReponses);

                given(meetingPlaceQueryService.getList(eq(mockedExistentMeetingId)))
                        .willReturn(returnResponse);

                String unJoinedUserToken = createToken(10L);

                mockMvc.perform(get("/meetings/{meetingId}/places", mockedExistentMeetingId)
                                .header("Authorization", unJoinedUserToken)
                        )
                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getCode())))
                        .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getMessage())))

                        .andDo(document("place-list-error-not-joined",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID")
                                ),
                                responseFields(beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                                ))
                        )
                ;
            }

            @Test
            @DisplayName("HOST????????? ?????? ????????? ????????? ?????? ?????? Forbidden??? ????????????.")
            public void ??????_????????????() throws Exception {

                Long returnId1 = 10L;
                Long returnApiId1 = 1000L;
                PlaceCategory returnCategory1 = PlaceCategory.CAFE;
                String returnMemo1 = "memo1";
                String returnName1 = "place name1";
                String returnAddress1 = "address1";
                Double returnLat1 = 10.1;
                Double returnLng1 = 20.1;
                Integer returnOrder1 = 1;

                MeetingPlaceListResponse returnPlaceResponse1 = MeetingPlaceListResponse.builder()
                        .id(returnId1)
                        .apiId(returnApiId1)
                        .category(returnCategory1)
                        .name(returnName1)
                        .address(returnAddress1)
                        .lat(returnLat1)
                        .lng(returnLng1)
                        .memo(returnMemo1)
                        .order(returnOrder1)
                        .build();

                Long returnId2 = 11L;
                Long returnApiId2 = 2000L;
                PlaceCategory returnCategory2 = PlaceCategory.ACCOMMODATION;
                String returnMemo2 = "memo2";
                String returnName2 = "place name2";
                String returnAddress2 = "address2";
                Double returnLat2 = 110.1;
                Double returnLng2 = 90.1;
                Integer returnOrder2 = 2;

                MeetingPlaceListResponse returnPlaceResponse2 = MeetingPlaceListResponse.builder()
                        .id(returnId2)
                        .apiId(returnApiId2)
                        .category(returnCategory2)
                        .name(returnName2)
                        .address(returnAddress2)
                        .lat(returnLat2)
                        .lng(returnLng2)
                        .memo(returnMemo2)
                        .order(returnOrder2)
                        .build();

                Long returnId3 = 12L;
                Long returnApiId3 = 3000L;
                PlaceCategory returnCategory3 = PlaceCategory.ACCOMMODATION;
                String returnMemo3 = "memo3";
                String returnName3 = "place name3";
                String returnAddress3 = "address3";
                Double returnLat3 = 50.1;
                Double returnLng3 = 20.1;
                Integer returnOrder3 = 3;

                MeetingPlaceListResponse returnPlaceResponse3 = MeetingPlaceListResponse.builder()
                        .id(returnId3)
                        .apiId(returnApiId3)
                        .category(returnCategory3)
                        .name(returnName3)
                        .address(returnAddress3)
                        .lat(returnLat3)
                        .lng(returnLng3)
                        .memo(returnMemo3)
                        .order(returnOrder3)
                        .build();

                List<MeetingPlaceListResponse> returnPlaceReponses = new ArrayList<>();
                returnPlaceReponses.add(returnPlaceResponse1);
                returnPlaceReponses.add(returnPlaceResponse2);
                returnPlaceReponses.add(returnPlaceResponse3);

                ListResponse<MeetingPlaceListResponse> returnResponse
                        = ListResponse.createListResponse(returnPlaceReponses);

                given(meetingPlaceQueryService.getList(eq(mockedExistentMeetingId)))
                        .willReturn(returnResponse);

                String participantUserToken = createToken(mockedParticipantUserId);

                mockMvc.perform(get("/meetings/{meetingId}/places", mockedExistentMeetingId)
                                .header("Authorization", participantUserToken)
                        )
                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.code", equalTo(ApiResponseCode.FORBIDDEN.name())))
                        .andExpect(jsonPath("$.data.errorCode", equalTo(ErrorCode.AUTHORIZATION_FAIL.getCode())))

                        .andDo(document("place-list-error-authorization",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization").description("????????? Bearer ??????, ????????? ????????? ????????? ????????? ??????").attributes(key("format").value("Bearer somejwttokens..."))
                                ),
                                pathParameters(
                                        parameterWithName("meetingId").description("????????? ?????? ????????? ????????? ????????? ID")
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