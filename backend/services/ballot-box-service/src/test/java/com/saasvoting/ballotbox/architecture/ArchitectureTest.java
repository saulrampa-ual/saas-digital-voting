package com.saasvoting.ballotbox.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static final String ROOT = "com.saasvoting.ballotbox";

    @Test
    void domainDoesNotDependOnSpringOrInfrastructure() {
        JavaClasses classes = new ClassFileImporter().importPackages(ROOT);
        ArchRule rule = noClasses().that().resideInAPackage(ROOT + ".domain..").should()
                .dependOnClassesThat().resideInAnyPackage("org.springframework..", ROOT + ".infrastructure..");

        rule.check(classes);
    }

    @Test
    void serviceDoesNotImportAnotherService() {
        JavaClasses classes = new ClassFileImporter().importPackages(ROOT);
        ArchRule rule = noClasses().that().resideInAPackage(ROOT + "..").should()
                .dependOnClassesThat().resideInAnyPackage("com.saasvoting.identity..", "com.saasvoting.election..",
                        "com.saasvoting.voteraccess..", "com.saasvoting.gateway..");

        rule.check(classes);
    }
}
