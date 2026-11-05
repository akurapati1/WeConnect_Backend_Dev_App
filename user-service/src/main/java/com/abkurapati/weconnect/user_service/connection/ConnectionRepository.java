package com.abkurapati.weconnect.user_service.connection;

import com.abkurapati.weconnect.user_service.connection.Connection.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    /** The connection between two users in either direction, if any. */
    @Query("""
            select c from Connection c
            where (c.requester.id = :a and c.receiver.id = :b) or (c.requester.id = :b and c.receiver.id = :a)
            """)
    Optional<Connection> findBetween(@Param("a") Long a, @Param("b") Long b);

    @Query("""
            select c from Connection c join fetch c.requester join fetch c.receiver
            where c.status = :status and (c.requester.id = :userId or c.receiver.id = :userId)
            order by c.id desc
            """)
    List<Connection> findForUser(@Param("userId") Long userId, @Param("status") Status status);

    @Query("""
            select c from Connection c join fetch c.requester join fetch c.receiver
            where c.status = 'PENDING' and c.receiver.id = :userId order by c.id desc
            """)
    List<Connection> findIncomingPending(@Param("userId") Long userId);

    @Query("""
            select count(c) from Connection c
            where c.status = 'ACCEPTED' and (c.requester.id = :userId or c.receiver.id = :userId)
            """)
    long countAccepted(@Param("userId") Long userId);
}
