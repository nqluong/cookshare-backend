package com.backend.cookshare.interaction.entity.mapper;

import com.backend.cookshare.interaction.entity.SearchHistory;
import com.backend.cookshare.interaction.entity.dto.response.SearchHistoryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SearchHistoryMapper {
    SearchHistoryResponse toSearchHistoryResponse (SearchHistory searchHistory);
}
