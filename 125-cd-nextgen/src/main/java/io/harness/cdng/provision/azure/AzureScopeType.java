package io.harness.cdng.provision.azure;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AzureResourceGroupSpec.class, name = AzureScopeTypesNames.RESOURCE_GROUP),
        @JsonSubTypes.Type(value = AzureSubscritionSpec.class, name = AzureScopeTypesNames.SUBSCRIPTION),
        @JsonSubTypes.Type(value = AzureManagementSpec.class, name = AzureScopeTypesNames.MANAGEMENT_GROUP),
        @JsonSubTypes.Type(value = AzureTenantSpec.class, name = AzureScopeTypesNames.TENANT)

})
public interface AzureScopeType {
    void validateParams();
}
