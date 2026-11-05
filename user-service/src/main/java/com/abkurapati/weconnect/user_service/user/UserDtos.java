package com.abkurapati.weconnect.user_service.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class UserDtos {

    private UserDtos() {}

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72, message = "password must be 8-72 characters") String password,
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @Size(max = 200) String headline,
            @Size(max = 120) String location,
            @Size(max = 2000) String about) {}

    /** Partial update: only non-null fields are applied. */
    public record UpdateRequest(
            @Size(min = 1, max = 80) String firstName,
            @Size(min = 1, max = 80) String lastName,
            @Size(max = 200) String headline,
            @Size(max = 120) String location,
            @Size(max = 2000) String about) {}

    public record UserResponse(Long id, String email, String firstName, String lastName, String headline,
                               String location, String about, Instant createdAt) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getHeadline(),
                    u.getLocation(), u.getAbout(), u.getCreatedAt());
        }
    }
}
