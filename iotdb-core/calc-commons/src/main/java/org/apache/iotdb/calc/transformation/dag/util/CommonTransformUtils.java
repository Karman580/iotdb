/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.calc.transformation.dag.util;

import org.apache.iotdb.calc.i18n.CalcMessages;
import org.apache.iotdb.commons.exception.SemanticException;
import org.apache.iotdb.commons.queryengine.plan.relational.sql.ast.Expression;
import org.apache.iotdb.commons.queryengine.plan.relational.sql.ast.StringLiteral;

import org.apache.tsfile.utils.Binary;

import java.util.Objects;
import java.util.Optional;

public class CommonTransformUtils {
  public static int compare(Binary first, Binary second) {
    if (Objects.requireNonNull(first) == Objects.requireNonNull(second)) {
      return 0;
    }

    return first.compareTo(second);
  }

  /**
   * Compares a long with a non-NaN double exactly, without converting the long to double. A double
   * cannot hold every long value ((double) Long.MAX_VALUE rounds up to 2^63).
   */
  public static int compareLongToDouble(long value, double other) {
    if (other >= 0x1p63) {
      return -1;
    }
    if (other < -0x1p63) {
      return 1;
    }
    // -2^63 <= other < 2^63, so the cast is exact truncation toward zero
    final long truncated = (long) other;
    if (value != truncated) {
      return Long.compare(value, truncated);
    }
    // value equals the integral part of other, so the fractional part decides
    final double fraction = other - truncated;
    return fraction > 0 ? -1 : (fraction < 0 ? 1 : 0);
  }

  public static Optional<Character> getEscapeCharacter(String escape) {
    if (escape.length() == 1) {
      return Optional.of(escape.charAt(0));
    } else {
      throw new SemanticException(CalcMessages.ESCAPE_STRING_MUST_BE_A_SINGLE_CHARACTER);
    }
  }

  public static boolean isStringLiteral(final Expression expression) {
    return expression instanceof StringLiteral;
  }
}
