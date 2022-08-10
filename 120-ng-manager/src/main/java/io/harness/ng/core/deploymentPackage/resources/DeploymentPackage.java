/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.deploymentPackage.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pms.yaml.YAMLFieldNameConstants.CONNECTOR_REFS;
import static io.harness.pms.yaml.YAMLFieldNameConstants.PIPELINE_INFRASTRUCTURE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.SPEC;
import static io.harness.pms.yaml.YAMLFieldNameConstants.TEMPLATE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.VARIABLES;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ProjectIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ngexception.NGTemplateException;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.remote.client.NGRestUtils;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.template.beans.PermissionTypes;
import io.harness.template.beans.TemplateResponseDTO;
import io.harness.template.remote.TemplateResourceClient;
import io.harness.utils.YamlPipelineUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NextGenManagerAuth
@Api("/deploymentPackage")
@Path("/deploymentPackage")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Tag(name = "DeploymentPackage", description = "This contains APIs related to Deployment Package")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = NGCommonEntityConstants.BAD_REQUEST_CODE,
    description = NGCommonEntityConstants.BAD_REQUEST_PARAM_MESSAGE,
    content =
    {
      @Content(mediaType = NGCommonEntityConstants.APPLICATION_JSON_MEDIA_TYPE,
          schema = @Schema(implementation = FailureDTO.class))
      ,
          @Content(mediaType = NGCommonEntityConstants.APPLICATION_YAML_MEDIA_TYPE,
              schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = NGCommonEntityConstants.INTERNAL_SERVER_ERROR_CODE,
    description = NGCommonEntityConstants.INTERNAL_SERVER_ERROR_MESSAGE,
    content =
    {
      @Content(mediaType = NGCommonEntityConstants.APPLICATION_JSON_MEDIA_TYPE,
          schema = @Schema(implementation = ErrorDTO.class))
      ,
          @Content(mediaType = NGCommonEntityConstants.APPLICATION_YAML_MEDIA_TYPE,
              schema = @Schema(implementation = ErrorDTO.class))
    })
@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class DeploymentPackage {
  public static final String DEPLOYMENT_PACKAGE_PARAM_MESSAGE = "Deployment Package Identifier for the entity";
  @Inject TemplateResourceClient templateResourceClient;
  private final AccessControlClient accessControlClient;

  @GET
  @Path("/variables/{templateIdentifier}")
  @ApiOperation(value = "Gets Infra variables from a Deployment Package by identifier",
      nickname = "getDeploymentPackageInfraVariables")
  @Operation(operationId = "getDeploymentPackageInfraVariables",
      summary = "Gets a Deployment Package Infra Variables by identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "The saved Infra Variables")
      })
  public ResponseDTO<String>
  getVariables(@Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
                   NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @Parameter(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @Parameter(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @Parameter(description = DEPLOYMENT_PACKAGE_PARAM_MESSAGE) @PathParam(
          "templateIdentifier") @ResourceIdentifier String templateIdentifier,
      @Parameter(description = "Version Label") @QueryParam(
          NGCommonEntityConstants.VERSION_LABEL_KEY) String versionLabel,
      @Parameter(description = "Specifies whether Template is deleted or not") @QueryParam(
          NGCommonEntityConstants.DELETED_KEY) @DefaultValue("false") boolean deleted) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(TEMPLATE, templateIdentifier), PermissionTypes.TEMPLATE_VIEW_PERMISSION);
    log.info(
        String.format("Retrieving Template with identifier %s and versionLabel %s in project %s, org %s, account %s",
            templateIdentifier, versionLabel, projectId, orgId, accountId));
    TemplateResponseDTO response = NGRestUtils.getResponse(
        templateResourceClient.get(templateIdentifier, accountId, orgId, projectId, versionLabel, deleted));
    return ResponseDTO.newResponse(getVariables(response.getYaml()));
  }

  @GET
  @Path("/connectors/{templateIdentifier}")
  @ApiOperation(value = "Gets a Deployment Package by identifier", nickname = "getDeploymentPackage")
  @Operation(operationId = "getDeploymentPackage", summary = "Gets a Deployment Package by identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "The saved Deployment Package")
      })
  public ResponseDTO<String>
  getConnectors(@Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
                    NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @Parameter(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @Parameter(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @Parameter(description = DEPLOYMENT_PACKAGE_PARAM_MESSAGE) @PathParam(
          "templateIdentifier") @ResourceIdentifier String templateIdentifier,
      @Parameter(description = "Version Label") @QueryParam(
          NGCommonEntityConstants.VERSION_LABEL_KEY) String versionLabel,
      @Parameter(description = "Specifies whether Template is deleted or not") @QueryParam(
          NGCommonEntityConstants.DELETED_KEY) @DefaultValue("false") boolean deleted) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(TEMPLATE, templateIdentifier), PermissionTypes.TEMPLATE_VIEW_PERMISSION);
    log.info(
        String.format("Retrieving Template with identifier %s and versionLabel %s in project %s, org %s, account %s",
            templateIdentifier, versionLabel, projectId, orgId, accountId));
    TemplateResponseDTO response = NGRestUtils.getResponse(
        templateResourceClient.get(templateIdentifier, accountId, orgId, projectId, versionLabel, deleted));
    return ResponseDTO.newResponse(getConnectors(response.getYaml()));
  }

  private ObjectNode getInfra(String yaml) {
    try {
      if (isEmpty(yaml)) {
        throw new NGTemplateException("Template yaml to create template inputs cannot be empty");
      }
      YamlField templateYamlField = YamlUtils.readTree(yaml).getNode().getField(TEMPLATE);
      if (templateYamlField == null) {
        log.error("Yaml provided is not a template yaml. Yaml:\n" + yaml);
        throw new NGTemplateException("Yaml provided is not a template yaml.");
      }
      ObjectNode templateNode = (ObjectNode) templateYamlField.getNode().getCurrJsonNode();
      JsonNode templateSpec = templateNode.get(SPEC);
      if (isEmpty(templateSpec.toString())) {
        log.error("Template yaml provided does not have spec in it.");
        throw new NGTemplateException("Template yaml provided does not have spec in it.");
      }
      JsonNode templateInfra = templateSpec.get(PIPELINE_INFRASTRUCTURE);
      if (isEmpty(templateInfra.toString())) {
        log.error("Template yaml provided does not have infrastructure in it.");
        throw new NGTemplateException("Template yaml provided does not have infrastructure in it.");
      }
      return (ObjectNode) templateInfra;
    } catch (IOException e) {
      log.error("Error occurred while fetching template infrastructure " + e);
      throw new NGTemplateException("Error occurred while fetching template infrastructure ", e);
    }
  }

  private String getVariables(String yaml) {
    try {
      ObjectNode templateInfra = getInfra(yaml);
      JsonNode templateVariables = templateInfra.get(VARIABLES);
      if (isEmpty(templateVariables.toString())) {
        log.error("Template yaml provided does not have variables in it.");
        return "Template yaml provided does not have variables in it.";
      }
      return YamlPipelineUtils.writeYamlString(templateVariables);
    } catch (Exception e) {
      log.error("Error occurred while fetching template infrastructure variables " + e);
      return "Error occurred while fetching template infrastructure variables " + e;
    }
  }

  private String getConnectors(String yaml) {
    try {
      ObjectNode templateInfra = getInfra(yaml);
      JsonNode templateConnectors = templateInfra.get(CONNECTOR_REFS);
      if (isEmpty(templateConnectors.toString())) {
        log.error("Template yaml provided does not have infrastructure connectors in it.");
        return "Template yaml provided does not have infrastructure connectors in it.";
      }
      return YamlPipelineUtils.writeYamlString(templateConnectors);
    } catch (NGTemplateException e) {
      log.error("Error occurred while fetching template infrastructure connectors " + e);
      return "Error occurred while fetching template infrastructure connectors " + e;
    }
  }
}
