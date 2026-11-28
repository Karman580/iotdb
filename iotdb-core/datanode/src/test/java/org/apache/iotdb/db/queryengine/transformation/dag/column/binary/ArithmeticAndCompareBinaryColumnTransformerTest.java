/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for transformeral information
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

package org.apache.iotdb.db.queryengine.transformation.dag.column.binary;

import org.apache.iotdb.calc.transformation.dag.column.ColumnTransformer;
import org.apache.iotdb.calc.transformation.dag.column.binary.ArithmeticAdditionColumnTransformer;
import org.apache.iotdb.calc.transformation.dag.column.binary.ArithmeticDivisionColumnTransformer;
import org.apache.iotdb.calc.transformation.dag.column.binary.ArithmeticModuloColumnTransformer;
import org.apache.iotdb.calc.transformation.dag.column.binary.ArithmeticMultiplicationColumnTransformer;
import org.apache.iotdb.calc.transformation.dag.column.binary.ArithmeticSubtractionColumnTransformer;
import org.apache.iotdb.calc.transformation.dag.column.binary.BinaryColumnTransformer;
import org.apache.iotdb.calc.transformation.dag.column.binary.CompareEqualToColumnTransformer;
import org.apache.iotdb.calc.transformation.dag.column.binary.CompareGreaterEqualColumnTransformer;
import org.apache.iotdb.calc.transformation.dag.column.binary.CompareGreaterThanColumnTransformer;
import org.apache.iotdb.calc.transformation.dag.column.binary.CompareLessEqualColumnTransformer;
import org.apache.iotdb.calc.transformation.dag.column.binary.CompareLessThanColumnTransformer;
import org.apache.iotdb.calc.transformation.dag.column.binary.CompareNonEqualColumnTransformer;
import org.apache.iotdb.calc.transformation.dag.column.leaf.IdentityColumnTransformer;

import org.apache.tsfile.block.column.Column;
import org.apache.tsfile.block.column.ColumnBuilder;
import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.read.common.block.TsBlock;
import org.apache.tsfile.read.common.block.TsBlockBuilder;
import org.apache.tsfile.read.common.block.column.TimeColumnBuilder;
import org.apache.tsfile.read.common.type.Type;
import org.apache.tsfile.read.common.type.TypeFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.function.IntPredicate;

public class ArithmeticAndCompareBinaryColumnTransformerTest {

  private static final Type returnType = TypeFactory.getType(TSDataType.INT32);

  private static final Type booleanType = TypeFactory.getType(TSDataType.BOOLEAN);

  private static final int POSITION_COUNT = 4;

  private static final int[] leftInput = new int[] {1, 2, 3, 4};

  private static final int[] rightInput = new int[] {1, 2, 3, 4};

  private static IdentityColumnTransformer leftOperand;

  private static IdentityColumnTransformer rightOperand;

  @Before
  public void setUp() {
    TsBlockBuilder builder =
        new TsBlockBuilder(POSITION_COUNT, Arrays.asList(TSDataType.INT32, TSDataType.INT32));
    TimeColumnBuilder timeColumnBuilder = builder.getTimeColumnBuilder();
    ColumnBuilder leftColumnBuilder = builder.getColumnBuilder(0);
    ColumnBuilder rightColumnBuilder = builder.getColumnBuilder(1);
    for (int i = 0; i < POSITION_COUNT; i++) {
      timeColumnBuilder.writeLong(i);
      leftColumnBuilder.writeInt(leftInput[i]);
      rightColumnBuilder.writeInt(rightInput[i]);
      builder.declarePosition();
    }
    TsBlock tsBlock = builder.build();
    leftOperand = new IdentityColumnTransformer(returnType, 0);
    rightOperand = new IdentityColumnTransformer(returnType, 1);
    leftOperand.addReferenceCount();
    rightOperand.addReferenceCount();
    leftOperand.initFromTsBlock(tsBlock);
    rightOperand.initFromTsBlock(tsBlock);
  }

  @Test
  public void testAddition() {
    BinaryColumnTransformer transformer =
        new ArithmeticAdditionColumnTransformer(returnType, leftOperand, rightOperand);
    transformer.addReferenceCount();
    transformer.evaluate();
    Column res = transformer.getColumn();
    Assert.assertEquals(POSITION_COUNT, res.getPositionCount());
    for (int i = 0; i < POSITION_COUNT; i++) {
      Assert.assertEquals(leftInput[i] + rightInput[i], res.getInt(i));
    }
  }

