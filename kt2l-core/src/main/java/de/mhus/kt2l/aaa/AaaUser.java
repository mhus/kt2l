/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.aaa;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;
import java.util.Set;

@Builder
@ToString
@EqualsAndHashCode
public class AaaUser {

    @Getter
    private String userId;
    @Getter
    private String displayName;
    private String encodedPassword;
    @Getter
    private String email;
    @Getter
    private String imageUrl;
    @Getter
    private Collection<String> roles;
    @Getter
    private String provider;
    @Getter
    private String providerId;

//    public static AaaUser copy(AaaUser user) {
//        return new AaaUser(
//                user.getUserId(),
//                user.getDisplayName() == null ? user.getUserId() : user.getDisplayName(),
//                user.getEncodedPassword(),
//                user.getEmail(),
//                user.getImageUrl(),
//                user.getRoles(),
//                user.getProvider(),
//                user.getProviderId());
//    }

    public static AaaUser copyNice(AaaUser user) {
        return new AaaUser(
                user.getUserId(),
                user.getDisplayName() == null ? user.getUserId() : user.getDisplayName(),
                user.getEncodedPassword(),
                user.getEmail(),
                user.getImageUrl(),
                user.getRoles(),
                user.getProvider(),
                user.getProviderId());
    }
    public static AaaUser update(AaaUser current, AaaUser update) {

        var roles = current.getRoles();
        var password = current.getEncodedPassword();
        var email = current.getEmail();
        var imageUrl = current.getImageUrl();
        var displayName = current.getDisplayName();

        // update only if not null
        if (update.getRoles() != null)
            roles = update.getRoles();
        // not allowed to change password only for admins
        if (update.getEncodedPassword() != null && !password.equals(update.getEncodedPassword())) {
            if (!SecurityUtils.hasUserRoles(Set.of("admin")))
                throw new IllegalArgumentException("Not allowed to change password");
            password = update.getEncodedPassword();
        }
        if (update.getEmail() != null)
            email = update.getEmail();
        if (update.getImageUrl() != null)
            imageUrl = update.getImageUrl();
        if (update.getDisplayName() != null)
            displayName = update.getDisplayName();

        // create new user object
        return new AaaUser(
                current.getUserId(), // read only
                displayName,
                password,
                email,
                imageUrl,
                roles,
                current.getProvider(), // read only
                current.getProviderId() // read only
        );

    }

//    public String getPassword() {
//        return password;
//    }

    public boolean validatePlainPassword(String tryPassword) {
        return encodedPassword.equals(tryPassword);
    }

    public void setEncodedPassword(String oldPassword, String newEncodedPassword) {
        if (!validatePlainPassword(oldPassword))
            throw new IllegalArgumentException("Old password is not correct");
        encodedPassword = newEncodedPassword;
    }

    public String getEncodedPassword() {
        return encodedPassword;
    }
}
