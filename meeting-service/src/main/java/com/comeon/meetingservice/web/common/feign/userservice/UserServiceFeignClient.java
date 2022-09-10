package com.comeon.meetingservice.web.common.feign.userservice;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserServiceFeignClient {

    @GetMapping("/users")
    UserServiceApiResponse<UserServiceListResponse<UserListResponse>> getUsers(
            @RequestParam("userIds") List<Long> userIds);

    @GetMapping("/users/{userId}")
    UserServiceApiResponse<UserDetailResponse> getUser(@PathVariable("userId") Long userId);

}