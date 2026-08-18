package com.taskmanager.api.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Freezes the dependency direction the whole codebase is built around: dependencies only
 * ever point inward, towards the domain. If one of these rules fails, the layering has
 * been violated and the fix is architectural, not cosmetic.
 */
@AnalyzeClasses(packages = "com.taskmanager.api")
class OnionArchitectureTest {

    @ArchTest
    static final ArchRule layers_respect_onion_dependency_direction = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("com.taskmanager.api.domain..")
            .layer("Application").definedBy("com.taskmanager.api.application..")
            .layer("Infrastructure").definedBy("com.taskmanager.api.infrastructure..")
            .whereLayer("Domain").mayNotAccessAnyLayer()
            .whereLayer("Application").mayOnlyAccessLayers("Domain")
            .whereLayer("Infrastructure").mayOnlyAccessLayers("Domain", "Application");

    @ArchTest
    static final ArchRule domain_is_free_of_framework_dependencies = noClasses()
            .that().resideInAPackage("com.taskmanager.api.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "jakarta.validation..");

    @ArchTest
    static final ArchRule jpa_entities_stay_inside_the_persistence_package = noClasses()
            .that().resideOutsideOfPackage("com.taskmanager.api.infrastructure.persistence..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.taskmanager.api.infrastructure.persistence.entity..");
}
