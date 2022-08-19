package com.comeon.meetingservice.web.common.response;

import lombok.*;

import static lombok.AccessLevel.*;

@Getter @Setter
@Builder
@AllArgsConstructor(access = PRIVATE)
public class ErrorResponse {

    private Integer code;
    private String message;

}