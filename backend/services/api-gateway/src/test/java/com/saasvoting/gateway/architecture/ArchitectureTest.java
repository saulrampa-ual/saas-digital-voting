package com.saasvoting.gateway.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    @Test
    void gatewayDoesNotImportServiceImplementationPackages() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.saasvoting.gateway");
        ArchRule rule = noClasses().that().resideInAPackage("com.saasvoting.gateway..").should()
                .dependOnClassesThat().resideInAnyPackage("com.saasvoting.identity..",
                        "com.saasvoting.election..", "com.saasvoting.voteraccess..", "com.saasvoting.ballotbox..");

        rule.check(classes);
    }
}
