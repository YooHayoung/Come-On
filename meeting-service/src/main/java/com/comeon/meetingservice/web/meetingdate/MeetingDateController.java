package com.comeon.meetingservice.web.meetingdate;

import com.comeon.meetingservice.domain.meetingdate.dto.MeetingDateAddDto;
import com.comeon.meetingservice.domain.meetingdate.dto.MeetingDateModifyDto;
import com.comeon.meetingservice.domain.meetingdate.dto.MeetingDateRemoveDto;
import com.comeon.meetingservice.domain.meetingdate.service.MeetingDateService;
import com.comeon.meetingservice.web.common.aop.ValidationRequired;
import com.comeon.meetingservice.web.common.argumentresolver.UserId;
import com.comeon.meetingservice.web.common.response.ApiResponse;
import com.comeon.meetingservice.web.meetingdate.query.MeetingDateQueryService;
import com.comeon.meetingservice.web.meetingdate.request.MeetingDateAddRequest;
import com.comeon.meetingservice.web.meetingdate.request.MeetingDateModifyRequest;
import com.comeon.meetingservice.web.meetingdate.response.MeetingDateDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/meetings/{meetingId}/dates")
@RequiredArgsConstructor
public class MeetingDateController {

    private final MeetingDateService meetingDateService;
    private final MeetingDateQueryService meetingDateQueryService;

    @PostMapping
    @ValidationRequired
    @ResponseStatus(CREATED)
    public ApiResponse<Long> meetingDateAdd(
            @PathVariable("meetingId") Long meetingId,
            @Validated @RequestBody MeetingDateAddRequest meetingDateAddRequest,
            BindingResult bindingResult,
            @UserId Long userId) {

        MeetingDateAddDto meetingDateAddDto = meetingDateAddRequest.toDto(meetingId, userId);

        Long savedId = meetingDateService.add(meetingDateAddDto);

        return ApiResponse.createSuccess(savedId);
    }

    @PatchMapping("/{dateId}")
    @ValidationRequired
    public ApiResponse meetingDateModify(
            @PathVariable("dateId") Long id,
            @Validated @RequestBody MeetingDateModifyRequest meetingDateModifyRequest,
            BindingResult bindingResult) {

        MeetingDateModifyDto meetingDateModifyDto = meetingDateModifyRequest.toDto(id);
        meetingDateService.modify(meetingDateModifyDto);

        return ApiResponse.createSuccess();
    }

    @DeleteMapping("/{dateId}")
    public ApiResponse meetingDateRemove(@PathVariable("dateId") Long id,
                                         @UserId Long userId) {

        MeetingDateRemoveDto meetingDateRemoveDto = MeetingDateRemoveDto.builder()
                .userId(userId)
                .id(id)
                .build();

        meetingDateService.remove(meetingDateRemoveDto);

        return ApiResponse.createSuccess();
    }

    @GetMapping("/{dateId}")
    public ApiResponse<MeetingDateDetailResponse> meetingDateDetail(
            @PathVariable("dateId") Long id) {

        MeetingDateDetailResponse meetingDateDetailResponse
                = meetingDateQueryService.getDetail(id);

        return ApiResponse.createSuccess(meetingDateDetailResponse);
    }

}