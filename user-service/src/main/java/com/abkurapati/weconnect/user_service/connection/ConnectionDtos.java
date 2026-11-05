package com.abkurapati.weconnect.user_service.connection;

import com.abkurapati.weconnect.user_service.user.UserDtos.UserResponse;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class ConnectionDtos {

    private ConnectionDtos() {}

    public record ConnectionRequest(@NotNull Long targetUserId) {}

    public record ConnectionResponse(Long id, UserResponse requester, UserResponse receiver,
                                     Connection.Status status, Instant createdAt, Instant respondedAt) {
        public static ConnectionResponse from(Connection c) {
            return new ConnectionResponse(c.getId(), UserResponse.from(c.getRequester()), UserResponse.from(c.getReceiver()),
                    c.getStatus(), c.getCreatedAt(), c.getRespondedAt());
        }
    }

    public record ConnectionCount(Long userId, long connections) {}
}
