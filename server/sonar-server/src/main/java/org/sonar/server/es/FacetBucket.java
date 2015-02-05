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

public class FacetBucket {

  private final String key;
  private final long value;

  public FacetBucket(String key, long value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public long getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FacetBucket that = (FacetBucket) o;
    if (value != that.value) {
      return false;
    }
    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    int result = key.hashCode();
    result = 31 * result + (int) (value ^ (value >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return String.format("{%s=%d}", getKey(), getValue());
  }
}
