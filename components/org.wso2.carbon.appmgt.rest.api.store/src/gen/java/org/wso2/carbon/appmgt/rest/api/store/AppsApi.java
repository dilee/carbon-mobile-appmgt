package org.wso2.carbon.appmgt.rest.api.store;

import io.swagger.annotations.ApiParam;
import org.wso2.carbon.appmgt.rest.api.store.dto.AppDTO;
import org.wso2.carbon.appmgt.rest.api.store.dto.AppListDTO;
import org.wso2.carbon.appmgt.rest.api.store.dto.AppRatingInfoDTO;
import org.wso2.carbon.appmgt.rest.api.store.dto.AppRatingListDTO;
import org.wso2.carbon.appmgt.rest.api.store.dto.EventsDTO;
import org.wso2.carbon.appmgt.rest.api.store.dto.InstallDTO;
import org.wso2.carbon.appmgt.rest.api.store.dto.ScheduleDTO;
import org.wso2.carbon.appmgt.rest.api.store.dto.UserIdListDTO;
import org.wso2.carbon.appmgt.rest.api.store.factories.AppsApiServiceFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.File;

@Path("/apps")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@io.swagger.annotations.Api(value = "/apps", description = "the apps API")
public class AppsApi  {

   private final AppsApiService delegate = AppsApiServiceFactory.getAppsApi();

