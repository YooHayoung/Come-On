package com.comeon.meetingservice.web.meetingdate.request;

import com.comeon.meetingservice.domain.meetingdate.dto.MeetingDateModifyDto;
import com.comeon.meetingservice.domain.meetingdate.entity.DateStatus;
import lombok.*;

import javax.validation.constraints.NotNull;

import static lombok.AccessLevel.PRIVATE;

@Getter @Setter
@Builder
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE)
public class MeetingDateModifyRequest {

    @NotNull
    private DateStatus dateStatus;

    public MeetingDateModifyDto toDto(Long meetingId, Long id) {
        return MeetingDateModifyDto.builder()
                .meetingId(meetingId)
                .id(id)
                .dateStatus(dateStatus)
                .build();
    }
}
