package org.wso2.carbon.appmgt.rest.api.store;

import org.wso2.carbon.appmgt.rest.api.store.dto.AppRatingInfoDTO;
import org.wso2.carbon.appmgt.rest.api.store.dto.EventsDTO;
import org.wso2.carbon.appmgt.rest.api.store.dto.InstallDTO;
import org.wso2.carbon.appmgt.rest.api.store.dto.ScheduleDTO;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public abstract class AppsApiService {
    public abstract Response appsDownloadPost(String contentType,InstallDTO install);
    public abstract Response appsMobileIdAppIdDownloadPost(String appId,String contentType);
    public abstract Response appsEventPublishPost(EventsDTO events,String contentType);
    public abstract Response appsFavouritePageGet(String accept,String ifNoneMatch);
    public abstract Response appsFavouritePagePost();
    public abstract Response appsFavouritePageDelete();
    public abstract Response appsMobileBinariesFileNameGet(String fileName,String ifMatch,String ifUnmodifiedSince);
    public abstract Response appsMobileBinariesOneTimeUuidGet(String uuid,String ifMatch,String ifUnmodifiedSince);
    public abstract Response appsMobilePlistAppIdUuidGet(String appId,String uuid,String ifMatch,String ifUnmodifiedSince);
    public abstract Response appsStaticContentsFileNameGet(String appType,String fileName,String ifMatch,String ifUnmodifiedSince);
    public abstract Response appsUninstallationPost(String contentType, InstallDTO install);
    public abstract Response appsAppTypeGet(String appType,String query,String fieldFilter,Integer limit,Integer offset,String accept,String ifNoneMatch);
    public abstract Response appsAppTypeIdAppIdGet(String appType,String appId,String accept,String ifNoneMatch,String ifModifiedSince);
    public abstract Response appsAppTypeIdAppIdFavouriteAppPost(String appType,String appId,String contentType);
    public abstract Response appsAppTypeIdAppIdFavouriteAppDelete(String appType,String appId,String contentType);
    public abstract Response appsAppTypeIdAppIdRateGet(String appType,String appId,Integer limit,Integer offset,String accept,String ifNoneMatch,String ifModifiedSince);
    public abstract Response appsAppTypeIdAppIdRatePut(String appType,String appId,AppRatingInfoDTO rating,String contentType,String ifMatch,String ifUnmodifiedSince);
    public abstract Response appsAppTypeIdAppIdStorageFileNameGet(String appType,String appId,String fileName,String ifMatch,String ifUnmodifiedSince);
    public abstract Response appsAppTypeIdAppIdSubscriptionGet(String appType,String appId,String accept,String ifNoneMatch,String ifModifiedSince);
    public abstract Response appsAppTypeIdAppIdSubscriptionPost(String appType,String appId,String contentType);
    public abstract Response appsAppTypeIdAppIdSubscriptionWorkflowPost(String appType,String appId,String contentType);
    public abstract Response appsAppTypeIdAppIdSubscriptionUsersGet(String appType,String appId,String accept,String ifNoneMatch,String ifModifiedSince);
    public abstract Response appsAppTypeIdAppIdUnsubscriptionPost(String appType,String appId,String contentType);
    public abstract Response appsMobileScheduleInstallPost(String contentType, ScheduleDTO schedule,
                                                           SecurityContext securityContext);
    public abstract Response appsMobileScheduleUpdatePost(String contentType, ScheduleDTO schedule,
                                                          SecurityContext securityContext);
}