    @POST
    @Path("/download")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Download App", notes = "Download an new App", response = Void.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK."),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request.\nInvalid request or validation error."),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden.\nThe request must be conditional but no condition has been specified."),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found.\nResource to be deleted does not exist.") })

    public Response appsDownloadPost(@ApiParam(value = "Media type of the entity in the body. Default is JSON." ,required=true , defaultValue="JSON")@HeaderParam("Content-Type") String contentType,
    @ApiParam(value = ""  ) InstallDTO install)
    {
    return delegate.appsDownloadPost(contentType,install);
    }

    @POST
    @Path("/mobile/id/{appId}/download")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Subscribe to app and get app url", notes = "Download a new App ", response = void.class, tags={ "Apps",  })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "OK. ", response = void.class),
            @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request. Invalid request or validation error. ", response = void.class),
            @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden. The request must be conditional but no condition has been specified. ", response = void.class),
            @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found. Resource to be deleted does not exist. ", response = void.class) })
    public Response appsMobileIdAppIdDownloadPost(
            @ApiParam(value = "**APP ID** consisting of the **UUID** of the App. The following combination is also accepted as a valid APP ID: the provider of the app, name of the App and the version. Should be formatted as **provider-name-version**. ",required=true) @PathParam("appId") String appId,
            @ApiParam(value = "Media type of the entity in the body. Default is JSON. " ,required=true, defaultValue="JSON")@HeaderParam("Content-Type") String contentType){
        return delegate.appsMobileIdAppIdDownloadPost(appId,contentType);
    }

    @POST
    @Path("/event-publish")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Publish Stats", notes = "Publish User hit Stats details.", response = Void.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 202, message = "Accepted."),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request.\nInvalid request or validation error."),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden.\nThe request must be conditional but no condition has been specified."),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found.\nResource to be deleted does not exist.") })

    public Response appsEventPublishPost(@ApiParam(value = "" ,required=true ) EventsDTO events,
    @ApiParam(value = "Media type of the entity in the body. Default is JSON." ,required=true , defaultValue="JSON")@HeaderParam("Content-Type") String contentType)
    {
    return delegate.appsEventPublishPost(events,contentType);
    }
    @GET
    @Path("/mobile/binaries/{fileName}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Retrieving mobile application binaries", notes = "Retrieving .apk, .ipa binaries for mobile apps.", response = File.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK.\nMobile app binary content retrieved successfully."),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request.\nInvalid request or validation error"),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found.\nRequested entity does not exist."),
        
        @io.swagger.annotations.ApiResponse(code = 412, message = "Precondition Failed.\nThe request has not been performed because one of the preconditions is not met.") })

    public Response appsMobileBinariesFileNameGet(@ApiParam(value = "file name.",required=true ) @PathParam("fileName") String fileName,
    @ApiParam(value = "Validator for conditional requests; based on ETag."  )@HeaderParam("If-Match") String ifMatch,
    @ApiParam(value = "Validator for conditional requests; based on Last Modified header."  )@HeaderParam("If-Unmodified-Since") String ifUnmodifiedSince)
    {
    return delegate.appsMobileBinariesFileNameGet(fileName,ifMatch,ifUnmodifiedSince);
    }

    @GET
    @Path("/mobile/plist/{appId}/{uuid}")
    @Consumes({"application/json"})
    @Produces({"text/xml"})
    @io.swagger.annotations.ApiOperation(value = "Retrieving plist for ios app installation", notes = "Retrieving plist for ios mobile apps. ", response = File.class, tags = {"MobileApps",})
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "OK. IOS Mobile app plist content retrieved successfully. ", response = File.class),
            @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request. Invalid request or validation error. ", response = File.class),
            @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found. Requested entity does not exist. ", response = File.class),
            @io.swagger.annotations.ApiResponse(code = 412, message = "Precondition Failed. The request has not been performed because one of the preconditions is not met. ", response = File.class)})
    public Response appsMobilePlistAppIdUuidGet(
            @ApiParam(value = "**APP ID** consisting of the **UUID** of the App. The following combination is also accepted as a valid APP ID: the provider of the app, name of the App and the version. Should be formatted as **provider-name-version**. ", required = true) @PathParam("appId") String appId,
            @ApiParam(value = "The UUID parameter of the one-time download link, which has the external representation of a downloadable binary file", required = true) @PathParam("uuid") String uuid,
            @ApiParam(value = "Validator for conditional requests; based on ETag. ") @HeaderParam("If-Match") String ifMatch,
            @ApiParam(value = "Validator for conditional requests; based on the Last Modified header. ") @HeaderParam("If-Unmodified-Since") String ifUnmodifiedSince) {
        return delegate.appsMobilePlistAppIdUuidGet(appId, uuid, ifMatch, ifUnmodifiedSince);
    }

    @GET
    @Path("/mobile/binaries/one-time/{uuid}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Retrieving mobile application binary", notes = "Retrieving .apk and .ipa binaries for mobile apps. ", response = File.class, tags={ "MobileApps",  })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "OK. Mobile app binary content retrieved successfully. ", response = File.class),
            @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request. Invalid request or validation error. ", response = File.class),
            @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found. Requested entity does not exist. ", response = File.class),
            @io.swagger.annotations.ApiResponse(code = 412, message = "Precondition Failed. The request has not been performed because one of the preconditions is not met. ", response = File.class),
            @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden.\nThe request must be conditional but no condition has been specified.") })
    public Response appsMobileBinariesOneTimeUuidGet(
            @ApiParam(value = "The UUID parameter of the one-time download link, which has the external representation of a downloadable binary file",required=true) @PathParam("uuid") String uuid,
            @ApiParam(value = "Validator for conditional requests; based on ETag. " )@HeaderParam("If-Match") String ifMatch,
            @ApiParam(value = "Validator for conditional requests; based on the Last Modified header. " )@HeaderParam("If-Unmodified-Since") String ifUnmodifiedSince)
    {
        return delegate.appsMobileBinariesOneTimeUuidGet(uuid,ifMatch,ifUnmodifiedSince);
    }

    @GET
    @Path("/static-contents/{fileName}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Retrieving images, pdf, documents files", notes = "Retrieving images for banners, screenshots etc.", response = File.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK.\nStatic content Retrieved successfully."),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request.\nInvalid request or validation error"),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found.\nRequested entity does not exist."),
        
        @io.swagger.annotations.ApiResponse(code = 412, message = "Precondition Failed.\nThe request has not been performed because one of the preconditions is not met.") })

    public Response appsStaticContentsFileNameGet(@ApiParam(value = "Application type",required=true) @QueryParam("appType") String appType,
    @ApiParam(value = "file name.",required=true ) @PathParam("fileName") String fileName,
    @ApiParam(value = "Validator for conditional requests; based on ETag."  )@HeaderParam("If-Match") String ifMatch,
    @ApiParam(value = "Validator for conditional requests; based on Last Modified header."  )@HeaderParam("If-Unmodified-Since") String ifUnmodifiedSince)
    {
    return delegate.appsStaticContentsFileNameGet(appType,fileName,ifMatch,ifUnmodifiedSince);
    }
    @POST
    @Path("/uninstallation")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Uninstall App", notes = "Uninstall an existing App", response = Void.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK."),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request.\nInvalid request or validation error."),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden.\nThe request must be conditional but no condition has been specified."),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found.\nResource to be deleted does not exist."),
        
        @io.swagger.annotations.ApiResponse(code = 412, message = "Precondition Failed.\nThe request has not been performed because one of the preconditions is not met.") })

    public Response appsUninstallationPost(@ApiParam(value = "Media type of the entity in the body. Default is JSON." ,required=true , defaultValue="JSON")@HeaderParam("Content-Type") String contentType,
    @ApiParam(value = ""  ) InstallDTO install)
    {
    return delegate.appsUninstallationPost(contentType,install);
    }

    @POST
    @Path("/mobile/schedule-install")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Schedule Install App", notes = "Schedule Install an App ", response = void.class, tags={ "Apps",  })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "OK. ", response = void.class),
            @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request.  Invalid request or validation error. ", response = void.class),
            @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden.  The request must be conditional but no condition has been specified. ", response = void.class),
            @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found.  Resource to be deleted does not exist. ", response = void.class) })
    public Response appsMobileScheduleInstallPost(
            @ApiParam(value = "Media type of the entity in the body. Default is JSON. " ,required=true, defaultValue="JSON")@HeaderParam("Content-Type") String contentType,
            @ApiParam(value = "" ) ScheduleDTO schedule,
            @Context SecurityContext securityContext)
    {
        return delegate.appsMobileScheduleInstallPost(contentType,schedule,securityContext);
    }
    @POST
    @Path("/mobile/schedule-update")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Schedule Update App", notes = "Schedule Update an App ", response = void.class, tags={ "Apps",  })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "OK. ", response = void.class),
            @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request.  Invalid request or validation error. ", response = void.class),
            @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden.  The request must be conditional but no condition has been specified. ", response = void.class),
            @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found.  Resource to be deleted does not exist. ", response = void.class) })
    public Response appsMobileScheduleUpdatePost(
            @ApiParam(value = "Media type of the entity in the body. Default is JSON. " ,required=true, defaultValue="JSON")@HeaderParam("Content-Type") String contentType,
            @ApiParam(value = "" ) ScheduleDTO schedule,
            @Context SecurityContext securityContext)
    {
        return delegate.appsMobileScheduleUpdatePost(contentType,schedule,securityContext);
    }
    @GET
    @Path("/{appType}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Get all Apps", notes = "Get a list of available Apps qualifying under a given search condition.", response = AppListDTO.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK.\nList of qualifying Apps is returned."),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request.\nInvalid request or validation error."),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden.\nThe request must be conditional but no condition has been specified."),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found.\nThe resource to be updated does not exist.") })

    public Response appsAppTypeGet(@ApiParam(value = "App Type. Either webapp or mobileapp",required=true ) @PathParam("appType") String appType,
    @ApiParam(value = "**Search condition**.\n\nYou can search in attributes by using an **\"attribute:\"** modifier.\n\nEg. \"provider:wso2\" will match an App if the provider of the App contains \"wso2\".\n\nSupported attribute modifiers are [*provider, app_name, app_version, app_id**]\n\nIf no advanced attribute modifier has been specified, search will match the\ngiven query string against App Name.") @QueryParam("query") String query,
    @ApiParam(value = "Used to limit the fields in response.\n\n\n\n\nSupported filters are [ **basic,all**]", allowableValues="{values=[basic, all]}", defaultValue="basic") @QueryParam("field-filter") String fieldFilter,
    @ApiParam(value = "Maximum size of resource array to return.", defaultValue="25") @QueryParam("limit") Integer limit,
    @ApiParam(value = "Starting point within the complete list of items qualified.", defaultValue="0") @QueryParam("offset") Integer offset,
    @ApiParam(value = "Media types acceptable for the response. Default is JSON."  , defaultValue="JSON")@HeaderParam("Accept") String accept,
    @ApiParam(value = "Validator for conditional requests; based on the ETag of the formerly retrieved\nvariant of the resourec."  )@HeaderParam("If-None-Match") String ifNoneMatch)
    {
    return delegate.appsAppTypeGet(appType,query,fieldFilter,limit,offset,accept,ifNoneMatch);
    }
    @GET
    @Path("/{appType}/id/{appId}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Get app details", notes = "Get details of an app.", response = AppDTO.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK.\nQualifying App is returned."),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request.\nInvalid request or validation error."),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden.\nThe request must be conditional but no condition has been specified."),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found.\nThe resource to be updated does not exist.") })

    public Response appsAppTypeIdAppIdGet(@ApiParam(value = "App Type. Either webapp or mobileapp",required=true ) @PathParam("appType") String appType,
    @ApiParam(value = "**APP ID** consisting of the **UUID** of the App.\nThe combination of the provider of the app, name of the appId and the version is also accepted as a valid App ID.\nShould be formatted as **provider-name-version**.",required=true ) @PathParam("appId") String appId,
    @ApiParam(value = "Media types acceptable for the response. Default is JSON."  , defaultValue="JSON")@HeaderParam("Accept") String accept,
    @ApiParam(value = "Validator for conditional requests; based on the ETag of the formerly retrieved\nvariant of the resourec."  )@HeaderParam("If-None-Match") String ifNoneMatch,
    @ApiParam(value = "Validator for conditional requests; based on Last Modified header of the\nformerly retrieved variant of the resource."  )@HeaderParam("If-Modified-Since") String ifModifiedSince)
    {
    return delegate.appsAppTypeIdAppIdGet(appType,appId,accept,ifNoneMatch,ifModifiedSince);
    }
       @GET
    @Path("/{appType}/id/{appId}/rate")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Get app rating details", notes = "Get rating details of an app.", response = AppRatingListDTO.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK.\nApp rating is returned."),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request.\nInvalid request or validation error."),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden.\nThe request must be conditional but no condition has been specified."),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found.\nThe resource to be updated does not exist.") })

    public Response appsAppTypeIdAppIdRateGet(@ApiParam(value = "App Type. Either webapp or mobileapp",required=true ) @PathParam("appType") String appType,
    @ApiParam(value = "**APP ID** consisting of the **UUID** of the App.\nThe combination of the provider of the app, name of the appId and the version is also accepted as a valid App ID.\nShould be formatted as **provider-name-version**.",required=true ) @PathParam("appId") String appId,
    @ApiParam(value = "Maximum size of resource array to return.", defaultValue="25") @QueryParam("limit") Integer limit,
    @ApiParam(value = "Starting point within the complete list of items qualified.", defaultValue="0") @QueryParam("offset") Integer offset,
    @ApiParam(value = "Media types acceptable for the response. Default is JSON."  , defaultValue="JSON")@HeaderParam("Accept") String accept,
    @ApiParam(value = "Validator for conditional requests; based on the ETag of the formerly retrieved\nvariant of the resourec."  )@HeaderParam("If-None-Match") String ifNoneMatch,
    @ApiParam(value = "Validator for conditional requests; based on Last Modified header of the\nformerly retrieved variant of the resource."  )@HeaderParam("If-Modified-Since") String ifModifiedSince)
    {
    return delegate.appsAppTypeIdAppIdRateGet(appType,appId,limit,offset,accept,ifNoneMatch,ifModifiedSince);
    }
    @PUT
    @Path("/{appType}/id/{appId}/rate")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Change App rating", notes = "Change App rating", response = Void.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Ok."),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request.\nInvalid request or validation error"),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden.\nThe request must be conditional but no condition has been specified."),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found.\nThe resource to be updated does not exist."),
        
        @io.swagger.annotations.ApiResponse(code = 412, message = "Precondition Failed.\nThe request has not been performed because one of the preconditions is not met.") })

    public Response appsAppTypeIdAppIdRatePut(@ApiParam(value = "App Type. Either webapp or mobileapp",required=true ) @PathParam("appType") String appType,
    @ApiParam(value = "**APP ID** consisting of the **UUID** of the App.\nThe combination of the provider of the app, name of the appId and the version is also accepted as a valid App ID.\nShould be formatted as **provider-name-version**.",required=true ) @PathParam("appId") String appId,
    @ApiParam(value = "App rating object that needs to be added" ,required=true ) AppRatingInfoDTO rating,
    @ApiParam(value = "Media type of the entity in the body. Default is JSON." ,required=true , defaultValue="JSON")@HeaderParam("Content-Type") String contentType,
    @ApiParam(value = "Validator for conditional requests; based on ETag."  )@HeaderParam("If-Match") String ifMatch,
    @ApiParam(value = "Validator for conditional requests; based on Last Modified header."  )@HeaderParam("If-Unmodified-Since") String ifUnmodifiedSince)
    {
    return delegate.appsAppTypeIdAppIdRatePut(appType,appId,rating,contentType,ifMatch,ifUnmodifiedSince);
    }
    @GET
    @Path("/{appType}/id/{appId}/storage/{fileName}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Retrieving file", notes = "Retrieving a file saved with an App.", response = File.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK."),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request.\nInvalid request or validation error"),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found.\nRequested entity does not exist.") })

    public Response appsAppTypeIdAppIdStorageFileNameGet(@ApiParam(value = "App Type. Either webapp or mobileapp",required=true ) @PathParam("appType") String appType,
    @ApiParam(value = "**APP ID** consisting of the **UUID** of the App.\nThe combination of the provider of the app, name of the appId and the version is also accepted as a valid App ID.\nShould be formatted as **provider-name-version**.",required=true ) @PathParam("appId") String appId,
    @ApiParam(value = "file name.",required=true ) @PathParam("fileName") String fileName,
    @ApiParam(value = "Validator for conditional requests; based on ETag."  )@HeaderParam("If-Match") String ifMatch,
    @ApiParam(value = "Validator for conditional requests; based on Last Modified header."  )@HeaderParam("If-Unmodified-Since") String ifUnmodifiedSince)
    {
    return delegate.appsAppTypeIdAppIdStorageFileNameGet(appType,appId,fileName,ifMatch,ifUnmodifiedSince);
    }
    @GET
    @Path("/{appType}/id/{appId}/subscription")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Get subscriptions for current user", notes = "Get subscriptions for current user.", response = AppListDTO.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK.\nQualifying App is returned."),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request.\nInvalid request or validation error."),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden.\nThe request must be conditional but no condition has been specified."),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found.\nThe resource to be updated does not exist.") })

    public Response appsAppTypeIdAppIdSubscriptionGet(@ApiParam(value = "App Type. Either webapp or mobileapp",required=true ) @PathParam("appType") String appType,
    @ApiParam(value = "**APP ID** consisting of the **UUID** of the App.\nThe combination of the provider of the app, name of the appId and the version is also accepted as a valid App ID.\nShould be formatted as **provider-name-version**.",required=true ) @PathParam("appId") String appId,
    @ApiParam(value = "Media types acceptable for the response. Default is JSON."  , defaultValue="JSON")@HeaderParam("Accept") String accept,
    @ApiParam(value = "Validator for conditional requests; based on the ETag of the formerly retrieved\nvariant of the resourec."  )@HeaderParam("If-None-Match") String ifNoneMatch,
    @ApiParam(value = "Validator for conditional requests; based on Last Modified header of the\nformerly retrieved variant of the resource."  )@HeaderParam("If-Modified-Since") String ifModifiedSince)
    {
    return delegate.appsAppTypeIdAppIdSubscriptionGet(appType,appId,accept,ifNoneMatch,ifModifiedSince);
    }
    @POST
    @Path("/{appType}/id/{appId}/subscription")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Subscribe App", notes = "Subscribe to an existing App", response = Void.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK."),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request.\nInvalid request or validation error."),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden.\nThe request must be conditional but no condition has been specified."),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found.\nResource to be deleted does not exist."),
        
        @io.swagger.annotations.ApiResponse(code = 412, message = "Precondition Failed.\nThe request has not been performed because one of the preconditions is not met.") })

    public Response appsAppTypeIdAppIdSubscriptionPost(@ApiParam(value = "App Type. Either webapp or mobileapp",required=true ) @PathParam("appType") String appType,
    @ApiParam(value = "**APP ID** consisting of the **UUID** of the App.\nThe combination of the provider of the app, name of the appId and the version is also accepted as a valid App ID.\nShould be formatted as **provider-name-version**.",required=true ) @PathParam("appId") String appId,
    @ApiParam(value = "Media type of the entity in the body. Default is JSON." ,required=true , defaultValue="JSON")@HeaderParam("Content-Type") String contentType)
    {
    return delegate.appsAppTypeIdAppIdSubscriptionPost(appType,appId,contentType);
    }
    @POST
    @Path("/{appType}/id/{appId}/subscription-workflow")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Check for subscription workflow", notes = "Check for subscription workflow.", response = Void.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK."),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request.\nInvalid request or validation error."),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden.\nThe request must be conditional but no condition has been specified."),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found.\nResource to be deleted does not exist."),
        
        @io.swagger.annotations.ApiResponse(code = 412, message = "Precondition Failed.\nThe request has not been performed because one of the preconditions is not met.") })

    public Response appsAppTypeIdAppIdSubscriptionWorkflowPost(@ApiParam(value = "App Type. Either webapp or mobileapp",required=true ) @PathParam("appType") String appType,
    @ApiParam(value = "**APP ID** consisting of the **UUID** of the App.\nThe combination of the provider of the app, name of the appId and the version is also accepted as a valid App ID.\nShould be formatted as **provider-name-version**.",required=true ) @PathParam("appId") String appId,
    @ApiParam(value = "Media type of the entity in the body. Default is JSON." ,required=true , defaultValue="JSON")@HeaderParam("Content-Type") String contentType)
    {
    return delegate.appsAppTypeIdAppIdSubscriptionWorkflowPost(appType,appId,contentType);
    }
    @GET
    @Path("/{appType}/id/{appId}/subscription/users")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Get subscribed user list", notes = "Get subscribed user list for a given App.", response = UserIdListDTO.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK.\nQualifying App is returned."),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request.\nInvalid request or validation error."),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden.\nThe request must be conditional but no condition has been specified."),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found.\nThe resource to be updated does not exist.") })

    public Response appsAppTypeIdAppIdSubscriptionUsersGet(@ApiParam(value = "App Type. Either webapp or mobileapp",required=true ) @PathParam("appType") String appType,
    @ApiParam(value = "**APP ID** consisting of the **UUID** of the App.\nThe combination of the provider of the app, name of the appId and the version is also accepted as a valid App ID.\nShould be formatted as **provider-name-version**.",required=true ) @PathParam("appId") String appId,
    @ApiParam(value = "Media types acceptable for the response. Default is JSON."  , defaultValue="JSON")@HeaderParam("Accept") String accept,
    @ApiParam(value = "Validator for conditional requests; based on the ETag of the formerly retrieved\nvariant of the resourec."  )@HeaderParam("If-None-Match") String ifNoneMatch,
    @ApiParam(value = "Validator for conditional requests; based on Last Modified header of the\nformerly retrieved variant of the resource."  )@HeaderParam("If-Modified-Since") String ifModifiedSince)
    {
    return delegate.appsAppTypeIdAppIdSubscriptionUsersGet(appType,appId,accept,ifNoneMatch,ifModifiedSince);
    }
    @POST
    @Path("/{appType}/id/{appId}/unsubscription")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Un subscribe App", notes = "Un subscribe to an existing App", response = Void.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK."),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request.\nInvalid request or validation error."),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden.\nThe request must be conditional but no condition has been specified."),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found.\nResource to be deleted does not exist."),
        
        @io.swagger.annotations.ApiResponse(code = 412, message = "Precondition Failed.\nThe request has not been performed because one of the preconditions is not met.") })

    public Response appsAppTypeIdAppIdUnsubscriptionPost(@ApiParam(value = "App Type. Either webapp or mobileapp",required=true ) @PathParam("appType") String appType,
    @ApiParam(value = "**APP ID** consisting of the **UUID** of the App.\nThe combination of the provider of the app, name of the appId and the version is also accepted as a valid App ID.\nShould be formatted as **provider-name-version**.",required=true ) @PathParam("appId") String appId,
    @ApiParam(value = "Media type of the entity in the body. Default is JSON." ,required=true , defaultValue="JSON")@HeaderParam("Content-Type") String contentType)
    {
    return delegate.appsAppTypeIdAppIdUnsubscriptionPost(appType,appId,contentType);
    }
}

