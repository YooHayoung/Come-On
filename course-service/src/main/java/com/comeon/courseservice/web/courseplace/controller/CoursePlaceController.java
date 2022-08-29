package com.comeon.courseservice.web.courseplace.controller;

import com.comeon.courseservice.config.argresolver.CurrentUserId;
import com.comeon.courseservice.domain.courseplace.service.CoursePlaceService;
import com.comeon.courseservice.domain.courseplace.service.dto.CoursePlaceDto;
import com.comeon.courseservice.web.common.aop.ValidationRequired;
import com.comeon.courseservice.web.common.response.ApiResponse;
import com.comeon.courseservice.web.courseplace.request.CoursePlacesBatchSaveRequest;
import com.comeon.courseservice.web.courseplace.request.CoursePlaceSaveRequest;
import com.comeon.courseservice.web.courseplace.response.CoursePlaceSaveResponse;
import com.comeon.courseservice.web.courseplace.response.CoursePlacesBatchSaveResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/course-places")
public class CoursePlaceController {

    private final CoursePlaceService coursePlaceService;

    // 코스 장소 등록
    @ValidationRequired
    @PostMapping
    public ApiResponse<CoursePlaceSaveResponse> coursePlaceSave(@CurrentUserId Long currentUserId,
                                                                @Validated @RequestBody CoursePlaceSaveRequest request,
                                                                BindingResult bindingResult) {
        Long courseId = request.getCourseId();
        CoursePlaceDto coursePlaceDto = request.toServiceDto();

        Long coursePlaceId = coursePlaceService.saveCoursePlace(courseId, currentUserId, coursePlaceDto);

        return ApiResponse.createSuccess(new CoursePlaceSaveResponse(coursePlaceId));
    }

    // 코스 장소 리스트 등록
    @ValidationRequired
    @PostMapping("/batch")
    public ApiResponse<CoursePlacesBatchSaveResponse> coursePlaceSaveBatch(@CurrentUserId Long currentUserId,
                                                                           @Validated @RequestBody CoursePlacesBatchSaveRequest request,
                                                                           BindingResult bindingResult) {
        Long courseId = request.getCourseId();
        List<CoursePlaceDto> coursePlaceDtos = request.getCoursePlaces().stream()
                .map(CoursePlacesBatchSaveRequest.CoursePlaceInfo::toServiceDto)
                .collect(Collectors.toList());

        coursePlaceService.batchSaveCoursePlace(courseId, currentUserId, coursePlaceDtos);

        // TODO 응답 값?
        return ApiResponse.createSuccess(new CoursePlacesBatchSaveResponse());
    }
}
