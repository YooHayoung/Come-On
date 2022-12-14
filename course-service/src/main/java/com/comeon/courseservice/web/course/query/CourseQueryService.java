package com.comeon.courseservice.web.course.query;

import com.comeon.courseservice.common.exception.CustomException;
import com.comeon.courseservice.common.exception.ErrorCode;
import com.comeon.courseservice.domain.common.exception.EntityNotFoundException;
import com.comeon.courseservice.domain.course.entity.Course;
import com.comeon.courseservice.domain.course.entity.CourseStatus;
import com.comeon.courseservice.web.common.file.FileManager;
import com.comeon.courseservice.web.common.response.SliceResponse;
import com.comeon.courseservice.web.course.query.repository.CourseLikeQueryRepository;
import com.comeon.courseservice.web.course.query.repository.cond.CourseCondition;
import com.comeon.courseservice.web.course.query.repository.cond.MyCourseCondition;
import com.comeon.courseservice.web.course.query.repository.dto.CourseListData;
import com.comeon.courseservice.web.course.query.repository.CourseQueryRepository;
import com.comeon.courseservice.web.course.query.repository.dto.MyPageCourseListData;
import com.comeon.courseservice.web.course.response.CourseDetailResponse;
import com.comeon.courseservice.web.course.response.CourseListResponse;
import com.comeon.courseservice.web.course.response.MyPageCourseListResponse;
import com.comeon.courseservice.web.course.response.UserDetailInfo;
import com.comeon.courseservice.web.feign.userservice.UserFeignService;
import com.comeon.courseservice.web.feign.userservice.response.UserDetailsResponse;
import com.comeon.courseservice.web.feign.userservice.response.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryService {

    @Value("${s3.folder-name.course}")
    private String dirName;

    private final FileManager fileManager;

    private final UserFeignService userFeignService;

    private final CourseQueryRepository courseQueryRepository;
    private final CourseLikeQueryRepository courseLikeQueryRepository;

    public CourseStatus getCourseStatus(Long courseId) {
        return courseQueryRepository.findById(courseId)
                .map(Course::getCourseStatus)
                .orElseThrow(
                        () -> new EntityNotFoundException("?????? ???????????? ????????? ???????????? ????????????. ????????? ?????? ????????? : " + courseId)
                );
    }

    public CourseDetailResponse getCourseDetails(Long courseId, Long userId) {
        Course course = courseQueryRepository.findByIdFetchAll(courseId)
                .orElseThrow(
                        () -> new EntityNotFoundException("?????? ???????????? ????????? ???????????? ????????????. ????????? ?????? ????????? : " + courseId)
                );

        // ?????? ?????? ???????????? ????????????, ?????? ???????????? ?????? ????????? ?????? X
        if (!course.getUserId().equals(userId)) {
            if (course.getCourseStatus() == CourseStatus.WRITING) {
                throw new CustomException("?????? ???????????? ?????? ???????????????. ????????? ?????? ????????? : " + courseId, ErrorCode.WRITING_COURSE);
            }
            if (course.getCourseStatus() == CourseStatus.DISABLED) {
                throw new CustomException("???????????? ??? ???????????????. ????????? ?????? ????????? : " + courseId, ErrorCode.DISABLED_COURSE);
            }
        }

        // ?????? ????????? ????????? ????????????
        UserDetailInfo userDetailInfo = getUserDetailInfo(course.getUserId());

        // ?????? ????????? ??????
        String fileUrl = getCourseImageUrl(course.getCourseImage().getStoredName());

        // ?????? ????????? ??????
        boolean userLiked = doesUserLikeCourse(userId, course);

        return new CourseDetailResponse(course, userDetailInfo, fileUrl, userLiked);
    }

    // ?????? ????????? ??????
    public SliceResponse<CourseListResponse> getCourseList(Long userId,
                                                           CourseCondition courseCondition,
                                                           Pageable pageable) {
        Slice<CourseListData> courseSlice = courseQueryRepository.findCourseSlice(userId, courseCondition, pageable);

        // ?????? ???????????? ????????? id ????????? ??????
        List<Long> writerIds = courseSlice.getContent().stream()
                .map(courseListData -> courseListData.getCourse().getUserId())
                .distinct()
                .collect(Collectors.toList());

        // ????????? id ???????????? ????????? ?????? ??????
        Map<Long, UserDetailInfo> userDetailInfoMap = getUserDetailInfoMap(writerIds);

        Slice<CourseListResponse> courseListResponseSlice = courseSlice.map(
                courseListData -> CourseListResponse.builder()
                        .course(courseListData.getCourse())
                        .coursePlace(courseListData.getCoursePlace())
                        .firstPlaceDistance(courseListData.getDistance())
                        .imageUrl(getCourseImageUrl(courseListData.getCourse().getCourseImage().getStoredName()))
                        .writer(
                                userDetailInfoMap.getOrDefault(
                                        courseListData.getCourse().getUserId(),
                                        new UserDetailInfo(courseListData.getCourse().getUserId(), null)
                                )
                        )
                        .userLiked(Objects.nonNull(courseListData.getUserLikeId()))
                        .build()
        );

        return SliceResponse.toSliceResponse(courseListResponseSlice);
    }

    // ????????? ????????? ?????? ????????? ??????
    public SliceResponse<MyPageCourseListResponse> getMyRegisteredCourseList(Long userId,
                                                                             MyCourseCondition condition,
                                                                             Pageable pageable) {
        Slice<MyPageCourseListData> myCourseSlice = courseQueryRepository.findMyCourseSlice(userId, condition, pageable);

        // ?????? ????????? ??????
        UserDetailInfo userDetailInfo = getUserDetailInfo(userId);

        Slice<MyPageCourseListResponse> myCourseListResponseSlice = myCourseSlice.map(
                myPageCourseListData -> MyPageCourseListResponse.builder()
                        .course(myPageCourseListData.getCourse())
                        .imageUrl(getCourseImageUrl(myPageCourseListData.getCourse().getCourseImage().getStoredName()))
                        .writer(userDetailInfo)
                        .userLiked(Objects.nonNull(myPageCourseListData.getUserLikeId()))
                        .build()
        );

        return SliceResponse.toSliceResponse(myCourseListResponseSlice);
    }

    // ????????? ???????????? ?????? ????????? ??????
    public SliceResponse<MyPageCourseListResponse> getMyLikedCourseList(Long userId, Pageable pageable) {
        // ?????? ????????? ??????
        Slice<MyPageCourseListData> myLikedCourseSlice = courseQueryRepository.findMyLikedCourseSlice(userId, pageable);

        // ?????? ???????????? ????????? id ????????? ??????
        List<Long> writerIds = myLikedCourseSlice.getContent().stream()
                .map(courseListData -> courseListData.getCourse().getUserId())
                .distinct()
                .collect(Collectors.toList());

        // ????????? id ???????????? ?????? ????????? ??????
        Map<Long, UserDetailInfo> userDetailInfoMap = getUserDetailInfoMap(writerIds);

        // ????????? ??????
        Slice<MyPageCourseListResponse> myLikedCourseListResponseSlice = myLikedCourseSlice.map(
                courseListData -> MyPageCourseListResponse.builder()
                        .course(courseListData.getCourse())
                        .imageUrl(getCourseImageUrl(courseListData.getCourse().getCourseImage().getStoredName()))
                        .writer(
                                userDetailInfoMap.getOrDefault(
                                        courseListData.getCourse().getUserId(),
                                        new UserDetailInfo(courseListData.getCourse().getUserId(), null)
                                )
                        )
                        .userLiked(Objects.nonNull(courseListData.getUserLikeId()))
                        .build()
        );

        return SliceResponse.toSliceResponse(myLikedCourseListResponseSlice);
    }

    public String getStoredFileName(Long courseId) {
        return courseQueryRepository.findByIdFetchCourseImg(courseId)
                .orElseThrow(
                        () -> new EntityNotFoundException("?????? ???????????? ????????? ???????????? ????????????. ????????? ?????? ????????? : " + courseId)
                )
                .getCourseImage()
                .getStoredName();
    }

    private UserDetailInfo getUserDetailInfo(Long userId) {
        UserDetailsResponse detailsResponse = userFeignService.getUserDetails(userId).orElse(null);

        String userNickname = null;
        if (Objects.nonNull(detailsResponse)) {
            if (detailsResponse.getStatus().equals(UserStatus.WITHDRAWN)) {
                userNickname = "????????? ???????????????.";
            } else {
                userNickname = detailsResponse.getNickname();
            }
        }

        return new UserDetailInfo(userId, userNickname);
    }

    private Map<Long, UserDetailInfo> getUserDetailInfoMap(List<Long> userIds) {
        // userStatus == WITHDRAWN ?????? ????????? ?????? ??????
        return userFeignService.getUserDetailsMap(userIds)
                .entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry ->
                                        new UserDetailInfo(
                                                entry.getValue().getUserId(),
                                                entry.getValue().getStatus().equals(UserStatus.WITHDRAWN)
                                                        ? "????????? ???????????????."
                                                        : entry.getValue().getNickname()
                                        )
                        )
                );

    }

    private boolean doesUserLikeCourse(Long userId, Course course) {
        if (userId != null) {
            return courseLikeQueryRepository.findByCourseAndUserId(course, userId).isPresent();
        }
        return false;
    }

    private String getCourseImageUrl(String storedFileName) {
        return fileManager.getFileUrl(storedFileName, dirName);
    }
}
