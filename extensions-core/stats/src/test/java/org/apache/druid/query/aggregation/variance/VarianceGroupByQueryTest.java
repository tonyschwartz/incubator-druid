/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.druid.query.aggregation.variance;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.druid.data.input.Row;
import org.apache.druid.java.util.common.granularity.PeriodGranularity;
import org.apache.druid.query.QueryRunner;
import org.apache.druid.query.QueryRunnerTestHelper;
import org.apache.druid.query.aggregation.LongSumAggregatorFactory;
import org.apache.druid.query.dimension.DefaultDimensionSpec;
import org.apache.druid.query.groupby.GroupByQuery;
import org.apache.druid.query.groupby.GroupByQueryConfig;
import org.apache.druid.query.groupby.GroupByQueryRunnerFactory;
import org.apache.druid.query.groupby.GroupByQueryRunnerTest;
import org.apache.druid.query.groupby.GroupByQueryRunnerTestHelper;
import org.apache.druid.query.groupby.having.GreaterThanHavingSpec;
import org.apache.druid.query.groupby.having.OrHavingSpec;
import org.apache.druid.query.groupby.orderby.DefaultLimitSpec;
import org.apache.druid.query.groupby.orderby.OrderByColumnSpec;
import org.apache.druid.segment.TestHelper;
import org.joda.time.Period;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 */
@RunWith(Parameterized.class)
public class VarianceGroupByQueryTest
{
  private final GroupByQueryConfig config;
  private final QueryRunner<Row> runner;
  private final GroupByQueryRunnerFactory factory;
  private final String testName;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<?> constructorFeeder()
  {
    return GroupByQueryRunnerTest.constructorFeeder();
  }

  public VarianceGroupByQueryTest(
      String testName,
      GroupByQueryConfig config,
      GroupByQueryRunnerFactory factory,
      QueryRunner runner
  )
  {
    this.testName = testName;
    this.config = config;
    this.factory = factory;
    this.runner = factory.mergeRunners(MoreExecutors.sameThreadExecutor(), ImmutableList.of(runner));
  }

  @Test
  public void testGroupByVarianceOnly()
  {
    GroupByQuery query = GroupByQuery
        .builder()
        .setDataSource(QueryRunnerTestHelper.dataSource)
        .setQuerySegmentSpec(QueryRunnerTestHelper.firstToThird)
        .setDimensions(new DefaultDimensionSpec("quality", "alias"))
        .setAggregatorSpecs(VarianceTestHelper.indexVarianceAggr)
        .setPostAggregatorSpecs(Collections.singletonList(VarianceTestHelper.stddevOfIndexPostAggr))
        .setGranularity(QueryRunnerTestHelper.dayGran)
        .build();

    VarianceTestHelper.RowBuilder builder =
        new VarianceTestHelper.RowBuilder(new String[]{"alias", "index_stddev", "index_var"});

    List<Row> expectedResults = builder
        .add("2011-04-01", "automotive", 0d, 0d)
        .add("2011-04-01", "business", 0d, 0d)
        .add("2011-04-01", "entertainment", 0d, 0d)
        .add("2011-04-01", "health", 0d, 0d)
        .add("2011-04-01", "mezzanine", 737.0179286322613d, 543195.4271253889d)
        .add("2011-04-01", "news", 0d, 0d)
        .add("2011-04-01", "premium", 726.6322593583996d, 527994.4403402924d)
        .add("2011-04-01", "technology", 0d, 0d)
        .add("2011-04-01", "travel", 0d, 0d)

        .add("2011-04-02", "automotive", 0d, 0d)
        .add("2011-04-02", "business", 0d, 0d)
        .add("2011-04-02", "entertainment", 0d, 0d)
        .add("2011-04-02", "health", 0d, 0d)
        .add("2011-04-02", "mezzanine", 611.3420766546617d, 373739.13468843425d)
        .add("2011-04-02", "news", 0d, 0d)
        .add("2011-04-02", "premium", 621.3898134843073d, 386125.30030206224d)
        .add("2011-04-02", "technology", 0d, 0d)
        .add("2011-04-02", "travel", 0d, 0d)
        .build();

    Iterable<Row> results = GroupByQueryRunnerTestHelper.runQuery(factory, runner, query);
    TestHelper.assertExpectedObjects(expectedResults, results, "");
  }

