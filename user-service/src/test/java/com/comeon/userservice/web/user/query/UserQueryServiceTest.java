package com.comeon.userservice.web.user.query;

import com.amazonaws.services.s3.AmazonS3;
import com.comeon.userservice.common.exception.CustomException;
import com.comeon.userservice.common.exception.ErrorCode;
import com.comeon.userservice.config.S3MockConfig;
import com.comeon.userservice.domain.common.exception.EntityNotFoundException;
import com.comeon.userservice.domain.profileimage.entity.ProfileImg;
import com.comeon.userservice.domain.profileimage.repository.ProfileImgRepository;
import com.comeon.userservice.domain.user.entity.OAuthProvider;
import com.comeon.userservice.domain.user.entity.User;
import com.comeon.userservice.domain.user.entity.UserAccount;
import com.comeon.userservice.domain.user.entity.UserStatus;
import com.comeon.userservice.domain.user.repository.UserRepository;
import com.comeon.userservice.web.common.file.FileManager;
import com.comeon.userservice.web.common.file.UploadedFileInfo;
import com.comeon.userservice.web.user.response.UserDetailResponse;
import com.comeon.userservice.web.user.response.UserSimpleResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@Transactional
@ActiveProfiles("test")
@Import({S3MockConfig.class})
@SpringBootTest
class UserQueryServiceTest {

    @Autowired
    UserQueryService userQueryService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProfileImgRepository profileImgRepository;

    @Autowired
    EntityManager em;

    @Autowired
    FileManager fileManager;

    @Value("${s3.folder-name.user}")
    String dirName;

    User user;
    ProfileImg profileImg;

    void initUser() {
        user = userRepository.save(
                User.builder()
                        .account(
                                UserAccount.builder()
                                        .oauthId("oauthId")
                                        .provider(OAuthProvider.KAKAO)
                                        .email("email")
                                        .name("name")
                                        .build()
                        )
                        .build()
        );
    }

    void initProfileImg() throws IOException {
        File imgFile = ResourceUtils.getFile(this.getClass().getResource("/static/test-img.png"));
        UploadedFileInfo uploadedFileInfo = fileManager.upload(getMockMultipartFile(imgFile), dirName);
        profileImg = profileImgRepository.save(
                ProfileImg.builder()
                        .user(user)
                        .originalName(uploadedFileInfo.getOriginalFileName())
                        .storedName(uploadedFileInfo.getStoredFileName())
                        .build()
        );
    }

