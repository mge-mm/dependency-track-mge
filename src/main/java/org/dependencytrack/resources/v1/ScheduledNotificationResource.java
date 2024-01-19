/*
 * This file is part of Dependency-Track.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) Steve Springett. All Rights Reserved.
 */

package org.dependencytrack.resources.v1;

import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.dependencytrack.auth.Permissions;
import org.dependencytrack.model.ScheduledNotificationsInfo;
import org.dependencytrack.persistence.QueryManager;

import alpine.persistence.PaginatedResult;
import alpine.server.auth.PermissionRequired;
import alpine.server.resources.AlpineResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;

@Path("/v1/schedulednotification")
@Api(value = "schedulednotification", authorizations = @Authorization(value = "X-Api-Key"))
public class ScheduledNotificationResource extends AlpineResource {
    
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Returns a list of all scheduled notifications",
        response = ScheduledNotificationsInfo.class,
        responseContainer = "List",
        responseHeaders = @ResponseHeader(name = TOTAL_COUNT_HEADER, response = Long.class, description = "Total number of scheduled notifications")
    )

    @ApiResponses(value = {
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 404, message = "Not found")
    })
    @PermissionRequired(Permissions.Constants.VIEW_PORTFOLIO)
    public Response getAllScheduledNotifications(){
        try (QueryManager qm = new QueryManager(getAlpineRequest())){
            final PaginatedResult result = qm.getScheduledNotificationsPaginatedResult();
            return Response.ok(result.getObjects()).header(TOTAL_COUNT_HEADER, result.getTotal()).build();
        }
    }


    @PUT
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Creates a new scheduled notification entry",
            response = ScheduledNotificationsInfo.class,
            code = 201
    )
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 404, message = "Not found")
    })
    public Response createScheduledNotification(ScheduledNotificationsInfo scheduledNotificationsInfo){
        final Validator validator = super.getValidator();
        failOnValidationError(validator.validateProperty(scheduledNotificationsInfo, "created"));
        failOnValidationError(validator.validateProperty(scheduledNotificationsInfo, "cronString"));
        failOnValidationError(validator.validateProperty(scheduledNotificationsInfo, "nextExecution"));
        failOnValidationError(validator.validateProperty(scheduledNotificationsInfo, "lastExecution"));
        failOnValidationError(validator.validateProperty(scheduledNotificationsInfo, "destinations"));
        failOnValidationError(validator.validateProperty(scheduledNotificationsInfo, "projectId"));
        try (QueryManager qm = new QueryManager(getAlpineRequest())){
            final ScheduledNotificationsInfo result = qm.createScheduledNotificationInfo(scheduledNotificationsInfo);
            if(result != null){
                return Response.status(Response.Status.CREATED).entity(result).build();
            }else{
                return Response.status(Response.Status.NOT_FOUND).entity("The scheduled notification could not be found.").build();
            }
        }
    }


    @DELETE
    @Path("/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "deletes a scheduled notification info entry",
            response = ScheduledNotificationsInfo.class,
            code = 201
    )
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 404, message = "Not found")
    })

    public Response deleteScheduledNotification(
        @ApiParam(value = "The ID of the scheduled notification info to delete", required = true)
        @PathParam("id") long id){
            try(QueryManager qm = new QueryManager(getAlpineRequest())) {
                final ScheduledNotificationsInfo result = qm.getObjectById(ScheduledNotificationsInfo.class, id);
            if(result != null){
                qm.deleteScheduledNotificationInfo(qm.getObjectById(ScheduledNotificationsInfo.class, id));
                return Response.status(Response.Status.NO_CONTENT).entity(result).build();
            }else{
                return Response.status(Response.Status.NOT_FOUND).entity("The scheduled notification could not be found.").build();
            }
            }

    }



    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Updates a scheduled notification",
            response = ScheduledNotificationsInfo.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 404, message = "The UUID of the project could not be found"),
            @ApiResponse(code = 409, message = "- An inactive Parent cannot be selected as parent\n- Project cannot be set to inactive if active children are present\n- A project with the specified name already exists\n- A project cannot select itself as a parent")
    })
    public Response updateScheduledNotification(ScheduledNotificationsInfo scheduledNotificationsInfo){
        final Validator validator = super.getValidator();
        failOnValidationError(validator.validateProperty(scheduledNotificationsInfo, "created"));
        failOnValidationError(validator.validateProperty(scheduledNotificationsInfo, "cronString"));
        failOnValidationError(validator.validateProperty(scheduledNotificationsInfo, "nextExecution"));
        failOnValidationError(validator.validateProperty(scheduledNotificationsInfo, "lastExecution"));
        failOnValidationError(validator.validateProperty(scheduledNotificationsInfo, "destinations"));
        failOnValidationError(validator.validateProperty(scheduledNotificationsInfo, "projectId"));
        try (QueryManager qm = new QueryManager(getAlpineRequest())){
            final ScheduledNotificationsInfo result = qm.updateScheduledNotificationsInfo(scheduledNotificationsInfo);
            if(result != null){
                return Response.ok(result).build();
            }else{
                return Response.status(Response.Status.NOT_FOUND).entity("The scheduled notification could not be found.").build();
            }
        }

    }


}
