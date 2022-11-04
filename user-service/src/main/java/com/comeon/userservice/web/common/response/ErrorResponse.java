package com.comeon.userservice.web.common.response;

import lombok.*;

@Getter @Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponse<T> {

    private Integer errorCode;
    private T message;

}
