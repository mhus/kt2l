package de.mhus.kt2l.aaa;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

@Builder
@ToString
public class AaaUser {

    @Getter
    private String username;
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
