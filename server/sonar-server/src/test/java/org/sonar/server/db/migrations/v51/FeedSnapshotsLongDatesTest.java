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

package org.sonar.server.db.migrations.v51;

import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.migrations.DatabaseMigration;

import static org.assertj.core.api.Assertions.assertThat;

public class FeedSnapshotsLongDatesTest {
  @ClassRule
  public static DbTester db = new DbTester().schema(FeedSnapshotsLongDatesTest.class, "schema.sql");

  @Test
  public void execute() throws Exception {
    db.prepareDbUnit(getClass(), "before.xml");

    DatabaseMigration migration = new FeedSnapshotsLongDates(db.database());
    migration.execute();

    int count = db
      .countSql("select count(*) from snapshots where created_at_ms is not null " +
        "and build_date_ms is not null " +
        "and period1_date is not null " +
        "and period2_date is not null " +
        "and period3_date is not null " +
        "and period4_date is not null " +
        "and period5_date is not null");
    assertThat(count).isEqualTo(2);
  }
}
