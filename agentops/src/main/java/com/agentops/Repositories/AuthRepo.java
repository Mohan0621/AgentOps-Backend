package com.agentops.Repositories;

import org.springframework.stereotype.Repository;

/**
 * @deprecated Renamed to {@link UserRepository}. Retained as alias for backward compatibility.
 */
@Deprecated
@Repository
public interface AuthRepo extends UserRepository {
}
