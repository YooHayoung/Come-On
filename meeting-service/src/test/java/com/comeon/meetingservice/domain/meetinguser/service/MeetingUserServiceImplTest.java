package com.comeon.meetingservice.domain.meetinguser.service;

import com.comeon.meetingservice.common.exception.CustomException;
import com.comeon.meetingservice.common.exception.ErrorCode;
import com.comeon.meetingservice.domain.meetingcode.entity.MeetingCodeEntity;
import com.comeon.meetingservice.domain.meeting.entity.MeetingEntity;
import com.comeon.meetingservice.domain.meeting.entity.MeetingFileEntity;
import com.comeon.meetingservice.domain.meeting.entity.MeetingRole;
import com.comeon.meetingservice.domain.meetinguser.dto.MeetingUserAddDto;
import com.comeon.meetingservice.domain.meetinguser.dto.MeetingUserModifyDto;
import com.comeon.meetingservice.domain.meetinguser.entity.MeetingUserEntity;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
class MeetingUserServiceImplTest {

    @Autowired
    MeetingUserService meetingUserService;

    @Autowired
    EntityManager em;

    @Nested
    @DisplayName("모임 유저 저장 (add)")
    class 모임유저생성 {

        String sampleCode = "AAABBB";
        MeetingEntity meetingEntity;

        @Nested
        @DisplayName("정상 흐름일 경우")
        class 정상흐름 {

            @BeforeEach
            public void initMeeting() {
                MeetingCodeEntity meetingCodeEntity = MeetingCodeEntity.builder()
                        .inviteCode(sampleCode)
                        .expiredDay(7)
                        .build();

                MeetingFileEntity meetingFileEntity = MeetingFileEntity.builder()
                        .originalName("ori")
                        .storedName("sto")
                        .build();

                meetingEntity = MeetingEntity.builder()
                        .title("title")
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusDays(7))
                        .build();

                meetingEntity.addMeetingFileEntity(meetingFileEntity);
                meetingEntity.addMeetingCodeEntity(meetingCodeEntity);

                em.persist(meetingEntity);
                em.flush();
                em.clear();
            }

            private MeetingUserEntity callAddMethodAndFind(MeetingUserAddDto meetingUserAddDto) {
                Long savedId = meetingUserService.add(meetingUserAddDto);
                em.flush();
                em.clear();

                MeetingUserEntity savedEntity = em.find(MeetingUserEntity.class, savedId);
                return savedEntity;
            }

            @Test
            @DisplayName("유저 정보(userId, nickname, imageLink)가 정상적으로 저장된다.")
            public void 모임유저엔티티_유저정보() throws Exception {
                // given
                MeetingUserAddDto meetingUserAddDto = MeetingUserAddDto.builder()
                        .userId(1L)
                        .inviteCode(sampleCode)
                        .build();

                // when
                MeetingUserEntity savedEntity = callAddMethodAndFind(meetingUserAddDto);
                // then
                assertThat(savedEntity.getUserId()).isEqualTo(meetingUserAddDto.getUserId());
            }

            @Test
            @DisplayName("모임 역할은 PARTICIPANT로 저장된다.")
            public void 모임유저엔티티_모임역할() throws Exception {
                // given
                MeetingUserAddDto meetingUserAddDto = MeetingUserAddDto.builder()
                        .userId(1L)
                        .inviteCode(sampleCode)
                        .build();

                // when
                MeetingUserEntity savedEntity = callAddMethodAndFind(meetingUserAddDto);

                // then
                assertThat(savedEntity.getMeetingRole()).isEqualTo(MeetingRole.PARTICIPANT);
            }

            @Test
            @DisplayName("해당 초대코드를 가진 모임의 ID를 저장한다.")
            public void 모임유저엔티티_모임() throws Exception {
                // given
                MeetingUserAddDto meetingUserAddDto = MeetingUserAddDto.builder()
                        .userId(1L)
                        .inviteCode(sampleCode)
                        .build();

                // when
                MeetingUserEntity savedEntity = callAddMethodAndFind(meetingUserAddDto);
                MeetingEntity meetingIncludingCode = em.createQuery(
                                "select m from MeetingEntity m " +
                                        "join m.meetingCodeEntity " +
                                        "where m.meetingCodeEntity.inviteCode = :inviteCode", MeetingEntity.class)
                        .setParameter("inviteCode", sampleCode)
                        .getSingleResult();

                // then
                assertThat(savedEntity.getMeetingEntity().getId()).isEqualTo(meetingIncludingCode.getId());
            }
        }

