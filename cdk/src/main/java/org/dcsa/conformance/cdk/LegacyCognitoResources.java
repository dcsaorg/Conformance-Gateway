package org.dcsa.conformance.cdk;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.cognito.AccountRecovery;
import software.amazon.awscdk.services.cognito.AuthFlow;
import software.amazon.awscdk.services.cognito.PasswordPolicy;
import software.amazon.awscdk.services.cognito.SignInAliases;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClientOptions;
import software.constructs.Construct;

/** Migration inventory only. These resources are not connected to application authentication. */
final class LegacyCognitoResources {

  private LegacyCognitoResources() {}

  static void retain(Construct scope, String prefix) {
    // Keep the original scope and IDs: wrapping these in a new Construct would change logical IDs.
    var pool = UserPool.Builder.create(scope, prefix + "UserPool")
            .userPoolName(prefix + "UserPool")
            .selfSignUpEnabled(false)
            .signInAliases(SignInAliases.builder().email(true).build())
            .accountRecovery(AccountRecovery.EMAIL_ONLY)
            .passwordPolicy(
                PasswordPolicy.builder()
                    .minLength(8)
                    .requireLowercase(true)
                    .requireUppercase(true)
                    .requireDigits(true)
                    .requireSymbols(false)
                    .build())
            .build();

    var client = pool.addClient(
            prefix + "UserPoolClient",
            UserPoolClientOptions.builder()
                .authFlows(AuthFlow.builder().userSrp(true).build())
                .preventUserExistenceErrors(true)
                .build());

    pool.applyRemovalPolicy(RemovalPolicy.RETAIN);
    client.applyRemovalPolicy(RemovalPolicy.RETAIN);
  }
}
