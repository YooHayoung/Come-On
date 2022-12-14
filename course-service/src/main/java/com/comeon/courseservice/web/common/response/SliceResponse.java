package com.comeon.courseservice.web.common.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;

@Getter
public class SliceResponse<T> {

    private int currentSlice;
    private int sizePerSlice;
    private int numberOfElements;
    private boolean hasPrevious;
    private boolean hasNext;
    private boolean isFirst;
    private boolean isLast;
    private List<T> contents;

    @Builder
    private SliceResponse(int currentSlice, int sizePerSlice, int numberOfElements,
                          boolean hasPrevious, boolean hasNext, boolean isFirst, boolean isLast, List<T> contents) {
        this.currentSlice = currentSlice;
        this.sizePerSlice = sizePerSlice;
        this.numberOfElements = numberOfElements;
        this.hasPrevious = hasPrevious;
        this.hasNext = hasNext;
        this.isFirst = isFirst;
        this.isLast = isLast;
        this.contents = contents;
    }

    public static<T> SliceResponse<T> toSliceResponse(Slice<T> slice) {
        return SliceResponse.<T>builder()
                .currentSlice(slice.getNumber())
                .sizePerSlice(slice.getSize())
                .numberOfElements(slice.getNumberOfElements())
                .hasPrevious(slice.hasPrevious())
                .hasNext(slice.hasNext())
                .isFirst(slice.isFirst())
                .isLast(slice.isLast())
                .contents(slice.getContent())
                .build();
    }
}
