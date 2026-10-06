package com.virgoai.pipeline.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FdicResponse(Meta meta, List<Row> data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(int total) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Row(Institution data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Institution(
            @JsonProperty("CERT") Long cert,
            @JsonProperty("NAME") String name,
            @JsonProperty("CITY") String city,
            @JsonProperty("STALP") String state,
            @JsonProperty("ASSET") BigDecimal assets,
            @JsonProperty("DEP") BigDecimal deposits,
            @JsonProperty("OFFICES") Integer offices,
            @JsonProperty("ESTYMD") String established,
            @JsonProperty("DATEUPDT") String dateUpdated) {
    }
}
