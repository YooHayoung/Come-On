package com.comeon.userservice.web.profileimage.query;

import com.comeon.userservice.common.exception.CustomException;
import com.comeon.userservice.common.exception.ErrorCode;
import com.comeon.userservice.config.S3MockConfig;
import com.comeon.userservice.domain.common.exception.EntityNotFoundException;
import com.comeon.userservice.domain.profileimage.entity.ProfileImg;
import com.comeon.userservice.domain.profileimage.repository.ProfileImgRepository;
import com.comeon.userservice.domain.user.entity.OAuthProvider;
import com.comeon.userservice.domain.user.entity.User;
import com.comeon.userservice.domain.user.entity.UserAccount;
import com.comeon.userservice.domain.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@Transactional
@ActiveProfiles("test")
@Import({S3MockConfig.class})
@SpringBootTest
class ProfileImgQueryServiceTest {

    @Autowired
    ProfileImgQueryService profileImgQueryService;

    @Autowired
    ProfileImgRepository profileImgRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EntityManager em;

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
    void initProfileImg() {
        profileImg = profileImgRepository.save(
                ProfileImg.builder()
                        .user(user)
                        .originalName("originalName")
                        .storedName("storedName")
                        .build()
        );
    }

    @Nested
    @DisplayName("?????? ???????????? ????????? ????????? ????????? ?????? ??????")
    class getStoredFileNameByUserId {

        @Test
        @DisplayName("????????? ????????? ????????? ???????????? ?????????, ???????????? storedName??? ????????????.")
        void whenUserHasProfileImg() {
            // given
            initUser();
            initProfileImg();

            // when
            String storedFileName = profileImgQueryService.getStoredFileNameByUserId(user.getId());

            // then
            assertThat(storedFileName).isNotNull();
            assertThat(storedFileName).isEqualTo(profileImg.getStoredName());
        }

        @Test
        @DisplayName("????????? ????????? ????????? ???????????? ?????????, null??? ????????????.")
        void whenUserHasNotProfileImg() {
            // given
            initUser();

            // when
            String storedFileName = profileImgQueryService.getStoredFileNameByUserId(user.getId());

            // then
            assertThat(storedFileName).isNull();
        }

        @Test
        @DisplayName("??????????????? ????????? userId??? ???????????? ????????? ????????? null??? ????????????.")
        void whenNotMatchUser() {
            // given
            Long invalidUserId = 100L;

            // when
            String storedFileName = profileImgQueryService.getStoredFileNameByUserId(invalidUserId);

            // then
            assertThat(storedFileName).isNull();
        }

        @Test
        @DisplayName("????????? ????????? ????????? ???????????? CustomException??? ???????????????. ?????? ?????? ErrorCode ??? ALREADY_WITHDRAW ??????.")
        void whenUserWasWithDrawn() {
            // given
            initUser();
            initProfileImg();
            user.withdrawal();
            em.flush();

            // when
            assertThatThrownBy(
                    () -> profileImgQueryService.getStoredFileNameByUserId(user.getId())
            )
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_WITHDRAW);
        }
    }

    @Nested
    @DisplayName("????????? ????????? ???????????? ?????? ?????? ???????????? ????????? ????????? ????????? ?????? ??????")
    class getStoredFileNameByProfileImgIdAndUserId {

        @Test
        @DisplayName("????????? ???????????? ????????? ????????? ???????????? ??????????????? ????????? userId??? ????????? ?????? ????????? ???????????? storedName??? ????????????.")
        void success() {
            // given
            initUser();
            initProfileImg();

            Long userId = user.getId();
            Long profileImgId = profileImg.getId();

            // when
            String storedFileName = profileImgQueryService.getStoredFileNameByProfileImgIdAndUserId(profileImgId, userId);

            // then
            assertThat(storedFileName).isNotNull();
            assertThat(storedFileName).isEqualTo(profileImg.getStoredName());
        }

        @Test
        @DisplayName("????????? ???????????? ????????? ?????? ???????????? ??????????????? ????????? userId??? ?????????, CustomException ????????????. ErrorCode.No_AUTHORITIES??? ?????????.")
        void failNoAuthorities() {
            // given
            initUser();
            initProfileImg();

            Long InvalidUserId = 100L;
            Long profileImgId = profileImg.getId();

            // when, then
            assertThatThrownBy(
                    () -> profileImgQueryService.getStoredFileNameByProfileImgIdAndUserId(profileImgId, InvalidUserId)
            )
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_AUTHORITIES);
        }

        @Test
        @DisplayName("??????????????? ????????? ????????? ???????????? ???????????? ???????????? ???????????? ????????? EntityNotFoundException ????????????.")
        void failEntityNotFound() {
            // given
            initUser();
            Long userId = user.getId();
            Long invalidProfileImgId = 100L;

            // when, then
            assertThatThrownBy(
                    () -> profileImgQueryService.getStoredFileNameByProfileImgIdAndUserId(invalidProfileImgId, userId)
            )
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("????????? ????????? ????????? ???????????? CustomException??? ???????????????. ?????? ?????? ErrorCode ??? ALREADY_WITHDRAW ??????.")
        void whenUserWasWithdrawn() {
            // given
            initUser();
            initProfileImg();
            user.withdrawal();
            em.flush();

            // when
            assertThatThrownBy(
                    () -> profileImgQueryService.getStoredFileNameByProfileImgIdAndUserId(profileImg.getId(), user.getId())
            )
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_WITHDRAW);
        }
    }

}