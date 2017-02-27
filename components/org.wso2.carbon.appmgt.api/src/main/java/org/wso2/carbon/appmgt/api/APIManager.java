/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.appmgt.api;

import org.wso2.carbon.appmgt.api.model.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core WebApp management interface which provides functionality related to APIs, WebApp metadata
 * and WebApp subscribers (consumers).
 */
public interface APIManager {

    /**
     * Checks the Availability of given APIIdentifier
     *
     * @param identifier APIIdentifier
     * @return true, if already exists. False, otherwise
     * @throws AppManagementException if failed to get WebApp availability
     */
    public boolean isAPIAvailable(APIIdentifier identifier) throws AppManagementException;

    /**
     * Checks whether the given WebApp context is already registered in the system
     *
     * @param context A String representing an WebApp context
     * @return true if the context already exists and false otherwise
     * @throws AppManagementException if failed to check the context availability
     */
    public boolean isContextExist(String context) throws AppManagementException;

    /**
     * Returns a set of WebApp versions for the given provider and WebApp name
     *
     * @param providerName name of the provider (common)
     * @param apiName      name of the api
     * @return Set of version strings (possibly empty)
     * @throws AppManagementException if failed to get version for api
     */
    public Set<String> getAPIVersions(String providerName, String apiName)
            throws AppManagementException;

    /**
     * Creates a new subscriber. The newly created subscriber id will be set in the given object.
     *
     * @param subscriber The subscriber to be added
     * @throws AppManagementException if failed add subscriber
     */
    public void addSubscriber(Subscriber subscriber) throws AppManagementException;

    /**
     * Updates the details of the given subscriber.
     *
     * @param subscriber The subscriber to be updated
     * @throws AppManagementException if failed to update subscriber
     */
    public void updateSubscriber(Subscriber subscriber) throws AppManagementException;

    /**
     * Returns the subscriber for the given subscriber id.
     *
     * @param subscriberId The subscriber id of the subscriber to be returned
     * @return The looked up subscriber or null if the requested subscriber does not exist
     * @throws AppManagementException if failed to get Subscriber
     */
    public Subscriber getSubscriber(int subscriberId) throws AppManagementException;

    /**
     * Associates the given icon image with the specified path.
     *
     * @param resourcePath a String representing the relative path of a resource.
     * @param icon         to be saved
     * @return a String URL pointing to the image that was added
     * @throws AppManagementException if an error occurs while adding the icon image
     */
    public String addIcon(String resourcePath, Icon icon) throws AppManagementException;

    /**
     * Retrieves the icon image associated with a particular WebApp as a stream.
     *
     * @param identifier ID representing the WebApp
     * @return an Icon containing image content and content type information
     * @throws AppManagementException if an error occurs while retrieving the image
     */
    public Icon getIcon(APIIdentifier identifier) throws AppManagementException;

    /**
     * Cleans up any resources acquired by this APIManager instance. It is recommended
     * to call this method once the APIManager instance is no longer required.
     *
     * @throws AppManagementException if an error occurs while cleaning up
     */
    public void cleanup() throws AppManagementException;


    /**
     * Check whether an application access token is already persist in database.
     *
     * @param accessToken
     * @return
     * @throws AppManagementException
     */
    public boolean isApplicationTokenExists(String accessToken) throws AppManagementException;

    /**
     * Check whether an application access token is already revoked.
     *
     * @param accessToken
     * @return
     * @throws AppManagementException
     */
    public boolean isApplicationTokenRevoked(String accessToken) throws AppManagementException;

    /**
     * Return information related to a specific access token
     *
     * @param accessToken AccessToken
     * @return
     * @throws AppManagementException
     */
    public APIKey getAccessTokenData(String accessToken) throws AppManagementException;
    /**
    /**
     * Return information related to access token by a searchTerm and searchType       *
     *
     *
     * @param searchType
     * @param searchTerm
     * @param loggedInUser
     * @return
     * @throws AppManagementException
     */
    public Map<Integer, APIKey> searchAccessToken(String searchType, String searchTerm, String loggedInUser)
            throws AppManagementException;

    /**
    * Returns a list of pre-defined # {@link org.wso2.carbon.apimgt.api.model.Tier} in the system.
    *
    * @return Set<Tier>
    * @throws AppManagementException if failed to get the predefined tiers
    */
    public Set<Tier> getTiers() throws AppManagementException;



    /**
     * Returns a list of pre-defined # {@link org.wso2.carbon.apimgt.api.model.Tier} in the system.
     *
     * @return Set<Tier>
     * @throws AppManagementException if failed to get the predefined tiers
     */
    public Set<Tier> getTiers(String tenantDomain) throws AppManagementException;

}
