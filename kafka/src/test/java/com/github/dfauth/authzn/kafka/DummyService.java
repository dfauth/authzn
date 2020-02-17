package com.github.dfauth.authzn.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyService {

    private static final Logger logger = LoggerFactory.getLogger(DummyService.class);

    public SampleResponse serviceCall(SampleRequest request) {
        return new SampleResponse("you said "+request);
    }
}
