package com.backend.cookshare.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse <T>{
    List<T> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
    boolean first;
    boolean last;
    boolean empty;

    @Builder.Default
    int numberOfElements = 0;

    @Builder.Default
    boolean sorted = false;
}
