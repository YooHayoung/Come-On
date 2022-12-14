package com.comeon.courseservice.web.course.controller;

import com.comeon.courseservice.docs.utils.RestDocsUtil;
import com.comeon.courseservice.domain.common.BaseTimeEntity;
import com.comeon.courseservice.domain.course.entity.CourseStatus;
import com.comeon.courseservice.utils.DistanceUtils;
import com.comeon.courseservice.common.exception.CustomException;
import com.comeon.courseservice.common.exception.ErrorCode;
import com.comeon.courseservice.domain.common.exception.EntityNotFoundException;
import com.comeon.courseservice.domain.course.entity.Course;
import com.comeon.courseservice.domain.course.service.CourseService;
import com.comeon.courseservice.domain.course.service.dto.CourseDto;
import com.comeon.courseservice.domain.courselike.entity.CourseLike;
import com.comeon.courseservice.domain.courselike.service.CourseLikeService;
import com.comeon.courseservice.domain.courseplace.entity.CoursePlace;
import com.comeon.courseservice.web.AbstractControllerTest;
import com.comeon.courseservice.web.common.aop.ValidationAspect;
import com.comeon.courseservice.web.common.response.SliceResponse;
import com.comeon.courseservice.web.course.query.CourseQueryService;
import com.comeon.courseservice.web.course.query.repository.cond.CourseCondition;
import com.comeon.courseservice.web.course.query.repository.cond.MyCourseCondition;
import com.comeon.courseservice.web.course.query.repository.dto.CourseListData;
import com.comeon.courseservice.web.course.query.repository.dto.MyPageCourseListData;
import com.comeon.courseservice.web.course.request.CourseListRequestValidator;
import com.comeon.courseservice.web.course.response.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.snippet.Attributes.attributes;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@Import({
        AopAutoConfiguration.class,
        ValidationAspect.class,
        CourseListRequestValidator.class
})
@WebMvcTest(CourseController.class)
@MockBean(JpaMetamodelMappingContext.class)
public class CourseControllerTest extends AbstractControllerTest {

    @MockBean
    CourseQueryService courseQueryService;

    @MockBean
    CourseLikeService courseLikeService;

    @MockBean
    CourseService courseService;

    @Nested
    @DisplayName("?????? ??????")
    class courseSave {

