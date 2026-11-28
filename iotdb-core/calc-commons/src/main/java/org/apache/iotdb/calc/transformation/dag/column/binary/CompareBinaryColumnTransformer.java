/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.calc.transformation.dag.column.binary;

import org.apache.iotdb.calc.i18n.CalcMessages;
import org.apache.iotdb.calc.transformation.dag.column.ColumnTransformer;
import org.apache.iotdb.calc.transformation.dag.util.CommonTransformUtils;

import org.apache.tsfile.block.column.Column;
import org.apache.tsfile.block.column.ColumnBuilder;
import org.apache.tsfile.read.common.block.column.RunLengthEncodedColumn;
import org.apache.tsfile.read.common.type.Type;
import org.apache.tsfile.read.common.type.TypeEnum;

import static org.apache.iotdb.calc.plan.relational.metadata.CommonMetadataUtils.isBlobType;
import static org.apache.iotdb.calc.plan.relational.metadata.CommonMetadataUtils.isBool;
import static org.apache.iotdb.calc.plan.relational.metadata.CommonMetadataUtils.isCharType;

public abstract class CompareBinaryColumnTransformer extends BinaryColumnTransformer {

  /** How a pair of values is compared, chosen once from the operand types. */
  private enum ComparisonMode {
    BINARY,
    BOOLEAN,
    // One side is INT64/TIMESTAMP, the other an integer type
    LONG_LONG,
    // Left is INT64/TIMESTAMP, right is FLOAT/DOUBLE
    LONG_DOUBLE,
    // Left is FLOAT/DOUBLE, right is INT64/TIMESTAMP
    DOUBLE_LONG,
    // Every other numeric pair, which a double holds exactly
    DOUBLE
  }

  private final Type leftType;
  private final Type rightType;
  private final ComparisonMode mode;

  protected CompareBinaryColumnTransformer(
      Type returnType, ColumnTransformer leftTransformer, ColumnTransformer rightTransformer) {
    super(returnType, leftTransformer, rightTransformer);
    this.leftType = leftTransformer.getType();
    this.rightType = rightTransformer.getType();
    this.mode = comparisonMode(leftType, rightType);
  }

  private static ComparisonMode comparisonMode(Type left, Type right) {
    if (isCharType(left) || isBlobType(left)) {
      return ComparisonMode.BINARY;
    }
    if (isBool(left)) {
      return ComparisonMode.BOOLEAN;
    }
    // A double cannot hold every INT64/TIMESTAMP value (Long.MAX_VALUE rounds up to 2^63), so
    // these are compared without converting the integer side to double.
    if (isWideInteger(left) && isInteger(right) || isWideInteger(right) && isInteger(left)) {
      return ComparisonMode.LONG_LONG;
    }
    if (isWideInteger(left) && isFloatingPoint(right)) {
      return ComparisonMode.LONG_DOUBLE;
    }
    if (isFloatingPoint(left) && isWideInteger(right)) {
      return ComparisonMode.DOUBLE_LONG;
    }
    return ComparisonMode.DOUBLE;
  }

  private static boolean isWideInteger(Type type) {
    return type.getTypeEnum() == TypeEnum.INT64 || type.getTypeEnum() == TypeEnum.TIMESTAMP;
  }

  private static boolean isInteger(Type type) {
    return type.getTypeEnum() == TypeEnum.INT32 || isWideInteger(type);
  }

  private static boolean isFloatingPoint(Type type) {
    return type.getTypeEnum() == TypeEnum.FLOAT || type.getTypeEnum() == TypeEnum.DOUBLE;
  }

  @Override
  protected void doTransform(
      Column leftColumn, Column rightColumn, ColumnBuilder builder, int positionCount) {
    doTransform(leftColumn, rightColumn, builder, positionCount, null);
  }

  /** {@code selection} may be null, which selects every position. */
  @Override
  protected void doTransform(
      Column leftColumn,
      Column rightColumn,
      ColumnBuilder builder,
      int positionCount,
      boolean[] selection) {
    // if either column is all null, append nullCount. For now, a RunLengthEncodeColumn with
    // mayHaveNull == true is all null
    if ((leftColumn.mayHaveNull() && leftColumn instanceof RunLengthEncodedColumn)
        || (rightColumn.mayHaveNull() && rightColumn instanceof RunLengthEncodedColumn)) {
      builder.appendNull(positionCount);
      return;
    }
    // Dispatch once per column rather than once per row
    switch (mode) {
      case BINARY:
        transformBinary(leftColumn, rightColumn, builder, positionCount, selection);
        break;
      case BOOLEAN:
        transformBoolean(leftColumn, rightColumn, builder, positionCount, selection);
        break;
      case LONG_LONG:
        transformLongLong(leftColumn, rightColumn, builder, positionCount, selection);
        break;
      case LONG_DOUBLE:
        transformLongDouble(leftColumn, rightColumn, builder, positionCount, selection);
        break;
      case DOUBLE_LONG:
        transformDoubleLong(leftColumn, rightColumn, builder, positionCount, selection);
        break;
      default:
        transformDouble(leftColumn, rightColumn, builder, positionCount, selection);
        break;
    }
  }

