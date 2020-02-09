package com.github.dfauth.authzn;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static com.github.dfauth.authzn.Assertions.*;

public class CompanyPermissionTest extends FlintstonesTestScenario {

    @BeforeTest
    public void setUp() {
        accountingDirectiveSetUp();
    }

    @Test
    public void testCompanyRole() {

        AuthorizationPolicy policy = new AuthorizationPolicyImpl(accountingDirectives);

        assertAllowed(policy.permit(fredSubject, bedrockCompany)); // fred works for bedrockResources
        assertDenied(policy.permit(fredSubject, flintstoneCompany)); // fred does not work for flintstoneFiresticks

        assertAllowed(policy.permit(fredSubject, readAccounts)); // fred is an accountant, can read accounts
        assertAllowed(policy.permit(fredSubject, writeAccounts)); // fred is an accountant, can write accounts
        assertAllowed(policy.permit(wilmaSubject, readAccounts)); // wilma is an admin, can read accounts
        assertDenied(policy.permit(wilmaSubject, writeAccounts)); // wilma is an admin, cannot write accounts
        assertDenied(policy.permit(pebblesSubject, readAccounts)); // pebbles is neither an accountant nor and admin
        assertDenied(policy.permit(pebblesSubject, writeAccounts)); // pebbles is neither an accountant nor and admin

        assertAllowed(policy.permit(fredSubject, readAccounts, bedrockCompany)); // fred is an accountant, can read accounts on his own company
        assertAllowed(policy.permit(fredSubject, writeAccounts, bedrockCompany)); // fred is an accountant, can write accounts on his own company
        assertAllowed(policy.permit(barneySubject, readAccounts, flintstoneCompany)); // barney is an accountant, can read accounts on his own company
        assertAllowed(policy.permit(barneySubject, writeAccounts, flintstoneCompany)); // barney is an accountant, can write accounts on his own company

        assertDenied(policy.permit(fredSubject, readAccounts, flintstoneCompany)); // fred is an accountant, but he cannot read accounts on another company
        assertDenied(policy.permit(fredSubject, writeAccounts, flintstoneCompany)); // fred is an accountant, but he cannot write accounts on another company
        assertDenied(policy.permit(barneySubject, readAccounts, bedrockCompany)); // barney is an accountant, but he cannot read accounts on another company
        assertDenied(policy.permit(barneySubject, writeAccounts, bedrockCompany)); // barney is an accountant, but he cannot write accounts on another company

        {
            // custom logic implemented in allows() method of Permission object
            // eg. cant close an account with non-zero balance
            Account account = new Account("a123456", BigDecimal.TEN);
            try {
                policy.permit(fredSubject, new AccountPermission(account, AccountAction.CLOSE)).run(() -> {
                    fail("shouldnt run");
                    return null;
                });
            } catch (SecurityException e) {
                // expected
                logger.error(e.getMessage(), e);
            }
        }

        // try again with a zero balance
        {
            // custom logic implemented in allows() method of Permission object
            // eg. can close an account with zero balance
            Account account = new Account("a123456", BigDecimal.ZERO);
            try {
                policy.permit(fredSubject, new AccountPermission(account, AccountAction.CLOSE)).run(() -> {
                    // expected
                    return null;
                });
            } catch (SecurityException e) {
                logger.error(e.getMessage(), e);
                Assert.fail("failed:"+e.getMessage());
            }
        }

        {
            // custom logic implemented in allows() method of Permission object
            // eg. but yuo can read an account with non-zero balance
            Account account = new Account("a123456", BigDecimal.TEN);
            try {
                policy.permit(fredSubject, new AccountPermission(account, AccountAction.READ)).run(() -> {
                    // expected
                    return null;
                });
            } catch (SecurityException e) {
                // expected
                logger.error(e.getMessage(), e);
                Assert.fail("failed:"+e.getMessage());
            }
        }
    }
}