        @Test
        @DisplayName("[docs] ?????? ????????? ????????? ????????????, ????????? ????????????, ?????? ?????? ???????????? ???????????? ????????????.")
        void success() throws Exception {
            //given
            String title = "courseTitle";
            String description = "courseDescription";
            Long userId = 1L;

            String accessToken = generateUserAccessToken(userId);

            // mocking
            MockMultipartFile mockMultipartFile = getMockMultipartFile("test-img.png");
            CourseDto courseDto = new CourseDto(userId, title, description, any());
            Long courseId = 1L;
            given(courseService.saveCourse(courseDto))
                    .willReturn(courseId);
            given(courseQueryService.getCourseStatus(courseId))
                    .willReturn(CourseStatus.WRITING);

            //when
            ResultActions perform = mockMvc.perform(
                    multipart("/courses")
                            .file(mockMultipartFile)
                            .param("title", title)
                            .param("description", description)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            //then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.courseId").value(1L));

            // docs
            perform.andDo(
                    restDocs.document(
                            requestHeaders(
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(HttpHeaders.AUTHORIZATION).description("????????? ??? ?????? ???????????? ?????? ???????????? Bearer AccessToken")
                            ),
                            requestParts(
                                    attributes(key("title").value("?????? ??????")),
                                    partWithName("imgFile").description("????????? ????????? ??????")
                            ),
                            requestParameters(
                                    attributes(key("title").value("?????? ????????????")),
                                    parameterWithName("title").description("????????? ??????"),
                                    parameterWithName("description").description("????????? ??????")
                            ),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("courseId").type(JsonFieldType.NUMBER).description("????????? ????????? ?????????"),
                                    fieldWithPath("courseStatus").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.COURSE_STATUS))
//                                    fieldWithPath("courseStatus").type(JsonFieldType.STRING).description("????????? ????????? ?????? ??????. " +
//                                                    "?????? ????????? ????????? ????????? ?????? ???????????? ?????? ????????? ?????? WRITING(?????????) ??????.")
                            )
                    )
            );
        }

        @Test
        @DisplayName("?????? ????????? ????????? ???????????? http status 400 ????????????, ?????? ????????? ????????? ???????????? ?????????.")
        void validationFail() throws Exception {
            Long userId = 1L;
            String accessToken = generateUserAccessToken(userId);

            // when
            ResultActions perform = mockMvc.perform(
                    multipart("/courses")
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            // then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.message.imgFile").exists())
                    .andExpect(jsonPath("$.data.message.description").exists())
                    .andExpect(jsonPath("$.data.message.title").exists());

            // docs
            perform.andDo(
                    restDocs.document(
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    fieldWithPath("message").type(JsonFieldType.OBJECT).description("?????? ?????????"),
                                    fieldWithPath("message.imgFile").ignored(),
                                    fieldWithPath("message.description").ignored(),
                                    fieldWithPath("message.title").ignored()
                            )
                    )
            );
        }

        // TODO ????????? ?????? ?????? ???????????? 401 Error
    }

    @Nested
    @DisplayName("?????? ?????? ??????")
    class courseDetails {

        @Test
        @DisplayName("?????? ????????? ????????? ??????????????? ????????????, ?????? ????????? ????????? ???????????? http status 200 ????????????.")
        void success() throws Exception {
            // given
            initData();
            Course course = getCourseList().stream()
                    .findFirst()
                    .orElseThrow();

            Long courseId = course.getId();
            Long currentUserId = 1L;

            // mocking
            String mockWriterNickname = "userNickname";
            CourseDetailResponse courseDetailResponse = new CourseDetailResponse(
                    course,
                    new UserDetailInfo(course.getUserId(), mockWriterNickname),
                    fileManager.getFileUrl(course.getCourseImage().getStoredName(), dirName),
                    true
            );

            given(courseQueryService.getCourseDetails(courseId, currentUserId))
                    .willReturn(courseDetailResponse);

            // when
            String accessToken = generateUserAccessToken(currentUserId);
            String path = "/courses/{courseId}";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path, courseId)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.courseId").value(courseId))
                    .andExpect(jsonPath("$.data.title").value(course.getTitle()))
                    .andExpect(jsonPath("$.data.description").value(course.getDescription()))
                    .andExpect(jsonPath("$.data.imageUrl").exists())
                    .andExpect(jsonPath("$.data.likeCount").value(course.getLikeCount()))
                    .andExpect(jsonPath("$.data.userLiked").exists())
                    .andExpect(jsonPath("$.data.updatedDate").exists())
                    .andExpect(jsonPath("$.data.writer").exists())
                    .andExpect(jsonPath("$.data.writer.id").value(course.getUserId()))
                    .andExpect(jsonPath("$.data.writer.nickname").value(mockWriterNickname))
                    .andExpect(jsonPath("$.data.courseStatus").value(CourseStatus.COMPLETE.name()))
                    .andExpect(jsonPath("$.data.coursePlaces").isNotEmpty())

                    .andExpect(jsonPath("$.data.coursePlaces[*].id").exists())
                    .andExpect(jsonPath("$.data.coursePlaces[*].name").exists())
                    .andExpect(jsonPath("$.data.coursePlaces[*].description").exists())
                    .andExpect(jsonPath("$.data.coursePlaces[*].lat").exists())
                    .andExpect(jsonPath("$.data.coursePlaces[*].lng").exists())
                    .andExpect(jsonPath("$.data.coursePlaces[*].order").exists())
                    .andExpect(jsonPath("$.data.coursePlaces[*].apiId").exists())
                    .andExpect(jsonPath("$.data.coursePlaces[*].category").exists())
                    .andExpect(jsonPath("$.data.coursePlaces[*].address").exists());


            // docs
            perform.andDo(
                    restDocs.document(
                            pathParameters(
                                    attributes(key("title").value(path)),
                                    parameterWithName("courseId").description("????????? ????????? ?????????")
                            ),
                            requestHeaders(
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(HttpHeaders.AUTHORIZATION).description("????????? ??? ?????? ???????????? ?????? ???????????? Bearer AccessToken").optional()
                            ),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("courseId").type(JsonFieldType.NUMBER).description("????????? ????????? ?????????"),
                                    fieldWithPath("title").type(JsonFieldType.STRING).description("????????? ?????? ??????"),
                                    fieldWithPath("description").type(JsonFieldType.STRING).description("????????? ??????"),
                                    fieldWithPath("imageUrl").type(JsonFieldType.STRING).description("????????? ????????? URL"),
                                    fieldWithPath("courseStatus").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.COURSE_STATUS)),
                                    fieldWithPath("updatedDate").type(JsonFieldType.STRING).description("?????? ????????? ??????????????? ????????? ??????"),

                                    fieldWithPath("writer").type(JsonFieldType.OBJECT).description("?????? ?????? ?????????"),
                                    fieldWithPath("writer.id").type(JsonFieldType.NUMBER).description("?????? ?????? ????????? ?????????"),
                                    fieldWithPath("writer.nickname").type(JsonFieldType.STRING).description("?????? ?????? ????????? ?????????"),

                                    fieldWithPath("likeCount").type(JsonFieldType.NUMBER).description("?????? ????????? ????????? ???"),
                                    fieldWithPath("userLiked").type(JsonFieldType.BOOLEAN).description("?????? ????????? ?????? ?????? ???????????? ????????? ??????"),

                                    fieldWithPath("coursePlaces").type(JsonFieldType.ARRAY).description("????????? ????????? ?????? ?????? ??????"),
                                    fieldWithPath("coursePlaces[].id").type(JsonFieldType.NUMBER).description("????????? ?????????"),
                                    fieldWithPath("coursePlaces[].name").type(JsonFieldType.STRING).description("?????? ??????"),
                                    fieldWithPath("coursePlaces[].description").type(JsonFieldType.STRING).description("?????? ??????"),
                                    fieldWithPath("coursePlaces[].lat").type(JsonFieldType.NUMBER).description("????????? ?????????"),
                                    fieldWithPath("coursePlaces[].lng").type(JsonFieldType.NUMBER).description("????????? ?????????"),
                                    fieldWithPath("coursePlaces[].order").type(JsonFieldType.NUMBER).description("????????? ?????????"),
                                    fieldWithPath("coursePlaces[].apiId").type(JsonFieldType.NUMBER).description("Kakao Map?????? ????????? ?????????"),
                                    fieldWithPath("coursePlaces[].category").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.PLACE_CATEGORY)),
                                    fieldWithPath("coursePlaces[].address").type(JsonFieldType.STRING).description("????????? ??????").optional()
                            )
                    )
            );
        }

        @Test
        @DisplayName("???????????? ?????? ?????? ???????????? ???????????? Http status 400 ????????????.")
        void notExistCourse() throws Exception {
            // given
            Long courseId = 100L;
            Long currentUserId = 1L;

            // mocking
            given(courseQueryService.getCourseDetails(courseId, currentUserId))
                    .willThrow(new EntityNotFoundException("?????? ???????????? ????????? ???????????? ????????????. ????????? ?????? ????????? : " + courseId));

            // when
            String accessToken = generateUserAccessToken(currentUserId);
            String path = "/courses/{courseId}";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path, courseId)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            //then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.errorCode").exists())
                    .andExpect(jsonPath("$.data.message").exists());

            // docs
            perform.andDo(
                    restDocs.document(
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                            )
                    )
            );
        }

        @Test
        @DisplayName("?????? ???????????? ?????? ????????? ???????????? ?????? ???????????? ????????? ??? ??????.")
        void canNotOpenCourseWhichDoesNotComplete() throws Exception {
            //given
            Long courseId = 10L;
            Long currentUserId = 1L;

            // mocking
            given(courseQueryService.getCourseDetails(courseId, currentUserId))
                    .willThrow(new CustomException("?????? ???????????? ?????? ???????????????. ????????? ?????? ????????? : " + courseId, ErrorCode.CAN_NOT_ACCESS_RESOURCE));

            // when
            String accessToken = generateUserAccessToken(currentUserId);
            String path = "/courses/{courseId}";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path, courseId)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            //then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.CAN_NOT_ACCESS_RESOURCE.getCode()))
                    .andExpect(jsonPath("$.data.message").value(ErrorCode.CAN_NOT_ACCESS_RESOURCE.getMessage()));

            // docs
            perform.andDo(
                    restDocs.document(
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ?????????")
                            )
                    )
            );
        }

        @Test
        @DisplayName("?????? ????????? ???????????? ?????? ???????????? ????????? ????????? ??? ??????.")
        void writerCanOpenCourseWhichDoesNotComplete() throws Exception {
            //given
            Long currentUserId = 1L;
            Course course = setCourses(currentUserId, 1)
                    .stream().findFirst().orElseThrow();
            Long courseId = course.getId();

            // mocking
            String mockWriterNickname = "userNickname";
            CourseDetailResponse courseDetailResponse = new CourseDetailResponse(
                    course,
                    new UserDetailInfo(course.getUserId(), mockWriterNickname),
                    fileManager.getFileUrl(course.getCourseImage().getStoredName(), dirName),
                    true
            );

            given(courseQueryService.getCourseDetails(courseId, currentUserId))
                    .willReturn(courseDetailResponse);

            // when
            String accessToken = generateUserAccessToken(currentUserId);
            String path = "/courses/{courseId}";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path, courseId)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            //then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.courseId").value(courseId))
                    .andExpect(jsonPath("$.data.title").value(course.getTitle()))
                    .andExpect(jsonPath("$.data.description").value(course.getDescription()))
                    .andExpect(jsonPath("$.data.imageUrl").exists())
                    .andExpect(jsonPath("$.data.likeCount").value(course.getLikeCount()))
                    .andExpect(jsonPath("$.data.userLiked").exists())
                    .andExpect(jsonPath("$.data.updatedDate").exists())
                    .andExpect(jsonPath("$.data.writer").exists())
                    .andExpect(jsonPath("$.data.writer.id").value(course.getUserId()))
                    .andExpect(jsonPath("$.data.writer.nickname").value(mockWriterNickname))
                    .andExpect(jsonPath("$.data.coursePlaces").isEmpty())
                    .andExpect(jsonPath("$.data.courseStatus").value(CourseStatus.WRITING.name()));

            // docs
            perform.andDo(
                    restDocs.document(
                            pathParameters(
                                    attributes(key("title").value(path)),
                                    parameterWithName("courseId").description("????????? ????????? ?????????")
                            ),
                            requestHeaders(
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(HttpHeaders.AUTHORIZATION).description("????????? ??? ?????? ???????????? ?????? ???????????? Bearer AccessToken").optional()
                            ),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("courseId").type(JsonFieldType.NUMBER).description("????????? ????????? ?????????"),
                                    fieldWithPath("title").type(JsonFieldType.STRING).description("????????? ?????? ??????"),
                                    fieldWithPath("description").type(JsonFieldType.STRING).description("????????? ??????"),
                                    fieldWithPath("imageUrl").type(JsonFieldType.STRING).description("????????? ????????? URL"),
                                    fieldWithPath("courseStatus").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.COURSE_STATUS)),
                                    fieldWithPath("updatedDate").type(JsonFieldType.STRING).description("?????? ????????? ??????????????? ????????? ??????"),

                                    fieldWithPath("writer").type(JsonFieldType.OBJECT).description("?????? ?????? ?????????"),
                                    fieldWithPath("writer.id").type(JsonFieldType.NUMBER).description("?????? ?????? ????????? ?????????"),
                                    fieldWithPath("writer.nickname").type(JsonFieldType.STRING).description("?????? ?????? ????????? ?????????"),

                                    fieldWithPath("likeCount").type(JsonFieldType.NUMBER).description("?????? ????????? ????????? ???"),
                                    fieldWithPath("userLiked").type(JsonFieldType.BOOLEAN).description("?????? ????????? ?????? ?????? ???????????? ????????? ??????"),

                                    subsectionWithPath("coursePlaces").type(JsonFieldType.ARRAY).description("????????? ????????? ?????? ?????? ??????. ?????? ????????? `?????????` ?????????, ???????????? ????????? ??????.")
                            )
                    )
            );
        }

        @Test
        @DisplayName("??????????????? ?????? ????????? ????????? ????????? ??? ??????.")
        void canOpenCourseWhoDoesNotLogin() throws Exception {
            // given
            initData();
            Course course = getCourseList().stream()
                    .findFirst()
                    .orElseThrow();

            Long courseId = course.getId();

            // mocking
            String mockWriterNickname = "userNickname";
            CourseDetailResponse courseDetailResponse = new CourseDetailResponse(
                    course,
                    new UserDetailInfo(course.getUserId(), mockWriterNickname),
                    fileManager.getFileUrl(course.getCourseImage().getStoredName(), dirName),
                    false
            );

            given(courseQueryService.getCourseDetails(courseId, null))
                    .willReturn(courseDetailResponse);

            // when
            String path = "/courses/{courseId}";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path, courseId)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.courseId").value(courseId))
                    .andExpect(jsonPath("$.data.title").value(course.getTitle()))
                    .andExpect(jsonPath("$.data.description").value(course.getDescription()))
                    .andExpect(jsonPath("$.data.imageUrl").exists())
                    .andExpect(jsonPath("$.data.likeCount").value(course.getLikeCount()))
                    .andExpect(jsonPath("$.data.userLiked").exists())
                    .andExpect(jsonPath("$.data.updatedDate").exists())
                    .andExpect(jsonPath("$.data.writer").exists())
                    .andExpect(jsonPath("$.data.writer.id").value(course.getUserId()))
                    .andExpect(jsonPath("$.data.writer.nickname").value(mockWriterNickname))
                    .andExpect(jsonPath("$.data.coursePlaces").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("?????? ????????? ??????")
    class courseList {

        private double getDistance(CoursePlace coursePlace) {
            return DistanceUtils.distance(37.555945, 126.972331, coursePlace.getLat(), coursePlace.getLng());
        }

        private double getDistance(Double userLat, Double userLng, CoursePlace coursePlace) {
            return DistanceUtils.distance(userLat, userLng, coursePlace.getLat(), coursePlace.getLng());
        }

        @Test
        @DisplayName("????????? ????????? ?????? ???????????? ????????? ?????? ????????? ????????? ????????????.")
        void success() throws Exception {
            //given
            initData();
            Long currentUserId = 1L;

            int pageSize = 10;

            Comparator<CourseListData> placeComparator = Comparator.comparing(CourseListData::getDistance);
            Comparator<CourseListData> lastModifyDateComparator = Comparator.comparing(o -> o.getCourse().getUpdatedDate(), Comparator.reverseOrder());
            Comparator<CourseListData> likeCountComparator = Comparator.comparing(o -> o.getCourse().getLikeCount(), Comparator.reverseOrder());

            List<CourseListData> dataList = getCourseList().stream()
                    .filter(course -> !course.getCoursePlaces().isEmpty())
                    .filter(Course::isWritingComplete)
                    .map(course -> {
                        CoursePlace place = course.getCoursePlaces().stream()
                                .filter(coursePlace -> coursePlace.getOrder().equals(1))
                                .findFirst()
                                .orElseThrow();
                        Long courseLikeId = getCourseLikeList().stream()
                                .filter(courseLike -> courseLike.getCourse().equals(course) && courseLike.getUserId().equals(currentUserId))
                                .findFirst()
                                .map(CourseLike::getId)
                                .orElse(null);
                        return new CourseListData(course, place, getDistance(place), courseLikeId);
                    })
                    .filter(courseListData -> courseListData.getDistance() <= 100)
                    .sorted(placeComparator.thenComparing(likeCountComparator).thenComparing(lastModifyDateComparator))
                    .limit(pageSize)
                    .collect(Collectors.toList());

            List<CourseListResponse> courseListResponses = new ArrayList<>();
            for (CourseListData courseListData : dataList) {
                Course course = courseListData.getCourse();
                CourseListResponse courseListResponse = CourseListResponse.builder()
                        .course(course)
                        .imageUrl(fileManager.getFileUrl(course.getCourseImage().getStoredName(), dirName))
                        .coursePlace(courseListData.getCoursePlace())
                        .firstPlaceDistance(courseListData.getDistance())
                        .writer(new UserDetailInfo(course.getUserId(), "writerNickname" + course.getUserId()))
                        .userLiked(Objects.nonNull(courseListData.getUserLikeId()))
                        .build();
                courseListResponses.add(courseListResponse);
            }

            SliceResponse<CourseListResponse> courseListResponseSliceResponse =
                    SliceResponse.toSliceResponse(new SliceImpl<>(courseListResponses, PageRequest.of(0, pageSize), true));

            // mocking
            given(courseQueryService.getCourseList(eq(currentUserId), any(CourseCondition.class), any(Pageable.class)))
                    .willReturn(courseListResponseSliceResponse);

            //when
            String accessToken = generateUserAccessToken(currentUserId);
            String path = "/courses";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            //then
            log.info(perform.andReturn().getResponse().getContentAsString());

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.contents[*].courseId").exists())
                    .andExpect(jsonPath("$.data.contents[*].title").exists())
                    .andExpect(jsonPath("$.data.contents[*].imageUrl").exists())
                    .andExpect(jsonPath("$.data.contents[*].courseStatus").exists())
                    .andExpect(jsonPath("$.data.contents[*].updatedDate").exists())
                    .andExpect(jsonPath("$.data.contents[*].likeCount").exists())
                    .andExpect(jsonPath("$.data.contents[*].userLiked").exists())
                    .andExpect(jsonPath("$.data.contents[*].writer").exists())
                    .andExpect(jsonPath("$.data.contents[*].writer.id").exists())
                    .andExpect(jsonPath("$.data.contents[*].writer.nickname").exists())
                    .andExpect(jsonPath("$.data.contents[*].firstPlace").exists())
                    .andExpect(jsonPath("$.data.contents[*].firstPlace.id").exists())
                    .andExpect(jsonPath("$.data.contents[*].firstPlace.lat").exists())
                    .andExpect(jsonPath("$.data.contents[*].firstPlace.lng").exists())
                    .andExpect(jsonPath("$.data.contents[*].firstPlace.distance").exists());

            // docs
            perform.andDo(
                    restDocs.document(
                            requestHeaders(
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(HttpHeaders.AUTHORIZATION).description("????????? ??? ?????? ???????????? ?????? ???????????? Bearer AccessToken").optional()
                            ),
                            requestParameters(
                                    attributes(key("title").value("?????? ????????????")),
                                    parameterWithName("page").description("????????? ????????? ??????. ????????? 0").optional(),
                                    parameterWithName("size").description("???????????? ????????? ????????? ??????. ????????? 10").optional(),
                                    parameterWithName("title").description("?????? ?????? ?????????. ???????????? ????????? ????????? ???????????? ???????????? ??????.").optional(),
                                    parameterWithName("lat").description("???????????? ?????????. ??????, ?????? ??? ????????? ????????? ?????? ??????.").optional(),
                                    parameterWithName("lng").description("???????????? ?????????. ??????, ?????? ??? ????????? ????????? ?????? ??????.").optional()
                            ),
                            responseFields(
                                    beneathPath("data.contents").withSubsectionId("contents"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("courseId").type(JsonFieldType.NUMBER).description("????????? ????????? ?????????"),
                                    fieldWithPath("title").type(JsonFieldType.STRING).description("????????? ?????? ??????"),
                                    fieldWithPath("imageUrl").type(JsonFieldType.STRING).description("????????? ????????? URL"),
                                    fieldWithPath("courseStatus").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.COURSE_STATUS)),
                                    fieldWithPath("updatedDate").type(JsonFieldType.STRING).description("?????? ????????? ??????????????? ????????? ??????"),

                                    fieldWithPath("likeCount").type(JsonFieldType.NUMBER).description("?????? ????????? ????????? ???"),
                                    fieldWithPath("userLiked").type(JsonFieldType.BOOLEAN).description("?????? ????????? ????????? ????????? ??????"),

                                    fieldWithPath("writer").type(JsonFieldType.OBJECT).description("?????? ?????? ?????????"),
                                    fieldWithPath("writer.id").type(JsonFieldType.NUMBER).description("?????? ?????? ????????? ?????????"),
                                    fieldWithPath("writer.nickname").type(JsonFieldType.STRING).description("?????? ?????? ????????? ?????????"),

                                    fieldWithPath("firstPlace").type(JsonFieldType.OBJECT).description("????????? ????????? ????????? ?????? ??????"),
                                    fieldWithPath("firstPlace.id").type(JsonFieldType.NUMBER).description("????????? ?????????"),
                                    fieldWithPath("firstPlace.lat").type(JsonFieldType.NUMBER).description("????????? ?????????"),
                                    fieldWithPath("firstPlace.lng").type(JsonFieldType.NUMBER).description("????????? ?????????"),
                                    fieldWithPath("firstPlace.distance").type(JsonFieldType.NUMBER).description("?????? ????????? ?????? ???????????? ??????. ????????? `km`")
                            )
                    )
            );
        }

        @Test
        @DisplayName("????????? ??????, ?????? ???????????? ?????? ???????????? ????????? ???????????? ????????? ?????? ???????????? ????????????.")
        void successByAllParams() throws Exception {
            //given
            initData();

            Long currentUserId = 1L;
            String searchWords = "1";
            Double userLat = 37.0;
            Double userLng = 127.0;

            int pageNum = 0;
            int pageSize = 10;

            Comparator<CourseListData> placeComparator = Comparator.comparing(CourseListData::getDistance);
            Comparator<CourseListData> lastModifyDateComparator = Comparator.comparing(o -> o.getCourse().getUpdatedDate(), Comparator.reverseOrder());
            Comparator<CourseListData> likeCountComparator = Comparator.comparing(o -> o.getCourse().getLikeCount(), Comparator.reverseOrder());

            List<CourseListData> dataList = getCourseList().stream()
                    .filter(course -> !course.getCoursePlaces().isEmpty())
                    .filter(Course::isWritingComplete)
                    .filter(course -> course.getTitle().toUpperCase().contains(searchWords.toUpperCase()))
                    .map(course -> {
                        CoursePlace place = course.getCoursePlaces().stream()
                                .filter(coursePlace -> coursePlace.getOrder().equals(1))
                                .findFirst()
                                .orElseThrow();
                        Long courseLikeId = getCourseLikeList().stream()
                                .filter(courseLike -> courseLike.getCourse().equals(course) && courseLike.getUserId().equals(currentUserId))
                                .findFirst()
                                .map(CourseLike::getId)
                                .orElse(null);
                        return new CourseListData(course, place, getDistance(userLat, userLng, place), courseLikeId);
                    })
                    .filter(courseListData -> courseListData.getDistance() <= 100)
                    .sorted(placeComparator.thenComparing(likeCountComparator).thenComparing(lastModifyDateComparator))
                    .limit(pageSize)
                    .collect(Collectors.toList());

            List<CourseListResponse> courseListResponses = new ArrayList<>();
            for (CourseListData courseListData : dataList) {
                Course course = courseListData.getCourse();
                CourseListResponse courseListResponse = CourseListResponse.builder()
                        .course(course)
                        .imageUrl(fileManager.getFileUrl(course.getCourseImage().getStoredName(), dirName))
                        .coursePlace(courseListData.getCoursePlace())
                        .firstPlaceDistance(courseListData.getDistance())
                        .writer(new UserDetailInfo(course.getUserId(), "writerNickname" + course.getUserId()))
                        .userLiked(Objects.nonNull(courseListData.getUserLikeId()))
                        .build();
                courseListResponses.add(courseListResponse);
            }

            SliceResponse<CourseListResponse> courseListResponseSliceResponse =
                    SliceResponse.toSliceResponse(new SliceImpl<>(courseListResponses, PageRequest.of(pageNum, pageSize), false));

            // mocking
            given(courseQueryService.getCourseList(eq(currentUserId), any(CourseCondition.class), any(Pageable.class)))
                    .willReturn(courseListResponseSliceResponse);

            //when
            String accessToken = generateUserAccessToken(currentUserId);
            String path = "/courses";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .param("page", String.valueOf(pageNum))
                            .param("title", searchWords)
                            .param("lat", String.valueOf(userLat))
                            .param("lng", String.valueOf(userLng))
            );

            //then
            log.info(perform.andReturn().getResponse().getContentAsString());

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.contents[*].courseId").exists())
                    .andExpect(jsonPath("$.data.contents[*].title").exists())
                    .andExpect(jsonPath("$.data.contents[*].imageUrl").exists())
                    .andExpect(jsonPath("$.data.contents[*].courseStatus").exists())
                    .andExpect(jsonPath("$.data.contents[*].updatedDate").exists())
                    .andExpect(jsonPath("$.data.contents[*].likeCount").exists())
                    .andExpect(jsonPath("$.data.contents[*].userLiked").exists())
                    .andExpect(jsonPath("$.data.contents[*].writer").exists())
                    .andExpect(jsonPath("$.data.contents[*].writer.id").exists())
                    .andExpect(jsonPath("$.data.contents[*].writer.nickname").exists())
                    .andExpect(jsonPath("$.data.contents[*].firstPlace").exists())
                    .andExpect(jsonPath("$.data.contents[*].firstPlace.id").exists())
                    .andExpect(jsonPath("$.data.contents[*].firstPlace.lat").exists())
                    .andExpect(jsonPath("$.data.contents[*].firstPlace.lng").exists())
                    .andExpect(jsonPath("$.data.contents[*].firstPlace.distance").exists());

            // docs
            perform.andDo(
                    restDocs.document(
                            requestHeaders(
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(HttpHeaders.AUTHORIZATION).description("????????? ??? ?????? ???????????? ?????? ???????????? Bearer AccessToken").optional()
                            ),
                            requestParameters(
                                    attributes(key("title").value("?????? ????????????")),
                                    parameterWithName("page").description("????????? ????????? ??????. ????????? 0").optional(),
                                    parameterWithName("size").description("???????????? ????????? ????????? ??????. ????????? 10").optional(),
                                    parameterWithName("title").description("?????? ?????? ?????????").optional(),
                                    parameterWithName("lat").description("???????????? ?????????").optional(),
                                    parameterWithName("lng").description("???????????? ?????????").optional()
                            ),
                            responseFields(
                                    beneathPath("data.contents").withSubsectionId("contents"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("courseId").type(JsonFieldType.NUMBER).description("????????? ????????? ?????????"),
                                    fieldWithPath("title").type(JsonFieldType.STRING).description("????????? ?????? ??????"),
                                    fieldWithPath("imageUrl").type(JsonFieldType.STRING).description("????????? ????????? URL"),
                                    fieldWithPath("courseStatus").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.COURSE_STATUS)),
                                    fieldWithPath("updatedDate").type(JsonFieldType.STRING).description("?????? ????????? ??????????????? ????????? ??????"),

                                    fieldWithPath("likeCount").type(JsonFieldType.NUMBER).description("?????? ????????? ????????? ???"),
                                    fieldWithPath("userLiked").type(JsonFieldType.BOOLEAN).description("?????? ????????? ????????? ????????? ??????"),

                                    fieldWithPath("writer").type(JsonFieldType.OBJECT).description("?????? ?????? ?????????"),
                                    fieldWithPath("writer.id").type(JsonFieldType.NUMBER).description("?????? ?????? ????????? ?????????"),
                                    fieldWithPath("writer.nickname").type(JsonFieldType.STRING).description("?????? ?????? ????????? ?????????"),

                                    fieldWithPath("firstPlace").type(JsonFieldType.OBJECT).description("????????? ????????? ????????? ?????? ??????"),
                                    fieldWithPath("firstPlace.id").type(JsonFieldType.NUMBER).description("????????? ?????????"),
                                    fieldWithPath("firstPlace.lat").type(JsonFieldType.NUMBER).description("????????? ?????????"),
                                    fieldWithPath("firstPlace.lng").type(JsonFieldType.NUMBER).description("????????? ?????????"),
                                    fieldWithPath("firstPlace.distance").type(JsonFieldType.NUMBER).description("?????? ????????? ?????? ???????????? ??????. ????????? `km`")
                            )
                    )
            );
        }

        @Test
        @DisplayName("????????? ???????????? ????????? ???????????? ????????? ?????? ????????? ???????????? http status 400 ????????????.")
        void validationFailByNoLng() throws Exception {
            //given

            //when
            String path = "/courses";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .param("lat", String.valueOf(37.0))
            );

            //then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.VALIDATION_FAIL.getCode()))
                    .andExpect(jsonPath("$.data.message").exists());

            // docs
            perform.andDo(
                    restDocs.document(
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    subsectionWithPath("message").type(JsonFieldType.OBJECT).description("?????? ?????????")
                            )
                    )
            );
        }

        @Test
        @DisplayName("????????? ???????????? ?????? ????????? ???????????? ?????? ????????? ???????????? http status 400 ????????????.")
        void   validationFailByNoLat() throws Exception {
            //given

            //when
            String path = "/courses";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .param("lng", String.valueOf(127.0))
            );

            //then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.VALIDATION_FAIL.getCode()))
                    .andExpect(jsonPath("$.data.message").exists());

            // docs
            perform.andDo(
                    restDocs.document(
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    subsectionWithPath("message").type(JsonFieldType.OBJECT).description("?????? ?????????")
                            )
                    )
            );
        }
    }

    @Nested
    @DisplayName("?????? ????????? ?????? ????????? ??????")
    class myCourseList {

        @Test
        @DisplayName("?????? ?????? ??????????????? COMPLETE??? ????????? ??????")
        void successCompleteCourses() throws Exception {
            //given
            int pageNum = 0;
            int pageSize = 10;
            Long currentUserId = 1L;

            initData(); // ?????? ????????? ????????? ??????
            getCourseList().addAll(setCourses(currentUserId, 10)); // ?????? ???????????? ?????? ?????? ??????
            CourseStatus courseStatus = CourseStatus.COMPLETE;

            List<MyPageCourseListData> listData = getCourseList().stream()
                    .filter(course -> course.getUserId().equals(currentUserId))
                    .filter(course -> course.getCourseStatus().equals(courseStatus)) // ?????? ?????? ???????????? ??? ??? ?????????
                    .map(course -> {
                        Long courseLikeId = getCourseLikeList().stream()
                                .filter(courseLike -> courseLike.getCourse().equals(course) && courseLike.getUserId().equals(currentUserId))
                                .findFirst()
                                .map(CourseLike::getId)
                                .orElse(null);
                        return new MyPageCourseListData(course, courseLikeId);
                    })
                    .sorted(Comparator.comparing(myPageCourseListData -> myPageCourseListData.getCourse().getUpdatedDate(), Comparator.reverseOrder()))
                    .limit(pageSize)
                    .collect(Collectors.toList());

            List<MyPageCourseListResponse> myPageCourseListResponses = new ArrayList<>();
            for (MyPageCourseListData courseListData : listData) {
                Course course = courseListData.getCourse();
                MyPageCourseListResponse myPageCourseListResponse = MyPageCourseListResponse.builder()
                        .course(course)
                        .imageUrl(fileManager.getFileUrl(course.getCourseImage().getStoredName(), dirName))
                        .writer(new UserDetailInfo(course.getUserId(), "writerNickname" + course.getUserId()))
                        .userLiked(Objects.nonNull(courseListData.getUserLikeId()))
                        .build();
                myPageCourseListResponses.add(myPageCourseListResponse);
            }

            SliceResponse<MyPageCourseListResponse> courseListResponseSliceResponse =
                    SliceResponse.toSliceResponse(new SliceImpl<>(myPageCourseListResponses, PageRequest.of(pageNum, pageSize), true));

            String accessToken = generateUserAccessToken(currentUserId);

            // mocking
            given(courseQueryService.getMyRegisteredCourseList(eq(currentUserId), refEq(new MyCourseCondition(courseStatus)), any(Pageable.class)))
                    .willReturn(courseListResponseSliceResponse);

            //when
            String path = "/courses/my";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path)
                            .param("courseStatus", courseStatus.name())
                            .param("page", String.valueOf(pageNum))
                            .param("size", String.valueOf(pageSize))
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            //then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.contents[*].courseId").exists())
                    .andExpect(jsonPath("$.data.contents[*].title").exists())
                    .andExpect(jsonPath("$.data.contents[*].imageUrl").exists())
                    .andExpect(jsonPath("$.data.contents[*].courseStatus").exists())
                    // COMPLETE??? ?????? courseStatus??? ??????.
                    .andExpect(jsonPath("$.data.contents[?(@.courseStatus != '%s')].courseStatus", courseStatus.name()).doesNotExist())
                    .andExpect(jsonPath("$.data.contents[*].updatedDate").exists())
                    .andExpect(jsonPath("$.data.contents[*].likeCount").exists())
                    .andExpect(jsonPath("$.data.contents[*].userLiked").exists())
                    .andExpect(jsonPath("$.data.contents[*].writer").exists())
                    .andExpect(jsonPath("$.data.contents[*].writer.id").exists())
                    .andExpect(jsonPath("$.data.contents[?(@.writer.id != %d)].writer.id", currentUserId).doesNotExist())
                    .andExpect(jsonPath("$.data.contents[*].writer.nickname").exists());

            // docs
            perform.andDo(
                    restDocs.document(
                            requestHeaders(
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(HttpHeaders.AUTHORIZATION).description("????????? ??? ?????? ???????????? ?????? ???????????? Bearer AccessToken")
                            ),
                            requestParameters(
                                    attributes(key("title").value("?????? ????????????")),
                                    parameterWithName("courseStatus").description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.COURSE_STATUS)),
                                    parameterWithName("page").description("????????? ????????? ??????. ????????? 0").optional(),
                                    parameterWithName("size").description("???????????? ????????? ????????? ??????. ????????? 10").optional()
                            ),
                            responseFields(
                                    beneathPath("data.contents").withSubsectionId("contents"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("courseId").type(JsonFieldType.NUMBER).description("????????? ????????? ?????????"),
                                    fieldWithPath("title").type(JsonFieldType.STRING).description("????????? ?????? ??????"),
                                    fieldWithPath("imageUrl").type(JsonFieldType.STRING).description("????????? ????????? URL"),
                                    fieldWithPath("courseStatus").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.COURSE_STATUS)),
                                    fieldWithPath("updatedDate").type(JsonFieldType.STRING).description("?????? ????????? ??????????????? ????????? ??????"),

                                    fieldWithPath("likeCount").type(JsonFieldType.NUMBER).description("?????? ????????? ????????? ???"),
                                    fieldWithPath("userLiked").type(JsonFieldType.BOOLEAN).description("?????? ????????? ????????? ????????? ??????"),

                                    fieldWithPath("writer").type(JsonFieldType.OBJECT).description("?????? ?????? ?????????"),
                                    fieldWithPath("writer.id").type(JsonFieldType.NUMBER).description("?????? ?????? ????????? ?????????"),
                                    fieldWithPath("writer.nickname").type(JsonFieldType.STRING).description("?????? ?????? ????????? ?????????")
                            )
                    )
            );
        }

        @Test
        @DisplayName("?????? ?????? ??????????????? WRITING?????? ????????? ??????")
        void successWritingCourses() throws Exception {
            //given
            int pageNum = 0;
            int pageSize = 10;
            Long currentUserId = 1L;

            initData(); // ?????? ????????? ????????? ??????
            getCourseList().addAll(setCourses(currentUserId, 10)); // ?????? ???????????? ?????? ?????? ??????
            CourseStatus courseStatus = CourseStatus.WRITING;

            List<MyPageCourseListData> listData = getCourseList().stream()
                    .filter(course -> course.getUserId().equals(currentUserId))
                    .filter(course -> course.getCourseStatus().equals(courseStatus)) // ?????? ?????? ???????????? ??? ??? ?????????
                    .map(course -> {
                        Long courseLikeId = getCourseLikeList().stream()
                                .filter(courseLike -> courseLike.getCourse().equals(course) && courseLike.getUserId().equals(currentUserId))
                                .findFirst()
                                .map(CourseLike::getId)
                                .orElse(null);
                        return new MyPageCourseListData(course, courseLikeId);
                    })
                    .sorted(Comparator.comparing(myPageCourseListData -> myPageCourseListData.getCourse().getUpdatedDate(), Comparator.reverseOrder()))
                    .limit(pageSize)
                    .collect(Collectors.toList());

            List<MyPageCourseListResponse> myPageCourseListResponses = new ArrayList<>();
            for (MyPageCourseListData courseListData : listData) {
                Course course = courseListData.getCourse();
                MyPageCourseListResponse myPageCourseListResponse = MyPageCourseListResponse.builder()
                        .course(course)
                        .imageUrl(fileManager.getFileUrl(course.getCourseImage().getStoredName(), dirName))
                        .writer(new UserDetailInfo(course.getUserId(), "writerNickname" + course.getUserId()))
                        .userLiked(Objects.nonNull(courseListData.getUserLikeId()))
                        .build();
                myPageCourseListResponses.add(myPageCourseListResponse);
            }

            SliceResponse<MyPageCourseListResponse> courseListResponseSliceResponse =
                    SliceResponse.toSliceResponse(new SliceImpl<>(myPageCourseListResponses, PageRequest.of(pageNum, pageSize), false));

            String accessToken = generateUserAccessToken(currentUserId);

            // mocking
            given(courseQueryService.getMyRegisteredCourseList(eq(currentUserId), refEq(new MyCourseCondition(courseStatus)), any(Pageable.class)))
                    .willReturn(courseListResponseSliceResponse);

            //when
            String path = "/courses/my";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path)
                            .param("courseStatus", courseStatus.name())
                            .param("page", String.valueOf(pageNum))
                            .param("size", String.valueOf(pageSize))
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            //then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.contents[*].courseId").exists())
                    .andExpect(jsonPath("$.data.contents[*].title").exists())
                    .andExpect(jsonPath("$.data.contents[*].imageUrl").exists())
                    .andExpect(jsonPath("$.data.contents[*].courseStatus").exists())
                    // WRITING??? ?????? courseStatus??? ??????.
                    .andExpect(jsonPath("$.data.contents[?(@.courseStatus != '%s')].courseStatus", courseStatus.name()).doesNotExist())
                    .andExpect(jsonPath("$.data.contents[*].updatedDate").exists())
                    .andExpect(jsonPath("$.data.contents[*].likeCount").exists())
                    .andExpect(jsonPath("$.data.contents[*].userLiked").exists())
                    .andExpect(jsonPath("$.data.contents[*].writer").exists())
                    .andExpect(jsonPath("$.data.contents[*].writer.id").exists())
                    .andExpect(jsonPath("$.data.contents[?(@.writer.id != %d)].writer.id", currentUserId).doesNotExist())
                    .andExpect(jsonPath("$.data.contents[*].writer.nickname").exists());

            // docs
            perform.andDo(
                    restDocs.document(
                            requestHeaders(
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(HttpHeaders.AUTHORIZATION).description("????????? ??? ?????? ???????????? ?????? ???????????? Bearer AccessToken")
                            ),
                            requestParameters(
                                    attributes(key("title").value("?????? ????????????")),
                                    parameterWithName("courseStatus").description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.COURSE_STATUS)),
                                    parameterWithName("page").description("????????? ????????? ??????. ????????? 0").optional(),
                                    parameterWithName("size").description("???????????? ????????? ????????? ??????. ????????? 10").optional()
                            ),
                            responseFields(
                                    beneathPath("data.contents").withSubsectionId("contents"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("courseId").type(JsonFieldType.NUMBER).description("????????? ????????? ?????????"),
                                    fieldWithPath("title").type(JsonFieldType.STRING).description("????????? ?????? ??????"),
                                    fieldWithPath("imageUrl").type(JsonFieldType.STRING).description("????????? ????????? URL"),
                                    fieldWithPath("courseStatus").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.COURSE_STATUS)),
                                    fieldWithPath("updatedDate").type(JsonFieldType.STRING).description("?????? ????????? ??????????????? ????????? ??????"),

                                    fieldWithPath("likeCount").type(JsonFieldType.NUMBER).description("?????? ????????? ????????? ???"),
                                    fieldWithPath("userLiked").type(JsonFieldType.BOOLEAN).description("?????? ????????? ????????? ????????? ??????"),

                                    fieldWithPath("writer").type(JsonFieldType.OBJECT).description("?????? ?????? ?????????"),
                                    fieldWithPath("writer.id").type(JsonFieldType.NUMBER).description("?????? ?????? ????????? ?????????"),
                                    fieldWithPath("writer.nickname").type(JsonFieldType.STRING).description("?????? ?????? ????????? ?????????")
                            )
                    )
            );
        }

        @Test
        @DisplayName("?????? ?????? ??????????????? ???????????? ????????? ????????? ????????????, http status 400 ????????????.")
        void noCourseStatusError() throws Exception {
            //given
            Long currentUserId = 1L;

            String accessToken = generateUserAccessToken(currentUserId);

            //when
            String path = "/courses/my";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            //then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.VALIDATION_FAIL.getCode()))
                    .andExpect(jsonPath("$.data.message").exists());

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
                                    subsectionWithPath("message").type(JsonFieldType.OBJECT).description("API ?????? ?????? ?????????")
                            )
                    )
            );
        }

        @Test
        @DisplayName("?????? ?????? ??????????????? ??? ??? ???????????? ?????? ????????? ???????????? http status 400 ????????????.")
        void validationError() throws Exception {
            //given
            Long currentUserId = 1L;

            String accessToken = generateUserAccessToken(currentUserId);

            //when
            String path = "/courses/my";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path)
                            .param("courseStatus", "CONTINUE")
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            //then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.errorCode").value(ErrorCode.VALIDATION_FAIL.getCode()))
                    .andExpect(jsonPath("$.data.message").exists());

            // docs
            perform.andDo(
                    restDocs.document(
                            requestHeaders(
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(HttpHeaders.AUTHORIZATION).description("????????? ??? ?????? ???????????? ?????? ???????????? Bearer AccessToken")
                            ),
                            requestParameters(
                                    attributes(key("title").value("?????? ????????????")),
                                    parameterWithName("courseStatus").description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.COURSE_STATUS)),
                                    parameterWithName("page").description("????????? ????????? ??????. ????????? 0").optional(),
                                    parameterWithName("size").description("???????????? ????????? ????????? ??????. ????????? 10").optional()
                            ),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    subsectionWithPath("message").type(JsonFieldType.OBJECT).description("API ?????? ?????? ?????????")
                            )
                    )
            );
        }
    }

    @Nested
    @DisplayName("?????? ???????????? ?????? ????????? ??????")
    class myCourseLikeList {

        @Test
        @DisplayName("???????????? ????????? ????????? ???????????? ?????? ????????? ????????? ????????????.")
        void success() throws Exception {
            //given
            int pageNum = 0;
            int pageSize = 10;
            Long currentUserId = 1L;
            initData();
            List<MyPageCourseListData> listData = getCourseLikeList().stream()
                    .filter(courseLike -> courseLike.getUserId().equals(currentUserId))
                    .sorted(Comparator.comparing(BaseTimeEntity::getLastModifiedDate, Comparator.reverseOrder()))
                    .map(CourseLike::getCourse)
                    .filter(Course::isWritingComplete)
                    .map(course -> {
                        Long courseLikeId = getCourseLikeList().stream()
                                .filter(courseLike -> courseLike.getCourse().equals(course) && courseLike.getUserId().equals(currentUserId))
                                .findFirst()
                                .map(CourseLike::getId)
                                .orElse(null);
                        return new MyPageCourseListData(course, courseLikeId);
                    })
                    .limit(pageSize)
                    .collect(Collectors.toList());

            List<MyPageCourseListResponse> myPageCourseListResponses = new ArrayList<>();
            for (MyPageCourseListData courseListData : listData) {
                Course course = courseListData.getCourse();
                MyPageCourseListResponse myPageCourseListResponse = MyPageCourseListResponse.builder()
                        .course(course)
                        .imageUrl(fileManager.getFileUrl(course.getCourseImage().getStoredName(), dirName))
                        .writer(new UserDetailInfo(course.getUserId(), "writerNickname" + course.getUserId()))
                        .userLiked(Objects.nonNull(courseListData.getUserLikeId()))
                        .build();
                myPageCourseListResponses.add(myPageCourseListResponse);
            }

            SliceResponse<MyPageCourseListResponse> courseListResponseSliceResponse =
                    SliceResponse.toSliceResponse(new SliceImpl<>(myPageCourseListResponses, PageRequest.of(pageNum, pageSize), true));

            String accessToken = generateUserAccessToken(currentUserId);

            // mocking
            given(courseQueryService.getMyLikedCourseList(eq(currentUserId), any(Pageable.class)))
                    .willReturn(courseListResponseSliceResponse);

            //when
            String path = "/courses/like";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.get(path)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            //then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.contents[*].courseId").exists())
                    .andExpect(jsonPath("$.data.contents[*].title").exists())
                    .andExpect(jsonPath("$.data.contents[*].imageUrl").exists())
                    .andExpect(jsonPath("$.data.contents[*].courseStatus").exists())
                    .andExpect(jsonPath("$.data.contents[*].updatedDate").exists())
                    .andExpect(jsonPath("$.data.contents[*].likeCount").exists())
                    .andExpect(jsonPath("$.data.contents[*].userLiked").exists())
                    .andExpect(jsonPath("$.data.contents[?(@.userLiked == false)].userLiked").doesNotExist())
                    .andExpect(jsonPath("$.data.contents[*].writer").exists())
                    .andExpect(jsonPath("$.data.contents[*].writer.id").exists())
                    .andExpect(jsonPath("$.data.contents[*].writer.nickname").exists());

            // docs
            perform.andDo(
                    restDocs.document(
                            requestHeaders(
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(HttpHeaders.AUTHORIZATION).description("????????? ??? ?????? ???????????? ?????? ???????????? Bearer AccessToken")
                            ),
                            requestParameters(
                                    attributes(key("title").value("?????? ????????????")),
                                    parameterWithName("page").description("????????? ????????? ??????. ????????? 0").optional(),
                                    parameterWithName("size").description("???????????? ????????? ????????? ??????. ????????? 10").optional()
                            ),
                            responseFields(
                                    beneathPath("data.contents").withSubsectionId("contents"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("courseId").type(JsonFieldType.NUMBER).description("????????? ????????? ?????????"),
                                    fieldWithPath("title").type(JsonFieldType.STRING).description("????????? ?????? ??????"),
                                    fieldWithPath("imageUrl").type(JsonFieldType.STRING).description("????????? ????????? URL"),
                                    fieldWithPath("courseStatus").type(JsonFieldType.STRING).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.COURSE_STATUS)),
                                    fieldWithPath("updatedDate").type(JsonFieldType.STRING).description("?????? ????????? ??????????????? ????????? ??????"),

                                    fieldWithPath("likeCount").type(JsonFieldType.NUMBER).description("?????? ????????? ????????? ???"),
                                    fieldWithPath("userLiked").type(JsonFieldType.BOOLEAN).description("?????? ????????? ????????? ????????? ??????"),

                                    fieldWithPath("writer").type(JsonFieldType.OBJECT).description("?????? ?????? ?????????"),
                                    fieldWithPath("writer.id").type(JsonFieldType.NUMBER).description("?????? ?????? ????????? ?????????"),
                                    fieldWithPath("writer.nickname").type(JsonFieldType.STRING).description("?????? ?????? ????????? ?????????")
                            )
                    )
            );
        }

        // TODO ??????????????? ????????? ????????? ??? ??????.
    }

    @Nested
    @DisplayName("?????? ??????")
    class courseModify {

        @Test
        @DisplayName("?????? ????????? ????????? ????????????, ?????? ????????? ????????????, ?????? ???????????? ???????????? ????????????.")
        void success() throws Exception {
            //given
            Long currentUserId = 1L;
            Course course = setCourses(currentUserId, 1).stream()
                    .findFirst()
                    .orElseThrow();
            Long courseId = course.getId();

            setCoursePlaces(course, 3);

            String title = "modifiedTitle";
            String description = "modifiedDescription";

            String accessToken = generateUserAccessToken(currentUserId);

            MockMultipartFile mockMultipartFile = getMockMultipartFile("test-img2.png");

            // mocking
            given(courseQueryService.getStoredFileName(courseId)).willReturn(course.getCourseImage().getStoredName());
            willDoNothing().given(courseService).modifyCourse(eq(courseId), any());

            //when
            String path = "/courses/{courseId}";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.multipart(path, courseId)
                            .file(mockMultipartFile)
                            .param("title", title)
                            .param("description", description)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            //then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.message").exists());

            // docs
            perform.andDo(
                    restDocs.document(
                            requestHeaders(
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(HttpHeaders.AUTHORIZATION).description("????????? ??? ?????? ???????????? ?????? ???????????? Bearer AccessToken")
                            ),
                            pathParameters(
                                    attributes(key("title").value(path)),
                                    parameterWithName("courseId").description("????????? ?????? ?????????")
                            ),
                            requestParts(
                                    attributes(key("title").value("?????? ??????")),
                                    partWithName("imgFile").description("????????? ????????? ??????").optional()
                            ),
                            requestParameters(
                                    attributes(key("title").value("?????? ????????????")),
                                    parameterWithName("title").description("????????? ??????"),
                                    parameterWithName("description").description("????????? ??????")
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
        @DisplayName("?????? ????????? ????????? ???????????? Http status 400 ????????????.")
        void validationFail() throws Exception {
            //given
            Long currentUserId = 1L;
            Course course = setCourses(currentUserId, 1).stream()
                    .findFirst()
                    .orElseThrow();
            Long courseId = course.getId();

            setCoursePlaces(course, 3);

            String accessToken = generateUserAccessToken(currentUserId);

            //when
            String path = "/courses/{courseId}";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.multipart(path, courseId)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            //then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.errorCode").exists())
                    .andExpect(jsonPath("$.data.message").exists());

            // docs
            perform.andDo(
                    restDocs.document(
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description(RestDocsUtil.generateLinkCode(RestDocsUtil.DocUrl.ERROR_CODE)),
                                    fieldWithPath("message").type(JsonFieldType.OBJECT).description("API ?????? ?????????"),
                                    fieldWithPath("message.description").ignored(),
                                    fieldWithPath("message.title").ignored()
                            )
                    )
            );
        }

        @Test
        @DisplayName("??????????????? ????????? ?????? ????????????, Http Status 400 ????????????.")
        void noCourseError() throws Exception {
            //given
            Long currentUserId = 1L;
            Long courseId = 4L;

            String title = "modifiedTitle";
            String description = "modifiedDescription";

            String accessToken = generateUserAccessToken(currentUserId);

            MockMultipartFile mockMultipartFile = getMockMultipartFile("test-img2.png");

            // mocking
            given(courseQueryService.getStoredFileName(courseId))
                    .willThrow(new EntityNotFoundException("?????? ???????????? ????????? ???????????? ????????????. ????????? ?????? ????????? : " + courseId));

            //when
            String path = "/courses/{courseId}";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.multipart(path, courseId)
                            .file(mockMultipartFile)
                            .param("title", title)
                            .param("description", description)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            //then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.errorCode").exists())
                    .andExpect(jsonPath("$.data.message").exists());

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
        @DisplayName("????????? ???????????? ?????? ????????? ???????????? ?????????, Http Status 403 ????????????.")
        void notWriterError() throws Exception {
            //given
            Long writerId = 3L;
            Course course = setCourses(writerId, 1).stream()
                    .findFirst()
                    .orElseThrow();
            Long courseId = course.getId();

            setCoursePlaces(course, 3);

            String title = "modifiedTitle";
            String description = "modifiedDescription";

            Long currentUserId = 1L;

            String accessToken = generateUserAccessToken(currentUserId);

            MockMultipartFile mockMultipartFile = getMockMultipartFile("test-img2.png");

            // mocking
            given(courseQueryService.getStoredFileName(courseId)).willReturn(course.getCourseImage().getStoredName());
            willThrow(new CustomException("?????? ????????? ???????????? ????????? ????????? ????????? ????????? ??? ????????????.", ErrorCode.NO_AUTHORITIES))
                    .given(courseService).modifyCourse(eq(courseId), any());

            //when
            String path = "/courses/{courseId}";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.multipart(path, courseId)
                            .file(mockMultipartFile)
                            .param("title", title)
                            .param("description", description)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            //then
            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.data.errorCode").exists())
                    .andExpect(jsonPath("$.data.message").exists());

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
    @DisplayName("?????? ??????")
    class courseRemove {

        @Test
        @DisplayName("?????? ????????? ????????????, ?????? ?????? ???????????? ????????????.")
        void success() throws Exception {
            //given
            Long currentUserId = 1L;
            Course course = setCourses(currentUserId, 1).stream()
                    .findFirst()
                    .orElseThrow();
            Long courseId = course.getId();

            setCoursePlaces(course, 3);

            String accessToken = generateUserAccessToken(currentUserId);

            // mocking
            given(courseQueryService.getStoredFileName(courseId)).willReturn(course.getCourseImage().getStoredName());
            willDoNothing().given(courseService).removeCourse(courseId, currentUserId);

            //when
            String path = "/courses/{courseId}";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.delete(path, courseId)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            //then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.message").exists());

            // docs
            perform.andDo(
                    restDocs.document(
                            requestHeaders(
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(HttpHeaders.AUTHORIZATION).description("????????? ??? ?????? ???????????? ?????? ???????????? Bearer AccessToken")
                            ),
                            pathParameters(
                                    attributes(key("title").value(path)),
                                    parameterWithName("courseId").description("????????? ?????? ?????????")
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
        @DisplayName("?????? ??????????????? ?????? ???????????? ?????? ????????? ????????? Http status 400 ????????????.")
        void noCourseError() throws Exception {
            //given
            Long currentUserId = 1L;
            Long courseId = 3L;

            String accessToken = generateUserAccessToken(currentUserId);

            // mocking
            given(courseQueryService.getStoredFileName(courseId))
                    .willThrow(new EntityNotFoundException("?????? ???????????? ????????? ???????????? ????????????. ????????? ?????? ????????? : " + courseId));

            //when
            String path = "/courses/{courseId}";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.delete(path, courseId)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            //then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.errorCode").exists())
                    .andExpect(jsonPath("$.data.message").exists());

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
        @DisplayName("????????? ????????? ??????????????? ????????? ???????????? ????????? Http status 403 ????????????.")
        void notWriter() throws Exception {
            //given
            Long courseWriterId = 3L;
            Course course = setCourses(courseWriterId, 1).stream()
                    .findFirst()
                    .orElseThrow();
            Long courseId = course.getId();

            setCoursePlaces(course, 3);

            Long currentUserId = 1L;
            String accessToken = generateUserAccessToken(currentUserId);

            // mocking
            given(courseQueryService.getStoredFileName(courseId)).willReturn(course.getCourseImage().getStoredName());
            willThrow(new CustomException("?????? ????????? ???????????? ????????? ????????? ????????? ????????? ??? ????????????.", ErrorCode.NO_AUTHORITIES))
                    .given(courseService).removeCourse(courseId, currentUserId);

            //when
            String path = "/courses/{courseId}";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.delete(path, courseId)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            //then
            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.data.errorCode").exists())
                    .andExpect(jsonPath("$.data.message").exists());

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
    @DisplayName("?????? ????????? ??????/??????")
    class courseLikeUpdate {

        private void mockingUpdateCourseLikeSuccess(Course course, Long currentUserId) {
            Long courseId = course.getId();
            given(courseLikeService.updateCourseLike(courseId, currentUserId))
                    .will(
                            invocation -> {
                                CourseLike like = getCourseLikeList().stream()
                                        .filter(courseLike -> courseLike.getCourse().equals(course)
                                                && courseLike.getUserId().equals(currentUserId))
                                        .findFirst()
                                        .orElse(null);

                                if (like != null) {
                                    return null;
                                } else {
                                    if (!course.isWritingComplete()) {
                                        throw new CustomException("?????? ???????????? ?????? ???????????????. ????????? ?????? ????????? : " + course.getId(), ErrorCode.CAN_NOT_ACCESS_RESOURCE);
                                    }

                                    setCourseLike(
                                            course,
                                            currentUserId
                                    );

                                    return getCourseLikeList().stream()
                                            .filter(courseLike -> courseLike.getCourse().equals(course)
                                                    && courseLike.getUserId().equals(currentUserId))
                                            .findFirst()
                                            .orElseThrow()
                                            .getId();
                                }
                            }
                    );
        }

        @Test
        @DisplayName("?????? ????????? ????????? ???????????? ???????????? ????????? ??????, ???????????? ???????????? userLiked = true ??? ????????????.")
        void successCreated() throws Exception {
            // given
            Course course = setCourses(1L, 1).stream().findFirst().orElseThrow();
            setCoursePlaces(course, 3);
            Long courseId = course.getId();
            Long currentUserId = 3L;

            String accessToken = generateUserAccessToken(currentUserId);

            // mocking
            mockingUpdateCourseLikeSuccess(course, currentUserId);

            // when
            String path = "/courses/{courseId}/like";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.post(path, courseId)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userLiked").value(true));

            // docs
            perform.andDo(
                    restDocs.document(
                            requestHeaders(
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(HttpHeaders.AUTHORIZATION).description("????????? ??? ?????? ???????????? ?????? ???????????? Bearer AccessToken")
                            ),
                            pathParameters(
                                    attributes(key("title").value(path)),
                                    parameterWithName("courseId").description("???????????? ????????? ?????? ?????? ?????????")
                            ),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("userLiked").type(JsonFieldType.BOOLEAN).description("????????? ?????? ?????? ???, ?????? ????????? ?????? ????????? ????????? ??????")
                            )
                    )
            );
        }

        @Test
        @DisplayName("?????? ????????? ????????? ???????????? ??????????????? ??????, ???????????? ???????????? userLiked = false??? ????????????.")
        void successDeleted() throws Exception {
            // given
            Course course = setCourses(1L, 1).stream().findFirst().orElseThrow();
            setCoursePlaces(course, 3);

            Long courseId = course.getId();
            Long currentUserId = 3L;

            setCourseLike(course, currentUserId);

            String accessToken = generateUserAccessToken(currentUserId);

            // mocking
            mockingUpdateCourseLikeSuccess(course, currentUserId);

            // when
            String path = "/courses/{courseId}/like";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.post(path, courseId)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userLiked").value(false));

            // docs
            perform.andDo(
                    restDocs.document(
                            requestHeaders(
                                    attributes(key("title").value("?????? ??????")),
                                    headerWithName(HttpHeaders.AUTHORIZATION).description("????????? ??? ?????? ???????????? ?????? ???????????? Bearer AccessToken")
                            ),
                            pathParameters(
                                    attributes(key("title").value(path)),
                                    parameterWithName("courseId").description("???????????? ????????? ?????? ?????? ?????????")
                            ),
                            responseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    attributes(key("title").value("?????? ??????")),
                                    fieldWithPath("userLiked").type(JsonFieldType.BOOLEAN).description("????????? ?????? ?????? ???, ?????? ????????? ?????? ????????? ????????? ??????")
                            )
                    )
            );
        }

        @Test
        @DisplayName("?????? ??????????????? ????????? ?????? ???????????? ???????????? ????????? ?????????, http status 400 ????????? ????????????.")
        void invalidCourseId() throws Exception {
            // given
            Long courseId = 100L;
            Long userId = 1L;

            given(courseLikeService.updateCourseLike(courseId, userId))
                    .willThrow(new EntityNotFoundException("?????? ???????????? ????????? ???????????? ????????????. ????????? ?????? ????????? : " + courseId));

            String accessToken = generateUserAccessToken(userId);

            // when
            String path = "/courses/{courseId}/like";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.post(path, courseId)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            // then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.errorCode").exists())
                    .andExpect(jsonPath("$.data.message").exists());

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
        @DisplayName("?????? ????????? ????????? ?????? ????????? ????????? '?????? ??????'??? ?????????, ?????? ????????? ?????? ????????? ????????? ??? ?????? ?????????, http status 400 ????????????.")
        void notCompleteCourseError() throws Exception {
            // given
            Course course = setCourses(1L, 1).stream().findFirst().orElseThrow();
            Long courseId = course.getId();
            Long currentUserId = 3L;

            String accessToken = generateUserAccessToken(currentUserId);

            // mocking
            mockingUpdateCourseLikeSuccess(course, currentUserId);

            // when
            String path = "/courses/{courseId}/like";
            ResultActions perform = mockMvc.perform(
                    RestDocumentationRequestBuilders.post(path, courseId)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_TYPE + accessToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            // then
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.errorCode").exists())
                    .andExpect(jsonPath("$.data.message").exists());

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

        // TODO ??????????????? ?????? ????????? ?????? ??????
    }
}
