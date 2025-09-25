package com.ubs.tariffapp.models.dto;

import java.util.List;

public record TariffOptionsResponse(
    List<TariffSearchResult> options,
    boolean fallbackUsed,
    String fallbackReason
) {
}
