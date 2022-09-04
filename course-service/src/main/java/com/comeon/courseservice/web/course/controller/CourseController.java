package com.comeon.courseservice.web.course.controller;

import com.comeon.courseservice.config.argresolver.CurrentUserId;
import com.comeon.courseservice.domain.course.service.CourseService;
import com.comeon.courseservice.domain.course.service.dto.CourseDto;
import com.comeon.courseservice.domain.course.service.dto.CourseImageDto;
import com.comeon.courseservice.web.common.aop.ValidationRequired;
import com.comeon.courseservice.web.common.file.FileManager;
import com.comeon.courseservice.web.common.file.UploadedFileInfo;
import com.comeon.courseservice.web.common.response.ApiResponse;
import com.comeon.courseservice.web.course.query.CourseQueryService;
import com.comeon.courseservice.web.course.request.CourseSaveRequest;
import com.comeon.courseservice.web.course.response.CourseDetailResponse;
import com.comeon.courseservice.web.course.response.CourseLikeRemoveResponse;
import com.comeon.courseservice.web.course.response.CourseLikeSaveResponse;
import com.comeon.courseservice.web.course.response.CourseSaveResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/courses")
public class CourseController {

    @Value("${s3.folder-name.course}")
    private String dirName;

    private final FileManager fileManager;
    private final CourseService courseService;

    private final CourseQueryService courseQueryService;

    // 코스 저장 POST /courses
    @ValidationRequired
    @PostMapping
    public ApiResponse<CourseSaveResponse> courseSave(@CurrentUserId Long currentUserId,
                                                      @Validated @ModelAttribute CourseSaveRequest request,
                                                      BindingResult bindingResult) {
        // 이미지 저장 후, 코스 이미지 dto로 변환
        CourseImageDto courseImageDto = generateCourseImageDto(
                fileManager.upload(request.getImgFile(), dirName)
        );

        // 요청 데이터 -> 코스 dto로 변환
        CourseDto courseDto = request.toServiceDto();
        courseDto.setUserId(currentUserId);
        courseDto.setCourseImageDto(courseImageDto);

        Long courseId = null;
        try {
            courseId = courseService.saveCourse(courseDto);
        } catch (RuntimeException e) {
            fileManager.delete(courseImageDto.getStoredName(), dirName);
            throw e;
        }

        return ApiResponse.createSuccess(new CourseSaveResponse(courseId));
    }

    // 코스 단건 조회 GET /courses/{courseId}
    @GetMapping("/{courseId}")
    public ApiResponse<CourseDetailResponse> courseDetails(@PathVariable Long courseId,
                                                           @CurrentUserId Long currentUserId) {
        // TODO 좋아요 여부
        CourseDetailResponse courseDetails = courseQueryService.getCourseDetails(courseId, currentUserId);

        return ApiResponse.createSuccess(courseDetails);
    }

    // 코스 목록 조회 GET /courses

    // 코스 수정 PATCH /courses/{courseId}

    // 코스 삭제 DELETE /courses/{courseId}

    // 코스 좋아요 등록 POST /courses/{courseId}/like
    @PostMapping("/{courseId}/like")
    public ApiResponse<CourseLikeSaveResponse> courseLikeSave(@CurrentUserId Long currentUserId,
                                                              @PathVariable Long courseId) {

        return ApiResponse.createSuccess(
                new CourseLikeSaveResponse(courseService.saveCourseLike(courseId, currentUserId))
        );
    }

    // 코스 좋아요 삭제 DELETE /courses/{courseId}/like/{likeId}
    @DeleteMapping("/{courseId}/like/{likeId}")
    public ApiResponse<CourseLikeRemoveResponse> courseLikeRemove(@CurrentUserId Long currentUserId,
                                                                  @PathVariable Long courseId,
                                                                  @PathVariable Long likeId) {
        courseService.removeCourseLike(likeId, courseId, currentUserId);

        return ApiResponse.createSuccess(
                new CourseLikeRemoveResponse()
        );
    }


    /* ### private method ### */
    private CourseImageDto generateCourseImageDto(UploadedFileInfo uploadedFileInfo) {
        return CourseImageDto.builder()
                .originalName(uploadedFileInfo.getOriginalFileName())
                .storedName(uploadedFileInfo.getStoredFileName())
                .build();
    }
}