  private static boolean isValid(
      Column leftColumn, Column rightColumn, int index, boolean[] selection) {
    return (selection == null || selection[index])
        && !leftColumn.isNull(index)
        && !rightColumn.isNull(index);
  }

  private void transformBinary(
      Column leftColumn,
      Column rightColumn,
      ColumnBuilder builder,
      int positionCount,
      boolean[] selection) {
    for (int i = 0; i < positionCount; i++) {
      if (isValid(leftColumn, rightColumn, i, selection)) {
        returnType.writeBoolean(
            builder,
            transform(
                CommonTransformUtils.compare(
                    leftType.getBinary(leftColumn, i), rightType.getBinary(rightColumn, i))));
      } else {
        builder.appendNull();
      }
    }
  }

  private void transformBoolean(
      Column leftColumn,
      Column rightColumn,
      ColumnBuilder builder,
      int positionCount,
      boolean[] selection) {
    for (int i = 0; i < positionCount; i++) {
      if (isValid(leftColumn, rightColumn, i, selection)) {
        returnType.writeBoolean(
            builder,
            transform(
                Boolean.compare(
                    leftType.getBoolean(leftColumn, i), rightType.getBoolean(rightColumn, i))));
      } else {
        builder.appendNull();
      }
    }
  }

  private void transformLongLong(
      Column leftColumn,
      Column rightColumn,
      ColumnBuilder builder,
      int positionCount,
      boolean[] selection) {
    for (int i = 0; i < positionCount; i++) {
      if (isValid(leftColumn, rightColumn, i, selection)) {
        returnType.writeBoolean(
            builder,
            transform(
                Long.compare(leftType.getLong(leftColumn, i), rightType.getLong(rightColumn, i))));
      } else {
        builder.appendNull();
      }
    }
  }

  private void transformLongDouble(
      Column leftColumn,
      Column rightColumn,
      ColumnBuilder builder,
      int positionCount,
      boolean[] selection) {
    for (int i = 0; i < positionCount; i++) {
      if (isValid(leftColumn, rightColumn, i, selection)) {
        final double right = rightType.getDouble(rightColumn, i);
        returnType.writeBoolean(
            builder,
            !Double.isNaN(right)
                && transform(
                    CommonTransformUtils.compareLongToDouble(
                        leftType.getLong(leftColumn, i), right)));
      } else {
        builder.appendNull();
      }
    }
  }

  private void transformDoubleLong(
      Column leftColumn,
      Column rightColumn,
      ColumnBuilder builder,
      int positionCount,
      boolean[] selection) {
    for (int i = 0; i < positionCount; i++) {
      if (isValid(leftColumn, rightColumn, i, selection)) {
        final double left = leftType.getDouble(leftColumn, i);
        returnType.writeBoolean(
            builder,
            !Double.isNaN(left)
                && transform(
                    -CommonTransformUtils.compareLongToDouble(
                        rightType.getLong(rightColumn, i), left)));
      } else {
        builder.appendNull();
      }
    }
  }

  private void transformDouble(
      Column leftColumn,
      Column rightColumn,
      ColumnBuilder builder,
      int positionCount,
      boolean[] selection) {
    for (int i = 0; i < positionCount; i++) {
      if (isValid(leftColumn, rightColumn, i, selection)) {
        final double left = leftType.getDouble(leftColumn, i);
        final double right = rightType.getDouble(rightColumn, i);
        returnType.writeBoolean(
            builder,
            !Double.isNaN(left) && !Double.isNaN(right) && transform(Double.compare(left, right)));
      } else {
        builder.appendNull();
      }
    }
  }

  @Override
  protected void checkType() {
    // Boolean type can only be compared by == or !=
    if (leftTransformer.typeNotEquals(TypeEnum.BOOLEAN)
        && rightTransformer.typeNotEquals(TypeEnum.BOOLEAN)) {
      return;
    }
    throw new UnsupportedOperationException(CalcMessages.UNSUPPORTED_TYPE_BINARY);
  }

  /**
   * Transform int value of flag to corresponding boolean value.
   *
   * @param flag input int value
   * @return result boolean value
   */
  protected abstract boolean transform(int flag);
}
