package com.github.dfauth.authzn.kafka;

public class SampleResponse {

    private String payload;

    public SampleResponse(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }
}
