package com.careprofileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderSearchResponse {
    private List<ProviderProfileResponse> providers;
    private Integer totalResults;
    private Integer page;
    private Integer size;
    private Integer totalPages;
}
