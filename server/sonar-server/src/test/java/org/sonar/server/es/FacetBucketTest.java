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
package org.sonar.server.es;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FacetBucketTest {

  @Test
  public void test() throws Exception {
    FacetBucket bucket1 = new FacetBucket("foo", 12);
    FacetBucket bucket1bis = new FacetBucket("foo", 12);
    FacetBucket bucket2 = new FacetBucket("foo", 25);
    FacetBucket bucket3 = new FacetBucket("bar", 12);

    assertThat(bucket1.getKey()).isEqualTo("foo");
    assertThat(bucket1.getValue()).isEqualTo(12);
    assertThat(bucket1.toString()).isEqualTo("{foo=12}");

    assertThat(bucket1.equals(bucket1)).isTrue();
    assertThat(bucket1.equals(bucket1bis)).isTrue();
    assertThat(bucket1.equals(bucket2)).isFalse();
    assertThat(bucket1.equals(bucket3)).isFalse();
    assertThat(bucket1.equals("foo")).isFalse();
    assertThat(bucket1.equals(null)).isFalse();

    assertThat(bucket1.hashCode()).isEqualTo(bucket1.hashCode());
    assertThat(bucket1.hashCode()).isEqualTo(bucket1bis.hashCode());
  }
}
