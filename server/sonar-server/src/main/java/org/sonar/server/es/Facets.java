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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.HasAggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class Facets {

  private static final Logger LOGGER = LoggerFactory.getLogger(Facets.class);

  private final Multimap<String, FacetBucket> bucketsByFacetName;

  public Facets(SearchResponse response) {
    bucketsByFacetName = LinkedHashMultimap.create();

    if (response.getAggregations() != null) {
      for (Aggregation facet : response.getAggregations()) {
        processAggregation(facet);
      }
    }
  }

  private void processAggregation(Aggregation aggregation) {
    if (Missing.class.isAssignableFrom(aggregation.getClass())) {
      processMissingAggregation(aggregation);
    } else if (Terms.class.isAssignableFrom(aggregation.getClass())) {
      processTermsAggregation(aggregation);
    } else if (HasAggregations.class.isAssignableFrom(aggregation.getClass())) {
      processSubAggregations(aggregation);
    } else if (DateHistogram.class.isAssignableFrom(aggregation.getClass())) {
      processDateHistogram(aggregation);
    } else {
      LOGGER.warn("Cannot process {} type of aggregation", aggregation.getClass());
    }
  }

  private void processMissingAggregation(Aggregation aggregation) {
    Missing missing = (Missing) aggregation;
    long docCount = missing.getDocCount();
    if (docCount > 0L) {
      this.bucketsByFacetName.put(aggregation.getName().replace("_missing", ""), new FacetBucket("", docCount));
    }
  }

  private void processTermsAggregation(Aggregation aggregation) {
    Terms termAggregation = (Terms) aggregation;
    for (Terms.Bucket value : termAggregation.getBuckets()) {
      String facetName = aggregation.getName();
      if (facetName.contains("__") && !facetName.startsWith("__")) {
        facetName = facetName.substring(0, facetName.indexOf("__"));
      }
      facetName = facetName.replace("_selected", "");
      this.bucketsByFacetName.put(facetName, new FacetBucket(value.getKey(), value.getDocCount()));
    }
  }

  private void processSubAggregations(Aggregation aggregation) {
    HasAggregations hasAggregations = (HasAggregations) aggregation;
    for (Aggregation internalAggregation : hasAggregations.getAggregations()) {
      this.processAggregation(internalAggregation);
    }
  }

  private void processDateHistogram(Aggregation aggregation) {
    DateHistogram dateHistogram = (DateHistogram) aggregation;
    for (DateHistogram.Bucket value : dateHistogram.getBuckets()) {
      this.bucketsByFacetName.put(dateHistogram.getName(), new FacetBucket(value.getKeyAsText().toString(), value.getDocCount()));
    }
  }

  public boolean contains(String facetName) {
    return bucketsByFacetName.containsKey(facetName);
  }

  public Map<String, Collection<FacetBucket>> getAllBuckets() {
    return this.bucketsByFacetName.asMap();
  }

  public Collection<FacetBucket> getBuckets(String facetName) {
    return this.bucketsByFacetName.get(facetName);
  }

  public Set<String> getNames() {
    return bucketsByFacetName.keySet();
  }

  public Facets addDefaultBucket(String facetName, FacetBucket bucket) {
    Collection<FacetBucket> buckets = bucketsByFacetName.get(facetName);
    if (buckets == null) {
      bucketsByFacetName.put(facetName, bucket);
    } else {
      for (FacetBucket f : buckets) {
        if (f.getKey().equals(bucket.getKey())) {
          // bucket already exists
          return this;
        }
      }
      bucketsByFacetName.put(facetName, bucket);
    }
    return this;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SIMPLE_STYLE);
  }
}
