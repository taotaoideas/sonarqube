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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.ws.WsTester;

import static org.mockito.Mockito.mock;

public class SearchActionMediumTest {

  @Rule
  public DbTester dbTester = new DbTester();

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new ComponentDao(), new AuthorizationDao(dbTester.myBatis()));
    tester = new WsTester(new ComponentsWs(mock(ComponentAppAction.class), new SearchAction(dbClient)));
  }

  @Test
  @Ignore
  public void return_projects() throws Exception {
    dbTester.prepareDbUnit(getClass(), "return_projects.xml");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "search").setParam("uuid", "ABCD").setParam("q", "test");
    request.execute().assertJson(getClass(), "return_projects.json");
  }
}
