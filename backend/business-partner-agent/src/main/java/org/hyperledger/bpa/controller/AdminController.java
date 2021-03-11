/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/business-partner-agent

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package org.hyperledger.bpa.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ledger.TAAInfo.TAARecord;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.RuntimeConfig;
import org.hyperledger.bpa.controller.api.admin.*;
import org.hyperledger.bpa.impl.aries.config.RestrictionsManager;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.impl.mode.indy.EndpointService;
import org.hyperledger.bpa.model.BPARestrictions;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller("/api/admin")
@Tag(name = "Configuration")
@Validated
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class AdminController {

    @Inject
    SchemaService schemaService;

    @Inject
    Optional<EndpointService> endpointService;

    @Inject
    RestrictionsManager restrictionsManager;

    @Inject
    RuntimeConfig config;

    @Inject
    AriesClient ac;

    // TODO
    // endpoint schemas/credential definition id to store a list of ids to each
    // schema

    // proof request allows all restriction that aca-py allows (expert mode)

    //
    // when no ledger explorer configured fall back to not filtering partners in the
    // can issue api
    // can issue endpoint compares configured cred def ids with the partners did if
    // match partner can issue
    // when no cred defs configured show no partners

    // check issuer with /ledger/did-verkey

    /**
     * Aries: List configured schemas
     *
     * @return list of {@link SchemaAPI}
     */
    @Get("/schema")
    public HttpResponse<List<SchemaAPI>> listSchemas() {
        return HttpResponse.ok(schemaService.listSchemas());
    }

    /**
     * Aries: Get a schema configuration
     *
     * @param id {@link UUID} the schema id
     * @return {@link HttpResponse}
     */
    @Get("/schema/{id}")
    public HttpResponse<SchemaAPI> getSchema(@PathVariable UUID id) {
        final Optional<SchemaAPI> schema = schemaService.getSchema(id);
        if (schema.isPresent()) {
            return HttpResponse.ok(schema.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * Aries: Add a schema configuration
     *
     * @param req {@link AddSchemaRequest}
     * @return {@link HttpResponse}
     */
    @Post("/schema")
    public HttpResponse<SchemaAPI> addSchema(@Body AddSchemaRequest req) {
        return HttpResponse.ok(schemaService.addSchema(req.getSchemaId(), req.getLabel(),
                req.getDefaultAttributeName()));
    }

    /**
     * Aries: Update a schema configuration
     *
     * @param id  {@link UUID} the schema id
     * @param req {@link UpdateSchemaRequest}
     * @return {@link HttpResponse}
     */
    @Put("/schema/{id}")
    public HttpResponse<SchemaAPI> updateSchema(@PathVariable UUID id, @Body UpdateSchemaRequest req) {
        Optional<SchemaAPI> schemaAPI = schemaService.updateSchema(id, req.getDefaultAttribute());
        if (schemaAPI.isPresent()) {
            return HttpResponse.ok(schemaAPI.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * Aries: Removes a schema configuration. Doing so means the BPA will not
     * process requests containing this schema id any more.
     *
     * @param id {@link UUID} the schema id
     * @return {@link HttpResponse}
     */
    @Delete("/schema/{id}")
    public HttpResponse<Void> removeSchema(@PathVariable UUID id) {
        Optional<SchemaAPI> schema = schemaService.getSchema(id);
        if (schema.isPresent()) {
            if (!schema.get().getIsReadOnly()) {
                schemaService.deleteSchema(id);
                return HttpResponse.ok();
            }
            return HttpResponse.notAllowed();
        }
        return HttpResponse.notFound();
    }

    /**
     * Aries: Add a restriction configuration to a schema
     * 
     * @param id      {@link UUID} the schema id
     * @param request {@link AddRestrictionRequest}
     * @return {@link RestrictionResponse}
     */
    @Post("/schema/{id}/restriction")
    public HttpResponse<RestrictionResponse> addRestriction(
            @PathVariable UUID id,
            @Body AddRestrictionRequest request) {
        Optional<RestrictionResponse> res = restrictionsManager.addRestriction(
                id, request.getIssuerDid(), request.getLabel());
        if (res.isPresent()) {
            return HttpResponse.ok(res.get());
        }
        throw new WrongApiUsageException("Credential definition could not be added. Check the logs");
    }

    /**
     * Aries: Update a restriction configuration
     * 
     * @param id            {@link UUID} the schema id
     * @param restrictionId {@link UUID} the credential definition configuration id
     * @param request       {@link UpdateCredentialDefinition}
     * @return {@link RestrictionResponse}
     */
    @Put("/schema/{id}/restriction/{restrictionId}")
    public HttpResponse<RestrictionResponse> updateRestriction(
            @SuppressWarnings("unused") @PathVariable UUID id,
            @PathVariable UUID restrictionId,
            @Body UpdateCredentialDefinition request) {
        restrictionsManager.updateLabel(restrictionId, request.getLabel());
        return HttpResponse.ok();
    }

    /**
     * Aries: Delete a restriction configuration
     * 
     * @param id            {@link UUID} the schema id
     * @param restrictionId {@link UUID} the restriction id
     * @return {@link HttpResponse}
     */
    @Delete("/schema/{id}/restriction/{restrictionId}")
    public HttpResponse<Void> deleteRestriction(
            @SuppressWarnings("unused") @PathVariable UUID id,
            @PathVariable UUID restrictionId) {
        Optional<BPARestrictions> config = restrictionsManager.findById(restrictionId);
        if (config.isPresent()) {
            if (!config.get().getIsReadOnly()) {
                restrictionsManager.deleteCredentialDefinition(restrictionId);
                return HttpResponse.ok();
            }
            return HttpResponse.notAllowed();
        }
        return HttpResponse.notFound();
    }

    /**
     * Get runtime configuration
     *
     * @return {@link RuntimeConfig}
     */
    @Get("/config")
    public HttpResponse<RuntimeConfig> getRuntimeConfig() {
        return HttpResponse.ok(config);
    }

    /**
     * Trigger the backend to write configured endpoints to the ledger. TAA digest
     * has to be passed to explicitly confirm prior TTA acceptance by the user for
     * this ledger interaction / session.
     * 
     * @param tAADigest {@link TAADigestRequest}
     * @return {@link HttpResponse}
     */
    @Post("/endpoints/register")
    public HttpResponse<Void> registerEndpoints(@Body TAADigestRequest tAADigest) {
        if (endpointService.isPresent()) {
            endpointService.get().registerEndpoints(tAADigest.getDigest());
            return HttpResponse.ok();
        }
        return HttpResponse.notFound();
    }

    /**
     * @return true if endpoint registration is required
     */
    @Get("/endpoints/registrationRequired")
    public HttpResponse<Boolean> isEndpointsWriteRequired() {
        if (endpointService.isPresent()) {
            return HttpResponse.ok(endpointService.get().getEndpointRegistrationRequired());
        }
        return HttpResponse.ok(Boolean.FALSE); // not writing to the ledger in this case
    }

    /**
     * Get TAA record (digest, text, version)
     * 
     * @return {@link TAARecord}
     */
    @Get("/taa/get")
    public HttpResponse<TAARecord> getTAARecord() {
        if (endpointService.isPresent()) {
            Optional<TAARecord> taa = endpointService.get().getTAA();
            if (taa.isPresent()) {
                return HttpResponse.ok(taa.get());
            }
        }
        return HttpResponse.notFound();
    }
}
