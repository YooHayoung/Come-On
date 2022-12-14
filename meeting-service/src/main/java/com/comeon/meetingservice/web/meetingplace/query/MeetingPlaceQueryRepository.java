package com.comeon.meetingservice.web.meetingplace.query;

import com.comeon.meetingservice.domain.meetingplace.entity.MeetingPlaceEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.comeon.meetingservice.domain.meetingplace.entity.QMeetingPlaceEntity.*;

@Repository
@RequiredArgsConstructor
public class MeetingPlaceQueryRepository {

    public final JPAQueryFactory queryFactory;

    public List<MeetingPlaceEntity> findAllByMeetingId(Long meetingId) {
        return queryFactory
                .selectFrom(meetingPlaceEntity)
                .where(meetingPlaceEntity.meetingEntity.id.eq(meetingId))
                .orderBy(meetingPlaceEntity.order.asc())
                .fetch();
    }

    public Optional<MeetingPlaceEntity> findById(Long meetingId, Long id) {
        return Optional.ofNullable(queryFactory
                .selectFrom(meetingPlaceEntity)
                .where(meetingPlaceEntity.meetingEntity.id.eq(meetingId),
                        meetingPlaceEntity.id.eq(id))
                .fetchOne());
    }
}
