package com.comeon.meetingservice.web.meeting;

import com.comeon.meetingservice.domain.meeting.dto.MeetingModifyDto;
import com.comeon.meetingservice.domain.meeting.dto.MeetingRemoveDto;
import com.comeon.meetingservice.domain.meeting.dto.MeetingAddDto;
import com.comeon.meetingservice.domain.meeting.entity.MeetingRole;
import com.comeon.meetingservice.domain.meeting.service.MeetingService;
import com.comeon.meetingservice.web.common.aop.ValidationRequired;
import com.comeon.meetingservice.web.common.argumentresolver.UserId;
import com.comeon.meetingservice.web.common.feign.courseservice.CourseFeignService;
import com.comeon.meetingservice.web.common.feign.courseservice.response.CourseListResponse;
import com.comeon.meetingservice.web.common.interceptor.MeetingAuth;
import com.comeon.meetingservice.web.common.response.ApiResponse;
import com.comeon.meetingservice.web.common.response.SliceResponse;
import com.comeon.meetingservice.web.common.util.fileutils.FileManager;
import com.comeon.meetingservice.web.common.util.fileutils.UploadFileDto;
import com.comeon.meetingservice.web.meeting.query.MeetingQueryService;
import com.comeon.meetingservice.web.meeting.query.MeetingCondition;
import com.comeon.meetingservice.web.meeting.request.MeetingModifyRequest;
import com.comeon.meetingservice.web.meeting.request.MeetingAddRequest;
import com.comeon.meetingservice.web.meeting.response.MeetingDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final Environment env;
    private final MeetingService meetingService;
    private final MeetingQueryService meetingQueryService;
    private final CourseFeignService courseFeignService;
    private final FileManager fileManager;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ValidationRequired
    public ApiResponse<Long> meetingAdd(@Validated @ModelAttribute MeetingAddRequest meetingAddRequest,
                                        BindingResult bindingResult,
                                        @UserId Long userId) {

        // ????????? ?????? ????????? ????????? ?????? ?????? ????????????
        List<CourseListResponse> coursePlaceList = new ArrayList<>();
        if (Objects.nonNull(meetingAddRequest.getCourseId())) {
            coursePlaceList = courseFeignService.getCoursePlaceList(meetingAddRequest.getCourseId());
        }

        // ????????? ?????? ?????????
        UploadFileDto uploadFileDto = uploadImage(meetingAddRequest.getImage());

        MeetingAddDto meetingAddDto = meetingAddRequest.toDto(
                userId,
                uploadFileDto.getOriginalFileName(),
                uploadFileDto.getStoredFileName(),
                coursePlaceList
        );

        try {
            Long savedId = meetingService.add(meetingAddDto);
            return ApiResponse.createSuccess(savedId);
        } catch (RuntimeException e) {
            deleteImage(uploadFileDto.getStoredFileName());
            throw e;
        }
    }

    @PostMapping("/{meetingId}")
    @ValidationRequired
    @MeetingAuth(meetingRoles = MeetingRole.HOST)
    public ApiResponse meetingModify(@PathVariable("meetingId") Long meetingId,
                                     @Validated @ModelAttribute MeetingModifyRequest meetingModifyRequest,
                                     BindingResult bindingResult) {

        MeetingModifyDto meetingModifyDto = meetingModifyRequest.toDto();
        meetingModifyDto.setId(meetingId);

        // ????????? ???????????????
        if (Objects.nonNull(meetingModifyRequest.getImage())) {
            // ????????? ?????? ?????? ??????
            UploadFileDto uploadFileDto = uploadImage(meetingModifyRequest.getImage());
            meetingModifyDto.setOriginalFileName(uploadFileDto.getOriginalFileName());
            meetingModifyDto.setStoredFileName(uploadFileDto.getStoredFileName());

            // ????????? ???????????? ????????? ???????????? ????????? ???????????? ?????? ?????? ????????? ???????????? (???????????? ?????? ??????)
            String fileNameToDelete = meetingQueryService.getStoredFileName(meetingId);
            try {
                // DB??? ?????? ??? ????????? ???????????? ????????? ????????? deleteFile ????????? ????????????
                meetingService.modify(meetingModifyDto);

                // Service ?????? ?????? ????????? ?????? ?????? ????????? ?????? ????????? ???????????? ??????,
                // ??? fileNameToDelete ????????? ?????? ????????? ?????? ????????? ???????????? ??????
            } catch (RuntimeException e) {
                fileNameToDelete = uploadFileDto.getStoredFileName();
                throw e;
            } finally {
                // ??????????????? ????????? ???????????? ???????????? ?????? ????????? ????????????, ????????? ???????????? ???????????? ????????? ????????? ???????????? ??????
                deleteImage(fileNameToDelete);
            }
        } else {
            // ????????? ???????????? ???????????? ?????? ????????? ?????? ?????? ??????, ????????? ????????? ?????? ??????
            meetingService.modify(meetingModifyDto);
        }

        return ApiResponse.createSuccess();
    }

    @DeleteMapping("/{meetingId}")
    @MeetingAuth(meetingRoles = {MeetingRole.HOST, MeetingRole.EDITOR, MeetingRole.PARTICIPANT})
    public ApiResponse meetingRemove(@PathVariable("meetingId") Long meetingId,
                                     @UserId Long userId) {

        String storedFileName = meetingQueryService.getStoredFileName(meetingId);

        meetingService.remove(
                MeetingRemoveDto.builder()
                        .id(meetingId)
                        .userId(userId)
                        .build()
        );

        deleteImage(storedFileName);
        return ApiResponse.createSuccess();
    }

    @GetMapping
    public ApiResponse<SliceResponse> meetingList(@UserId Long userId,
                                                  @PageableDefault(size = 5, page = 0) Pageable pageable,
                                                  MeetingCondition meetingCondition) {
        return ApiResponse.createSuccess(
                meetingQueryService.getList(userId, pageable, meetingCondition));
    }

    @GetMapping("/{meetingId}")
    @MeetingAuth(meetingRoles = {MeetingRole.HOST, MeetingRole.EDITOR, MeetingRole.PARTICIPANT})
    public ApiResponse<MeetingDetailResponse> meetingDetail(@PathVariable("meetingId") Long meetingId,
                                                            @UserId Long userId) {
        return ApiResponse.createSuccess(
                meetingQueryService.getDetail(meetingId, userId));
    }

    private UploadFileDto uploadImage(MultipartFile image) {
        return fileManager.upload(
                image,
                env.getProperty("meeting-file.dir"));
    }

    private void deleteImage(String storedFileName) {
        fileManager.delete(
                storedFileName,
                env.getProperty("meeting-file.dir"));
    }
}