        @Nested
        @DisplayName("예외가 발생하는 경우")
        class 예외 {

            @Test
            @DisplayName("유효기간이 지난 코드라면 예외가 발생한다.")
            public void 유효기간예외() throws Exception {
                // given
                MeetingCodeEntity meetingCodeEntity = MeetingCodeEntity.builder()
                        .inviteCode(sampleCode)
                        .expiredDay(-1)
                        .build();

                MeetingFileEntity meetingFileEntity = MeetingFileEntity.builder()
                        .originalName("ori")
                        .storedName("sto")
                        .build();

                meetingEntity = MeetingEntity.builder()
                        .title("title")
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusDays(7))
                        .build();

                meetingEntity.addMeetingFileEntity(meetingFileEntity);
                meetingEntity.addMeetingCodeEntity(meetingCodeEntity);

                em.persist(meetingEntity);
                em.flush();
                em.clear();

                MeetingUserAddDto meetingUserAddDto = MeetingUserAddDto.builder()
                        .userId(1L)
                        .inviteCode(sampleCode)
                        .build();

                // when then
                assertThatThrownBy(() -> meetingUserService.add(meetingUserAddDto))
                        .isInstanceOf(CustomException.class)
                        .hasMessage("해당 초대코드는 만료되었습니다.");
            }

            @Test
            @DisplayName("해당 초대코드를 가진 모임이 없다면 예외가 발생한다.")
            public void 없는초대코드() throws Exception {
                // given
                MeetingUserAddDto meetingUserAddDto = MeetingUserAddDto.builder()
                        .userId(1L)
                        .inviteCode(sampleCode)
                        .build();

                // when then
                assertThatThrownBy(() -> meetingUserService.add(meetingUserAddDto))
                        .isInstanceOf(CustomException.class)
                        .hasMessage("해당 초대코드를 가진 모임이 없습니다.");
            }

            @Test
            @DisplayName("이미 가입한 회원이라면 예외가 발생한다.")
            public void 이미가입된회원() throws Exception {
                // given
                MeetingCodeEntity meetingCodeEntity = MeetingCodeEntity.builder()
                        .inviteCode(sampleCode)
                        .expiredDay(7)
                        .build();

                MeetingFileEntity meetingFileEntity = MeetingFileEntity.builder()
                        .originalName("ori")
                        .storedName("sto")
                        .build();

                meetingEntity = MeetingEntity.builder()
                        .title("title")
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusDays(7))
                        .build();

                meetingEntity.addMeetingFileEntity(meetingFileEntity);
                meetingEntity.addMeetingCodeEntity(meetingCodeEntity);

                em.persist(meetingEntity);
                em.flush();
                em.clear();

                MeetingUserAddDto meetingUserAddDto = MeetingUserAddDto.builder()
                        .userId(1L)
                        .inviteCode(sampleCode)
                        .build();

                meetingUserService.add(meetingUserAddDto);
                em.flush();
                em.clear();

