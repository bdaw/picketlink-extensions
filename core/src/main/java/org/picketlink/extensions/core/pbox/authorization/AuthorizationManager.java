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

package org.picketlink.extensions.core.pbox.authorization;

import static org.picketlink.extensions.core.pbox.util.AnnotationUtil.getDeclaredAnnotation;

import javax.enterprise.context.ApplicationScoped;
import javax.interceptor.InvocationContext;

import org.apache.deltaspike.security.api.authorization.annotation.Secures;
import org.picketlink.Identity;
import org.picketlink.extensions.core.pbox.PicketBoxIdentity;

/**
 * <p>
 * Provides all authorization capabilities for applications using PicketBox.
 * </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 *
 */
@ApplicationScoped
public class AuthorizationManager {

    /**
     * <p>
     * Authorization method for the {@link RolesAllowed} annotation.
     * </p>
     *
     * @param ctx
     * @param identity
     * @return
     */
    @Secures
    @RolesAllowed
    public boolean restrictRoles(InvocationContext ctx, PicketBoxIdentity identity) {
        if (!identity.isLoggedIn()) {
            return false;
        }

        String[] restrictedRoles = getRestrictedRoles(ctx);

        for (String restrictedRole : restrictedRoles) {
            if (identity.hasRole(restrictedRole)) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>Checks if the resources protected with the {@link UserLoggedIn} annotation are visible only for authenticated users.</p>
     *
     * @param ctx
     * @param identity
     * @return
     */
    @Secures
    @UserLoggedIn
    public boolean isUserLoggedIn(InvocationContext ctx, Identity identity) {
        return identity.isLoggedIn();
    }

    /**
     * <p>
     * Returns the restricted roles defined by the use of the {@link RolesAllowed} annotation. If the annotation is not
     * present a empty array is returned.
     * </p>
     *
     * @param ctx
     * @return
     */
    private String[] getRestrictedRoles(InvocationContext ctx) {
        RolesAllowed restrictedRolesAnnotation = getDeclaredAnnotation(RolesAllowed.class, ctx);

        if (restrictedRolesAnnotation != null) {
            return restrictedRolesAnnotation.value();
        }

        return new String[] {};
    }

}
