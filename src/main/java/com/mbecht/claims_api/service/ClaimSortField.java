package com.mbecht.claims_api.service;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The only fields a client may sort claims by. Each maps its public name (used in the
 * ?sort= parameter) to the entity property it sorts on, so the database column is
 * never exposed directly.
 */
public enum ClaimSortField {

    SUBMITTED_AT("submittedAt", "createdAt"),
    AMOUNT("amount", "amount"),
    INCIDENT_DATE("incidentDate", "incidentDate");

    private final String apiName;
    private final String entityProperty;

    ClaimSortField(String apiName, String entityProperty) {
        this.apiName = apiName;
        this.entityProperty = entityProperty;
    }

    public String entityProperty() {
        return entityProperty;
    }

    public static Optional<ClaimSortField> fromApiName(String apiName) {
        return Arrays.stream(values())
                .filter(field -> field.apiName.equals(apiName))
                .findFirst();
    }

    public static String allowedNames() {
        return Arrays.stream(values())
                .map(field -> field.apiName)
                .collect(Collectors.joining(", "));
    }
}
