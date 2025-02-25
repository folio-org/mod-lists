package org.folio.list;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.folio.list.context.TestcontainerCallbackExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({ "test", "db-test" })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(TestcontainerCallbackExtension.class)
class ModListApplicationTest {

  @Test
  void shouldAnswerWithTrueToMakeSonarHappy() {
    // Sonarqube doesn't think this class tests anything, when in fact it does verify that the application can start
    // up with minimum config without problems. This dummy test method is just here to make Sonar happy.
    assertTrue(true);
  }
}
