package com.abkurapati.weconnect.user_service.connection;

import com.abkurapati.weconnect.user_service.connection.ConnectionDtos.ConnectionCount;
import com.abkurapati.weconnect.user_service.connection.ConnectionDtos.ConnectionRequest;
import com.abkurapati.weconnect.user_service.connection.ConnectionDtos.ConnectionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The acting user is supplied in the {@code X-User-Id} header. In the full platform an API gateway would
 * authenticate the caller and inject this header; this service trusts it and must not be exposed directly.
 */
@RestController
@RequestMapping("/api/v1")
public class ConnectionController {

    private static final String USER_HEADER = "X-User-Id";

    private final ConnectionService service;

    public ConnectionController(ConnectionService service) {
        this.service = service;
    }

    @PostMapping("/connections")
    @ResponseStatus(HttpStatus.CREATED)
    public ConnectionResponse send(@RequestHeader(USER_HEADER) Long userId, @Valid @RequestBody ConnectionRequest request) {
        return service.sendRequest(userId, request.targetUserId());
    }

    @PostMapping("/connections/{id}/accept")
    public ConnectionResponse accept(@PathVariable Long id, @RequestHeader(USER_HEADER) Long userId) {
        return service.accept(id, userId);
    }

    @PostMapping("/connections/{id}/reject")
    public ConnectionResponse reject(@PathVariable Long id, @RequestHeader(USER_HEADER) Long userId) {
        return service.reject(id, userId);
    }

    @DeleteMapping("/connections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id, @RequestHeader(USER_HEADER) Long userId) {
        service.remove(id, userId);
    }

    @GetMapping("/users/{userId}/connections")
    public List<ConnectionResponse> accepted(@PathVariable Long userId) {
        return service.accepted(userId);
    }

    @GetMapping("/users/{userId}/connections/pending")
    public List<ConnectionResponse> pending(@PathVariable Long userId) {
        return service.incomingPending(userId);
    }

    @GetMapping("/users/{userId}/connections/count")
    public ConnectionCount count(@PathVariable Long userId) {
        return new ConnectionCount(userId, service.count(userId));
    }
}
