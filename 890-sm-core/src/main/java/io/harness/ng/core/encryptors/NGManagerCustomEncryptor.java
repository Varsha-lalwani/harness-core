/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.encryptors;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegatetasks.ValidateCustomSecretManagerSecretRefereneTaskParameters;
import io.harness.delegatetasks.ValidateSecretManagerConfigurationTaskParameters;
import io.harness.encryptors.CustomEncryptor;
import io.harness.security.encryption.EncryptedDataParams;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@OwnedBy(PL)
@Singleton
public class NGManagerCustomEncryptor implements CustomEncryptor {
  private final DelegateGrpcClientWrapper delegateService;
  private final NGManagerEncryptorHelper ngManagerEncryptorHelper;

  @Inject
  public NGManagerCustomEncryptor(
      DelegateGrpcClientWrapper delegateService, NGManagerEncryptorHelper ngManagerEncryptorHelper) {
    this.delegateService = delegateService;
    this.ngManagerEncryptorHelper = ngManagerEncryptorHelper;
  }

  @Override
  public boolean validateReference(
      String accountId, Set<EncryptedDataParams> params, EncryptionConfig encryptionConfig) {
    String script = getParameter("Script", params);
    return validateReference(accountId, script, params, encryptionConfig);
  }

  //@Override
  public boolean validateReference(
      String accountId, String script, Set<EncryptedDataParams> params, EncryptionConfig encryptionConfig) {
    ValidateCustomSecretManagerSecretRefereneTaskParameters parameters =
        ValidateCustomSecretManagerSecretRefereneTaskParameters.builder()
            .encryptedRecord(EncryptedRecordData.builder().parameters(params).build())
            .encryptionConfig(encryptionConfig)
            .script(script)
            .build();
    int expressionFunctorToken = Integer.parseInt(getParameter("expressionFunctorToken", params));
    return ngManagerEncryptorHelper.validateCustomSecretManagerSecretReference(
        accountId, expressionFunctorToken, parameters);
  }

  @Override
  public char[] fetchSecretValue(String accountId, EncryptedRecord encryptedRecord, EncryptionConfig encryptionConfig) {
    // get script out of encrypted record. This has all resolved yaml expressions and secret is in form of CG Secret
    // expression
    String script = getParameter("Script", encryptedRecord);
    int expressionFunctorToken = Integer.parseInt(getParameter("expressionFunctorToken", encryptedRecord));
    return ngManagerEncryptorHelper.fetchSecretValue(
        accountId, script, expressionFunctorToken, encryptedRecord, encryptionConfig);
  }

  /*//@Override
  public char[] fetchSecretValue(String accountId, String script, EncryptedRecord encryptedRecord, EncryptionConfig
  encryptionConfig) { return ngManagerEncryptorHelper.fetchSecretValue(accountId, script, encryptedRecord,
  encryptionConfig);
  }*/

  @Override
  public boolean validateCustomConfiguration(String accountId, EncryptionConfig encryptionConfig) {
    ValidateSecretManagerConfigurationTaskParameters parameters =
        ValidateSecretManagerConfigurationTaskParameters.builder().encryptionConfig(encryptionConfig).build();
    return ngManagerEncryptorHelper.validateConfiguration(accountId, parameters);
  }

  public String getParameter(String parameterName, EncryptedRecord encryptedRecord) {
    return getParameter(parameterName, encryptedRecord.getParameters());
  }

  public String getParameter(String parameterName, Set<EncryptedDataParams> encryptedDataParamsSet) {
    return encryptedDataParamsSet.stream().filter(x -> x.getName().equals(parameterName)).findFirst().get().getValue();
  }
}
