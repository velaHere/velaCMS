package com.vela.velaCMS.dto.response;

import java.util.List;

public record PageResponse<O>(
        List<O> content,
        int page,
        int limit,
        boolean hasNext
) {
}
