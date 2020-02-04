package com.github.dfauth.authzn;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.dfauth.authzn.Assertions.*;
import static com.github.dfauth.authzn.PrincipalType.ROLE;
import static com.github.dfauth.authzn.PrincipalType.USER;

public class CompanyPermissionTest {

    @Test
    public void testCompanyRole() {

        Principal user = ROLE.of("user");
        Principal admin = ROLE.of("admin");
        Principal accountant = ROLE.of("accountant");

        ImmutablePrincipal fred = USER.of("fred");
        ImmutablePrincipal barney = USER.of("barney");
        ImmutablePrincipal wilma = USER.of("wilma");
        ImmutablePrincipal betty = USER.of("betty");
        ImmutablePrincipal pebbles = USER.of("pebbles");
        ImmutablePrincipal bambam = USER.of("bambam");
        ImmutableSubject fredSubject = ImmutableSubject.of(fred,user, accountant);
        ImmutableSubject barneySubject = ImmutableSubject.of(barney,user, accountant);
        ImmutableSubject wilmaSubject = ImmutableSubject.of(wilma,user, admin);
        ImmutableSubject bettySubject = ImmutableSubject.of(betty,user, admin);
        ImmutableSubject pebblesSubject = ImmutableSubject.of(pebbles,user);
        ImmutableSubject bambamSubject = ImmutableSubject.of(bambam,user);

        // company resources
        ResourcePath bedrockResources = new ResourcePath("/companies/bedrockResources");
        ResourcePath flintstoneFiresticks = new ResourcePath("/companies/flintstoneFiresticks");

        // account resources
        ResourcePath accounts = new ResourcePath("/accounts");
        ResourcePath a123456 = new ResourcePath("/accounts/bedrockResources/a123456");
        ResourcePath b98765 = new ResourcePath("/accounts/flintstoneFiresticks/b98765");


        // directives
        List<Directive> directives = new ArrayList();

        // fred and wilma and pebbles work for bedrockResources
        directives.add(new Directive(fred, bedrockResources));
        directives.add(new Directive(wilma, bedrockResources));
        directives.add(new Directive(pebbles, bedrockResources));

        // barney and betty and bambam work for flintstoneFiresticks
        directives.add(new Directive(barney, flintstoneFiresticks));
        directives.add(new Directive(betty, flintstoneFiresticks));
        directives.add(new Directive(bambam, flintstoneFiresticks));

        // any admin working for a company can view its accounts accounts
        directives.add(new Directive(admin, accounts, ActionSet.from(AccountAction.READ)));

        // any accountant working for a company can read and write and close its accounts
        directives.add(new Directive(accountant, accounts, ActionSet.from(AccountAction.WRITE, AccountAction.READ, AccountAction.CLOSE)));

        // permissions
        Permission bedrockCompany = new CompanyPermission("bedrockResources");
        Permission flintstoneCompany = new CompanyPermission("flintstoneFiresticks");
        Permission readAccounts = new AccountPermission(accounts, AccountAction.READ);
        Permission writeAccounts = new AccountPermission(accounts, AccountAction.WRITE);


        AuthorizationPolicy policy = new AuthorizationPolicyImpl(directives);


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

    static class CompanyPermission extends Permission {

        public CompanyPermission(String company) {
            super(new ResourcePath("/companies/"+company));
        }

    }

    static class Account {
        private String accountId;
        private BigDecimal balance;

        Account(String accountId, BigDecimal balance) {
            this.accountId = accountId;
            this.balance = balance;
        }
    }

    static class AccountPermission extends Permission {

        private final Optional<Account> account;

        public AccountPermission(ResourcePath path, AccountAction action) {
            super(path, action);
            account = Optional.empty();
        }

        public AccountPermission(Account account, AccountAction action) {
            super(new ResourcePath("/accounts/"+account.accountId), action);
            this.account = Optional.of(account);
        }

        @Override
        public boolean allows(AuthorizationDecision decision) {
            // cant close an account with non-zero balance
            return account.map(a -> !AccountPermission.this.action.equals(Optional.of(AccountAction.CLOSE)) || a.balance.equals(BigDecimal.ZERO)).orElse(true);
        }
    }

    enum AccountAction implements Action {
        READ, WRITE, CLOSE;

        @Override
        public boolean implies(Action action) {
            if(action instanceof AccountAction) {
                AccountAction accountAction = ((AccountAction) action);
                return ordinal() >= accountAction.ordinal();
            }
            return action == this;
        }
    }
}
