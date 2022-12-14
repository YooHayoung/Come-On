package com.comeon.meetingservice.web;

import com.comeon.meetingservice.domain.meeting.entity.MeetingRole;
import com.comeon.meetingservice.domain.meeting.service.MeetingService;
import com.comeon.meetingservice.domain.meetingcode.service.MeetingCodeService;
import com.comeon.meetingservice.domain.meetingdate.service.MeetingDateService;
import com.comeon.meetingservice.domain.meetingplace.service.MeetingPlaceService;
import com.comeon.meetingservice.domain.meetinguser.entity.MeetingUserEntity;
import com.comeon.meetingservice.domain.meetinguser.service.MeetingUserService;
import com.comeon.meetingservice.web.common.aop.ValidationAspect;
import com.comeon.meetingservice.web.common.feign.courseservice.CourseFeignService;
import com.comeon.meetingservice.web.common.util.TokenUtils;
import com.comeon.meetingservice.web.common.util.ValidationUtils;
import com.comeon.meetingservice.web.common.util.fileutils.FileManager;
import com.comeon.meetingservice.web.meeting.MeetingController;
import com.comeon.meetingservice.web.meeting.query.MeetingQueryService;
import com.comeon.meetingservice.web.meetingcode.MeetingCodeController;
import com.comeon.meetingservice.web.meetingcode.query.MeetingCodeQueryService;
import com.comeon.meetingservice.web.meetingdate.MeetingDateController;
import com.comeon.meetingservice.web.meetingdate.query.MeetingDateQueryService;
import com.comeon.meetingservice.web.meetingplace.MeetingPlaceController;
import com.comeon.meetingservice.web.meetingplace.query.MeetingPlaceQueryService;
import com.comeon.meetingservice.web.meetingplace.request.PlaceModifyRequestValidator;
import com.comeon.meetingservice.web.meetinguser.MeetingUserController;
import com.comeon.meetingservice.web.meetinguser.query.MeetingUserQueryRepository;
import com.comeon.meetingservice.web.meetinguser.query.MeetingUserQueryService;
import com.comeon.meetingservice.web.restdocs.docscontroller.DocsController;
import com.comeon.meetingservice.web.s3.S3MockConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.BDDMockito.given;

@WebMvcTest({
        MeetingController.class,
        MeetingPlaceController.class,
        MeetingUserController.class,
        MeetingDateController.class,
        MeetingCodeController.class,
        DocsController.class
})
@Import({AopAutoConfiguration.class,
        ValidationAspect.class,
        ValidationUtils.class,
        PlaceModifyRequestValidator.class,
        S3MockConfig.class
})
@AutoConfigureRestDocs
@ActiveProfiles("test")
@MockBean(JpaMetamodelMappingContext.class)
public abstract class ControllerTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String errorCodeLink = "link:popup/error-codes.html[?????? ?????? ??????,role=\"popup\"]";
    protected String categoryLink = "link:popup/place-categories.html[???????????? ??????,role=\"popup\"]";

    protected String createJson(Object dto) throws JsonProcessingException {
        return objectMapper.writeValueAsString(dto);
    }

    // ===== TOKENUTILS MOCKING ===== //

    MockedStatic<TokenUtils> tokenUtilsMock;

    protected String createToken(Long userId) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusSeconds(3600);

        String secret = "secretKeyValueForMeetingControllerTestCaseSecretKeyValueForMeetingControllerTestCase";
        String token = "Bearer " + Jwts.builder()
                .setSubject(String.valueOf(userId))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                .claim("auth", "ROLE_USER")
                .setIssuer("come-on")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .compact();

        // MockMvc ????????? ??? TokenUtils??? ????????? ???????????? ?????? static method ?????????
        // ArgumentResolver, Interceptor ?????? ?????? ???????????? ???????????? ????????? ??????.
        given(TokenUtils.getUserId(token)).willReturn(userId);

        return token;
    }

    // static method ???????????? ?????? ?????? (????????? ????????? ?????? ???????????? ???),
    // ?????? ?????? -> ?????? ???(?????? ???)?????? ?????? ????????? ????????? ???????????? ?????? ????????? ???????????? ???????????? ?????????
    @BeforeEach
    protected void initTokenUtils() {
        tokenUtilsMock = Mockito.mockStatic(TokenUtils.class);
    }

    @AfterEach
    protected void closeTokenUtils() {
        tokenUtilsMock.close();
    }

    // ===== INTERCEPTOR MOCKING ===== //

    @MockBean
    MeetingUserQueryRepository meetingUserQueryRepository;

    // ?????? ???????????? ??????????????? ???????????? ???
    protected Long mockedExistentMeetingId;
    protected Long mockedNonexistentMeetingId;
    protected Long mockedHostUserId;
    protected Long mockedEditorUserId;
    protected Long mockedParticipantUserId;

    // Interceptor?????? meetingUserQueryRepository ?????? ??? ?????? ?????? ?????? ???????????? ???????????? ?????? ??????????????? ?????????
    @BeforeEach
    protected void mockingMeetingUserQueryRepository() {

        mockedHostUserId = 1000L;
        mockedEditorUserId = 2000L;
        mockedParticipantUserId = 3000L;

        List<MeetingUserEntity> meetingUserEntities = new ArrayList<>();

        MeetingUserEntity hostUser = MeetingUserEntity.builder()
                .userId(mockedHostUserId)
                .meetingRole(MeetingRole.HOST)
                .build();

        MeetingUserEntity editorUser = MeetingUserEntity.builder()
                .userId(mockedEditorUserId)
                .meetingRole(MeetingRole.EDITOR)
                .build();

        MeetingUserEntity participantUser = MeetingUserEntity.builder()
                .userId(mockedParticipantUserId)
                .meetingRole(MeetingRole.PARTICIPANT)
                .build();

        meetingUserEntities.add(hostUser);
        meetingUserEntities.add(editorUser);
        meetingUserEntities.add(participantUser);

        // ???????????? ??????????????? ?????? ???????????? ??????, ???????????? ??? ????????? ??????
        // ?????????????????? ????????? ??????????????? ???????????? ??? ??????, ????????? ????????? ??????????????? ??????, ????????? HOST?????? ??????????????? ??????
        mockedExistentMeetingId = 1000L;
        mockedNonexistentMeetingId = 2000L;
        given(meetingUserQueryRepository.findAllByMeetingId(mockedExistentMeetingId)).willReturn(meetingUserEntities);
        given(meetingUserQueryRepository.findAllByMeetingId(mockedNonexistentMeetingId)).willReturn(new ArrayList<>());
    }

    // === Meeting Controller === //
    @MockBean
    protected MeetingService meetingService;

    @MockBean
    protected MeetingQueryService meetingQueryService;

    @MockBean
    protected CourseFeignService courseFeignService;

    @MockBean
    protected FileManager fileManager;

    // === Meeting Code Controller === //
    @MockBean
    protected MeetingCodeService meetingCodeService;

    @MockBean
    protected MeetingCodeQueryService meetingCodeQueryService;

    // === Meeting Date Controller === //
    @MockBean
    protected MeetingDateService meetingDateService;

    @MockBean
    protected MeetingDateQueryService meetingDateQueryService;

    // === Meeting Date Controller === //
    @MockBean
    protected MeetingPlaceService meetingPlaceService;

    @MockBean
    protected MeetingPlaceQueryService meetingPlaceQueryService;

    // === Meeting User Controller === //
    @MockBean
    protected MeetingUserService meetingUserService;

    @MockBean
    protected MeetingUserQueryService meetingUserQueryService;

}

