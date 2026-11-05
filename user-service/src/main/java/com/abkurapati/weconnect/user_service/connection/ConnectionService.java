package com.abkurapati.weconnect.user_service.connection;

import com.abkurapati.weconnect.user_service.common.ApiExceptions.*;
import com.abkurapati.weconnect.user_service.connection.Connection.Status;
import com.abkurapati.weconnect.user_service.connection.ConnectionDtos.ConnectionResponse;
import com.abkurapati.weconnect.user_service.user.User;
import com.abkurapati.weconnect.user_service.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** LinkedIn-style connection workflow: send request -> receiver accepts or rejects. */
@Service
@Transactional(readOnly = true)
public class ConnectionService {

    private final ConnectionRepository connections;
    private final UserRepository users;

    public ConnectionService(ConnectionRepository connections, UserRepository users) {
        this.connections = connections;
        this.users = users;
    }

    @Transactional
    public ConnectionResponse sendRequest(Long requesterId, Long targetId) {
        if (requesterId.equals(targetId)) {
            throw new BadRequestException("You cannot connect with yourself");
        }
        User requester = user(requesterId);
        User receiver = user(targetId);
        connections.findBetween(requesterId, targetId).ifPresent(existing -> {
            throw new ConflictException("A connection (" + existing.getStatus() + ") already exists between these users");
        });
        Connection c = new Connection();
        c.setRequester(requester);
        c.setReceiver(receiver);
        return ConnectionResponse.from(connections.save(c));
    }

    @Transactional
    public ConnectionResponse accept(Long connectionId, Long actingUserId) {
        return respond(connectionId, actingUserId, Status.ACCEPTED);
    }

    @Transactional
    public ConnectionResponse reject(Long connectionId, Long actingUserId) {
        return respond(connectionId, actingUserId, Status.REJECTED);
    }

    public List<ConnectionResponse> accepted(Long userId) {
        user(userId);
        return connections.findForUser(userId, Status.ACCEPTED).stream().map(ConnectionResponse::from).toList();
    }

    public List<ConnectionResponse> incomingPending(Long userId) {
        user(userId);
        return connections.findIncomingPending(userId).stream().map(ConnectionResponse::from).toList();
    }

    public long count(Long userId) {
        user(userId);
        return connections.countAccepted(userId);
    }

    @Transactional
    public void remove(Long connectionId, Long actingUserId) {
        Connection c = connections.findById(connectionId)
                .orElseThrow(() -> new NotFoundException("Connection " + connectionId + " not found"));
        if (!isParticipant(c, actingUserId)) {
            throw new ForbiddenException("Only a participant can remove this connection");
        }
        connections.delete(c);
    }

    private ConnectionResponse respond(Long connectionId, Long actingUserId, Status newStatus) {
        Connection c = connections.findById(connectionId)
                .orElseThrow(() -> new NotFoundException("Connection " + connectionId + " not found"));
        if (!c.getReceiver().getId().equals(actingUserId)) {
            throw new ForbiddenException("Only the receiver can respond to a connection request");
        }
        if (c.getStatus() != Status.PENDING) {
            throw new ConflictException("Connection request was already " + c.getStatus());
        }
        c.setStatus(newStatus);
        c.setRespondedAt(Instant.now());
        return ConnectionResponse.from(c);
    }

    private static boolean isParticipant(Connection c, Long userId) {
        return c.getRequester().getId().equals(userId) || c.getReceiver().getId().equals(userId);
    }

    private User user(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }
}
