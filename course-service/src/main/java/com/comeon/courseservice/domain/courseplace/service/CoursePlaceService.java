package com.comeon.courseservice.domain.courseplace.service;

import com.comeon.courseservice.common.exception.CustomException;
import com.comeon.courseservice.common.exception.ErrorCode;
import com.comeon.courseservice.domain.common.exception.EntityNotFoundException;
import com.comeon.courseservice.domain.course.entity.Course;
import com.comeon.courseservice.domain.course.repository.CourseRepository;
import com.comeon.courseservice.domain.courseplace.entity.CoursePlace;
import com.comeon.courseservice.domain.courseplace.repository.CoursePlaceRepository;
import com.comeon.courseservice.domain.courseplace.service.dto.CoursePlaceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CoursePlaceService {

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;

    public Long coursePlaceAdd(Long courseId, Long userId, CoursePlaceDto coursePlaceDto) {
        Course course = getCourse(courseId);
        checkWriter(userId, course);

        coursePlaceDto.setOrder(course.getCoursePlaces().size() + 1);
        Long coursePlaceId = coursePlaceRepository.save(coursePlaceDto.toEntity(course)).getId();

        course.availableCourse();

        return coursePlaceId;
    }

    public void coursePlaceModify(Long courseId, Long userId, Long coursePlaceId, CoursePlaceDto coursePlaceDto) {
        CoursePlace coursePlace = findCoursePlace(coursePlaceId, courseId, userId);

        if (coursePlaceDto.getDescription() != null) {
            coursePlace.updateDescription(coursePlaceDto.getDescription());
        }
        if (coursePlaceDto.getPlaceCategory() != null) {
            coursePlace.updatePlaceCategory(coursePlaceDto.getPlaceCategory());
        }

        if (coursePlaceDto.getOrder() != null && !Objects.equals(coursePlace.getOrder(), coursePlaceDto.getOrder())) {
            Integer originalOrder = coursePlace.getOrder();
            Integer targetOrder = coursePlaceDto.getOrder();

            CoursePlace targetPlace = coursePlaceRepository.findByCourseIdAndOrder(courseId, targetOrder)
                    .orElseThrow(
                            () -> new CustomException("????????? " + targetOrder + "??? ????????? ????????? ????????????.", ErrorCode.NOT_EXIST_PLACE_ORDER)
                    );

            coursePlace.updateOrder(targetOrder);
            targetPlace.updateOrder(originalOrder);
        }
    }

    public void coursePlaceRemove(Long courseId, Long userId, Long coursePlaceId) {
        CoursePlace coursePlace = findCoursePlace(coursePlaceId, courseId, userId);

        coursePlaceRepository.delete(coursePlace);

        List<CoursePlace> coursePlaces = coursePlaceRepository.findAllByCourseId(courseId);
        decreaseAfterOrder(coursePlaces, coursePlace.getOrder());

        if (coursePlaces.size() == 0) {
            coursePlace.getCourse().disabledCourse();
        }
    }

    private CoursePlace findCoursePlace(Long coursePlaceId, Long courseId, Long userId) {
        CoursePlace coursePlace = coursePlaceRepository.findByIdFetchCourse(coursePlaceId)
                .orElseThrow(
                        () -> new EntityNotFoundException("?????? ????????? ????????????. ????????? ?????? ????????? : " + courseId + ", ????????? ?????? ????????? : " + coursePlaceId)
                );
        checkWriter(userId, coursePlace.getCourse());
        return coursePlace;
    }

    private void decreaseAfterOrder(List<CoursePlace> coursePlaces, Integer deletedOrder) {
        coursePlaces.stream()
                .filter(coursePlace -> coursePlace.getOrder() > deletedOrder)
                .forEach(CoursePlace::decreaseOrder);
    }

    public void batchUpdateCoursePlace(Long courseId, Long userId,
                                       List<CoursePlaceDto> dtosToSave,
                                       List<CoursePlaceDto> dtosToModify,
                                       List<Long> coursePlaceIdsToDelete) {

        Course course = getCourse(courseId);

        checkWriter(userId, course);

        // ??????
        coursePlaceIdsToDelete.forEach(
                coursePlaceId -> course.getCoursePlaces().removeIf(coursePlace -> coursePlace.getId().equals(coursePlaceId))
        );

        // ??????
        dtosToModify.forEach(
                coursePlaceDto -> course.getCoursePlaces().stream()
                        .filter(coursePlace -> coursePlace.getId().equals(coursePlaceDto.getCoursePlaceId()))
                        .findFirst()
                        .ifPresent(coursePlace -> modify(coursePlace, coursePlaceDto))
        );

        // ??????
        dtosToSave.forEach(coursePlaceDto -> coursePlaceDto.toEntity(course));

        // ?????? ??????
        checkPlaceOrders(course);

        course.updateCourseState();
    }


    /* === private method === */
    private Course getCourse(Long courseId) {
        return courseRepository.findByIdFetchCoursePlaces(courseId)
                .orElseThrow(
                        () -> new EntityNotFoundException("?????? ???????????? ?????? Course??? ????????????. ????????? Course ????????? : " + courseId)
                );
    }

    private void checkWriter(Long userId, Course course) {
        if (!Objects.equals(course.getUserId(), userId)) {
            throw new CustomException("?????? ????????? ???????????? ????????????. ????????? ?????? ????????? : " + userId, ErrorCode.NO_AUTHORITIES);
        }
    }

    private void checkPlaceOrders(Course course) {
        List<Integer> orderList = course.getCoursePlaces().stream()
                .map(CoursePlace::getOrder)
                .sorted()
                .distinct()
                .collect(Collectors.toList());

        if (orderList.size() != course.getCoursePlaces().size()) {
            throw new CustomException("????????? ????????? ?????????????????????.", ErrorCode.PLACE_ORDER_DUPLICATE);
        }
        if (!orderList.get(0).equals(1)) {
            throw new CustomException("????????? ????????? 1?????? ???????????? ????????????.", ErrorCode.PLACE_ORDER_NOT_START_ONE);
        }
        for (int i = 0; i < orderList.size() - 1; i++) {
            if (orderList.get(i) + 1 != orderList.get(i + 1)) {
                throw new CustomException("????????? ????????? ???????????? ????????? ????????????.", ErrorCode.PLACE_ORDER_NOT_CONSECUTIVE);
            }
        }
    }

    private void modify(CoursePlace coursePlace, CoursePlaceDto coursePlaceDto) {
        if (Objects.nonNull(coursePlaceDto.getName())) {
            coursePlace.updateName(coursePlaceDto.getName());
        }

        if (Objects.nonNull(coursePlaceDto.getDescription())) {
            coursePlace.updateDescription(coursePlaceDto.getDescription());
        }

        if (Objects.nonNull(coursePlaceDto.getLat())) {
            coursePlace.updateLat(coursePlaceDto.getLat());
        }

        if (Objects.nonNull(coursePlaceDto.getLng())) {
            coursePlace.updateLng(coursePlaceDto.getLng());
        }

        if (Objects.nonNull(coursePlaceDto.getOrder())) {
            coursePlace.updateOrder(coursePlaceDto.getOrder());
        }

        if (Objects.nonNull(coursePlaceDto.getKakaoPlaceId())) {
            coursePlace.updateKakaoPlaceId(coursePlaceDto.getKakaoPlaceId());
        }

        if (Objects.nonNull(coursePlaceDto.getPlaceCategory())) {
            coursePlace.updatePlaceCategory(coursePlaceDto.getPlaceCategory());
        }
    }
}
