/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.picketlink.extensions.core.rest.interceptors;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;

import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.picketbox.jaxrs.model.AuthenticationResponse;
import org.picketlink.authentication.AuthenticationException;
import org.picketlink.extensions.core.pbox.DefaultPicketBoxIdentity;
import org.picketlink.extensions.core.pbox.PicketBoxIdentity;
import org.picketlink.extensions.core.rest.AccountRegistrationEndpoint;
import org.picketlink.extensions.core.rest.CheckUserNameEndpoint;
import org.picketlink.extensions.core.rest.LogoutEndpoint;
import org.picketlink.extensions.core.rest.SignInEndpoint;
import org.picketlink.extensions.core.rest.UserInfoEndpoint;

/**
 * <p>
 * Implementation of {@link PreProcessInterceptor} that checks the existence of the authentication token before invoking the
 * destination endpoint.
 * </p>
 * <p>
 * If the token is valid, the {@link DefaultPicketBoxIdentity} will restored with the all user information.
 * </p>
 * 
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * 
 */
@ApplicationScoped
@ServerInterceptor
public class SecurityInterceptor implements PreProcessInterceptor {

    private Logger log = Logger.getLogger(SecurityInterceptor.class);
    
    private static final String AUTH_TOKEN_HEADER_NAME = "Auth-Token";
    
    @Inject
    private PicketBoxIdentity identity;

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.resteasy.spi.interception.PreProcessInterceptor#preProcess(org.jboss.resteasy.spi.HttpRequest,
     * org.jboss.resteasy.core.ResourceMethod)
     */
    @Override
    public ServerResponse preProcess(HttpRequest request, ResourceMethod method) throws Failure, WebApplicationException {
        ServerResponse response = null;

        if (requiresAuthentication(method) && !this.identity.isLoggedIn()) {
            boolean isLoggedIn = false;
            String token = getToken(request);

            if (token != null) {
                try {
                    isLoggedIn = identity.restoreSession(token);
                } catch (AuthenticationException e) {
                    log.error("Authentiation Failed:", e);
                }
            }

            if (!isLoggedIn) {
                AuthenticationResponse authcResponse = new AuthenticationResponse();

                authcResponse.setLoggedIn(false);

                response = new ServerResponse();
                response.setEntity(authcResponse);
                response.setStatus(HttpStatus.SC_FORBIDDEN);
            }
        }

        return response;
    }

    /**
     * <p>
     * Retrieve the token from the request, if present.
     * </p>
     * 
     * @param request
     * @return
     */
    private String getToken(HttpRequest request) {
        HttpHeaders httpHeaders = request.getHttpHeaders();
        List<String> tokenHeader = httpHeaders.getRequestHeader(AUTH_TOKEN_HEADER_NAME);
        String token = null;

        if (tokenHeader != null && !tokenHeader.isEmpty()) {
            token = tokenHeader.get(0);
        }
        
        //Check cookies
        if(token == null){
            Map<String,Cookie> cookies = httpHeaders.getCookies();
            if(cookies != null){
                Cookie cookie = cookies.get(AUTH_TOKEN_HEADER_NAME);
                token = cookie.getValue();
            }
        }

        return token;
    }

    /**
     * <p>
     * Checks if the {@link ResourceMethod} requires authentication.
     * </p>
     * 
     * @param method
     * @return
     */
    private boolean requiresAuthentication(ResourceMethod method) {
        Class<?> declaringClass = method.getMethod().getDeclaringClass();

        Class<?>[] arr = new Class[] { SignInEndpoint.class, LogoutEndpoint.class, AccountRegistrationEndpoint.class,
                CheckUserNameEndpoint.class, UserInfoEndpoint.class };

        List<Class<?>> classes = Arrays.asList(arr);

        return classes.contains(declaringClass) == false;
    }
}