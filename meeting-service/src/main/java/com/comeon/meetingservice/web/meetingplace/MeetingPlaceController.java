package com.comeon.meetingservice.web.meetingplace;

import com.comeon.meetingservice.domain.meetingplace.dto.MeetingPlaceModifyDto;
import com.comeon.meetingservice.domain.meetingplace.dto.MeetingPlaceRemoveDto;
import com.comeon.meetingservice.domain.meetingplace.dto.MeetingPlaceAddDto;
import com.comeon.meetingservice.domain.meetingplace.service.MeetingPlaceService;
import com.comeon.meetingservice.web.common.aop.ValidationRequired;
import com.comeon.meetingservice.web.common.response.ApiResponse;
import com.comeon.meetingservice.web.meetingplace.query.MeetingPlaceQueryService;
import com.comeon.meetingservice.web.meetingplace.request.MeetingPlaceModifyRequest;
import com.comeon.meetingservice.web.meetingplace.request.MeetingPlaceAddRequest;
import com.comeon.meetingservice.web.meetingplace.request.PlaceModifyRequestValidator;
import com.comeon.meetingservice.web.meetingplace.response.MeetingPlaceDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestController
@RequestMapping("/meetings/{meetingId}/places")
@RequiredArgsConstructor
public class MeetingPlaceController {

    private final MeetingPlaceService meetingPlaceService;
    private final MeetingPlaceQueryService meetingPlaceQueryService;
    private final PlaceModifyRequestValidator placeModifyRequestValidator;

    @InitBinder("meetingPlaceModifyRequest")
    public void init(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(placeModifyRequestValidator);
    }

    @PostMapping
    @ValidationRequired
    @ResponseStatus(CREATED)
    public ApiResponse<Long> meetingPlaceAdd(
            @PathVariable("meetingId") Long meetingId,
            @Validated @RequestBody MeetingPlaceAddRequest meetingPlaceAddRequest,
            BindingResult bindingResult) {

        MeetingPlaceAddDto meetingPlaceAddDto = meetingPlaceAddRequest.toDto(meetingId);

        Long savedId = meetingPlaceService.add(meetingPlaceAddDto);

        return ApiResponse.createSuccess(savedId);
    }

    @PatchMapping("/{placeId}")
    @ValidationRequired
    public ApiResponse meetingPlaceModify(
            @PathVariable("placeId") Long id,
            @Validated @RequestBody MeetingPlaceModifyRequest meetingPlaceModifyRequest,
            BindingResult bindingResult) {

        MeetingPlaceModifyDto meetingPlaceModifyDto = meetingPlaceModifyRequest.toDto(id);

        meetingPlaceService.modify(meetingPlaceModifyDto);

        return ApiResponse.createSuccess();
    }

    @DeleteMapping("/{placeId}")
    public ApiResponse meetingPlaceRemove(@PathVariable("placeId") Long id) {

        meetingPlaceService.remove(MeetingPlaceRemoveDto.builder().id(id).build());

        return ApiResponse.createSuccess();
    }

    @GetMapping("/{placeId}")
    public ApiResponse<MeetingPlaceDetailResponse> meetingDetail(
            @PathVariable("placeId") Long id) {

        return ApiResponse.createSuccess(meetingPlaceQueryService.getDetail(id));
    }

}