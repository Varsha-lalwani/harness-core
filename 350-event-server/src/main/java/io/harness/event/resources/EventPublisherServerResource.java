/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.event.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.dropwizard.jersey.protobuf.ProtocolBufferMediaType;
import io.grpc.Context;
import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.PublishRequest;
import io.harness.event.PublishResponse;
import io.harness.event.service.intfc.EventPublisherService;
import io.harness.security.annotations.PublicApi;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.grpc.IdentifierKeys.DELEGATE_ID;
import static io.harness.grpc.auth.DelegateAuthServerInterceptor.ACCOUNT_ID_CTX_KEY;
import static java.util.Objects.requireNonNull;

@Api("/event-publisher-server")
@Path("/event-publisher-server")
@Consumes(ProtocolBufferMediaType.APPLICATION_PROTOBUF)
@Produces(ProtocolBufferMediaType.APPLICATION_PROTOBUF)
@PublicApi
@Slf4j
@ExposeInternalException
@OwnedBy(CE)
public class EventPublisherServerResource {
  private final EventPublisherService eventPublisherService;

  @Inject
  public EventPublisherServerResource(EventPublisherService eventPublisherService) {
    this.eventPublisherService = eventPublisherService;
  }

  @GET
  @Path("/publish")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "publish", nickname = "publish")
  public Response publish(@RequestBody(description = "Publish Request") PublishRequest request) {
    log.info("Received publish request with {} messages", request.getMessagesCount());
    String accountId = requireNonNull(ACCOUNT_ID_CTX_KEY.get(Context.current()));
    String delegateId = request.getMessages(0).getAttributesMap().getOrDefault(DELEGATE_ID, "");
    try {
      eventPublisherService.publish(accountId, delegateId, request.getMessagesList(), request.getMessagesCount());
      return Response.ok(PublishResponse.newBuilder().build()).build();
    } catch (Exception e) {
      log.error("Exception in Event Publisher Service", e);
      return Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(PublishResponse.newBuilder().build())
              .build();
    }
  }
}
