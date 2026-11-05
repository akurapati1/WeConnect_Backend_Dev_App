package com.abkurapati.weconnect.user_service.user;

import com.abkurapati.weconnect.user_service.common.ApiExceptions.ConflictException;
import com.abkurapati.weconnect.user_service.common.ApiExceptions.NotFoundException;
import com.abkurapati.weconnect.user_service.user.UserDtos.RegisterRequest;
import com.abkurapati.weconnect.user_service.user.UserDtos.UpdateRequest;
import com.abkurapati.weconnect.user_service.user.UserDtos.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A user with email " + email + " already exists");
        }
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setFirstName(req.firstName().trim());
        u.setLastName(req.lastName().trim());
        u.setHeadline(req.headline());
        u.setLocation(req.location());
        u.setAbout(req.about());
        return UserResponse.from(users.save(u));
    }

    public UserResponse get(Long id) {
        return UserResponse.from(require(id));
    }

    public Page<UserResponse> search(String query, Pageable pageable) {
        Page<User> page = (query == null || query.isBlank()) ? users.findAll(pageable) : users.search(query.trim(), pageable);
        return page.map(UserResponse::from);
    }

    @Transactional
    public UserResponse update(Long id, UpdateRequest req) {
        User u = require(id);
        if (req.firstName() != null) u.setFirstName(req.firstName().trim());
        if (req.lastName() != null) u.setLastName(req.lastName().trim());
        if (req.headline() != null) u.setHeadline(req.headline());
        if (req.location() != null) u.setLocation(req.location());
        if (req.about() != null) u.setAbout(req.about());
        return UserResponse.from(u);
    }

    @Transactional
    public void delete(Long id) {
        users.delete(require(id));
    }

    User require(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }
}
