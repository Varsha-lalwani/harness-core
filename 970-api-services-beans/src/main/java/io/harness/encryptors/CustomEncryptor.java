/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.encryptors;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataParams;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;

import java.util.Set;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
public interface CustomEncryptor {
  // Params should have resolved semi resolved Script and expressionFunctorToken.
  boolean validateReference(
      @NotEmpty String accountId, @NotNull Set<EncryptedDataParams> params, @NotNull EncryptionConfig encryptionConfig);

  // EncrypedRecord parameter should have semi resolved Script and expression functor token.
  char[] fetchSecretValue(
      @NotEmpty String accountId, @NotNull EncryptedRecord encryptedRecord, @NotNull EncryptionConfig encryptionConfig);

  /*default boolean validateReference(String accountId, String script,
                                    @NotNull Set<EncryptedDataParams> params, @NotNull EncryptionConfig
  encryptionConfig) { throw new UnsupportedOperationException("Can not validate the secret");
  }*/

  default boolean validateCustomConfiguration(String accountId, EncryptionConfig encryptionConfig) {
    throw new UnsupportedOperationException(
        "Validating SecretManager Configuration on Delegate in not available yet for:" + encryptionConfig);
  }

  /*
  default char[] fetchSecretValue(
          @NotEmpty String accountId, String script, @NotNull EncryptedRecord encryptedRecord, @NotNull EncryptionConfig
  encryptionConfig){ throw new UnsupportedOperationException( "Fetching secret in not available yet for:" +
  encryptionConfig);
  }*/
}
