/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.roles.api;

import static io.harness.accesscontrol.AccessControlPermissions.EDIT_ROLE_PERMISSION;
import static io.harness.accesscontrol.AccessControlResourceTypes.ROLE;
import static io.harness.accesscontrol.roles.api.RoleDTOMapper.fromDTO;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.springdata.TransactionUtils.DEFAULT_TRANSACTION_RETRY_POLICY;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.roles.events.RoleCreateEvent;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeLevel;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.outbox.api.OutboxService;
import io.harness.spec.server.accesscontrol.AccountRolesApi;
import io.harness.spec.server.accesscontrol.model.CreateRoleRequest;
import io.harness.spec.server.accesscontrol.model.RolesResponse;
import io.harness.spec.server.accesscontrol.model.Sorting;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.transaction.support.TransactionTemplate;

@ValidateOnExecution
@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class AccountRolesApiImpl implements AccountRolesApi {
  private final RoleService roleService;
  private final ScopeService scopeService;
  private final RoleDTOMapper roleDTOMapper;
  private final TransactionTemplate transactionTemplate;
  private final OutboxService outboxService;
  private final AccessControlClient accessControlClient;

  private final RetryPolicy<Object> transactionRetryPolicy = DEFAULT_TRANSACTION_RETRY_POLICY;

  @Inject
  public AccountRolesApiImpl(RoleService roleService, ScopeService scopeService, RoleDTOMapper roleDTOMapper,
      @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate, OutboxService outboxService,
      AccessControlClient accessControlClient) {
    this.roleService = roleService;
    this.scopeService = scopeService;
    this.roleDTOMapper = roleDTOMapper;
    this.transactionTemplate = transactionTemplate;
    this.outboxService = outboxService;
    this.accessControlClient = accessControlClient;
  }

  @Override
  public Response createRoleAcc(CreateRoleRequest body, @AccountIdentifier String account) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(account, null, null), Resource.of(ROLE, null), EDIT_ROLE_PERMISSION);
    Scope scope =
        scopeService.getOrCreate(Scope.builder().instanceId(account).level(HarnessScopeLevel.ACCOUNT).build());
    RoleDTO roleDTO = RoleApiMapper.getRoleDTO(body);
    roleDTO.setAllowedScopeLevels(Sets.newHashSet(scope.getLevel().toString()));

    RolesResponse response = Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      RoleResponseDTO responseDTO = roleDTOMapper.toResponseDTO(roleService.create(fromDTO(scope.toString(), roleDTO)));
      outboxService.save(new RoleCreateEvent(account, responseDTO.getRole(), responseDTO.getScope()));
      return RoleApiMapper.getRolesResponse(ResponseDTO.newResponse(responseDTO));
    }));
    return Response.ok().entity(response).build();
  }

  @Override
  public Response deleteRoleAcc(String role, String account) {
    return null;
  }

  @Override
  public Response getRoleAcc(String role, String account) {
    return null;
  }

  @Override
  public Response listRolesAcc(
      String account, Integer page, Integer limit, String searchTerm, List<Sorting> sortOrders) {
    return null;
  }

  @Override
  public Response updateRoleAcc(String role, CreateRoleRequest body, String account) {
    return null;
  }
}
