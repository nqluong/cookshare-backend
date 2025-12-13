package com.backend.cookshare.common.mapper;

import com.backend.cookshare.common.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PageMapperTest {

    private PageMapper pageMapper;

    @BeforeEach
    void setUp() {
        pageMapper = new PageMapper() {}; // Implementation tạm thời
    }

    @Test
    void testPageMapperInstance() {
        PageMapper mapper = Mappers.getMapper(PageMapper.class);

        assertNotNull(mapper);
        assertTrue(mapper instanceof PageMapper);
    }
    @Test
    void toPageResponse_WithMapper_ShouldMapContent() {
        // Mock Page
        Page<String> page = mock(Page.class);
        when(page.getContent()).thenReturn(List.of("a", "b"));
        when(page.getNumber()).thenReturn(0);
        when(page.getSize()).thenReturn(2);
        when(page.getTotalElements()).thenReturn(4L);
        when(page.getTotalPages()).thenReturn(2);
        when(page.isFirst()).thenReturn(true);
        when(page.isLast()).thenReturn(false);
        when(page.isEmpty()).thenReturn(false);
        when(page.getNumberOfElements()).thenReturn(2);
        when(page.getSort()).thenReturn(Sort.unsorted());

        // Mapper function
        Function<String, Integer> mapper = String::length;

        PageResponse<Integer> response = pageMapper.toPageResponse(page, mapper);

        assertNotNull(response);
        assertEquals(List.of(1,1), response.getContent());
        assertEquals(0, response.getPage());
        assertEquals(2, response.getSize());
        assertEquals(4L, response.getTotalElements());
        assertEquals(2, response.getTotalPages());
        assertTrue(response.isFirst());
        assertFalse(response.isLast());
        assertFalse(response.isEmpty());
        assertEquals(2, response.getNumberOfElements());
        assertFalse(response.isSorted());
    }

    @Test
    void toPageResponse_WithoutMapper_ShouldCopyContent() {
        Page<String> page = mock(Page.class);
        when(page.getContent()).thenReturn(List.of("x", "y"));
        when(page.getNumber()).thenReturn(1);
        when(page.getSize()).thenReturn(2);
        when(page.getTotalElements()).thenReturn(4L);
        when(page.getTotalPages()).thenReturn(2);
        when(page.isFirst()).thenReturn(false);
        when(page.isLast()).thenReturn(true);
        when(page.isEmpty()).thenReturn(false);
        when(page.getNumberOfElements()).thenReturn(2);
        when(page.getSort()).thenReturn(Sort.unsorted());

        PageResponse<String> response = pageMapper.toPageResponse(page);

        assertNotNull(response);
        assertEquals(List.of("x", "y"), response.getContent());
        assertEquals(1, response.getPage());
        assertEquals(2, response.getSize());
        assertEquals(4L, response.getTotalElements());
        assertEquals(2, response.getTotalPages());
        assertFalse(response.isFirst());
        assertTrue(response.isLast());
        assertFalse(response.isEmpty());
        assertEquals(2, response.getNumberOfElements());
    }

    @Test
    void toPageResponse_WithContentList_ShouldUseContentSize() {
        Page<String> page = mock(Page.class);
        when(page.getNumber()).thenReturn(0);
        when(page.getSize()).thenReturn(10);
        when(page.getTotalElements()).thenReturn(50L);
        when(page.getTotalPages()).thenReturn(5);
        when(page.isFirst()).thenReturn(true);
        when(page.isLast()).thenReturn(false);
        when(page.isEmpty()).thenReturn(false);

        List<Integer> content = List.of(1, 2, 3);

        PageResponse<Integer> response = pageMapper.toPageResponse(content, page);

        assertNotNull(response);
        assertEquals(content, response.getContent());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(50L, response.getTotalElements());
        assertEquals(5, response.getTotalPages());
        assertTrue(response.isFirst());
        assertFalse(response.isLast());
        assertEquals(3, response.getNumberOfElements());
    }
}
