package com.github.dfauth.authzn;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.dfauth.authzn.PrincipalType.ROLE;
import static com.github.dfauth.authzn.PrincipalType.USER;

public class FlintstonesTestScenario {

    Principal user = ROLE.of("user");
    Principal admin = ROLE.of("admin");
    Principal accountant = ROLE.of("accountant");
    Principal trader = ROLE.of("trader");

    ImmutablePrincipal fred = USER.of("fred");
    ImmutablePrincipal barney = USER.of("barney");
    ImmutablePrincipal wilma = USER.of("wilma");
    ImmutablePrincipal betty = USER.of("betty");
    ImmutablePrincipal pebbles = USER.of("pebbles");
    ImmutablePrincipal bambam = USER.of("bambam");
    ImmutableSubject fredSubject = ImmutableSubject.of(fred,user, accountant, trader);
    ImmutableSubject barneySubject = ImmutableSubject.of(barney,user, accountant, trader);
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

    // permissions
    Permission bedrockCompany = new CompanyPermission("bedrockResources");
    Permission flintstoneCompany = new CompanyPermission("flintstoneFiresticks");
    Permission readAccounts = new AccountPermission(accounts, AccountAction.READ);
    Permission writeAccounts = new AccountPermission(accounts, AccountAction.WRITE);

    // accounting directives
    // directives
    List<Directive> accountingDirectives = new ArrayList();

    protected void accountingDirectiveSetUp() {

        // fred and wilma and pebbles work for bedrockResources
        accountingDirectives.add(new Directive(fred, bedrockResources));
        accountingDirectives.add(new Directive(wilma, bedrockResources));
        accountingDirectives.add(new Directive(pebbles, bedrockResources));

        // barney and betty and bambam work for flintstoneFiresticks
        accountingDirectives.add(new Directive(barney, flintstoneFiresticks));
        accountingDirectives.add(new Directive(betty, flintstoneFiresticks));
        accountingDirectives.add(new Directive(bambam, flintstoneFiresticks));

        // any admin working for a company can view its accounts accounts
        accountingDirectives.add(new Directive(admin, accounts, ActionSet.from(AccountAction.READ)));

        // any accountant working for a company can read and write and close its accounts
        accountingDirectives.add(new Directive(accountant, accounts, ActionSet.from(AccountAction.WRITE, AccountAction.READ, AccountAction.CLOSE)));
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