                // when then
                assertThatThrownBy(() -> meetingUserService.add(meetingUserAddDto))
                        .isInstanceOf(CustomException.class)
                        .hasMessage("이미 모임에 가입된 회원입니다.");
            }
        }
    }

    @Nested
    @DisplayName("모임 유저 수정 (modify)")
    class 모임유저수정 {

        MeetingEntity meetingEntity;
        MeetingUserEntity meetingUserEntity;

        @BeforeEach
        public void initMeeting() {
            MeetingCodeEntity meetingCodeEntity = MeetingCodeEntity.builder()
                    .inviteCode("code")
                    .expiredDay(7)
                    .build();

            MeetingFileEntity meetingFileEntity = MeetingFileEntity.builder()
                    .originalName("ori")
                    .storedName("sto")
                    .build();

            meetingEntity = MeetingEntity.builder()
                    .title("title")
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusDays(7))
                    .build();

            meetingEntity.addMeetingFileEntity(meetingFileEntity);
            meetingEntity.addMeetingCodeEntity(meetingCodeEntity);

            em.persist(meetingEntity);
            em.flush();
            em.clear();
        }

        @Nested
        @DisplayName("정상 흐름일 경우")
        class 정상흐름 {

            @Test
            @DisplayName("역할이 정상적으로 변경된다.")
            public void 역할변경() throws Exception {
                // given
                MeetingRole originalRole = MeetingRole.PARTICIPANT;

                MeetingUserEntity meetingUserEntity = MeetingUserEntity.builder()
                        .userId(1L)
                        .meetingRole(originalRole)
                        .build();
                meetingUserEntity.addMeetingEntity(meetingEntity);

                em.persist(meetingUserEntity);
                em.flush();
                em.clear();

                MeetingRole modifiedRole = MeetingRole.EDITOR;
                MeetingUserModifyDto meetingUserModifyDto = MeetingUserModifyDto.builder()
                        .meetingRole(modifiedRole)
                        .meetingId(meetingEntity.getId())
                        .id(meetingUserEntity.getId())
                        .build();
                // when

                meetingUserService.modify(meetingUserModifyDto);
                em.flush();
                em.clear();

                MeetingUserEntity modified = em.find(MeetingUserEntity.class, meetingUserEntity.getId());

                // then
                assertThat(modified.getMeetingRole()).isEqualTo(modifiedRole);
            }
        }

        @Nested
        @DisplayName("예외가 발생하는 경우")
        class 예외 {

            @Test
            @DisplayName("HOST로 변경할 경우 MODIFY_HOST_NOT_SUPPORT이 발생한다.")
            public void HOST변경_예외() throws Exception {
                // given
                MeetingRole originalRole = MeetingRole.PARTICIPANT;

                MeetingUserEntity meetingUserEntity = MeetingUserEntity.builder()
                        .userId(1L)
                        .meetingRole(originalRole)
                        .build();
                meetingUserEntity.addMeetingEntity(meetingEntity);

                em.persist(meetingUserEntity);
                em.flush();
                em.clear();

                MeetingRole modifiedRole = MeetingRole.HOST;
                MeetingUserModifyDto meetingUserModifyDto = MeetingUserModifyDto.builder()
                        .meetingRole(modifiedRole)
                        .id(meetingUserEntity.getId())
                        .meetingId(meetingEntity.getId())
                        .build();

                // when then
                assertThatThrownBy(() -> meetingUserService.modify(meetingUserModifyDto))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MODIFY_HOST_NOT_SUPPORT);
            }

            @Test
            @DisplayName("회원이 HOST인 경우 변경할 수 없기에 MODIFY_HOST_IMPOSSIBLE이 발생한다.")
            public void 회원HOST_예외() throws Exception {
                // given
                MeetingRole originalRole = MeetingRole.HOST;

                MeetingUserEntity meetingUserEntity = MeetingUserEntity.builder()
                        .userId(1L)
                        .meetingRole(originalRole)
                        .build();
                meetingUserEntity.addMeetingEntity(meetingEntity);

                em.persist(meetingUserEntity);
                em.flush();
                em.clear();

                MeetingRole modifiedRole = MeetingRole.PARTICIPANT;
                MeetingUserModifyDto meetingUserModifyDto = MeetingUserModifyDto.builder()
                        .meetingRole(modifiedRole)
                        .meetingId(meetingEntity.getId())
                        .id(meetingUserEntity.getId())
                        .build();

                // when then
                assertThatThrownBy(() -> meetingUserService.modify(meetingUserModifyDto))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MODIFY_HOST_IMPOSSIBLE);
            }

            @Test
            @DisplayName("변경하려는 회원이 해당 모임에 가입되지 않았다면 ENTITY_NOT_FOUND가 발생한다.")
            public void 회원미가입_예외() throws Exception {
                // given
                MeetingUserModifyDto meetingUserModifyDto = MeetingUserModifyDto.builder()
                        .meetingRole(MeetingRole.PARTICIPANT)
                        .meetingId(meetingEntity.getId())
                        .id(100L)
                        .build();

                // when then
                assertThatThrownBy(() -> meetingUserService.modify(meetingUserModifyDto))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENTITY_NOT_FOUND);
            }
        }
    }
}