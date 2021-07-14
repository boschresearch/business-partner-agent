/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hyperledger.bpa.model.prooftemplate2;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.micronaut.core.annotation.Introspected;
import lombok.*;
import org.hyperledger.bpa.controller.api.prooftemplates.AttributeGroup;
import org.hyperledger.bpa.impl.verification.prooftemplates.DistinctAttributeNames;
import org.hyperledger.bpa.impl.verification.prooftemplates.ValidAttributeCondition;
import org.hyperledger.bpa.impl.verification.prooftemplates.ValidAttributeGroup;
import org.hyperledger.bpa.impl.verification.prooftemplates.ValidBPASchemaId;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Introspected
@ValidAttributeGroup
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
// using a concrete class instead of a generic list does not unmarshal correctly
// see https://github.com/micronaut-projects/micronaut-data/issues/1064
public class BPAAttributeGroup {
    @ValidBPASchemaId
    String schemaId;
    @NotNull
    @Singular
    @Valid
    @DistinctAttributeNames
    List<BPAAttribute> attributes;

    @NotNull
    @Singular
    @Valid
    @ValidAttributeCondition
    List<BPACondition> schemaLevelConditions;

    public AttributeGroup toRepresentation() {
        return new AttributeGroup(
                schemaId,
                attributes.stream()
                        .map(BPAAttribute::toRepresentation)
                        .collect(Collectors.toList()),
                schemaLevelConditions.stream()
                        .map(BPACondition::toRepresentation)
                        .collect(Collectors.toList()));
    }

    public static BPAAttributeGroup fromRepresentation(AttributeGroup attributeGroup) {
        return new BPAAttributeGroup(
                attributeGroup.getSchemaId(),
                attributeGroup.getAttributes().stream()
                        .map(BPAAttribute::fromRepresentation)
                        .collect(Collectors.toList()),
                attributeGroup.getSchemaLevelConditions().stream()
                        .map(BPACondition::fromRepresentation)
                        .collect(Collectors.toList()));
    }
}