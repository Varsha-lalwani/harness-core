package io.harness.cdng.infra.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.pdcconnector.HostFilterType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@OwnedBy(CDP)
@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
public interface Filter {
  HostFilterType getType();
}