  @Test
  public void testSubtraction() {
    BinaryColumnTransformer transformer =
        new ArithmeticSubtractionColumnTransformer(returnType, leftOperand, rightOperand);
    transformer.addReferenceCount();
    transformer.evaluate();
    Column res = transformer.getColumn();
    Assert.assertEquals(POSITION_COUNT, res.getPositionCount());
    for (int i = 0; i < POSITION_COUNT; i++) {
      Assert.assertEquals(leftInput[i] - rightInput[i], res.getInt(i));
    }
  }

  @Test
  public void testMultiplication() {
    BinaryColumnTransformer transformer =
        new ArithmeticMultiplicationColumnTransformer(returnType, leftOperand, rightOperand);
    transformer.addReferenceCount();
    transformer.evaluate();
    Column res = transformer.getColumn();
    Assert.assertEquals(POSITION_COUNT, res.getPositionCount());
    for (int i = 0; i < POSITION_COUNT; i++) {
      Assert.assertEquals(leftInput[i] * rightInput[i], res.getInt(i));
    }
  }

  @Test
  public void testDivision() {
    BinaryColumnTransformer transformer =
        new ArithmeticDivisionColumnTransformer(returnType, leftOperand, rightOperand);
    transformer.addReferenceCount();
    transformer.evaluate();
    Column res = transformer.getColumn();
    Assert.assertEquals(POSITION_COUNT, res.getPositionCount());
    for (int i = 0; i < POSITION_COUNT; i++) {
      Assert.assertEquals(leftInput[i] / rightInput[i], res.getInt(i));
    }
  }

  @Test
  public void testModulo() {
    BinaryColumnTransformer transformer =
        new ArithmeticModuloColumnTransformer(returnType, leftOperand, rightOperand);
    transformer.addReferenceCount();
    transformer.evaluate();
    Column res = transformer.getColumn();
    Assert.assertEquals(POSITION_COUNT, res.getPositionCount());
    for (int i = 0; i < POSITION_COUNT; i++) {
      Assert.assertEquals(leftInput[i] % rightInput[i], res.getInt(i));
    }
  }

  @Test
  public void testEqualTo() {
    BinaryColumnTransformer transformer =
        new CompareEqualToColumnTransformer(booleanType, leftOperand, rightOperand);
    transformer.addReferenceCount();
    transformer.evaluate();
    Column res = transformer.getColumn();
    Assert.assertEquals(POSITION_COUNT, res.getPositionCount());
    for (int i = 0; i < POSITION_COUNT; i++) {
      Assert.assertEquals(leftInput[i] == rightInput[i], res.getBoolean(i));
    }
  }

  @Test
  public void testGreaterEqual() {
    BinaryColumnTransformer transformer =
        new CompareGreaterEqualColumnTransformer(booleanType, leftOperand, rightOperand);
    transformer.addReferenceCount();
    transformer.evaluate();
    Column res = transformer.getColumn();
    Assert.assertEquals(POSITION_COUNT, res.getPositionCount());
    for (int i = 0; i < POSITION_COUNT; i++) {
      Assert.assertEquals(leftInput[i] >= rightInput[i], res.getBoolean(i));
    }
  }

  @Test
  public void testGreaterThan() {
    BinaryColumnTransformer transformer =
        new CompareGreaterThanColumnTransformer(booleanType, leftOperand, rightOperand);
    transformer.addReferenceCount();
    transformer.evaluate();
    Column res = transformer.getColumn();
    Assert.assertEquals(POSITION_COUNT, res.getPositionCount());
    for (int i = 0; i < POSITION_COUNT; i++) {
      Assert.assertEquals(leftInput[i] > rightInput[i], res.getBoolean(i));
    }
  }

  @Test
  public void testLessEqual() {
    BinaryColumnTransformer transformer =
        new CompareLessEqualColumnTransformer(booleanType, leftOperand, rightOperand);
    transformer.addReferenceCount();
    transformer.evaluate();
    Column res = transformer.getColumn();
    Assert.assertEquals(POSITION_COUNT, res.getPositionCount());
    for (int i = 0; i < POSITION_COUNT; i++) {
      Assert.assertEquals(leftInput[i] <= rightInput[i], res.getBoolean(i));
    }
  }

