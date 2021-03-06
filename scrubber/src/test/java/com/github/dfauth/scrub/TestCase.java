package com.github.dfauth.scrub;

import com.github.dfauth.authzn.*;
import com.github.dfauth.scrub.rfq.CreateNegotiationEvent;
import com.github.dfauth.scrub.uievents.NegotiationUIEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.testng.Assert.assertNotNull;

public class TestCase {

    private static final Logger logger = LoggerFactory.getLogger(TestCase.class);

    private AuthenticationContext<UserModelImpl> ctx = new AuthenticationContextImpl("blahX0jkghfkbigfuckofftokenXwejJiuergydklhdklh", new UserModelImpl("fred", new CompanyImpl("ABC"), Collections.singleton(new Role("default","user"))));
    private Company originator = new CompanyImpl("originator");
    private Company broker = new CompanyImpl("broker");
    private Company tradingCompany = new CompanyImpl("tradingCompany");

    @Test
    public void testIt() {
        CreateNegotiationEvent event = new CreateNegotiationEvent(originator, broker, tradingCompany, 10, 100, "instrumentId");
        NegotiationUIEvent result = new NegotiationScrubberBuilder(event).build(ctx.payload().company()).scrub(new CreateNegotiationEvent(originator, broker, tradingCompany, 1000, 10, "instrument"));
        assertNotNull(result);
    }

}
