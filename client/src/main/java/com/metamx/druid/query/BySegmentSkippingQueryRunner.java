/*
 * Druid - a distributed column store.
 * Copyright (C) 2012  Metamarkets Group Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.metamx.druid.query;

import com.metamx.common.guava.Sequence;
import io.druid.query.Query;
import io.druid.query.QueryRunner;

/**
 */
public abstract class BySegmentSkippingQueryRunner<T> implements QueryRunner<T>
{
  private final QueryRunner<T> baseRunner;

  public BySegmentSkippingQueryRunner(
      QueryRunner<T> baseRunner
  )
  {
    this.baseRunner = baseRunner;
  }

  @Override
  public Sequence<T> run(Query<T> query)
  {
    if (Boolean.parseBoolean(query.getContextValue("bySegment"))) {
      return baseRunner.run(query);
    }

    return doRun(baseRunner, query);
  }

  protected abstract Sequence<T> doRun(QueryRunner<T> baseRunner, Query<T> query);
}
