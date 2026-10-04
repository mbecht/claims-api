package com.mbecht.claims_api.dto;

import java.util.List;

/**
 * Stable paging envelope for list endpoints. Deliberately not Spring Data's Page,
 * whose JSON shape is an internal detail of the framework, not a client contract.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
