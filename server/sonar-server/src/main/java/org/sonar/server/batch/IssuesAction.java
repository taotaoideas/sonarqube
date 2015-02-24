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

package org.sonar.server.batch;

import com.google.common.base.Charsets;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.batch.protocol.input.issues.PreviousIssue;
import org.sonar.batch.protocol.input.issues.PreviousIssueHelper;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.plugins.MimeTypes;
import org.sonar.server.user.UserSession;

import javax.annotation.Nullable;

import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class IssuesAction implements RequestHandler {

  private static final String PARAM_KEY = "key";

  private final DbClient dbClient;

  private final IssueIndex issueIndex;

  public IssuesAction(DbClient dbClient, IssueIndex issueIndex) {
    this.dbClient = dbClient;
    this.issueIndex = issueIndex;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("issues")
      .setDescription("Return open issues")
      .setSince("5.1")
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_KEY)
      .setRequired(true)
      .setDescription("Project, module or file key")
      .setExampleValue("org.codehaus.sonar:sonar");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    UserSession.get().checkGlobalPermission(GlobalPermissions.PREVIEW_EXECUTION);
    final String moduleKey = request.mandatoryParam(PARAM_KEY);

    response.stream().setMediaType(MimeTypes.JSON);
    PreviousIssueHelper previousIssueHelper = PreviousIssueHelper.create(new OutputStreamWriter(response.stream().output(), Charsets.UTF_8));
    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto component = dbClient.componentDao().getByKey(session, moduleKey);
      Map<String, String> keysByUUid = keysByUUid(session, component);
      ToPreviousIssue toPreviousIssue = new ToPreviousIssue(keysByUUid);

      for (Iterator<IssueDoc> issueDocIterator = issueIndex.searchNonClosedIssuesByComponent(component); issueDocIterator.hasNext();){
        previousIssueHelper.addIssue(issueDocIterator.next(), toPreviousIssue);
      }
    } finally {
      previousIssueHelper.close();
      MyBatis.closeQuietly(session);
    }
  }

  private static class ToPreviousIssue implements PreviousIssueHelper.Function<IssueDoc, PreviousIssue> {

    private final Map<String, String> keysByUUid;

    public ToPreviousIssue(Map<String, String> keysByUUid) {
      this.keysByUUid = keysByUUid;
    }

    @Override
    public PreviousIssue apply(@Nullable IssueDoc issueDoc) {
      if (issueDoc != null) {
        return new PreviousIssue()
          .setKey(issueDoc.key())
          .setComponentKey(keysByUUid.get(issueDoc.moduleUuid()))
          .setChecksum(issueDoc.checksum())
          .setAssigneeLogin(issueDoc.assignee())
          .setLine(issueDoc.line())
          .setRuleKey(issueDoc.ruleKey().repository(), issueDoc.ruleKey().rule())
          .setMessage(issueDoc.message())
          .setResolution(issueDoc.resolution())
          .setSeverity(issueDoc.severity())
          .setManualSeverity(issueDoc.isManualSeverity())
          .setStatus(issueDoc.status())
          .setCreationDate(issueDoc.creationDate());
      }
      return null;
    }
  }

  private Map<String, String> keysByUUid(DbSession session, ComponentDto component){
    Map<String, String> keysByUUid = newHashMap();
    if (Scopes.PROJECT.equals(component.scope())) {
      List<ComponentDto> modulesTree = dbClient.componentDao().selectModulesTree(session, component.uuid());
      for (ComponentDto componentDto : modulesTree) {
        keysByUUid.put(componentDto.uuid(), componentDto.key());
      }
    } else {
      String moduleUuid = component.moduleUuid();
      if (moduleUuid == null) {
        throw new IllegalArgumentException(String.format("The component '%s' has no module uuid", component.uuid()));
      }
      ComponentDto module = dbClient.componentDao().getByUuid(session, moduleUuid);
      keysByUUid.put(module.uuid(), module.key());
    }
    return keysByUUid;
  }
}