  @Test
  public void testLessThan() {
    BinaryColumnTransformer transformer =
        new CompareGreaterThanColumnTransformer(booleanType, leftOperand, rightOperand);
    transformer.addReferenceCount();
    transformer.evaluate();
    Column res = transformer.getColumn();
    Assert.assertEquals(POSITION_COUNT, res.getPositionCount());
    for (int i = 0; i < POSITION_COUNT; i++) {
      Assert.assertEquals(leftInput[i] < rightInput[i], res.getBoolean(i));
    }
  }

  @Test
  public void testInt64ComparisonDoesNotRoundThroughDouble() {
    // Every pair below collides once the INT64 side is converted to double
    long[] longs = {Long.MAX_VALUE, 9007199254740993L, Long.MIN_VALUE, Long.MAX_VALUE - 1};
    double[] doubles = {0x1p63, 9007199254740992.0, -0x1p63, 0x1p63};
    long[] otherLongs = {Long.MAX_VALUE - 1, 9007199254740992L, Long.MIN_VALUE, Long.MAX_VALUE};

    TsBlockBuilder builder =
        new TsBlockBuilder(
            longs.length, Arrays.asList(TSDataType.INT64, TSDataType.DOUBLE, TSDataType.INT64));
    for (int i = 0; i < longs.length; i++) {
      builder.getTimeColumnBuilder().writeLong(i);
      builder.getColumnBuilder(0).writeLong(longs[i]);
      builder.getColumnBuilder(1).writeDouble(doubles[i]);
      builder.getColumnBuilder(2).writeLong(otherLongs[i]);
      builder.declarePosition();
    }
    TsBlock tsBlock = builder.build();

    // INT64 = DOUBLE
    Column res = evaluateComparison(tsBlock, TSDataType.INT64, 0, TSDataType.DOUBLE, 1, true);
    Assert.assertFalse(res.getBoolean(0));
    Assert.assertFalse(res.getBoolean(1));
    Assert.assertTrue(res.getBoolean(2));
    Assert.assertFalse(res.getBoolean(3));
    // DOUBLE = INT64, with the INT64 side on the right
    res = evaluateComparison(tsBlock, TSDataType.DOUBLE, 1, TSDataType.INT64, 0, true);
    Assert.assertFalse(res.getBoolean(0));
    Assert.assertTrue(res.getBoolean(2));
    // INT64 > DOUBLE: 9007199254740993 > 9007199254740992.0
    res = evaluateComparison(tsBlock, TSDataType.INT64, 0, TSDataType.DOUBLE, 1, false);
    Assert.assertFalse(res.getBoolean(0));
    Assert.assertTrue(res.getBoolean(1));
    Assert.assertFalse(res.getBoolean(2));
    // INT64 = INT64 above 2^53
    res = evaluateComparison(tsBlock, TSDataType.INT64, 0, TSDataType.INT64, 2, true);
    Assert.assertFalse(res.getBoolean(0));
    Assert.assertFalse(res.getBoolean(1));
    Assert.assertTrue(res.getBoolean(2));
    Assert.assertFalse(res.getBoolean(3));
  }

  @Test
  public void testInt64AndDoubleComparisonEdgeCases() {
    double belowTwoTo63 = Math.nextDown(0x1p63); // 9223372036854774784.0
    Number[] longs = {
      0L,
      -1L,
      -2L,
      0L,
      0L,
      Long.MAX_VALUE,
      Long.MIN_VALUE,
      9223372036854774784L,
      9223372036854774785L,
      9223372036854774783L
    };
    Number[] doubles = {
      -0.5,
      -1.5,
      -1.5,
      -0.0,
      Double.NaN,
      Double.POSITIVE_INFINITY,
      Double.NEGATIVE_INFINITY,
      belowTwoTo63,
      belowTwoTo63,
      belowTwoTo63
    };
    // Sign of long compared with double; null means NaN, where every comparison is false
    Integer[] expected = {1, 1, -1, 0, null, -1, 1, 0, 1, -1};
    assertComparisons(TSDataType.INT64, longs, TSDataType.DOUBLE, doubles, expected);
    assertComparisons(TSDataType.DOUBLE, doubles, TSDataType.INT64, longs, negate(expected));
  }

