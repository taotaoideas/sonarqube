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

package org.sonar.server.component.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.UserSession;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class SearchAction implements RequestHandler {

  private static final String PARAM_UUID = "uuid";
  private static final String PARAM_QUERY = "q";

  private final DbClient dbClient;

  public SearchAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("search")
      .setDescription("Search for projects that are containing in a view or a sub view")
      .setSince("5.1")
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_UUID)
      .setRequired(true)
      .setDescription("View or sub view UUID")
      .setExampleValue("d6d9e1e5-5e13-44fa-ab82-3ec29efa8935");

    action
      .createParam(PARAM_QUERY)
      .setRequired(true)
      .setDescription("UTF-8 search query")
      .setExampleValue("sonar");
  }

  @Override
  public void handle(Request request, Response response) {
    String query = request.mandatoryParam(PARAM_QUERY);
    String viewOrSubUuid = request.mandatoryParam(PARAM_UUID);

    JsonWriter json = response.newJsonWriter();
    json.beginObject();

    DbSession session = dbClient.openSession(false);
    try {
      // TODO check authorization of the view / sub view
      Set<Long> projectIds = newHashSet(dbClient.componentIndexDao().selectProjectIdsFromQueryAndViewOrSubViewUuid(session, query, viewOrSubUuid));
      dbClient.authorizationDao().keepAuthorizedComponentIds(session, projectIds, UserSession.get().userId(), UserRole.USER);
      // TODO pagination
      // TODO load projects uuid and name

    } finally {
      MyBatis.closeQuietly(session);
    }

    json.endObject();
    json.close();
  }
}
