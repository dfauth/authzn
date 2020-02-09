package com.github.dfauth.scrub;

import com.github.dfauth.scrub.rfq.CreateNegotiationEvent;
import com.github.dfauth.scrub.rfq.RfqVisibilityModel;
import com.github.dfauth.scrub.uievents.NegotiationUIEvent;
import org.testng.annotations.Test;

import java.util.stream.Stream;

import static com.github.dfauth.scrub.AssertionUtils.assertOptional;
import static org.testng.Assert.*;

public class RfqVisibilityModelTestCase {

    private Company originator = new CompanyImpl("originator");
    private Company broker = new CompanyImpl("broker");
    private Company tc = new CompanyImpl("tc");

    // the model is immutable
    private RfqVisibilityModel<NegotiationUIEvent> model = new CreateNegotiationEvent(originator, broker, tc, 10, 100, "instrumentId");

    private UserContext<UserModelImpl> originatorUserCtx = new UserContextImpl("blahX0jkghfkbigfuckofftokenXwejJiuergydklhdklh", new UserModelImpl("fred", originator));
    private UserContext<UserModelImpl> brokerUserCtx = new UserContextImpl("blahX0jkghfkbigfuckofftokenXwejJiuergydklhdklh", new UserModelImpl("fred", broker));
    private UserContext<UserModelImpl> tcUserCtx = new UserContextImpl("blahX0jkghfkbigfuckofftokenXwejJiuergydklhdklh", new UserModelImpl("fred", tc));
    private UserContext<UserModelImpl> outsiderUserCtx = new UserContextImpl("blahX0jkghfkbigfuckofftokenXwejJiuergydklhdklh", new UserModelImpl("fred", new CompanyImpl("ABC")));

    @Test
    public void testFiltering() {
        assertNotNull(model);
        assertEquals(model.getOriginator().payload(), originator);
        assertFalse(model.getOriginator().parent().isPresent());
        assertTrue(model.getOriginator().child().isPresent());
        assertOptional(model.getOriginator().child(), c -> assertEquals(c,model.getBroker()));

        assertEquals(model.getBroker().payload(), broker);
        assertTrue(model.getBroker().parent().isPresent());
        assertTrue(model.getBroker().child().isPresent());
        assertOptional(model.getBroker().parent(), c -> assertEquals(c,model.getOriginator()));
        assertOptional(model.getBroker().child(), c -> assertEquals(c,model.getTradingCompany()));

        assertEquals(model.getTradingCompany().payload(), tc);
        assertTrue(model.getTradingCompany().parent().isPresent());
        assertOptional(model.getTradingCompany().parent(), c -> assertEquals(c,model.getBroker()));
        assertFalse(model.getTradingCompany().child().isPresent());

        // any participant can see the model
        Stream.of(new Company[]{originator, broker, tc}).forEach(p -> {
            assertTrue(model.isVisibleTo(() -> p), "model is not visible to "+p);
        });
        // but it is not visible to anyone else
        assertFalse(model.isVisibleTo(() -> () -> "blah"));
    }

    @Test
    public void testScrubbing() {
        assertTrue(model.getOriginator().isVisibleTo(() -> originator));
        assertTrue(model.getOriginator().isVisibleTo(() -> broker));
        assertFalse(model.getOriginator().isVisibleTo(() -> tc));
        assertFalse(model.getOriginator().isVisibleTo(() -> () -> "blah"));

        assertTrue(model.getBroker().isVisibleTo(() -> originator));
        assertTrue(model.getBroker().isVisibleTo(() -> broker));
        assertTrue(model.getBroker().isVisibleTo(() -> tc));
        assertFalse(model.getBroker().isVisibleTo(() -> () -> "blah"));

        assertFalse(model.getTradingCompany().isVisibleTo(() -> originator));
        assertTrue(model.getTradingCompany().isVisibleTo(() -> broker));
        assertTrue(model.getTradingCompany().isVisibleTo(() -> tc));
        assertFalse(model.getTradingCompany().isVisibleTo(() -> () -> "blah"));
    }

    @Test
    public void testRender() {
        // outsiders cannot see the model at all
        {
            UserModelImpl userModel = outsiderUserCtx.payload();
            assertFalse(model.isVisibleTo(userModel));
            // and cannot see any sensisitve fields
            NegotiationUIEvent result = model.render(userModel);
            assertNull(result.getOriginator());
            assertNull(result.getBroker());
            assertNull(result.getTradingCompany());
            assertNull(result.getInstrument());
            assertNull(result.getPrice());
            assertNull(result.getVolume());
        }
        // but an originator can see the model
        {
            UserModelImpl userModel = originatorUserCtx.payload();
            assertTrue(model.isVisibleTo(userModel));
            // but the fields he can see may be limited
            NegotiationUIEvent result = model.render(userModel);
            assertNotNull(result.getOriginator());
            assertNotNull(result.getBroker());
            assertNull(result.getTradingCompany());
            assertNotNull(result.getInstrument());
            assertNotNull(result.getPrice());
            assertNotNull(result.getVolume());
        }
        // a broker can see the model
        {
            UserModelImpl userModel = brokerUserCtx.payload();
            assertTrue(model.isVisibleTo(userModel));
            // and all the fields
            NegotiationUIEvent result = model.render(userModel);
            assertNotNull(result.getOriginator());
            assertNotNull(result.getBroker());
            assertNotNull(result.getTradingCompany());
            assertNotNull(result.getInstrument());
            assertNotNull(result.getPrice());
            assertNotNull(result.getVolume());
        }
        // and a trading company can see the model
        {
            UserModelImpl userModel = tcUserCtx.payload();
            assertTrue(model.isVisibleTo(userModel));
            // but the fields he can see may be limited
            NegotiationUIEvent result = model.render(userModel);
            assertNull(result.getOriginator());
            assertNotNull(result.getBroker());
            assertNotNull(result.getTradingCompany());
            assertNotNull(result.getInstrument());
            assertNotNull(result.getPrice());
            assertNotNull(result.getVolume());
        }
    }

}