  @Test
  public void testInt64ComparedWithOtherNumericTypes() {
    // INT64 vs FLOAT
    Number[] longs = {Long.MAX_VALUE, Long.MIN_VALUE, 1L, 16777217L};
    Number[] floats = {0x1p63f, -0x1p63f, 1.5f, 16777216f};
    Integer[] expected = {-1, 0, -1, 1};
    assertComparisons(TSDataType.INT64, longs, TSDataType.FLOAT, floats, expected);
    assertComparisons(TSDataType.FLOAT, floats, TSDataType.INT64, longs, negate(expected));

    // INT64 vs INT32
    Number[] wide = {Long.MAX_VALUE, 2147483648L, -2147483649L, 5L};
    Number[] ints = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, 5};
    expected = new Integer[] {1, 1, -1, 0};
    assertComparisons(TSDataType.INT64, wide, TSDataType.INT32, ints, expected);
    assertComparisons(TSDataType.INT32, ints, TSDataType.INT64, wide, negate(expected));

    // TIMESTAMP vs DOUBLE and TIMESTAMP vs INT64
    Number[] timestamps = {Long.MAX_VALUE, Long.MAX_VALUE - 1, 9007199254740993L};
    assertComparisons(
        TSDataType.TIMESTAMP,
        timestamps,
        TSDataType.DOUBLE,
        new Number[] {0x1p63, 0x1p63, 9007199254740992.0},
        new Integer[] {-1, -1, 1});
    assertComparisons(
        TSDataType.TIMESTAMP,
        timestamps,
        TSDataType.INT64,
        new Number[] {Long.MAX_VALUE - 1, Long.MAX_VALUE, 9007199254740993L},
        new Integer[] {1, -1, 0});
  }

  @Test
  public void testComparisonWithSelection() {
    boolean[] selection = {true, false, true};
    TsBlock tsBlock =
        buildBlock(
            TSDataType.INT64,
            new Number[] {Long.MAX_VALUE, 1L, 2L},
            TSDataType.DOUBLE,
            new Number[] {0x1p63, 1.0, 2.0});
    Column res =
        evaluate(
            CompareEqualToColumnTransformer::new,
            tsBlock,
            TSDataType.INT64,
            TSDataType.DOUBLE,
            selection);
    Assert.assertFalse(res.getBoolean(0));
    Assert.assertTrue(res.isNull(1));
    Assert.assertTrue(res.getBoolean(2));

    tsBlock =
        buildBlock(
            TSDataType.INT32, new Number[] {1, 2, 3}, TSDataType.INT32, new Number[] {1, 2, 4});
    res =
        evaluate(
            CompareLessThanColumnTransformer::new,
            tsBlock,
            TSDataType.INT32,
            TSDataType.INT32,
            selection);
    Assert.assertFalse(res.getBoolean(0));
    Assert.assertTrue(res.isNull(1));
    Assert.assertTrue(res.getBoolean(2));
  }

  private interface CompareFactory {
    BinaryColumnTransformer create(
        Type returnType, ColumnTransformer leftTransformer, ColumnTransformer rightTransformer);
  }

  /**
   * Checks every comparison operator. {@code expected[i]} is the sign of {@code left[i]} compared
   * with {@code right[i]}, or null when every comparison must be false.
   */
  private static void assertComparisons(
      TSDataType leftType,
      Number[] left,
      TSDataType rightType,
      Number[] right,
      Integer[] expected) {
    TsBlock tsBlock = buildBlock(leftType, left, rightType, right);
    assertOperator(
        CompareEqualToColumnTransformer::new, s -> s == 0, tsBlock, leftType, rightType, expected);
    assertOperator(
        CompareNonEqualColumnTransformer::new, s -> s != 0, tsBlock, leftType, rightType, expected);
    assertOperator(
        CompareGreaterThanColumnTransformer::new,
        s -> s > 0,
        tsBlock,
        leftType,
        rightType,
        expected);
    assertOperator(
        CompareGreaterEqualColumnTransformer::new,
        s -> s >= 0,
        tsBlock,
        leftType,
        rightType,
        expected);
    assertOperator(
        CompareLessThanColumnTransformer::new, s -> s < 0, tsBlock, leftType, rightType, expected);
    assertOperator(
        CompareLessEqualColumnTransformer::new,
        s -> s <= 0,
        tsBlock,
        leftType,
        rightType,
        expected);
  }

  private static void assertOperator(
      CompareFactory factory,
      IntPredicate operator,
      TsBlock tsBlock,
      TSDataType leftType,
      TSDataType rightType,
      Integer[] expected) {
    Column res = evaluate(factory, tsBlock, leftType, rightType, null);
    Assert.assertEquals(expected.length, res.getPositionCount());
    for (int i = 0; i < expected.length; i++) {
      Assert.assertEquals(
          factory.getClass() + " row " + i,
          expected[i] != null && operator.test(expected[i]),
          res.getBoolean(i));
    }
  }

  private static Integer[] negate(Integer[] signs) {
    Integer[] negated = new Integer[signs.length];
    for (int i = 0; i < signs.length; i++) {
      negated[i] = signs[i] == null ? null : -signs[i];
    }
    return negated;
  }

  private static TsBlock buildBlock(
      TSDataType leftType, Number[] left, TSDataType rightType, Number[] right) {
    TsBlockBuilder builder = new TsBlockBuilder(left.length, Arrays.asList(leftType, rightType));
    for (int i = 0; i < left.length; i++) {
      builder.getTimeColumnBuilder().writeLong(i);
      write(builder.getColumnBuilder(0), leftType, left[i]);
      write(builder.getColumnBuilder(1), rightType, right[i]);
      builder.declarePosition();
    }
    return builder.build();
  }

  private static void write(ColumnBuilder builder, TSDataType type, Number value) {
    switch (type) {
      case INT32:
        builder.writeInt(value.intValue());
        break;
      case INT64:
      case TIMESTAMP:
        builder.writeLong(value.longValue());
        break;
      case FLOAT:
        builder.writeFloat(value.floatValue());
        break;
      case DOUBLE:
        builder.writeDouble(value.doubleValue());
        break;
      default:
        throw new IllegalArgumentException(type.toString());
    }
  }

  private static Column evaluate(
      CompareFactory factory,
      TsBlock tsBlock,
      TSDataType leftType,
      TSDataType rightType,
      boolean[] selection) {
    IdentityColumnTransformer left =
        new IdentityColumnTransformer(TypeFactory.getType(leftType), 0);
    IdentityColumnTransformer right =
        new IdentityColumnTransformer(TypeFactory.getType(rightType), 1);
    left.addReferenceCount();
    right.addReferenceCount();
    left.initFromTsBlock(tsBlock);
    right.initFromTsBlock(tsBlock);
    BinaryColumnTransformer transformer = factory.create(booleanType, left, right);
    transformer.addReferenceCount();
    if (selection == null) {
      transformer.evaluate();
    } else {
      transformer.evaluateWithSelection(selection);
    }
    return transformer.getColumn();
  }

  private static Column evaluateComparison(
      TsBlock tsBlock,
      TSDataType leftType,
      int leftIndex,
      TSDataType rightType,
      int rightIndex,
      boolean equal) {
    IdentityColumnTransformer left =
        new IdentityColumnTransformer(TypeFactory.getType(leftType), leftIndex);
    IdentityColumnTransformer right =
        new IdentityColumnTransformer(TypeFactory.getType(rightType), rightIndex);
    left.addReferenceCount();
    right.addReferenceCount();
    left.initFromTsBlock(tsBlock);
    right.initFromTsBlock(tsBlock);
    BinaryColumnTransformer transformer =
        equal
            ? new CompareEqualToColumnTransformer(booleanType, left, right)
            : new CompareGreaterThanColumnTransformer(booleanType, left, right);
    transformer.addReferenceCount();
    transformer.evaluate();
    return transformer.getColumn();
  }

  @Test
  public void testNonEqual() {
    BinaryColumnTransformer transformer =
        new CompareNonEqualColumnTransformer(booleanType, leftOperand, rightOperand);
    transformer.addReferenceCount();
    transformer.evaluate();
    Column res = transformer.getColumn();
    Assert.assertEquals(POSITION_COUNT, res.getPositionCount());
    for (int i = 0; i < POSITION_COUNT; i++) {
      Assert.assertEquals(leftInput[i] != rightInput[i], res.getBoolean(i));
    }
  }
}
