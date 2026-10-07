package com.taskmanager.api.infrastructure.persistence;

import com.taskmanager.api.infrastructure.persistence.entity.TaskJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, UUID>, JpaSpecificationExecutor<TaskJpaEntity> {
}
