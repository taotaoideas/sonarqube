/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.step;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.ZipUtils;
import org.sonar.api.utils.internal.DefaultTempFolder;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchOutputWriter;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.user.UserDto;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.db.AnalysisReportDao;
import org.sonar.server.computation.issue.IssueComputation;
import org.sonar.server.db.DbClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class ParseReportStepTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static DbTester dbTester = new DbTester();

  ParseReportStep sut;

  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
  }

  @Test
  public void extract_report_from_db_and_browse_components() throws Exception {
    AnalysisReportDto reportDto = prepareAnalysisReportInDb();
    IssueComputation issueComputation = mock(IssueComputation.class);
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new AnalysisReportDao());
    sut = new ParseReportStep(issueComputation, dbClient, new DefaultTempFolder(temp.newFolder()));
    ComputationContext context = new ComputationContext(reportDto, mock(ComponentDto.class));
    context.setProjectSettings(mock(Settings.class, Mockito.RETURNS_DEEP_STUBS));

    sut.execute(context);

    // verify that all components are processed (currently only for issues)
    verify(issueComputation).processComponentIssues(context, "PROJECT_UUID", Collections.<BatchReport.Issue>emptyList(), null);
    verify(issueComputation).processComponentIssues(context, "FILE1_UUID", Collections.<BatchReport.Issue>emptyList(), null);
    verify(issueComputation).processComponentIssues(context, "FILE2_UUID", Collections.<BatchReport.Issue>emptyList(), null);
    verify(issueComputation).afterReportProcessing();
    assertThat(context.getReportMetadata().getRootComponentRef()).isEqualTo(1);
  }

  @Test
  public void default_assignee_is_null_when_no_property() throws Exception {
    sut = new ParseReportStep(mock(IssueComputation.class), mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS), mock(TempFolder.class));

    String assignee = sut.defaultAssignee(null);

    assertThat(assignee).isNull();
  }

  @Test
  public void default_assignee_is_null_when_not_found_in_database() throws Exception {
    DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);
    when(dbClient.userDao().selectActiveUserByLogin(any(DbSession.class), anyString())).thenReturn(null);
    sut = new ParseReportStep(mock(IssueComputation.class), dbClient, mock(TempFolder.class));

    String assignee = sut.defaultAssignee("defaultAssignee");

    assertThat(assignee).isNull();
  }

  @Test
  public void default_assignee_is_returned_when_found_in_database() throws Exception {
    DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);
    when(dbClient.userDao().selectActiveUserByLogin(any(DbSession.class), anyString())).thenReturn(new UserDto());
    sut = new ParseReportStep(mock(IssueComputation.class), dbClient, mock(TempFolder.class));

    String assignee = sut.defaultAssignee("defaultAssignee");

    assertThat(assignee).isEqualTo("defaultAssignee");
  }

  private AnalysisReportDto prepareAnalysisReportInDb() throws IOException {
    File dir = temp.newFolder();
    // project and 2 files
    BatchOutputWriter writer = new BatchOutputWriter(dir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setProjectKey("PROJECT_KEY")
      .setAnalysisDate(150000000L)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid("PROJECT_UUID")
      .addChildRefs(2)
      .addChildRefs(3)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE1_UUID")
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE2_UUID")
      .build());
    File zipFile = temp.newFile();
    ZipUtils.zipDir(dir, zipFile);

    AnalysisReportDto dto = new AnalysisReportDto();
    DbSession dbSession = dbTester.myBatis().openSession(false);
    try {
      dto.setProjectKey("PROJECT_KEY");
      dto.setCreatedAt(System.currentTimeMillis());
      dto.setSnapshotId(1L);
      dto.setStatus(AnalysisReportDto.Status.PENDING);
      FileInputStream inputStream = new FileInputStream(zipFile);
      dto.setData(inputStream);
      AnalysisReportDao dao = new AnalysisReportDao();
      dao.insert(dbSession, dto);
      inputStream.close();
      dbSession.commit();

      // dao#insert() does not set the generated id, so the row
      // is loaded again to get its id
      return dao.selectByProjectKey(dbSession, "PROJECT_KEY").get(0);
    } finally {
      dbSession.close();
    }
  }
}
