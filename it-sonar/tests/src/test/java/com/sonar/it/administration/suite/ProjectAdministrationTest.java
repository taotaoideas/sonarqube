/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.administration.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.services.ProjectDeleteQuery;
import org.sonar.wsclient.services.PropertyQuery;
import org.sonar.wsclient.services.ResourceQuery;

import javax.annotation.Nullable;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.fest.assertions.Assertions.assertThat;

public class ProjectAdministrationTest {

  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  private static final String PROJECT_KEY = "sample";
  private static final String FILE_KEY = "sample:sample.Sample";

  @Before
  public void deleteAnalysisData() throws SQLException {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void should_delete_project_by_web_service() {
    scanSampleWithDate("2012-01-01");

    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(PROJECT_KEY))).isNotNull();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(FILE_KEY))).isNotNull();

    orchestrator.getServer().getAdminWsClient().delete(ProjectDeleteQuery.create(PROJECT_KEY));

    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(PROJECT_KEY))).isNull();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(FILE_KEY))).isNull();
  }

  @Test(expected = ConnectionException.class)
  public void should_delete_only_projects() {
    scanSampleWithDate("2012-01-01");

    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(PROJECT_KEY))).isNotNull();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(FILE_KEY))).isNotNull();

    // it's forbidden to delete only some files
    orchestrator.getServer().getAdminWsClient().delete(ProjectDeleteQuery.create(FILE_KEY));
  }

  @Test(expected = ConnectionException.class)
  public void admin_role_should_be_required_to_delete_project() {
    scanSampleWithDate("2012-01-01");

    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(PROJECT_KEY))).isNotNull();

    // use getWsClient() instead of getAdminWsClient()
    orchestrator.getServer().getWsClient().delete(ProjectDeleteQuery.create(PROJECT_KEY));
  }

  /**
   * Test updated for SONAR-3570
   */
  @Test
  public void test_project_deletion() throws Exception {
    // For an unknown reason, this test fails if the analysis id one with SonarRunner...
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
        .setCleanSonarGoals()
        .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build.setProperty("sonar.projectDate", "2012-01-01"));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-deletion", "/selenium/administration/project-deletion/project-deletion.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void test_project_administration() throws Exception {
    GregorianCalendar today = new GregorianCalendar();

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
        .setCleanSonarGoals()
        .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build.setProperty("sonar.projectDate", (today.get(Calendar.YEAR) - 1) + "-01-01"));
    // The analysis must be run once again to have an history so that it is possible to delete a snapshot
    orchestrator.executeBuild(build.setProperty("sonar.projectDate", (today.get(Calendar.YEAR)) + "-01-01"));

    Selenese selenese = Selenese
        .builder()
        .setHtmlTestsInClasspath("project-administration",
            "/selenium/administration/project-administration/project-exclusions.html",
            "/selenium/administration/project-administration/project-general-exclusions.html",
            "/selenium/administration/project-administration/project-test-exclusions.html",
            "/selenium/administration/project-administration/project-general-test-exclusions.html",
            "/selenium/administration/project-administration/project-links.html",
            "/selenium/administration/project-administration/project-modify-versions.html",
            "/selenium/administration/project-administration/project-rename-current-version.html",
            "/selenium/administration/project-administration/project-history-deletion.html", // SONAR-3206
            "/selenium/administration/project-administration/project-quality-profile.html" // SONAR-3517
        ).build();
    orchestrator.executeSelenese(selenese);
  }

  // SONAR-4203
  @Test
  public void should_delete_version_of_multimodule_project() throws Exception {
    GregorianCalendar today = new GregorianCalendar();
    SonarRunner build = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
        .setProperty("sonar.dynamicAnalysis", "false")
        .setProperty("sonar.projectDate", (today.get(Calendar.YEAR) - 1) + "-01-01");
    orchestrator.executeBuild(build);

    // The analysis must be run once again to have an history so that it is possible
    // to set/delete version on old snapshot
    build.setProperty("sonar.projectDate", today.get(Calendar.YEAR) + "-01-01");
    orchestrator.executeBuild(build);

    // There are 7 modules
    assertThat(count("events where category='Version'")).as("Different number of events").isEqualTo(7);

    Selenese selenese = Selenese
        .builder()
        .setHtmlTestsInClasspath("delete_version_of_multimodule_project",
            "/selenium/administration/project-administration/multimodule-project-modify-version.html"
        ).build();
    orchestrator.executeSelenese(selenese);

    assertThat(count("events where category='Version'")).as("Different number of events").isEqualTo(14);

    selenese = Selenese
        .builder()
        .setHtmlTestsInClasspath("delete_version_of_multimodule_project",
            "/selenium/administration/project-administration/multimodule-project-delete-version.html"
        ).build();
    orchestrator.executeSelenese(selenese);

    assertThat(count("events where category='Version'")).as("Different number of events").isEqualTo(7);
  }

  // SONAR-3326
  @Test
  public void should_display_alerts_correctly_in_history_page() throws Exception {
    // with this configuration, project should have an Orange alert
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/administration/ProjectAdministrationTest/low-alert-thresholds-profile-backup.xml"));
    scanSample("2012-01-01", "alert-profile");
    // with this configuration, project should have a Green alert
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/administration/ProjectAdministrationTest/high-alert-thresholds-profile-backup.xml"));
    scanSample("2012-01-02", "alert-profile");

    Selenese selenese = Selenese
        .builder()
        .setHtmlTestsInClasspath("display-alerts-history-page",
            "/selenium/administration/display-alerts-history-page/should-display-alerts-correctly-history-page.html"
        ).build();
    orchestrator.executeSelenese(selenese);
  }

  // SONAR-1352
  @Test
  public void should_display_period_alert_on_project_dashboard() throws Exception {
    // No alert
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/administration/ProjectAdministrationTest/period-alert-thresholds-profile-backup.xml"));
    scanSample("2012-01-01", "alert-profile");

    // Red alert because lines number has not changed since previous analysis
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/administration/ProjectAdministrationTest/period-alert-thresholds-profile-backup.xml"));
    scanSampleWithProfile("alert-profile");

    Selenese selenese = Selenese
        .builder()
        .setHtmlTestsInClasspath("display-period-alerts",
            "/selenium/administration/display-alerts/should-display-period-alerts-correctly.html"
        ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-3425
   */
  @Test
  public void project_settings() {
    scanSampleWithDate("2012-01-01");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-settings",
        // SONAR-3425
        "/selenium/administration/project-settings/override-global-settings.html",

        "/selenium/administration/project-settings/only-on-project-settings.html"
        ).build();
    orchestrator.executeSelenese(selenese);

    assertThat(orchestrator.getServer().getAdminWsClient().find(PropertyQuery.createForResource("sonar.skippedModules", "sample")).getValue())
        .isEqualTo("my-excluded-module");
  }

  /**
   * SONAR-1608
   */
  @Test
  public void should_bulk_update_project_keys() {
    MavenBuild build = MavenBuild.builder()
        .setPom(ItUtils.locateProjectPom("shared/multi-modules-sample"))
        .addSonarGoal()
        .withDynamicAnalysis(false)
        .build();
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese
        .builder()
        .setHtmlTestsInClasspath("project-bulk-update-keys",
            "/selenium/administration/project-update-keys/bulk-update-impossible-because-duplicate-keys.html",
            "/selenium/administration/project-update-keys/bulk-update-impossible-because-no-input.html",
            "/selenium/administration/project-update-keys/bulk-update-impossible-because-no-match.html",
            "/selenium/administration/project-update-keys/bulk-update-success.html"
        ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-1608
   */
  @Test
  public void should_fine_grain_update_project_keys() {
    MavenBuild build = MavenBuild.builder()
        .setPom(ItUtils.locateProjectPom("shared/multi-modules-sample"))
        .addSonarGoal()
        .withDynamicAnalysis(false)
        .build();
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese
        .builder()
        .setHtmlTestsInClasspath("project-fine-grained-update-keys",
            "/selenium/administration/project-update-keys/fine-grained-update-impossible.html",
            "/selenium/administration/project-update-keys/fine-grained-update-success.html"
        ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-3956
   */
  @Test
  public void manage_permissions() {
    scanSample();

    Selenese selenese = Selenese
        .builder()
        .setHtmlTestsInClasspath("manage-permissions",
            "/selenium/administration/manage_project_roles/change_roles_of_users.html"

            // Ignored while WS doesn't return 'Anyone' group
            // "/selenium/administration/manage_project_roles/change_roles_of_groups.html"
        ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4050
   */
  @Test
  @Ignore
  public void do_not_reset_default_project_roles() {
    scanSample();

    Selenese selenese = Selenese.builder()
        .setHtmlTestsInClasspath("do_not_reset_default_roles_1",
            "/selenium/administration/do_not_reset_default_roles/1_set_project_roles.html"
        ).build();
    orchestrator.executeSelenese(selenese);

    scanSample();

    selenese = Selenese.builder()
        .setHtmlTestsInClasspath("do_not_reset_default_roles_2",
            "/selenium/administration/do_not_reset_default_roles/2_project_roles_are_unchanged.html"
        ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  @Ignore
  public void anonymous_should_have_user_role_to_access_project() {
    scanSample();

    Selenese selenese = Selenese.builder()
        .setHtmlTestsInClasspath("anonymous_should_have_user_role_to_access_project",
            "/selenium/administration/anonymous_should_have_user_role_to_access_project/remove_user_role.html"
        ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4060
   */
  @Test
  public void should_display_module_settings() {
    orchestrator.executeBuild(MavenBuild.create(ItUtils.locateProjectPom("maven/modules-declaration"))
        .setCleanSonarGoals()
        .setProperty("sonar.dynamicAnalysis", "false"));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("module-settings",
        // SONAR-3425
        "/selenium/administration/module-settings/display-module-settings.html"
        ).build();
    orchestrator.executeSelenese(selenese);
  }

  private void scanSample(@Nullable String date, @Nullable String profile) {
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/sample"))
        .setProperties("sonar.cpd.skip", "true");
    if (date != null) {
      scan.setProperty("sonar.projectDate", date);
    }
    if (profile != null) {
      scan.setProfile(profile);
    }
    orchestrator.executeBuild(scan);
  }

  private void scanSampleWithProfile(String profile) {
    scanSample(null, profile);
  }

  private void scanSampleWithDate(String date) {
    scanSample(date, null);
  }

  private void scanSample() {
    scanSample(null, null);
  }

  private int count(String condition) {
    return orchestrator.getDatabase().countSql("select count(*) from " + condition);
  }

}