    private MockMultipartFile getMockMultipartFile(File imgFile) throws IOException {
        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "imgFile",
                "test-img.png",
                ContentType.IMAGE_JPEG.getMimeType(),
                new FileInputStream(imgFile)
        );
        return mockMultipartFile;
    }


    @Nested
    @DisplayName("?????? ?????? ?????? ??????")
    class getUserDetails {

        @Test
        @DisplayName("????????? ???????????? ??????, ????????? ????????? ???????????? ???????????? " +
                "userId, nickname, profileImgId, profileImgUrl, role, email, name ????????? ????????????.")
        void whenUserHasProfileImg() throws IOException {
            // given
            initUser();
            initProfileImg();
            Long userId = user.getId();

            // when
            UserDetailResponse userDetails = userQueryService.getUserDetails(userId);

            // then
            String profileImgUrl = userDetails.getProfileImg() != null ? userDetails.getProfileImg().getImageUrl() : null;
            log.info("userProfileImgUrl : {}", profileImgUrl);
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUserId()).isEqualTo(userId);
            assertThat(userDetails.getNickname()).isEqualTo(user.getNickname());
            assertThat(userDetails.getEmail()).isEqualTo(user.getAccount().getEmail());
            assertThat(userDetails.getName()).isEqualTo(user.getAccount().getName());
            assertThat(userDetails.getRole()).isEqualTo(user.getRole().getRoleValue());
            assertThat(userDetails.getProfileImg()).isNotNull();
            assertThat(userDetails.getProfileImg().getId()).isEqualTo(profileImg.getId());
            assertThat(userDetails.getProfileImg().getImageUrl()).isNotNull();
        }

        @Test
        @DisplayName("????????? ???????????? ??????, ????????? ????????? ???????????? ???????????? " +
                "userId, nickname, role, email, name ????????? ????????????.")
        void whenUserDoesNotHaveProfileImg() {
            // given
            initUser();
            Long userId = user.getId();

            // when
            UserDetailResponse userDetails = userQueryService.getUserDetails(userId);

            // then
            String profileImgUrl = userDetails.getProfileImg() != null ? userDetails.getProfileImg().getImageUrl() : null;
            log.info("userProfileImgUrl : {}", profileImgUrl);
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUserId()).isEqualTo(userId);
            assertThat(userDetails.getNickname()).isEqualTo(user.getNickname());
            assertThat(userDetails.getEmail()).isEqualTo(user.getAccount().getEmail());
            assertThat(userDetails.getName()).isEqualTo(user.getAccount().getName());
            assertThat(userDetails.getRole()).isEqualTo(user.getRole().getRoleValue());
            assertThat(userDetails.getProfileImg()).isNull();
        }

        @Test
        @DisplayName("????????? ????????? ????????????, CustomException ????????????. ????????? ErrorCode.ALREADY_WITHDRAW ?????????.")
        void whenUserWithdrawn() {
            // given
            initUser();
            Long userId = user.getId();

            user.withdrawal();
            em.flush();

            // when, then
            assertThatThrownBy(
                    () -> userQueryService.getUserDetails(userId)
            )
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_WITHDRAW);
        }

        @Test
        @DisplayName("???????????? ?????? ?????? ???????????? ????????????, EntityNotFoundException ????????????.")
        void whenInvalidUserId() {
            // given
            Long invalidUserId = 100L;

            // when, then
            assertThatThrownBy(
                    () -> userQueryService.getUserSimple(invalidUserId)
            ).isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("?????? ?????? ?????? ??????")
    class getUserSimple {

        @Test
        @DisplayName("????????? ???????????? ??????, ????????? ????????? ???????????? ???????????? " +
                "userId, nickname, profileImgUrl ????????? ????????????.")
        void whenUserHasProfileImg() throws IOException {
            // given
            initUser();
            initProfileImg();
            Long userId = user.getId();

            // when
            UserSimpleResponse userSimple = userQueryService.getUserSimple(userId);

            // then
            log.info("userProfileImgUrl : {}", userSimple.getProfileImgUrl());
            assertThat(userSimple).isNotNull();
            assertThat(userSimple.getUserId()).isEqualTo(userId);
            assertThat(userSimple.getNickname()).isEqualTo(user.getNickname());
            assertThat(userSimple.getProfileImgUrl()).isNotNull();
        }

        @Test
        @DisplayName("????????? ???????????? ??????, ????????? ????????? ???????????? ???????????? " +
                "userId, nickname ????????? ????????????.")
        void whenUserDoesNotHaveProfileImg() {
            // given
            initUser();
            Long userId = user.getId();

            // when
            UserSimpleResponse userSimple = userQueryService.getUserSimple(userId);

            // then
            log.info("userProfileImgUrl : {}", userSimple.getProfileImgUrl());
            assertThat(userSimple).isNotNull();
            assertThat(userSimple.getUserId()).isEqualTo(userId);
            assertThat(userSimple.getNickname()).isEqualTo(user.getNickname());
            assertThat(userSimple.getProfileImgUrl()).isNull();
        }

        @Test
        @DisplayName("????????? ????????? ????????????, ???????????? userId, status ????????? ?????????. status ????????? WITHDRAWN ??????.")
        void whenUserWithdrawn() {
            // given
            initUser();
            Long userId = user.getId();

            user.withdrawal();
            em.flush();

            // when
            UserSimpleResponse userSimple = userQueryService.getUserSimple(userId);

            // then
            assertThat(userSimple.getUserId()).isEqualTo(userId);
            assertThat(userSimple.getStatus()).isEqualTo(UserStatus.WITHDRAWN.name());
            assertThat(userSimple.getNickname()).isNull();
            assertThat(userSimple.getProfileImgUrl()).isNull();
        }

        @Test
        @DisplayName("???????????? ?????? ?????? ???????????? ????????????, EntityNotFoundException ????????????.")
        void whenInvalidUserId() {
            // given
            Long invalidUserId = 100L;

            // when, then
            assertThatThrownBy(
                    () -> userQueryService.getUserSimple(invalidUserId)
            ).isInstanceOf(EntityNotFoundException.class);
        }
    }

}