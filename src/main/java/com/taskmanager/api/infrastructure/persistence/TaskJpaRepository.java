package com.taskmanager.api.infrastructure.persistence;

import com.taskmanager.api.infrastructure.persistence.entity.TaskJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, UUID> {
}
