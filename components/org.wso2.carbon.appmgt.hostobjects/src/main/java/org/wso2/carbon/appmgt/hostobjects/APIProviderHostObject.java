/*
*  Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.appmgt.hostobjects;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.woden.WSDLFactory;
import org.apache.woden.WSDLReader;
import org.jaggeryjs.hostobjects.file.FileHostObject;
import org.jaggeryjs.scriptengine.exceptions.ScriptException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.wso2.carbon.appmgt.api.APIProvider;
import org.wso2.carbon.appmgt.api.AppManagementException;
import org.wso2.carbon.appmgt.api.dto.AppHitsStatsDTO;
import org.wso2.carbon.appmgt.api.dto.UserApplicationAPIUsage;
import org.wso2.carbon.appmgt.api.dto.UserHitsPerAppDTO;
import org.wso2.carbon.appmgt.api.exception.AppUsageQueryServiceClientException;
import org.wso2.carbon.appmgt.api.model.APIIdentifier;
import org.wso2.carbon.appmgt.api.model.APIKey;
import org.wso2.carbon.appmgt.api.model.APIStatus;
import org.wso2.carbon.appmgt.api.model.AppDefaultVersion;
import org.wso2.carbon.appmgt.api.model.AppStore;
import org.wso2.carbon.appmgt.api.model.EntitlementPolicyGroup;
import org.wso2.carbon.appmgt.api.model.LifeCycleEvent;
import org.wso2.carbon.appmgt.api.model.SSOProvider;
import org.wso2.carbon.appmgt.api.model.Subscriber;
import org.wso2.carbon.appmgt.api.model.Tier;
import org.wso2.carbon.appmgt.api.model.URITemplate;
import org.wso2.carbon.appmgt.api.model.WebApp;
import org.wso2.carbon.appmgt.api.model.entitlement.EntitlementPolicyPartial;
import org.wso2.carbon.appmgt.api.model.entitlement.EntitlementPolicyValidationResult;
import org.wso2.carbon.appmgt.hostobjects.internal.HostObjectComponent;
import org.wso2.carbon.appmgt.hostobjects.internal.ServiceReferenceHolder;
import org.wso2.carbon.appmgt.impl.APIManagerFactory;
import org.wso2.carbon.appmgt.impl.AppMConstants;
import org.wso2.carbon.appmgt.impl.AppManagerConfiguration;
import org.wso2.carbon.appmgt.impl.UserAwareAPIProvider;
import org.wso2.carbon.appmgt.impl.dto.TierPermissionDTO;
import org.wso2.carbon.appmgt.impl.idp.sso.SSOConfiguratorUtil;
import org.wso2.carbon.appmgt.impl.service.AppUsageStatisticsService;
import org.wso2.carbon.appmgt.impl.utils.APIVersionStringComparator;
import org.wso2.carbon.appmgt.impl.utils.AppManagerUtil;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.mgt.stub.UserAdminStub;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.net.ssl.SSLHandshakeException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@SuppressWarnings("unused")
public class APIProviderHostObject extends ScriptableObject {

    private static final Log log = LogFactory.getLog(APIProviderHostObject.class);

    private String username;

    private APIProvider apiProvider;

    public String getClassName() {
        return "APIProvider";
    }

    // The zero-argument constructor used for create instances for runtime
    public APIProviderHostObject() throws AppManagementException {

    }

    public APIProviderHostObject(String loggedUser) throws AppManagementException {
        username = loggedUser;
        apiProvider = APIManagerFactory.getInstance().getAPIProvider(loggedUser);
    }

    public String getUsername() {
        return username;
    }

    public static Scriptable jsConstructor(Context cx, Object[] args, Function Obj,
                                           boolean inNewExpr)
            throws AppManagementException {
        if (args != null && args.length != 0) {
            String username = (String) args[0];
            return new APIProviderHostObject(username);
        }
        return new APIProviderHostObject();
    }

    public APIProvider getApiProvider() {
        return apiProvider;
    }

    private static APIProvider getAPIProvider(Scriptable thisObj) {
        return ((APIProviderHostObject) thisObj).getApiProvider();
    }

    private static void handleException(String msg) throws AppManagementException {
        log.error(msg);
        throw new AppManagementException(msg);
    }

    private static void handleException(String msg, Throwable t) throws AppManagementException {
        log.error(msg, t);
        throw new AppManagementException(msg, t);
    }

    public static NativeObject jsFunction_login(Context cx, Scriptable thisObj,
                                                Object[] args, Function funObj)
            throws AppManagementException {

        if (args==null || args.length == 0 || !isStringValues(args)) {
            handleException("Invalid input parameters to the login method");
        }

        String username = (String) args[0];
        String password = (String) args[1];

        AppManagerConfiguration config = HostObjectComponent.getAPIManagerConfiguration();
        String url = config.getFirstProperty(AppMConstants.AUTH_MANAGER_URL);
        if (url == null) {
            handleException("WebApp key manager URL unspecified");
        }

        NativeObject row = new NativeObject();
        try {

            UserAdminStub userAdminStub = new UserAdminStub(url + "UserAdmin");
            CarbonUtils.setBasicAccessSecurityHeaders(username, password,
                    true, userAdminStub._getServiceClient());
            //If multiple user stores are in use, and if the user hasn't specified the domain to which
            //he needs to login to
            /* Below condition is commented out as per new multiple users-store implementation,users from
            different user-stores not needed to input domain names when tried to login,APIMANAGER-1392*/
            // if (userAdminStub.hasMultipleUserStores() && !username.contains("/")) {
            //      handleException("Domain not specified. Please provide your username as domain/username");
            // }
        } catch (Exception e) {
            log.error("Error occurred while checking for multiple user stores");
        }

        try {
            AuthenticationAdminStub authAdminStub = new AuthenticationAdminStub(null, url + "AuthenticationAdmin");
            ServiceClient client = authAdminStub._getServiceClient();
            Options options = client.getOptions();
            options.setManageSession(true);

            String host = new URL(url).getHost();
            if (!authAdminStub.login(username, password, host)) {
                handleException("Login failed! Please recheck the username and password and try again..");
            }
            ServiceContext serviceContext = authAdminStub.
                    _getServiceClient().getLastOperationContext().getServiceContext();
            String sessionCookie = (String) serviceContext.getProperty(HTTPConstants.COOKIE_STRING);

            String usernameWithDomain = AppManagerUtil.getLoggedInUserInfo(sessionCookie,url).getUserName();
            usernameWithDomain = AppManagerUtil.setDomainNameToUppercase(usernameWithDomain);
            String tenantDomain = MultitenantUtils.getTenantDomain(username);
            boolean isSuperTenant = false;

            if (tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
                isSuperTenant = true;
            }else {
                usernameWithDomain = usernameWithDomain+"@"+tenantDomain;
            }

            boolean   authorized =
                    AppManagerUtil.checkPermissionQuietly(usernameWithDomain, AppMConstants.Permissions.WEB_APP_CREATE) ||
                            AppManagerUtil.checkPermissionQuietly(usernameWithDomain, AppMConstants.Permissions.WEB_APP_PUBLISH);


            if (authorized) {

                row.put("user", row, usernameWithDomain);
                row.put("sessionId", row, sessionCookie);
                row.put("isSuperTenant", row, isSuperTenant);
                row.put("error", row, false);
            } else {
                handleException("Login failed! Insufficient privileges.");
            }
        } catch (Exception e) {
            row.put("error", row, true);
            row.put("detail", row, e.getMessage());
        }

        return row;
    }

    public static String jsFunction_getAuthServerURL(Context cx, Scriptable thisObj,
                                                     Object[] args, Function funObj)
            throws AppManagementException {

        AppManagerConfiguration config = HostObjectComponent.getAPIManagerConfiguration();
        String url = config.getFirstProperty(AppMConstants.AUTH_MANAGER_URL);
        if (url == null) {
            handleException("WebApp key manager URL unspecified");
        }
        return url;
    }

    public static String jsFunction_getHTTPsURL(Context cx, Scriptable thisObj,
                                                Object[] args, Function funObj)
            throws AppManagementException {
        String hostName = CarbonUtils.getServerConfiguration().getFirstProperty("HostName");
        String backendHttpsPort = HostObjectUtils.getBackendPort("https");
        if (hostName == null) {
            hostName = System.getProperty("carbon.local.ip");
        }
        return "https://" + hostName + ":" + backendHttpsPort;

    }

    /**
     * Check whether the application with a given name, provider and version already exists
     *
     * @param ctx Rhino context
     * @param thisObj Scriptable object
     * @param args passing arguments
     * @param funObj Function object
     * @return true if the webapp already exists
     * @throws org.wso2.carbon.appmgt.api.AppManagementException
     * @throws ScriptException
     */
    public static boolean jsFunction_isWebappExists(Context ctx, Scriptable thisObj,Object[] args, Function funObj)
            throws AppManagementException, ScriptException {

        if (args == null || args.length != 3) {
            handleException("Invalid number of input parameters.");
        }

        if (args[0] == null || args[1] == null || args[2] == null) {
            handleException("Error while checking for existence of web app: NULL value in expected parameters ->"
                    + "[webapp name:" + args[0] + ",provider:" + args[1] + ",version:" + args[0] + "]");

        }
        String name = (String) args[0];
        String provider = (String) args[1];
        String version = (String) args[2];

        APIIdentifier apiId = new APIIdentifier(provider, name, version);
        APIProvider apiProvider = getAPIProvider(thisObj);

        return apiProvider.isAPIAvailable(apiId);
    }

    private static String getTransports(NativeObject apiData) {
        String transportStr = String.valueOf(apiData.get("overview_transports", apiData));
        String transport  = transportStr;
        if (transportStr != null) {
            if ((transportStr.indexOf(",") == 0) || (transportStr.indexOf(",") == (transportStr.length()-1))) {
                transport =transportStr.replace(",","");
            }
        }
        return transport;
    }

    /**
     * Generates entitlement policies for applied policy partials for the given app.
     *
     * @param context Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @throws org.wso2.carbon.appmgt.api.AppManagementException Wrapped exception by org.wso2.carbon.apimgt.api.AppManagementException
     */
    public static void jsFunction_generateEntitlementPolicies(Context context, Scriptable thisObj,
                                                              Object[] args,
                                                              Function funObj) throws
            AppManagementException {
        if (args == null || args.length != 2) {
            handleException("Invalid number of input parameters.");
        }
        if (args[0] == null) {
            handleException("Error while generating entitlement policy. The application identifier is null");
        }

        NativeObject appIdentifierNativeObject = (NativeObject) args[0];
        String authorizedAdminCookie = (String) args[1];
        APIIdentifier apiIdentifier = new APIIdentifier(
                (String) (appIdentifierNativeObject.get("provider", appIdentifierNativeObject)),
                (String) (appIdentifierNativeObject.get("name", appIdentifierNativeObject)),
                (String) (appIdentifierNativeObject.get("version", appIdentifierNativeObject)));
        APIProvider apiProvider = getAPIProvider(thisObj);
        apiProvider.generateEntitlementPolicies(apiIdentifier, authorizedAdminCookie);
    }

    /**
     * Retrieve policy content of a given policy
     *
     * @param context Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return policy content
     * @throws org.wso2.carbon.appmgt.api.AppManagementException Wrapped exception by org.wso2.carbon.apimgt.api.AppManagementException
     */
    public static String jsFunction_getEntitlementPolicyContent(Context context, Scriptable thisObj,
                                                                Object[] args,
                                                                Function funObj) throws
                                                                                 AppManagementException {
        if (args == null || args.length != 2) {
            handleException("Invalid number of input parameters.");
        }
        if (args[0] == null || args[1] == null) {
            handleException("Error while retrieving entitlement policy content. Entitlement policy id is null");
        }

        String policyId = args[0].toString();
        String authorizedAdminCookie = args[1].toString();

        APIProvider apiProvider = getAPIProvider(thisObj);
        return apiProvider.getEntitlementPolicy(policyId, authorizedAdminCookie);
    }

    /**
     * Get webapp ID of a webapp with a given uuid
     *
     * @param context Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return webapp id
     * @throws org.wso2.carbon.appmgt.api.AppManagementException Wrapped exception by org.wso2.carbon.apimgt.api.AppManagementException
     */
    public static int jsFunction_getWebAppId(Context context, Scriptable thisObj,
                                             Object[] args,
                                             Function funObj) throws AppManagementException {

        if (args == null || args.length != 1) {
            handleException("Invalid number of input parameters.");
        }
        if (args[0] == null) {
            handleException("Error while retrieving webapp id. The webapp uuid is null ");
        }

        String uuid = args[0].toString();
        APIProvider apiProvider = getAPIProvider(thisObj);
        return apiProvider.getWebAppId(uuid);
    }

    /**
     * Saves the given entitlement policy partial in database
     *
     * @param context Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return entitlement policy partial id
     * @throws org.wso2.carbon.appmgt.api.AppManagementException Wrapped exception by org.wso2.carbon.apimgt.api.AppManagementException
     */
    public static int jsFunction_saveEntitlementPolicyPartial(Context context, Scriptable thisObj,
                                                              Object[] args,
                                                              Function funObj) throws
                                                                               AppManagementException {
        if (args == null || args.length != 4) {
            handleException("Invalid number of input parameters.");
        }
        if (args[0] == null || args[1] == null ) {
            handleException("Error while saving policy partial. Policy partial content is null");
        }

        String policyPartialName = args[0].toString();
        String policyPartial = args[1].toString();
        String isShared = args[2].toString();
        String policyPartialDesc = args[3].toString();
        boolean isSharedPartial = isShared.equalsIgnoreCase("true");
        String currentUser = ((APIProviderHostObject) thisObj).getUsername();

        APIProvider apiProvider = getAPIProvider(thisObj);
        return apiProvider.saveEntitlementPolicyPartial(policyPartialName, policyPartial, isSharedPartial, currentUser,policyPartialDesc);
    }

    /**
     * Update a given entitlement policy partial with the given partial name and partial content
     *
     * @param context Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return if success true else false
     * @throws org.wso2.carbon.appmgt.api.AppManagementException
     */
    public static boolean jsFunction_updateEntitlementPolicyPartial(Context context, Scriptable thisObj,
                                                                    Object[] args,
                                                                    Function funObj) throws
                                                                                     AppManagementException {
        if (args == null || args.length != 5) {
            handleException("Invalid number of input parameters.");
        }
        if (args[0] == null || args[1] == null || args[2] == null || args[4] == null) {
            handleException("Error in updating policy parital :NULL value in expected parameters ->"
                    + "[policyPartialId:" + args[0] + ",policyPartial:" + args[1] + ",isShared:" + args[0] + "]");
        }
        int policyPartialId = Integer.parseInt(args[0].toString());
        String policyPartial = args[1].toString();
        String isShared = args[2].toString();
        String policyPartialDesc = args[3].toString();
        boolean isSharedPartial = isShared.equalsIgnoreCase("true");
        String currentUser = ((APIProviderHostObject) thisObj).getUsername();
        String authorizedAdminCookie = args[4].toString();

        APIProvider apiProvider = getAPIProvider(thisObj);
        return apiProvider.updateEntitlementPolicyPartial(policyPartialId, policyPartial, currentUser, isSharedPartial,
                                                          policyPartialDesc, authorizedAdminCookie);
    }

    /**
     * Delete entitlement policy partial
     *
     * @param context Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return true if success else false
     * @throws org.wso2.carbon.appmgt.api.AppManagementException
     */
    public static boolean jsFunction_deleteEntitlementPolicyPartial(Context context, Scriptable thisObj,
                                                                    Object[] args,
                                                                    Function funObj) throws
            AppManagementException {
        if (args == null || args.length != 1) {
            handleException("Invalid number of input parameters.");
        }
        if (args == null) {
            handleException("Error while deleting entitlement policy partial. The policy partial id is null");
        }

        int policyPartialId = Integer.parseInt(args[0].toString());
        String currentUser = ((APIProviderHostObject) thisObj).getUsername();

        APIProvider apiProvider = getAPIProvider(thisObj);
        return apiProvider.deleteEntitlementPolicyPartial(policyPartialId, currentUser);
    }

    /**
     * Retrieve the name and the content of a given policy partial using policy id
     *
     * @param cx      Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return policy partial name and content
     * @throws org.wso2.carbon.appmgt.api.AppManagementException Wrapped exception by org.wso2.carbon.apimgt.api.AppManagementException
     */
    public static NativeArray jsFunction_getEntitlementPolicyPartial(Context cx, Scriptable thisObj,
                                                                     Object[] args,
                                                                     Function funObj) throws
                                                                                      AppManagementException {
        if (args == null || args.length != 1) {
            handleException("Invalid number of input parameters.");
        }
        if (args[0] == null) {
            handleException("Error while retrieving entitlement policy partial. The policy partial id is null ");
        }
        NativeArray myn = new NativeArray(0);
        int policyPartialId = Integer.parseInt(args[0].toString());

        APIProvider apiProvider = getAPIProvider(thisObj);
        EntitlementPolicyPartial entitlementPolicyPartial = apiProvider.getPolicyPartial(policyPartialId);
        String policyPartialName = entitlementPolicyPartial.getPolicyPartialName();
        String policyPartialContent = entitlementPolicyPartial.getPolicyPartialContent();

        myn.put(0, myn, checkValue(policyPartialName));
        myn.put(1, myn, checkValue(policyPartialContent));
        return myn;
    }

    /**
     * Retrieve the shared policy partials
     *
     * @param cx      Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return shared policy partials
     * @throws org.wso2.carbon.appmgt.api.AppManagementException
     */
    public static NativeArray jsFunction_getSharedPolicyPartialList(Context cx, Scriptable thisObj,
                                                                    Object[] args,
                                                                    Function funObj) throws
                                                                                     AppManagementException {

        NativeArray myn = new NativeArray(0);
        APIProvider apiProvider = getAPIProvider(thisObj);
        List<EntitlementPolicyPartial> policyPartialList = apiProvider.getSharedPolicyPartialsList();
        int count = 0;
        for (EntitlementPolicyPartial entitlementPolicyPartial : policyPartialList) {
            NativeObject row = new NativeObject();
            row.put("partialId", row, entitlementPolicyPartial.getPolicyPartialId());
            row.put("partialName", row, entitlementPolicyPartial.getPolicyPartialName());
            row.put("partialContent", row, entitlementPolicyPartial.getPolicyPartialContent());
            row.put("ruleEffect", row, entitlementPolicyPartial.getRuleEffect());
            row.put("isShared", row, entitlementPolicyPartial.isShared());
            row.put("author", row, entitlementPolicyPartial.getAuthor());
            row.put("description", row, entitlementPolicyPartial.getDescription());
            count++;
            myn.put(count, myn, row);
        }

        return myn;
    }


    /**
     * Get application wise policy group list
     * @param context Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return Policy Group Array
     * @throws AppManagementException on error
     */
    public static NativeArray jsFunction_getPolicyGroupListByApplication(Context context, Scriptable thisObj,
                                                                          Object[] args,
                                                                          Function funObj) throws
            AppManagementException {
        NativeArray policyGroupArr = new NativeArray(0);
        int applicationId = Integer.parseInt(args[0].toString());
        APIProvider apiProvider = getAPIProvider(thisObj);
        List<EntitlementPolicyGroup> policyGroupList = apiProvider.getPolicyGroupListByApplication(applicationId);
        int count = 0;
        String policyPartials;
        for (EntitlementPolicyGroup entitlementPolicyGroup : policyGroupList) {
            NativeObject row = new NativeObject();
            row.put("policyGroupId", row, entitlementPolicyGroup.getPolicyGroupId());
            row.put("policyGroupName", row, entitlementPolicyGroup.getPolicyGroupName());
            row.put("throttlingTier", row, entitlementPolicyGroup.getThrottlingTier());
            row.put("userRoles", row, entitlementPolicyGroup.getUserRoles());
            row.put("allowAnonymous", row, entitlementPolicyGroup.isAllowAnonymous());
            policyPartials = entitlementPolicyGroup.getPolicyPartials().toString();
            row.put("policyPartials", row, policyPartials);
            row.put("policyGroupDesc",row,entitlementPolicyGroup.getPolicyDescription());

            count++;
            policyGroupArr.put(count, policyGroupArr, row);
        }

        return policyGroupArr;
    }


    /**
     * Retrieve the apps which use the given policy partial
     * @param cx      Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return list of app names
     * @throws org.wso2.carbon.appmgt.api.AppManagementException
     */
    public static NativeArray jsFunction_getAssociatedAppsNameList(Context cx, Scriptable thisObj,
                                                                   Object[] args,
                                                                   Function funObj) throws
                                                                                    AppManagementException {

        if (args == null || args.length != 1) {
            handleException("Invalid number of input parameters.");
        }
        if (args[0] == null) {
            handleException("Error while getting associated app names list for the parital :Policy id is NULL");
        }

        NativeArray myn = new NativeArray(0);
        int policyPartialId = Integer.parseInt(args[0].toString());
        APIProvider apiProvider = getAPIProvider(thisObj);
        List<APIIdentifier> apiIdentifiers = apiProvider.getAssociatedApps(policyPartialId);

        int count = 0;
        for (APIIdentifier identifier : apiIdentifiers) {
            NativeObject row = new NativeObject();
            row.put("appName", row, identifier.getApiName());
            count++;
            myn.put(count, myn, row);
        }

        return myn;
    }

    /**
     * Validate the given entitlement policy partial
     * @param context      Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return whether the policy partial is valid or not
     * @throws org.wso2.carbon.appmgt.api.AppManagementException Wrapped exception by org.wso2.carbon.apimgt.api.AppManagementException
     */
    public static EntitlementPolicyValidationResult jsFunction_validateEntitlementPolicyPartial(
            Context context, Scriptable thisObj, Object[] args, Function funObj) throws
                                                                                 AppManagementException {

        if (args == null || args.length != 1) {
            handleException("Invalid number of input parameters.");
        }
        if (args[0] == null) {
            handleException("Error while validating policy partial. The policy partial content is null");
        }
        String policyPartial = args[0].toString();

        APIProvider apiProvider = getAPIProvider(thisObj);
        return apiProvider.validateEntitlementPolicyPartial(policyPartial);

    }


    /**
     * Updates given entitlement policies using the relevant entitlement service implementation.
     *
     * @param cx      Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @throws org.wso2.carbon.appmgt.api.AppManagementException Wrapped exception by org.wso2.carbon.apimgt.api.AppManagementException
     */
    public static void jsFunction_updateEntitlementPolicies(Context cx, Scriptable thisObj, Object[] args,
                                                            Function funObj) throws
                                                                                 AppManagementException {
        if (args == null || args.length != 2) {
            handleException("Invalid number of input parameters.");
        }

        NativeArray policies = (NativeArray) args[0];
        String authorizedAdminCookie = (String) args[1];
        APIProvider apiProvider = getAPIProvider(thisObj);
        apiProvider.updateEntitlementPolicies(policies, authorizedAdminCookie);
    }

    public static boolean jsFunction_updateSubscriptionStatus(Context cx, Scriptable thisObj,
                                                              Object[] args,
                                                              Function funObj)
            throws AppManagementException {
        if (args==null ||args.length == 0) {
            handleException("Invalid input parameters.");
        }

        NativeObject apiData = (NativeObject) args[0];
        boolean success = false;
        String provider = (String) apiData.get("provider", apiData);
        String name = (String) apiData.get("name", apiData);
        String version = (String) apiData.get("version", apiData);
        String newStatus = (String) args[1];
        int appId = Integer.parseInt((String) args[2]);

        try {
            APIProvider apiProvider = getAPIProvider(thisObj);
            APIIdentifier apiId = new APIIdentifier(provider, name, version);
            apiProvider.updateSubscription(apiId, newStatus, appId);
            return true;

        } catch (AppManagementException e) {
            handleException("Error while updating subscription status", e);
            return false;
        }

    }

    private static void checkFileSize(FileHostObject fileHostObject)
            throws ScriptException, AppManagementException {
        if (fileHostObject != null) {
            long length = fileHostObject.getJavaScriptFile().getLength();
            if (length / 1024.0 > 1024) {
                handleException("Image file exceeds the maximum limit of 1MB");
            }
        }
    }

    public static boolean jsFunction_updateTierPermissions(Context cx, Scriptable thisObj,
                                                           Object[] args,
                                                           Function funObj)
            throws AppManagementException {
        if (args == null ||args.length == 0) {
            handleException("Invalid input parameters.");
        }

        NativeObject tierData = (NativeObject) args[0];
        boolean success = false;
        String tierName = (String) tierData.get("tierName", tierData);
        String permissiontype = (String) tierData.get("permissiontype", tierData);
        String roles = (String) tierData.get("roles", tierData);

        try {
            APIProvider apiProvider = getAPIProvider(thisObj);
            apiProvider.updateTierPermissions(tierName, permissiontype, roles);
            return true;

        } catch (AppManagementException e) {
            handleException("Error while updating subscription status", e);
            return false;
        }

    }

    public static NativeArray jsFunction_getTierPermissions(Context cx, Scriptable thisObj,
                                                            Object[] args,
                                                            Function funObj) {
        NativeArray myn = new NativeArray(0);
        APIProvider apiProvider = getAPIProvider(thisObj);
         /* Create an array with everyone role */
        String everyOneRoleName = ServiceReferenceHolder.getInstance().getRealmService().
                getBootstrapRealmConfiguration().getEveryOneRoleName();
        String defaultRoleArray[] = new String[1];
        defaultRoleArray[0] = everyOneRoleName;
        try {
            Set<Tier> tiers = apiProvider.getTiers();
            Set<TierPermissionDTO> tierPermissions = apiProvider.getTierPermissions();
            int i = 0;
            if (tiers != null) {

                for (Tier tier: tiers) {
                    NativeObject row = new NativeObject();
                    boolean found = false;
                    for (TierPermissionDTO permission : tierPermissions) {
                        if (permission.getTierName().equals(tier.getName())) {
                            row.put("tierName", row, permission.getTierName());
                            row.put("tierDisplayName", row, tier.getDisplayName());
                            row.put("permissionType", row,
                                    permission.getPermissionType());
                            String[] roles = permission.getRoles();
                             /*If no roles defined return default role list*/
                            if (roles == null ||  roles.length == 0) {
                                row.put("roles", row, defaultRoleArray);
                            } else {
                                row.put("roles", row,
                                        permission.getRoles());
                            }
                            found = true;
                            break;
                        }
                    }
            		 /* If no permissions has defined for this tier*/
                    if (!found) {
                        row.put("tierName", row, tier.getName());
                        row.put("tierDisplayName", row, tier.getDisplayName());
                        row.put("permissionType", row,
                                AppMConstants.TIER_PERMISSION_ALLOW);
                        row.put("roles", row, defaultRoleArray);
                    }
                    myn.put(i, myn, row);
                    i++;
                }
            }
        } catch (Exception e) {
            log.error("Error while getting available tiers", e);
        }
        return myn;
    }

    /**
     * This method is to functionality of getting an existing WebApp to WebApp-Provider based
     *
     * @param cx      Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return a native array
     * @throws org.wso2.carbon.appmgt.api.AppManagementException Wrapped exception by org.wso2.carbon.apimgt.api.AppManagementException
     */

    public static NativeArray jsFunction_getAPI(Context cx, Scriptable thisObj,
                                                Object[] args,
                                                Function funObj) throws AppManagementException {
        NativeArray myn = new NativeArray(0);

        if (args == null || !isStringValues(args)) {
            handleException("Invalid number of parameters or their types.");
        }
        String providerName = args[0].toString();
        String providerNameTenantFlow = args[0].toString();
        providerName= AppManagerUtil.replaceEmailDomain(providerName);
        String apiName = args[1].toString();
        String version = args[2].toString();

        APIIdentifier apiId = new APIIdentifier(providerName, apiName, version);
        APIProvider apiProvider = getAPIProvider(thisObj);
        boolean isTenantFlowStarted = false;
        try {
            String tenantDomain = MultitenantUtils.getTenantDomain(AppManagerUtil.replaceEmailDomainBack(providerNameTenantFlow));
            if(tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
            }
            WebApp api = apiProvider.getAPI(apiId);
            if (api != null) {
                Set<URITemplate> uriTemplates = api.getUriTemplates();
                myn.put(0, myn, checkValue(api.getId().getApiName()));
                myn.put(1, myn, checkValue(api.getDescription()));
                myn.put(2, myn, checkValue(api.getUrl()));
                myn.put(3, myn, checkValue(api.getWsdlUrl()));
                myn.put(4, myn, checkValue(api.getId().getVersion()));
                StringBuilder tagsSet = new StringBuilder("");
                for (int k = 0; k < api.getTags().toArray().length; k++) {
                    tagsSet.append(api.getTags().toArray()[k].toString());
                    if (k != api.getTags().toArray().length - 1) {
                        tagsSet.append(",");
                    }
                }
                myn.put(5, myn, checkValue(tagsSet.toString()));
                StringBuilder tiersSet = new StringBuilder("");
                StringBuilder tiersDisplayNamesSet = new StringBuilder("");
                StringBuilder tiersDescSet = new StringBuilder("");
                Set<Tier> tierSet = api.getAvailableTiers();
                Iterator it = tierSet.iterator();
                int j = 0;
                while (it.hasNext()) {
                    Object tierObject = it.next();
                    Tier tier = (Tier) tierObject;
                    tiersSet.append(tier.getName());
                    tiersDisplayNamesSet.append(tier.getDisplayName());
                    tiersDescSet.append(tier.getDescription());
                    if (j != tierSet.size() - 1) {
                        tiersSet.append(",");
                        tiersDisplayNamesSet.append(",");
                        tiersDescSet.append(",");
                    }
                    j++;
                }

                myn.put(6, myn, checkValue(tiersSet.toString()));
                myn.put(7, myn, checkValue(api.getStatus().toString()));
                myn.put(8, myn, getWebContextRoot(api.getThumbnailUrl()));
                myn.put(9, myn, api.getContext());
                myn.put(10, myn, checkValue(Long.valueOf(api.getLastUpdated().getTime()).toString()));
                myn.put(11, myn, getSubscriberCount(apiId, thisObj));

                if (uriTemplates.size() != 0) {
                    NativeArray uriTempArr = new NativeArray(uriTemplates.size());
                    Iterator i = uriTemplates.iterator();
                    List<NativeArray> uriTemplatesArr = new ArrayList<NativeArray>();
                    while (i.hasNext()) {
                        List<String> utArr = new ArrayList<String>();
                        URITemplate ut = (URITemplate) i.next();
                        utArr.add(ut.getUriTemplate());
                        utArr.add(ut.getMethodsAsString().replaceAll("\\s", ","));
                        utArr.add(ut.getAuthTypeAsString().replaceAll("\\s", ","));
                        utArr.add(ut.getThrottlingTiersAsString().replaceAll("\\s", ","));
                        NativeArray utNArr = new NativeArray(utArr.size());
                        for (int p = 0; p < utArr.size(); p++) {
                            utNArr.put(p, utNArr, utArr.get(p));
                        }
                        uriTemplatesArr.add(utNArr);
                    }

                    for (int c = 0; c < uriTemplatesArr.size(); c++) {
                        uriTempArr.put(c, uriTempArr, uriTemplatesArr.get(c));
                    }

                    myn.put(12, myn, uriTempArr);
                }

                myn.put(13, myn, checkValue(api.getSandboxUrl()));
                myn.put(14, myn, checkValue(tiersDescSet.toString()));
                myn.put(17, myn, checkValue(api.getTechnicalOwner()));
                myn.put(18, myn, checkValue(api.getTechnicalOwnerEmail()));
                myn.put(19, myn, checkValue(api.getWadlUrl()));
                myn.put(20, myn, checkValue(api.getVisibility()));
                myn.put(21, myn, checkValue(api.getVisibleRoles()));
                myn.put(22, myn, checkValue(api.getVisibleTenants()));
                myn.put(23, myn, checkValue(api.getEndpointUTUsername()));
                myn.put(24, myn, checkValue(api.getEndpointUTPassword()));
                myn.put(25, myn, checkValue(Boolean.toString(api.isEndpointSecured())));
                myn.put(26, myn, AppManagerUtil.replaceEmailDomainBack(checkValue(api.getId().getProviderName())));
                myn.put(27, myn, checkTransport("http",api.getTransports()));
                myn.put(28, myn, checkTransport("https",api.getTransports()));
                myn.put(29, myn, checkValue(api.getInSequence()));
                myn.put(30, myn, checkValue(api.getOutSequence()));

                myn.put(31, myn, checkValue(api.getSubscriptionAvailability()));
                myn.put(32, myn, checkValue(api.getSubscriptionAvailableTenants()));

                //@todo need to handle backword compatibility
                myn.put(33, myn, checkValue(api.getEndpointConfig()));

                myn.put(34, myn, checkValue(api.getResponseCache()));
                myn.put(35, myn, checkValue(Integer.toString(api.getCacheTimeout())));
                myn.put(36, myn, checkValue(tiersDisplayNamesSet.toString()));
            } else {
                handleException("Cannot find the requested WebApp- " + apiName +
                        "-" + version);
            }
        } catch (Exception e) {
            handleException("Error occurred while getting WebApp information of the api- " + apiName +
                    "-" + version, e);
        } finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
        return myn;
    }

    /**
     * This method returns the user subscribed APPs
     * @param cx      Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return Native array
     * @throws org.wso2.carbon.appmgt.api.AppManagementException
     */
    public static NativeArray jsFunction_getSubscribedAPIsByUsers(Context cx, Scriptable thisObj,
                                                                  Object[] args,
                                                                  Function funObj)
            throws AppManagementException {
        NativeArray myn = new NativeArray(0);
        String providerName = null;
        String fromDate = null;
        String toDate = null;
        APIProvider apiProvider = getAPIProvider(thisObj);
        if (args == null || args.length == 0) {
            handleException("Invalid input parameters.");
        }

        if (args[0] == null || args[1] == null || args[2] == null) {

            handleException("Error while getting subscribed apps by users :NULL value in expected parameters->" +
                    "[providerName:" + args[0] + ",+fromDate:" + args[1] + ",toDate:" + args[2] + "]");
        }
        boolean isTenantFlowStarted = false;
        try {
            providerName = AppManagerUtil.replaceEmailDomain((String) args[0]);
            fromDate = (String) args[1];
            toDate = (String) args[2];
            String tenantDomain = MultitenantUtils.getTenantDomain(AppManagerUtil.replaceEmailDomainBack(providerName));
            if (tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
            }

            if (providerName != null) {


                Map<String, List> subscribedApps = apiProvider.getSubscribedAPPsByUsers(fromDate, toDate);
                int i = 0;

                for (Map.Entry<String, List> entry : subscribedApps.entrySet()) {
                    //                    List<WebAppInfoDTO> webAppList = entry.getValue();
//                    for (WebAppInfoDTO webApp : webAppList) {
//                        NativeObject row = new NativeObject();
//                        row.put("user", row, entry.getKey());
//                        row.put("apiName", row, webApp.getWebAppName() + "(" + webApp.getProviderId() + ")");
//                        myn.put(i, myn, row);
//                        i++;
//                    }

                    List<Subscriber> subscribers = entry.getValue();
                    for (Subscriber subscriber : subscribers) {
                        NativeObject row = new NativeObject();
                        String[] parts = entry.getKey().split("/");
                        row.put("apiName", row, parts[0]);
                        row.put("version", row, parts[1]);
                        row.put("user", row, subscriber.getName());
                        row.put("subscribeDate", row,(subscriber.getSubscribedDate()+""));
                        myn.put(i, myn, row);
                        i++;
                    }
                }

            }
        } finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
        return myn;
    }

    /**
     * This method returns the Subscription(Subscriber) count of APPs
     * @param cx      Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return Native array
     * @throws org.wso2.carbon.appmgt.api.AppManagementException
     */
    public static NativeArray jsFunction_getSubscriberCountByAPIs(Context cx, Scriptable thisObj,
                                                                  Object[] args,
                                                                  Function funObj)
            throws AppManagementException {
        NativeArray myn = new NativeArray(0);
        String providerName = null;
        String fromDate = null;
        String toDate = null;
        APIProvider apiProvider = getAPIProvider(thisObj);
        if (args == null ||  args.length==0) {
            handleException("Invalid input parameters.");
        }

        if (args[0] == null || args[1] == null || args[2] == null) {

            handleException("Error while getting subscriber count by apps :NULL value in expected parameters->" +
                    "[providerName:" + args[0] + ",+fromDate:" + args[1] + ",toDate:" + args[2] + "]");
        }
        boolean isTenantFlowStarted = false;
        try {
            providerName = AppManagerUtil.replaceEmailDomain((String) args[0]);
            fromDate = (String) args[1];
            toDate = (String) args[2];
            String tenantDomain = MultitenantUtils.getTenantDomain(AppManagerUtil.replaceEmailDomainBack(providerName));
            if(tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)){
                isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
            }

            if (providerName != null) {
                AppManagerConfiguration config = HostObjectComponent.getAPIManagerConfiguration();
                Boolean selfSubscriptionStatus = Boolean.valueOf(config.getFirstProperty(
                        AppMConstants.ENABLE_SELF_SUBSCRIPTION));
                Boolean enterpriseSubscriptionStatus = Boolean.valueOf(config.getFirstProperty(
                        AppMConstants.ENABLE_ENTERPRISE_SUBSCRIPTION));

                boolean isSubscriptionOn = (selfSubscriptionStatus || enterpriseSubscriptionStatus);

                // Map consists data as <<appProvider,appName>,subscriptionCount>
                Map<String, Long> subscriptions = apiProvider.getSubscriptionCountByAPPs(providerName, fromDate, toDate,
                                                                                         isSubscriptionOn);

                List<APISubscription> subscriptionData = new ArrayList<APISubscription>();

                for (Map.Entry<String, Long> entry : subscriptions.entrySet()) {

                    String[] parts = entry.getKey().split("/");
                    String part1 = parts[0];


                    String part2 = parts[1];


                    String[] uuidpart = part2.split("&");
                    String version = uuidpart[0];
                    String uuid = uuidpart[1];


                    APISubscription sub = new APISubscription();
                    sub.name = part1 + "(v" + version + ")";
                    sub.version = version;
                    sub.count = entry.getValue();
                    sub.uuid=uuid;
                    subscriptionData.add(sub);


                }
                Collections.sort(subscriptionData, new Comparator<APISubscription>() {
                    public int compare(APISubscription o1, APISubscription o2) {
                        // Note that o2 appears before o1
                        // This is because we need to sort in the descending order
                        return (int) (o2.count - o1.count);
                    }
                });

                int i = 0;
                for (APISubscription sub : subscriptionData) {
                    NativeObject row = new NativeObject();
                    row.put("apiName", row, sub.name);
                    row.put("count", row, sub.count);
                    row.put("version", row, sub.version);
                    row.put("uuid", row, sub.uuid);

                    myn.put(i, myn, row);
                    i++;
                }
            }
        }  finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
        return myn;
    }

    public static NativeArray jsFunction_getTiers(Context cx, Scriptable thisObj,
                                                  Object[] args,
                                                  Function funObj) {
        NativeArray myn = new NativeArray(1);
        APIProvider apiProvider = getAPIProvider(thisObj);
        try {
            Set<Tier> tiers = apiProvider.getTiers();
            int i = 0;
            if (tiers != null) {
                for (Tier tier : tiers) {
                    NativeObject row = new NativeObject();
                    row.put("tierName", row, tier.getName());
                    row.put("tierDisplayName", row, tier.getDisplayName());
                    row.put("tierDescription", row,
                            tier.getDescription() != null ? tier.getDescription() : "");
                    row.put("tierSortKey", row, tier.getRequestPerMinute());
                    myn.put(i, myn, row);
                    i++;
                }
            }
        } catch (Exception e) {
            log.error("Error while getting available tiers", e);
        }
        return myn;
    }

    public static NativeArray jsFunction_getSubscriberCountByAPIVersions(Context cx,
                                                                         Scriptable thisObj,
                                                                         Object[] args,
                                                                         Function funObj)
            throws AppManagementException {
        NativeArray myn = new NativeArray(0);
        String providerName = null;
        String apiName = null;
        APIProvider apiProvider = getAPIProvider(thisObj);
        if (args == null || args.length==0) {
            handleException("Invalid input parameters.");
        }
        boolean isTenantFlowStarted = false;
        try {
            providerName = AppManagerUtil.replaceEmailDomain((String) args[0]);
            String tenantDomain = MultitenantUtils.getTenantDomain(AppManagerUtil.replaceEmailDomainBack(providerName));
            if(tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
            }
            apiName = (String) args[1];
            if (providerName != null && apiName != null) {
                Map<String, Long> subscriptions = new TreeMap<String, Long>();
                Set<String> versions = apiProvider.getAPIVersions(AppManagerUtil.replaceEmailDomain(providerName), apiName);
                for (String version : versions) {
                    APIIdentifier id = new APIIdentifier(providerName, apiName, version);
                    WebApp api = apiProvider.getAPI(id);
                    if (api.getStatus() == APIStatus.CREATED) {
                        continue;
                    }
                    long count = apiProvider.getAPISubscriptionCountByAPI(api.getId());
                    if (count == 0) {
                        continue;
                    }
                    subscriptions.put(api.getId().getVersion(), count);
                }

                int i = 0;
                for (Map.Entry<String, Long> entry : subscriptions.entrySet()) {
                    NativeObject row = new NativeObject();
                    row.put("apiVersion", row, entry.getKey());
                    row.put("count", row, entry.getValue().longValue());
                    myn.put(i, myn, row);
                    i++;
                }
            }
        } catch (Exception e) {
            log.error("Error while getting subscribers of the " +
                    "provider: " + providerName + " and WebApp: " + apiName, e);
        }finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
        return myn;
    }

    private static int getSubscriberCount(APIIdentifier apiId, Scriptable thisObj)
            throws AppManagementException {
        APIProvider apiProvider = getAPIProvider(thisObj);
        Set<Subscriber> subs = apiProvider.getSubscribersOfAPI(apiId);
        Set<String> subscriberNames = new HashSet<String>();
        if (subs != null) {
            for (Subscriber sub : subs) {
                subscriberNames.add(sub.getName());
            }
            return subscriberNames.size();
        } else {
            return 0;
        }
    }

    private static String checkTransport(String compare, String transport)
            throws AppManagementException {
        if(transport!=null){
            List<String> transportList = new ArrayList<String>();
            transportList.addAll(Arrays.asList(transport.split(",")));
            if(transportList.contains(compare)){
                return "checked";
            }else{
                return "";
            }

        }else{
            return "";
        }
    }

    /**
     * Get the identity provider URL from app-manager.xml file
     *
     * @param context Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return identity provider URL
     * @throws org.wso2.carbon.appmgt.api.AppManagementException Wrapped exception by org.wso2.carbon.apimgt.api.AppManagementException
     */
    public static String jsFunction_getIdentityProviderUrl(Context context, Scriptable thisObj,
                                                           Object[] args,
                                                           Function funObj) throws AppManagementException {
        AppManagerConfiguration config = HostObjectComponent.getAPIManagerConfiguration();
        String url = config.getFirstProperty(AppMConstants.SSO_CONFIGURATION_IDENTITY_PROVIDER_URL);
        if (url == null) {
            handleException("Identity provider URL unspecified");
        }
        return url;
    }

    /**
     * This method is to functionality of getting all the APIs stored
     *
     * @param cx      Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return a native array
     * @throws org.wso2.carbon.appmgt.api.AppManagementException Wrapped exception by org.wso2.carbon.apimgt.api.AppManagementException
     */
    public static NativeArray jsFunction_getAllAPIs(Context cx, Scriptable thisObj,
                                                    Object[] args,
                                                    Function funObj)
            throws AppManagementException {
        NativeArray myn = new NativeArray(0);
        APIProvider apiProvider = getAPIProvider(thisObj);
        /*String tenantDomain = MultitenantUtils.getTenantDomain(AppManagerUtil.replaceEmailDomainBack(providerName));
        if(tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
        }*/
        try {
            List<WebApp> apiList = apiProvider.getAllAPIs();
            if (apiList != null) {
                Iterator it = apiList.iterator();
                int i = 0;
                while (it.hasNext()) {
                    NativeObject row = new NativeObject();
                    Object apiObject = it.next();
                    WebApp api = (WebApp) apiObject;
                    APIIdentifier apiIdentifier = api.getId();
                    row.put("name", row, apiIdentifier.getApiName());
                    row.put("version", row, apiIdentifier.getVersion());
                    row.put("provider", row, AppManagerUtil.replaceEmailDomainBack(apiIdentifier.getProviderName()));
                    row.put("status", row, checkValue(api.getStatus().toString()));
                    row.put("thumb", row, getWebContextRoot(api.getThumbnailUrl()));
                    row.put("subs", row, getSubscriberCount(apiIdentifier, thisObj));
                    myn.put(i, myn, row);
                    i++;

                }
            }
        } catch (Exception e) {
            handleException("Error occurred while getting the APIs", e);
        }
        return myn;
    }

    /**
     * This method is to functionality of getting all the APIs stored per provider
     *
     * @param cx      Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return a native array
     * @throws org.wso2.carbon.appmgt.api.AppManagementException Wrapped exception by org.wso2.carbon.apimgt.api.AppManagementException
     */
    public static NativeArray jsFunction_getAPIsByProvider(Context cx, Scriptable thisObj,
                                                           Object[] args,
                                                           Function funObj)
            throws AppManagementException {
        NativeArray myn = new NativeArray(0);
        if (args==null ||args.length == 0) {
            handleException("Invalid number of parameters.");
        }
        String providerName = (String) args[0];
        if (providerName != null) {
            APIProvider apiProvider = getAPIProvider(thisObj);
            boolean isTenantFlowStarted = false;
            try {
                String tenantDomain = MultitenantUtils.getTenantDomain(AppManagerUtil.replaceEmailDomainBack(providerName));
                if(tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                    isTenantFlowStarted = true;
                    PrivilegedCarbonContext.startTenantFlow();
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
                }
                List<WebApp> apiList = apiProvider.getAPIsByProvider(AppManagerUtil.replaceEmailDomain(providerName));
                if (apiList != null) {
                    Iterator it = apiList.iterator();
                    int i = 0;
                    while (it.hasNext()) {
                        NativeObject row = new NativeObject();
                        Object apiObject = it.next();
                        WebApp api = (WebApp) apiObject;
                        APIIdentifier apiIdentifier = api.getId();
                        row.put("name", row, apiIdentifier.getApiName());
                        row.put("version", row, apiIdentifier.getVersion());
                        row.put("provider", row, AppManagerUtil.replaceEmailDomainBack(apiIdentifier.getProviderName()));
                        row.put("lastUpdatedDate", row, api.getLastUpdated().toString());
                        myn.put(i, myn, row);
                        i++;
                    }
                }
            } catch (Exception e) {
                handleException("Error occurred while getting APIs for " +
                        "the provider: " + providerName, e);
            } finally {
                if (isTenantFlowStarted) {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            }
        }
        return myn;
    }

    public static NativeArray jsFunction_getSubscribedAPIs(Context cx, Scriptable thisObj,
                                                           Object[] args,
                                                           Function funObj)
            throws AppManagementException {
        String userName = null;
        NativeArray myn = new NativeArray(0);
        APIProvider apiProvider = getAPIProvider(thisObj);

        if (args == null || !isStringValues(args)) {
            handleException("Invalid number of parameters or their types.");
        }
        try {
            userName = (String) args[0];
            Subscriber subscriber = new Subscriber(userName);
            Set<WebApp> apiSet = apiProvider.getSubscriberAPIs(subscriber);
            if (apiSet != null) {
                Iterator it = apiSet.iterator();
                int i = 0;
                while (it.hasNext()) {
                    NativeObject row = new NativeObject();
                    Object apiObject = it.next();
                    WebApp api = (WebApp) apiObject;
                    APIIdentifier apiIdentifier = api.getId();
                    row.put("apiName", row, apiIdentifier.getApiName());
                    row.put("version", row, apiIdentifier.getVersion());
                    row.put("provider", row, AppManagerUtil.replaceEmailDomainBack(apiIdentifier.getProviderName()));
                    row.put("updatedDate", row, api.getLastUpdated().toString());
                    myn.put(i, myn, row);
                    i++;
                }
            }
        } catch (Exception e) {
            handleException("Error occurred while getting the subscribed APIs information " +
                    "for the subscriber-" + userName, e);
        }
        return myn;
    }

    public static NativeArray jsFunction_getAllAPIUsageByProvider(Context cx, Scriptable thisObj,
                                                                  Object[] args, Function funObj)
            throws AppManagementException {

        NativeArray myn = new NativeArray(0);
        String providerName = null;
        APIProvider apiProvider = getAPIProvider(thisObj);

        if (args == null || !isStringValues(args)) {
            handleException("Invalid input parameters.");
        }
        try {
            providerName = (String) args[0];
            if (providerName != null) {
                UserApplicationAPIUsage[] apiUsages = apiProvider.getAllAPIUsageByProvider(providerName);
                for (int i = 0; i < apiUsages.length; i++) {
                    NativeObject row = new NativeObject();
                    row.put("userName", row, apiUsages[i].getUserId());
                    row.put("application", row, apiUsages[i].getApplicationName());
                    row.put("appId", row, "" + apiUsages[i].getAppId());
                    row.put("token", row, apiUsages[i].getAccessToken());
                    row.put("tokenStatus", row, apiUsages[i].getAccessTokenStatus());
                    row.put("subStatus", row, apiUsages[i].getSubStatus());

                    StringBuilder apiSet = new StringBuilder("");
                    for (int k = 0; k < apiUsages[i].getApiSubscriptions().length; k++) {
                        apiSet.append(apiUsages[i].getApiSubscriptions()[k].getSubStatus());
                        apiSet.append("::");
                        apiSet.append(apiUsages[i].getApiSubscriptions()[k].getApiId().getApiName());
                        apiSet.append("::");
                        apiSet.append(apiUsages[i].getApiSubscriptions()[k].getApiId().getVersion());
                        if (k != apiUsages[i].getApiSubscriptions().length - 1) {
                            apiSet.append(",");
                        }
                    }
                    row.put("apis", row, apiSet.toString());
                    myn.put(i, myn, row);
                }
            }
        } catch (Exception e) {
            handleException("Error occurred while getting subscribers of the provider: " + providerName, e);
        }
        return myn;
    }

    public static NativeArray jsFunction_getSubscribersOfAPI(Context cx, Scriptable thisObj,
                                                             Object[] args, Function funObj)
            throws AppManagementException {
        String apiName;
        String version;
        String providerName;
        NativeArray myn = new NativeArray(0);
        if (args == null || !isStringValues(args)) {
            handleException("Invalid number of parameters or their types.");
        }

        providerName = (String) args[0];
        apiName = (String) args[1];
        version = (String) args[2];

        APIIdentifier apiId = new APIIdentifier(providerName, apiName, version);
        Set<Subscriber> subscribers;
        APIProvider apiProvider = getAPIProvider(thisObj);
        try {
            subscribers = apiProvider.getSubscribersOfAPI(apiId);
            Iterator it = subscribers.iterator();
            int i = 0;
            while (it.hasNext()) {
                NativeObject row = new NativeObject();
                Object subscriberObject = it.next();
                Subscriber user = (Subscriber) subscriberObject;
                row.put("username", row, user.getName());
                row.put("tenantID", row, user.getTenantId());
                row.put("emailAddress", row, user.getEmail());
                row.put("subscribedDate", row, checkValue(Long.valueOf(user.getSubscribedDate().getTime()).toString()));
                myn.put(i, myn, row);
                i++;
            }

        } catch (AppManagementException e) {
            handleException("Error occurred while getting subscribers of the WebApp- " + apiName +
                    "-" + version, e);
        }
        return myn;
    }

    public static String jsFunction_isContextExist(Context cx, Scriptable thisObj,
                                                   Object[] args, Function funObj)
            throws AppManagementException {
        Boolean contextExist = false;
        if (args != null && isStringValues(args)) {
            String context = (String) args[0];
            String oldContext = (String) args[1];

            if (context.equals(oldContext)) {
                return contextExist.toString();
            }
            APIProvider apiProvider = getAPIProvider(thisObj);
            try {
                contextExist = apiProvider.isContextExist(context);
            } catch (AppManagementException e) {
                handleException("Error from registry while checking the input context is already exist", e);
            }
        } else {
            handleException("Input context value is null");
        }
        return contextExist.toString();
    }

    private static boolean isStringValues(Object[] args) {
        int i = 0;
        for (Object arg : args) {

            if (!(arg instanceof String)) {
                return false;

            }
            i++;
        }
        return true;
    }

    private static String checkValue(String input) {
        return input != null ? input : "";
    }


    private static APIStatus getApiStatus(String status) {
        APIStatus apiStatus = null;
        for (APIStatus aStatus : APIStatus.values()) {
            if (aStatus.getStatus().equalsIgnoreCase(status)) {
                apiStatus = aStatus;
            }

        }
        return apiStatus;
    }


    public static NativeArray jsFunction_searchAPIs(Context cx, Scriptable thisObj,
                                                    Object[] args,
                                                    Function funObj) throws AppManagementException {
        NativeArray myn = new NativeArray(0);

        if (args == null || args.length==0) {
            handleException("Invalid number of parameters.");
        }
        String providerName = (String) args[0];
        providerName= AppManagerUtil.replaceEmailDomain(providerName);
        String searchValue = (String) args[1];
        String searchTerm;
        String searchType;

        if (searchValue.contains(":")) {
            if (searchValue.split(":").length > 1) {
                searchType = searchValue.split(":")[0];
                searchTerm = searchValue.split(":")[1];
            } else {
                throw new AppManagementException("Search term is missing. Try again with valid search query.");
            }

        } else {
            searchTerm = searchValue;
            searchType = "default";
        }
        try {
            if ("*".equals(searchTerm) || searchTerm.startsWith("*")) {
                searchTerm = searchTerm.replaceFirst("\\*", ".*");
            }
            APIProvider apiProvider = getAPIProvider(thisObj);

            List<WebApp> searchedList = apiProvider.searchAPIs(searchTerm, searchType, providerName);
            Iterator it = searchedList.iterator();
            int i = 0;
            while (it.hasNext()) {
                NativeObject row = new NativeObject();
                Object apiObject = it.next();
                WebApp api = (WebApp) apiObject;
                APIIdentifier apiIdentifier = api.getId();
                row.put("name", row, apiIdentifier.getApiName());
                row.put("provider", row, AppManagerUtil.replaceEmailDomainBack(apiIdentifier.getProviderName()));
                row.put("version", row, apiIdentifier.getVersion());
                row.put("status", row, checkValue(api.getStatus().toString()));
                row.put("thumb", row, getWebContextRoot(api.getThumbnailUrl()));
                row.put("subs", row, apiProvider.getSubscribersOfAPI(api.getId()).size());
                if (providerName != null) {
                    row.put("lastUpdatedDate", row, checkValue(api.getLastUpdated().toString()));
                }
                myn.put(i, myn, row);
                i++;


            }
        } catch (Exception e) {
            handleException("Error occurred while getting the searched WebApp- " + searchValue, e);
        }
        return myn;
    }


    public static boolean jsFunction_hasCreatePermission(Context cx, Scriptable thisObj,
                                                         Object[] args,
                                                         Function funObj) {
        APIProvider provider = getAPIProvider(thisObj);
        if (provider instanceof UserAwareAPIProvider) {
            try {
                ((UserAwareAPIProvider) provider).checkCreatePermission();
                return true;
            } catch (AppManagementException e) {
                return false;
            }
        }
        return false;
    }

    public static boolean jsFunction_hasManageTierPermission(Context cx, Scriptable thisObj,
                                                             Object[] args,
                                                             Function funObj) {
        APIProvider provider = getAPIProvider(thisObj);
        if (provider instanceof UserAwareAPIProvider) {
            try {
                ((UserAwareAPIProvider) provider).checkManageTiersPermission();
                return true;
            } catch (AppManagementException e) {
                return false;
            }
        }
        return false;
    }

    public static boolean jsFunction_hasUserPermissions(Context cx, Scriptable thisObj,
                                                        Object[] args,
                                                        Function funObj)
            throws AppManagementException {
        if (args == null || !isStringValues(args)) {
            handleException("Invalid input parameters.");
        }
        String username = (String) args[0];
        return AppManagerUtil.checkPermissionQuietly(username, AppMConstants.Permissions.WEB_APP_CREATE) ||
                AppManagerUtil.checkPermissionQuietly(username, AppMConstants.Permissions.WEB_APP_PUBLISH);
    }

    public static boolean jsFunction_hasPublishPermission(Context cx, Scriptable thisObj,
                                                          Object[] args,
                                                          Function funObj) {
        APIProvider provider = getAPIProvider(thisObj);
        if (provider instanceof UserAwareAPIProvider) {
            try {
                ((UserAwareAPIProvider) provider).checkPublishPermission();
                return true;
            } catch (AppManagementException e) {
                return false;
            }
        }
        return false;
    }

    public static void jsFunction_loadRegistryOfTenant(Context cx, Scriptable thisObj,
            Object[] args, Function funObj) {
        String tenantDomain = args[0].toString();
        if (tenantDomain != null
                && !org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME
                .equals(tenantDomain)) {
            try {
                int tenantId = ServiceReferenceHolder.getInstance().getRealmService().
                        getTenantManager().getTenantId(tenantDomain);
                AppManagerUtil.loadTenantRegistry(tenantId);
            } catch (org.wso2.carbon.user.api.UserStoreException | AppManagementException e) {
                log.error(
                        "Could not load tenant registry. Error while getting tenant id from tenant domain "
                                + tenantDomain);
            }
        }

    }

    public static NativeArray jsFunction_getLifeCycleEvents(Context cx, Scriptable thisObj,
                                                            Object[] args,
                                                            Function funObj)
            throws AppManagementException {
        NativeArray lifeCycles = new NativeArray(0);
        if (args == null) {
            handleException("Invalid input parameters.");
        }
        NativeObject apiData = (NativeObject) args[0];
        String provider = (String) apiData.get("provider", apiData);
        String name = (String) apiData.get("name", apiData);
        String version = (String) apiData.get("version", apiData);
        APIIdentifier apiId = new APIIdentifier(provider, name, version);
        APIProvider apiProvider = getAPIProvider(thisObj);
        try {
            List<LifeCycleEvent> lifeCycleEvents = apiProvider.getLifeCycleEvents(apiId);
            int i = 0;
            if (lifeCycleEvents != null) {
                for (LifeCycleEvent lcEvent : lifeCycleEvents) {
                    NativeObject event = new NativeObject();
                    event.put("username", event, AppManagerUtil.replaceEmailDomainBack(checkValue(lcEvent.getUserId())));
                    event.put("newStatus", event, lcEvent.getNewStatus() != null ? lcEvent.getNewStatus().toString() : "");
                    event.put("oldStatus", event, lcEvent.getOldStatus() != null ? lcEvent.getOldStatus().toString() : "");

                    event.put("date", event, checkValue(Long.valueOf(lcEvent.getDate().getTime()).toString()));
                    lifeCycles.put(i, lifeCycles, event);
                    i++;
                }
            }
        } catch (AppManagementException e) {
            log.error("Error from registry while checking the input context is already exist", e);
        }
        return lifeCycles;
    }

    private static class APISubscription {
        private String name;
        private long count;
        private String version;
        private String uuid;
    }

    /**
     * Remove a given application
     *
     * @param context Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return true if success else false
     * @throws org.wso2.carbon.appmgt.api.AppManagementException
     */
    public static boolean jsFunction_deleteApp(Context context, Scriptable thisObj,
                                               Object[] args,
                                               Function funObj) throws AppManagementException {
        if (args == null || args.length != 4) {
            handleException("Invalid number of input parameters.");
        }
        if (args[0] == null || args[2] == null) {
            handleException("Error while deleting application. The required parameters are null.");
        }
        boolean isAppDeleted = false;

        NativeJavaObject appIdentifierNativeJavaObject = (NativeJavaObject) args[0];
        APIIdentifier apiIdentifier = (APIIdentifier) appIdentifierNativeJavaObject.unwrap();
        String username = (String) args[1];
        username = AppManagerUtil.replaceEmailDomain(username);
        NativeJavaObject ssoProviderNativeJavaObject = (NativeJavaObject) args[2];
        SSOProvider ssoProvider = (SSOProvider) ssoProviderNativeJavaObject.unwrap();

        boolean isTenantFlowStarted = false;
        String authorizedAdminCookie = (String) args[3];
        try {
            String tenantDomain = MultitenantUtils.getTenantDomain(AppManagerUtil.replaceEmailDomainBack(username));
            if (tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
            }
            APIProvider appProvider = getAPIProvider(thisObj);
           // isAppDeleted = appProvider.deleteApp(apiIdentifier, ssoProvider, authorizedAdminCookie);
        } finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
        return isAppDeleted;
    }

    public static boolean jsFunction_isAPIOlderVersionExist(Context cx, Scriptable thisObj,
                                                            Object[] args, Function funObj)
            throws AppManagementException {
        boolean apiOlderVersionExist = false;
        if (args==null ||args.length == 0) {
            handleException("Invalid number of input parameters.");
        }

        NativeObject apiData = (NativeObject) args[0];
        String provider = (String) apiData.get("provider", apiData);
        provider= AppManagerUtil.replaceEmailDomain(provider);
        String name = (String) apiData.get("name", apiData);
        String currentVersion = (String) apiData.get("version", apiData);
        boolean isTenantFlowStarted = false;
        try {
            String tenantDomain = MultitenantUtils.getTenantDomain(AppManagerUtil.replaceEmailDomainBack(provider));
            if(tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
            }

            APIProvider apiProvider = getAPIProvider(thisObj);
            Set<String> versions = apiProvider.getAPIVersions(provider, name);
            APIVersionStringComparator comparator = new APIVersionStringComparator();
            for (String version : versions) {
                if (comparator.compare(version, currentVersion) < 0) {
                    apiOlderVersionExist = true;
                    break;
                }
            }
        } finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
        return apiOlderVersionExist;
    }

    public static String jsFunction_isURLValid(Context cx, Scriptable thisObj,
                                               Object[] args, Function funObj)
            throws AppManagementException {
        String response = "";
        if (args == null || !isStringValues(args)) {
            handleException("Invalid input parameters.");
        }
        String urlVal = (String) args[1];
        String type = (String) args[0];
        if (urlVal != null && !urlVal.equals("")) {
            try {
                if (type != null && type.equals("wsdl")) {
                    validateWsdl(urlVal);
                } else {
                    URL url = new URL(urlVal);
                    URLConnection conn = url.openConnection();
                    conn.connect();
                }
                response = "success";
            } catch (MalformedURLException e) {
                response = "malformed";
            } catch (UnknownHostException e) {
                response = "unknown";
            } catch (ConnectException e) {
                response = "Cannot establish connection to the provided address";
            } catch (SSLHandshakeException e) {
                response = "ssl_error";
            } catch (Exception e) {
                response = e.getMessage();
            }
        }
        return response;

    }

    private boolean resourceMethodMatches(String[] resourceMethod1,
                                          String[] resourceMethod2) {
        for (String m1 : resourceMethod1) {
            for (String m2 : resourceMethod2) {
                if (m1.equals(m2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void validateWsdl(String url) throws AppManagementException {
        try {
            URL wsdl = new URL(url);
            BufferedReader in = new BufferedReader(new InputStreamReader(wsdl.openStream()));
            String inputLine;
            boolean isWsdl2 = false;
            boolean isWsdl10 = false;
            StringBuilder urlContent = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                String wsdl2NameSpace = "http://www.w3.org/ns/wsdl";
                String wsdl10NameSpace = "http://schemas.xmlsoap.org/wsdl/";
                urlContent.append(inputLine);
                isWsdl2 = urlContent.indexOf(wsdl2NameSpace) > 0;
                isWsdl10 = urlContent.indexOf(wsdl10NameSpace) > 0;
            }
            in.close();
            if (isWsdl10) {
                javax.wsdl.xml.WSDLReader wsdlReader11 = javax.wsdl.factory.WSDLFactory.newInstance().newWSDLReader();
                wsdlReader11.readWSDL(url);
            } else if (isWsdl2) {
                WSDLReader wsdlReader20 = WSDLFactory.newInstance().newWSDLReader();
                wsdlReader20.readWSDL(url);
            } else {
                handleException("URL is not in format of wsdl1/wsdl2");
            }
        } catch (Exception e) {
            handleException("Error occurred while validating the Wsdl", e);
        }
    }

    private static String getWebContextRoot(String postfixUrl) {
        String webContext = CarbonUtils.getServerConfiguration().getFirstProperty("WebContextRoot");
        if (postfixUrl != null && webContext != null && !webContext.equals("/")) {
            postfixUrl = webContext + postfixUrl;
        }
        return postfixUrl;
    }


    public static NativeArray jsFunction_searchAccessTokens(Context cx, Scriptable thisObj,
                                                            Object[] args,
                                                            Function funObj) throws AppManagementException {
        NativeObject tokenInfo;
        NativeArray tokenInfoArr = new NativeArray(0);
        if (args == null || !isStringValues(args)) {
            handleException("Invalid input parameters.");
        }
        String searchValue = (String) args[0];
        String searchTerm;
        String searchType;
        APIProvider apiProvider = getAPIProvider(thisObj);
        Map<Integer, APIKey> tokenData = null;
        String loggedInUser = ((APIProviderHostObject) thisObj).getUsername();

        if (searchValue.contains(":")) {
            searchTerm = searchValue.split(":")[1];
            searchType = searchValue.split(":")[0];
            if ("*".equals(searchTerm) || searchTerm.startsWith("*")) {
                searchTerm = searchTerm.replaceFirst("\\*", ".*");
            }
            tokenData = apiProvider.searchAccessToken(searchType, searchTerm, loggedInUser);
        } else {
            //Check whether old access token is already available
            if (apiProvider.isApplicationTokenExists(searchValue)) {
                APIKey tokenDetails = apiProvider.getAccessTokenData(searchValue);
                if (tokenDetails.getAccessToken() == null) {
                    throw new AppManagementException("The requested access token is already revoked or No access token available as per requested.");
                }
                tokenData = new HashMap<Integer, APIKey>();
                tokenData.put(0, tokenDetails);
            } else {
                if ("*".equals(searchValue) || searchValue.startsWith("*")) {
                    searchValue = searchValue.replaceFirst("\\*", ".*");
                }
                tokenData = apiProvider.searchAccessToken(null, searchValue, loggedInUser);
            }
        }
        if (tokenData != null && tokenData.size() != 0) {
            for (int i = 0; i < tokenData.size(); i++) {
                tokenInfo = new NativeObject();
                tokenInfo.put("token", tokenInfo, tokenData.get(i).getAccessToken());
                tokenInfo.put("user", tokenInfo, tokenData.get(i).getAuthUser());
                tokenInfo.put("scope", tokenInfo, tokenData.get(i).getTokenScope());
                tokenInfo.put("createTime", tokenInfo, tokenData.get(i).getCreatedDate());
                if (tokenData.get(i).getValidityPeriod() == Long.MAX_VALUE) {
                    tokenInfo.put("validTime", tokenInfo, "Won't Expire");
                } else {
                    tokenInfo.put("validTime", tokenInfo, tokenData.get(i).getValidityPeriod());
                }
                tokenInfo.put("consumerKey", tokenInfo, tokenData.get(i).getConsumerKey());
                tokenInfoArr.put(i, tokenInfoArr, tokenInfo);
            }
        } else {
            throw new AppManagementException("The requested access token is already revoked or No access token available as per requested.");
        }

        return tokenInfoArr;
    }

    public static boolean jsFunction_validateRoles(Context cx,
                                                   Scriptable thisObj, Object[] args,
                                                   Function funObj) {
        if (args == null || args.length==0) {
            return false;
        }

        boolean valid=false;
        String inputRolesSet = (String)args[0];
        String username=  (String) args[1];
        String[] inputRoles=null;
        if (inputRolesSet != null) {
            inputRoles = inputRolesSet.split(",");
        }

        try {
            String[] roles= AppManagerUtil.getRoleNames(username);

            if (roles != null && inputRoles != null) {
                for (String inputRole : inputRoles) {
                    for (String role : roles) {
                        valid= (inputRole.equals(role));
                        if(valid){ //If we found a match for the input role,then no need to process the for loop further
                            break;
                        }
                    }
                    //If the input role doesn't match with any of the role existing in the system
                    if(!valid){
                        return valid;
                    }

                }
                return valid;
            }
        }catch (Exception e) {
            log.error("Error while validating the input roles.",e);
        }

        return valid;
    }

    public static NativeArray jsFunction_getExternalAPIStores(Context cx,
                                                              Scriptable thisObj, Object[] args,
                                                              Function funObj)
            throws AppManagementException {
        Set<AppStore> apistoresList = AppManagerUtil.getExternalAPIStores();
        NativeArray myn = new NativeArray(0);
        if (apistoresList == null) {
            return null;
        } else {
            Iterator it = apistoresList.iterator();
            int i = 0;
            while (it.hasNext()) {
                NativeObject row = new NativeObject();
                Object apistoreObject = it.next();
                AppStore apiStore = (AppStore) apistoreObject;
                row.put("displayName", row, apiStore.getDisplayName());
                row.put("name", row, apiStore.getName());
                row.put("endpoint", row, apiStore.getEndpoint());

                myn.put(i, myn, row);
                i++;

            }
            return myn;
        }

    }

    /**
     * Retrieves custom sequences from registry
     * @param cx
     * @param thisObj
     * @param args
     * @param funObj
     * @return
     * @throws org.wso2.carbon.appmgt.api.AppManagementException
     */
    public static NativeArray jsFunction_getCustomOutSequences(Context cx, Scriptable thisObj,
                                                               Object[] args, Function funObj)
            throws AppManagementException {
        APIProvider apiProvider = getAPIProvider(thisObj);
        List<String> sequenceList = apiProvider.getCustomOutSequences();

        NativeArray myn = new NativeArray(0);
        if (sequenceList == null) {
            return null;
        } else {
            for (int i = 0; i < sequenceList.size(); i++) {
                myn.put(i, myn, sequenceList.get(i));
            }
            return myn;
        }

    }

    /**
     * Retrieves custom sequences from registry
     * @param cx
     * @param thisObj
     * @param args
     * @param funObj
     * @return
     * @throws org.wso2.carbon.appmgt.api.AppManagementException
     */
    public static NativeArray jsFunction_getCustomInSequences(Context cx, Scriptable thisObj,
                                                              Object[] args, Function funObj)
            throws AppManagementException {
        APIProvider apiProvider = getAPIProvider(thisObj);
        List<String> sequenceList = apiProvider.getCustomInSequences();

        NativeArray myn = new NativeArray(0);
        if (sequenceList == null) {
            return null;
        } else {
            for (int i = 0; i < sequenceList.size(); i++) {
                myn.put(i, myn, sequenceList.get(i));
            }
            return myn;
        }

    }

    /**
     * Retrieves TRACKING_CODE sequences from APM_APP Table
     * @param cx
     * @param thisObj
     * @param args
     * @param funObj
     * @return
     * @throws org.wso2.carbon.appmgt.api.AppManagementException
     */
    public static String jsFunction_getTrackingID(Context cx, Scriptable thisObj,
                                                  Object[] args, Function funObj) throws AppManagementException {
        String uuid = (String) args[0];
        APIProvider apiProvider =  getAPIProvider(thisObj);
        return apiProvider.getTrackingID(uuid);
    }

    /**
     * This method returns the endpoint for the webapps
     *
     * @param ctx      Rhino context
     * @param thisObj Scriptable object
     * @param args    Passing arguments
     * @param funObj  Function object
     * @return Native array
     * @throws org.wso2.carbon.appmgt.api.AppManagementException
     *
     */
    public static NativeArray jsFunction_getAppsForTenantDomain(Context ctx, Scriptable thisObj,
                                                           Object[] args,
                                                           Function funObj)
            throws AppManagementException {
        NativeArray myn = new NativeArray(0);
        APIProvider apiProvider = getAPIProvider(thisObj);

        String tenantDomain = null;
        try {
            tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain(true);
            List<WebApp> apiList = apiProvider.getAppsWithEndpoint(tenantDomain);
            if (apiList != null) {
                Iterator it = apiList.iterator();
                int i = 0;
                while (it.hasNext()) {
                    NativeObject row = new NativeObject();
                    Object appObject = it.next();
                    WebApp app = (WebApp) appObject;
                    APIIdentifier apiIdentifier = app.getId();
                    row.put("name", row, apiIdentifier.getApiName());

                    // This WebApp is for read the registry values.
                    WebApp tempApp = apiProvider.getAPI(apiIdentifier);
                    row.put("version", row, apiIdentifier.getVersion());
                    row.put("endpoint", row, tempApp.getUrl());
                    myn.put(i, myn, row);
                    i++;
                }
            }
        } catch (AppManagementException e) {
            handleException("Error occurred while getting the application endpoints for the tenant domain "
                    + tenantDomain, e);
        }
        return myn;
    }

    public static NativeArray jsFunction_getAppsByPopularity(Context ctx, Scriptable hostObj, Object[] args,
                                                             Function funObj) throws AppManagementException {
        List<AppHitsStatsDTO> appStatsList = null;
        NativeArray popularApps = new NativeArray(0);
        APIProvider apiProvider = getAPIProvider(hostObj);
        if (!AppManagerUtil.isUIActivityDASPublishEnabled()) {
            return popularApps;
        }
        if (args.length != 3) {
            handleException("Invalid number of parameters!");
        }
        String providerName = (String) args[0];
        String fromDate = (String) args[1];
        String toDate = (String) args[2];

        boolean isTenantFlowStarted = false;

        try {
            String tenantDomain = MultitenantUtils.getTenantDomain(
                    AppManagerUtil.replaceEmailDomainBack(providerName));
            if(tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(
                    tenantDomain)) {
                isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(
                        tenantDomain, true);
            }
            int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
            AppUsageStatisticsService appUsageStatisticsService = new
                    AppUsageStatisticsService(((APIProviderHostObject) hostObj).getUsername());
            appStatsList = appUsageStatisticsService.getAppHitsOverTime(fromDate, toDate, tenantId);
        } catch (AppUsageQueryServiceClientException e) {
            handleException("Error occurred while invoking APPUsageStatisticsClient " +
                        "for ProviderAPPUsage", e);
        }

        if (appStatsList != null) {
            for (int i = 0; i < appStatsList.size(); i++) {
                NativeObject row = new NativeObject();
                Object usageObject = appStatsList.get(i);
                AppHitsStatsDTO usage = (AppHitsStatsDTO) usageObject;
                row.put("AppName", row, usage.getAppName());
                row.put("Context", row, usage.getContext());
                row.put("TotalHits", row, usage.getTotalHitCount());
                List<UserHitsPerAppDTO> userHits = usage.getUserHitsList();
                if (userHits != null) {
                    NativeArray userHitsArray = new NativeArray(userHits.size());
                    for (int j = 0; j < userHits.size(); j++) {
                        NativeObject userHitRow = new NativeObject();
                        Object userHitsObject = userHits.get(j);
                        UserHitsPerAppDTO userHitsPerAppDTO = (UserHitsPerAppDTO) userHitsObject;
                        userHitRow.put("UserName", userHitRow, userHitsPerAppDTO.getUserName());
                        userHitRow.put("Hits", userHitRow, userHitsPerAppDTO.getUserHitsCount());
                        userHitsArray.put(j, userHitsArray, userHitRow);
                    }
                    row.put("UserHits", row, userHitsArray);
                }
                popularApps.put(i, popularApps, row);
            }
        }
        return popularApps;
    }

    /**
     * This methods update(add/remove) the external app stores for given web app
     * @param cx
     * @param thisObj
     * @param args
     * @param funObj
     * @throws AppManagementException
     */
    public static void jsFunction_updateExternalAppStores(Context cx, Scriptable thisObj, Object[] args,
                                                          Function funObj)
            throws AppManagementException {
        if (args == null || args.length != 4) {
            handleException("Invalid number of parameters to the updateExternalAPPStores method,Expected number of" +
                    "parameters " + 4);
        }

        if (!(args[3] instanceof NativeArray)) {
            handleException("Invalid input parameter, 4th parameter  should be a instance of NativeArray");
        }
        String provider = (String) args[0];
        if (provider != null) {
            provider = AppManagerUtil.replaceEmailDomain(provider);
        }
        String name = (String) args[1];
        String version = (String) args[2];
        //Getting selected external App stores from UI and publish app to them.
        NativeArray externalAppStores = (NativeArray) args[3];

        if (log.isDebugEnabled()) {
            String msg = String.format("Update external stores  for web app ->" +
                    " app provider : %s, app name :%s, app version : %s", provider, name, version);
            log.debug(msg);
        }

        boolean isTenantFlowStarted = false;
        try {
            String tenantDomain = MultitenantUtils.getTenantDomain(AppManagerUtil.replaceEmailDomainBack(provider));
            if (tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
            }

            APIProvider appProvider = getAPIProvider(thisObj);
            APIIdentifier identifier = new APIIdentifier(provider, name, version);
            WebApp webApp = appProvider.getAPI(identifier);
            int tenantId = ServiceReferenceHolder.getInstance().getRealmService().
                    getTenantManager().getTenantId(tenantDomain);
            //Check if no external AppStore selected from UI
            if (externalAppStores != null) {
                Set<AppStore> inputStores = new HashSet<AppStore>();
                for (Object store : externalAppStores) {
                    inputStores.add(AppManagerUtil.getExternalAppStore((String) store, tenantId));
                }
                appProvider.updateAppsInExternalAppStores(webApp, inputStores);
            }
        } catch (UserStoreException e) {
            handleException("Error while updating external app stores", e);
        } finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
    }

    /**
     * This method returns the published and unpublished external stores.
     * @param cx
     * @param thisObj
     * @param args
     * @param funObj
     * @return
     * @throws AppManagementException
     */
    public static NativeArray jsFunction_getExternalAppStoresList(Context cx, Scriptable thisObj, Object[] args,
                                                                  Function funObj)
            throws AppManagementException {

        if (args == null || args.length != 3) {
            handleException("Invalid number of parameters to the getExternalAPPStoresList method,Expected number of" +
                    "parameters : " + 3);
        }

        if (!isStringValues(args)) {
            handleException("Input parameters are not type of String");
        }

        String provider = (String) args[0];
        String appName = (String) args[1];
        String appVersion = (String) args[2];

        if (log.isDebugEnabled()) {
            String msg = String.format("Getting external store details for web app ->" +
                    " app provider : %s, app name :%s, app version : %s", provider, appName, appVersion);
            log.debug(msg);
        }

        APIProvider appProvider = getAPIProvider(thisObj);
        APIIdentifier identifier = new APIIdentifier(provider, appName, appVersion);
        Set<AppStore> storesSet = appProvider.getExternalAppStores(identifier);
        NativeArray appStoresArray = new NativeArray(0);

        if (storesSet != null && storesSet.size() != 0) {
            int i = 0;
            for (AppStore store : storesSet) {
                NativeObject storeObject = new NativeObject();
                storeObject.put("name", storeObject, store.getName());
                storeObject.put("displayName", storeObject, store.getDisplayName());
                storeObject.put("published", storeObject, store.isPublished());
                appStoresArray.put(i, appStoresArray, storeObject);
                i++;
            }
        }
        return appStoresArray;
    }

    /**
     * Get default version of a WebApp by Application Name and Provider and filtered by lifecycle state (isPublished).
     *
     * @param cx
     * @param thisObj
     * @param args
     * @param funObj
     * @return Default version of the WebApp relevant to given Name and Provider
     * @throws AppManagementException
     */
    public static String jsFunction_getDefaultVersion(Context cx, Scriptable thisObj, Object[] args, Function funObj)
            throws AppManagementException {
        NativeArray myn = new NativeArray(0);
        if (args == null || args.length != 3) {
            handleException("Invalid input parameters. Expecting Name, Provider and Publish State.");
        }
        String appName = (String) args[0];
        String provider = (String) args[1];
        AppDefaultVersion appStatus = null;

        try {
            appStatus = AppDefaultVersion.valueOf((String) args[2]);
        } catch (IllegalArgumentException e) {
            handleException(String.format("There is no value with name '%s' in Enum %s", (String) args[2],
                                          AppDefaultVersion.class.getName()
            ));
        }

        APIProvider apiProvider = getAPIProvider(thisObj);
        return apiProvider.getDefaultVersion(appName, provider, appStatus);
    }

    /**
     * Check if the given WebApp is the default version.
     *
     * @param cx
     * @param thisObj
     * @param args
     * @param funObj
     * @return true if given app is the default version
     * @throws AppManagementException
     */
    public static boolean jsFunction_isDefaultVersion(Context cx, Scriptable thisObj, Object[] args, Function funObj)
            throws AppManagementException {
        NativeArray myn = new NativeArray(0);
        if (args == null || args.length != 1) {
            handleException("Invalid input parameters. Expecting APIIdentifier.");
        }
        NativeJavaObject appIdentifierNativeJavaObject = (NativeJavaObject) args[0];
        APIIdentifier apiIdentifier = (APIIdentifier) appIdentifierNativeJavaObject.unwrap();
        APIProvider apiProvider = getAPIProvider(thisObj);
        return apiProvider.isDefaultVersion(apiIdentifier);
    }

    /**
     * Check if the WebApp has more versions or not.
     *
     * @param cx
     * @param thisObj
     * @param args
     * @param funObj
     * @return
     * @throws AppManagementException
     */
    public static boolean jsFunction_hasMoreVersions(Context cx, Scriptable thisObj, Object[] args, Function funObj)
            throws AppManagementException {
        NativeArray myn = new NativeArray(0);
        if (args == null || args.length != 1) {
            handleException("Invalid input parameters. Expecting APIIdentifier.");
        }
        NativeJavaObject appIdentifierNativeJavaObject = (NativeJavaObject) args[0];
        APIIdentifier apiIdentifier = (APIIdentifier) appIdentifierNativeJavaObject.unwrap();
        APIProvider apiProvider = getAPIProvider(thisObj);
        return apiProvider.hasMoreVersions(apiIdentifier);
    }

    /**
     * Get WebApp details by UUID.
     *
     * @param cx
     * @param thisObj
     * @param args
     * @param funObj
     * @return Asset basic details
     * @throws AppManagementException
     */
    public static NativeObject jsFunction_getAppDetailsFromUUID(Context cx, Scriptable thisObj, Object[] args,
                                                               Function funObj)
            throws AppManagementException {
        NativeObject webAppNativeObj = new NativeObject();
        if (args == null || args.length != 1) {
            handleException("Invalid input parameters. Expecting UUID.");
        }
        String uuid = (String) args[0];
        APIProvider apiProvider = getAPIProvider(thisObj);
        WebApp api = apiProvider.getAppDetailsFromUUID(uuid);

        if (api != null) {
            webAppNativeObj.put("name", webAppNativeObj, api.getId().getApiName());
            webAppNativeObj.put("provider", webAppNativeObj, api.getId().getProviderName());
            webAppNativeObj.put("version", webAppNativeObj, api.getId().getVersion());
            webAppNativeObj.put("context", webAppNativeObj, api.getContext());
        } else {
            handleException("Cannot find the requested WebApp related to UUID - " + uuid);
        }
        return webAppNativeObj;
    }

    /**
     * Returns the current subscription configuration defined in app-manager.xml.
     * @param cx
     * @param thisObj
     * @param args
     * @param funObj
     * @return Subscription Configuration
     * @throws AppManagementException
     */
    public static NativeObject jsFunction_getSubscriptionConfiguration(Context cx, Scriptable thisObj, Object[] args,
                                                                       Function funObj) throws AppManagementException {
        Map<String, Boolean> subscriptionConfigurationData = HostObjectUtils.getSubscriptionConfiguration();
        NativeObject subscriptionConfiguration = new NativeObject();
        for (Map.Entry<String, Boolean> entry : subscriptionConfigurationData.entrySet()) {
            subscriptionConfiguration.put(entry.getKey(), subscriptionConfiguration, entry.getValue().booleanValue());
        }
        return subscriptionConfiguration;
    }

    public static NativeObject jsFunction_getDefaultThumbnail(Context cx, Scriptable thisObj, Object[] args,
                                                              Function funObj) throws AppManagementException {
        if (args == null || args.length != 1) {
            throw new AppManagementException("Invalid number of arguments. Arguments length should be one.");
        }
        if (!(args[0] instanceof String)) {
            throw new AppManagementException("Invalid argument type. App name should be a String.");
        }
        String appName = (String) args[0];

        Map<String, String> defaultThumbnailData;
        try {
            defaultThumbnailData = HostObjectUtils.getDefaultThumbnail(appName);
        } catch (IllegalArgumentException e) {
            throw new AppManagementException("App name cannot be null or empty string.", e);
        }

        NativeObject defaultThumbnail = new NativeObject();
        for (Map.Entry<String, String> entry : defaultThumbnailData.entrySet()) {
            defaultThumbnail.put(entry.getKey(), defaultThumbnail, entry.getValue());
        }
        return defaultThumbnail;
    }

	/**
     * Returns the enabled asset type list in app-manager.xml
     *
     * @param cx
     * @param thisObj
     * @param args
     * @param funObj
     * @return
     * @throws AppManagementException
     */
    public static NativeArray jsFunction_getEnabledAssetTypeList(Context cx, Scriptable thisObj,
                                                                 Object[] args, Function funObj)
            throws AppManagementException {
        NativeArray availableAssetTypes = new NativeArray(0);
        List<String> typeList = HostObjectUtils.getEnabledAssetTypes();
        for (int i = 0; i < typeList.size(); i++) {
            availableAssetTypes.put(i, availableAssetTypes, typeList.get(i));
        }
        return availableAssetTypes;
    }

    /**
     * Returns asset type enabled or not in app-manager.xml
     *
     * @param cx
     * @param thisObj
     * @param args
     * @param funObj
     * @return
     * @throws AppManagementException
     */
    public static boolean jsFunction_isAssetTypeEnabled(Context cx, Scriptable thisObj,
                                                        Object[] args, Function funObj)
            throws AppManagementException {
        if (args == null || args.length != 1) {
            throw new AppManagementException(
                    "Invalid number of arguments. Arguments length should be one.");
        }
        if (!(args[0] instanceof String)) {
            throw new AppManagementException("Invalid argument type. App name should be a String.");
        }
        String assetType = (String) args[0];
        List<String> typeList = HostObjectUtils.getEnabledAssetTypes();

        for (String type : typeList) {
            if (assetType.equals(type)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns binary file storage location configured in app-manager.xml
     *
     * @param cx
     * @param thisObj
     * @param args
     * @param funObj
     * @return file storage location
     * @throws AppManagementException
     */
    public static String jsFunction_getBinaryFileStorage(Context cx, Scriptable thisObj, Object[] args,
                                                         Function funObj) throws AppManagementException {
        return HostObjectUtils.getBinaryStorageConfiguration();
    }

    /**
     * Is Service Provider Create is enabled for skip gateway apps
     * @param cx
     * @param thisObj
     * @param args
     * @param funObj
     * @return
     * @throws AppManagementException
     */
    public static boolean jsFunction_isSPCreateEnabledForSkipGatewayApps(Context cx, Scriptable thisObj, Object[] args,
                                                                         Function funObj) throws AppManagementException{
        return HostObjectUtils.isServiceProviderCreateEnabledForSkipGatewayApp();
    }

    /**
     * Remove mobile application binary files
     * @param cx
     * @param thisObj
     * @param args
     * @param funObj
     * @throws AppManagementException
     */
    public static void jsFunction_removeBinaryFilesFromStorage(Context cx, Scriptable thisObj, Object[] args,
                                                         Function funObj) throws AppManagementException {
        if (args == null || args.length != 1) {
            throw new AppManagementException(
                    "Invalid number of arguments. Arguments length should be one.");
        }
        if (!(args[0] instanceof NativeArray)) {
            throw new AppManagementException("Invalid argument type. App name should be a String.");
        }
        APIProvider apiProvider = getAPIProvider(thisObj);
        NativeArray fileNames = (NativeArray) args[0];
        for (int i = 0; i < fileNames.getLength(); i++) {
            apiProvider.removeBinaryFromStorage(AppManagerUtil.resolvePath(HostObjectUtils.getBinaryStorageConfiguration(),
                    fileNames.get(i).toString()));
        }
    }


    /**
     * Get Gateway endpoint url
     *
     * @param cx
     * @param thisObj
     * @param args
     * @param funObj
     * @return Gateway endpoint url
     * @throws AppManagementException
     */
    public static String jsFunction_getGatewayEndpoint(Context cx, Scriptable thisObj, Object[] args,
                                                       Function funObj) throws AppManagementException {
        APIProvider provider = getAPIProvider(thisObj);
        return provider.getGatewayEndpoint();
    }

    /**
     * Returns the generated Issuer name
     *
     * @param cx
     * @param thisObj
     * @param args
     * @param funObj
     * @return
     * @throws AppManagementException
     */
    public static String jsFunction_populateIssuerName(Context cx, Scriptable thisObj, Object[] args,
                                                       Function funObj) throws AppManagementException {
        if (args == null || args.length != 2) {
            throw new AppManagementException(
                    "Invalid number of arguments. Arguments length should be one.");
        }

        String appName = (String) args[0];
        String version = (String) args[1];
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain(true);

        String saml2SsoIssuer;
        if (!"carbon.super".equalsIgnoreCase(tenantDomain)) {
            saml2SsoIssuer = appName + "-" + tenantDomain + "-" + version;
        } else {
            saml2SsoIssuer = appName + "-" + version;
        }
        return saml2SsoIssuer;
    }

    public static String jsFunction_getAscUrl(Context cx, Scriptable thisObj, Object[] args,
                                                   Function funObj) throws AppManagementException {
        if (args == null || args.length != 3) {
            throw new AppManagementException(
                    "Invalid number of arguments. Arguments length should be one.");
        }

        String version = (String) args[0];
        String context = (String) args[1];
        String transport = (String) args[2];

        APIIdentifier appIdentifier = new APIIdentifier(null, null, version);
        WebApp webApp = new WebApp(appIdentifier);
        webApp.setTransports(transport);
        webApp.setContext(context);
        String acsUrl = SSOConfiguratorUtil.getACSURL(webApp);
        return acsUrl;
    }
}





