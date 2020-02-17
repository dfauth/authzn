package com.github.dfauth.authzn.kafka;

public class SampleRequest {

    private String payload;

    public SampleRequest(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }
}
