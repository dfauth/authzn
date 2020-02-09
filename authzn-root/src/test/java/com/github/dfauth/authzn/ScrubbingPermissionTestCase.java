package com.github.dfauth.authzn;

import com.github.dfauth.scrub.rfq.CreateNegotiationEvent;
import com.github.dfauth.scrub.rfq.RfqVisibilityModel;
import com.github.dfauth.scrub.uievents.NegotiationUIEvent;
import org.testng.annotations.Test;

import java.util.Collections;

import static com.github.dfauth.authzn.Assertions.assertAllowed;
import static com.github.dfauth.authzn.Assertions.assertDenied;
import static com.github.dfauth.authzn.PrincipalType.ROLE;
import static com.github.dfauth.authzn.Role.role;

public class ScrubbingPermissionTestCase extends FlintstonesTestScenario {

    private Company originator = new CompanyImpl("originator");
    private Company broker = new CompanyImpl("broker");
    private Company tc = new CompanyImpl("tc");

    private UserContext<UserModelImpl> originatorUserCtx = new UserContextImpl("blahX0jkghfkbigfuckofftokenXwejJiuergydklhdklh", new UserModelImpl("fred", originator, Collections.singleton(role("trader"))));
    private UserContext<UserModelImpl> brokerUserCtx = new UserContextImpl("blahX0jkghfkbigfuckofftokenXwejJiuergydklhdklh", new UserModelImpl("fred", broker, Collections.singleton(role("trader"))));
    private UserContext<UserModelImpl> tcUserCtx = new UserContextImpl("blahX0jkghfkbigfuckofftokenXwejJiuergydklhdklh", new UserModelImpl("fred", tc, Collections.singleton(role("trader"))));
    private UserContext<UserModelImpl> outsiderUserCtx = new UserContextImpl("blahX0jkghfkbigfuckofftokenXwejJiuergydklhdklh", new UserModelImpl("george", new CompanyImpl("blah"), Collections.singleton(role("trader"))));

    // the model is immutable
    private RfqVisibilityModel<NegotiationUIEvent> model = new CreateNegotiationEvent(originator, broker, tc, 10, 100, "instrumentId");

    @Test
    public void testIt() {

        // only traders can see RFQ messages
        Directive d = new Directive(ROLE.of("trader"));
        AuthorizationPolicy policy = new AuthorizationPolicyImpl(d);

        {
            UserContext<UserModelImpl> userCtx = originatorUserCtx;
            UserModel u = userCtx.payload();
            Subject subject = userCtx.getSubject();

            assertAllowed(policy.permit(subject, new TestUtils.RolePermission()));
            assertAllowed(policy.permit(subject, new VisibilityModelPermission(model, u.company())));
        }

        {
            UserContext<UserModelImpl> userCtx = brokerUserCtx;
            UserModel u = userCtx.payload();
            Subject subject = userCtx.getSubject();

            assertAllowed(policy.permit(subject, new TestUtils.RolePermission()));
            assertAllowed(policy.permit(subject, new VisibilityModelPermission(model, u.company())));
        }

        {
            UserContext<UserModelImpl> userCtx = tcUserCtx;
            UserModel u = userCtx.payload();
            Subject subject = userCtx.getSubject();

            assertAllowed(policy.permit(subject, new TestUtils.RolePermission()));
            assertAllowed(policy.permit(subject, new VisibilityModelPermission(model, u.company())));
        }

        {
            UserContext<UserModelImpl> userCtx = outsiderUserCtx;
            UserModel u = userCtx.payload();
            Subject subject = userCtx.getSubject();

            assertAllowed(policy.permit(subject, new TestUtils.RolePermission()));
            assertDenied(policy.permit(subject, new VisibilityModelPermission(model, u.company())));
        }
    }
}
