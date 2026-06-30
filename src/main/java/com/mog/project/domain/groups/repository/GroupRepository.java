package com.mog.project.domain.groups.repository;

import com.mog.project.domain.groups.entity.Group;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
    boolean existsByInviteCode(String inviteCode);

    Optional<Group> findByInviteCode(String inviteCode);
}