  @Test
  public void testGroupBy()
  {
    GroupByQuery query = GroupByQuery
        .builder()
        .setDataSource(QueryRunnerTestHelper.dataSource)
        .setQuerySegmentSpec(QueryRunnerTestHelper.firstToThird)
        .setDimensions(new DefaultDimensionSpec("quality", "alias"))
        .setAggregatorSpecs(
            QueryRunnerTestHelper.rowsCount,
            VarianceTestHelper.indexVarianceAggr,
            new LongSumAggregatorFactory("idx", "index")
        )
        .setPostAggregatorSpecs(Collections.singletonList(VarianceTestHelper.stddevOfIndexPostAggr))
        .setGranularity(QueryRunnerTestHelper.dayGran)
        .build();

    VarianceTestHelper.RowBuilder builder =
        new VarianceTestHelper.RowBuilder(new String[]{"alias", "rows", "idx", "index_stddev", "index_var"});

    List<Row> expectedResults = builder
        .add("2011-04-01", "automotive", 1L, 135L, 0d, 0d)
        .add("2011-04-01", "business", 1L, 118L, 0d, 0d)
        .add("2011-04-01", "entertainment", 1L, 158L, 0d, 0d)
        .add("2011-04-01", "health", 1L, 120L, 0d, 0d)
        .add("2011-04-01", "mezzanine", 3L, 2870L, 737.0179286322613d, 543195.4271253889d)
        .add("2011-04-01", "news", 1L, 121L, 0d, 0d)
        .add("2011-04-01", "premium", 3L, 2900L, 726.6322593583996d, 527994.4403402924d)
        .add("2011-04-01", "technology", 1L, 78L, 0d, 0d)
        .add("2011-04-01", "travel", 1L, 119L, 0d, 0d)

        .add("2011-04-02", "automotive", 1L, 147L, 0d, 0d)
        .add("2011-04-02", "business", 1L, 112L, 0d, 0d)
        .add("2011-04-02", "entertainment", 1L, 166L, 0d, 0d)
        .add("2011-04-02", "health", 1L, 113L, 0d, 0d)
        .add("2011-04-02", "mezzanine", 3L, 2447L, 611.3420766546617d, 373739.13468843425d)
        .add("2011-04-02", "news", 1L, 114L, 0d, 0d)
        .add("2011-04-02", "premium", 3L, 2505L, 621.3898134843073d, 386125.30030206224d)
        .add("2011-04-02", "technology", 1L, 97L, 0d, 0d)
        .add("2011-04-02", "travel", 1L, 126L, 0d, 0d)
        .build();

    Iterable<Row> results = GroupByQueryRunnerTestHelper.runQuery(factory, runner, query);
    TestHelper.assertExpectedObjects(expectedResults, results, "");
  }

  @Test
  public void testPostAggHavingSpec()
  {
    VarianceTestHelper.RowBuilder expect = new VarianceTestHelper.RowBuilder(
        new String[]{"alias", "rows", "index", "index_var", "index_stddev"}
    );

    List<Row> expectedResults = expect
        .add("2011-04-01", "automotive", 2L, 269L, 299.0009819048282, 17.29164485827847)
        .add("2011-04-01", "mezzanine", 6L, 4420L, 254083.76447001836, 504.06722217380724)
        .add("2011-04-01", "premium", 6L, 4416L, 252279.2020389339, 502.27403082275106)
        .build();

    GroupByQuery query = GroupByQuery
        .builder()
        .setDataSource(QueryRunnerTestHelper.dataSource)
        .setInterval("2011-04-02/2011-04-04")
        .setDimensions(new DefaultDimensionSpec("quality", "alias"))
        .setAggregatorSpecs(
            QueryRunnerTestHelper.rowsCount,
            QueryRunnerTestHelper.indexLongSum,
            VarianceTestHelper.indexVarianceAggr
        )
        .setPostAggregatorSpecs(ImmutableList.of(VarianceTestHelper.stddevOfIndexPostAggr))
        .setGranularity(new PeriodGranularity(new Period("P1M"), null, null))
        .setHavingSpec(
            new OrHavingSpec(
                ImmutableList.of(
                    new GreaterThanHavingSpec(VarianceTestHelper.stddevOfIndexMetric, 15L) // 3 rows
                )
            )
        )
        .build();

    Iterable<Row> results = GroupByQueryRunnerTestHelper.runQuery(factory, runner, query);
    TestHelper.assertExpectedObjects(expectedResults, results, "");

    query = query.withLimitSpec(
        new DefaultLimitSpec(
            Collections.singletonList(
                OrderByColumnSpec.asc(
                    VarianceTestHelper.stddevOfIndexMetric
                )
            ), 2
        )
    );

    expectedResults = expect
        .add("2011-04-01", "automotive", 2L, 269L, 299.0009819048282, 17.29164485827847)
        .add("2011-04-01", "premium", 6L, 4416L, 252279.2020389339, 502.27403082275106)
        .build();

    results = GroupByQueryRunnerTestHelper.runQuery(factory, runner, query);
    TestHelper.assertExpectedObjects(expectedResults, results, "");
  }
}