package io.harness.cdng.provision.azure;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotations.dev.OwnedBy;


import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;
import static io.harness.annotations.dev.HarnessTeam.CDP;

@OwnedBy(CDP)
@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AzureInlineTemplateFileSpec.class, name = AzureCreateTemplateFileTypes.Inline),
        @JsonSubTypes.Type(value = AzureRemoteTemplateFileSpec.class, name = AzureCreateTemplateFileTypes.Remote),
})
public interface AzureCreateTemplateFileSpec {
    String getType();
}
