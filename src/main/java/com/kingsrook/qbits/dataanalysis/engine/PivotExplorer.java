/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qbits.dataanalysis.engine;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qbits.dataanalysis.result.Metric;
import com.kingsrook.qbits.dataanalysis.result.PivotCell;
import com.kingsrook.qbits.dataanalysis.result.PivotDimension;
import com.kingsrook.qbits.dataanalysis.result.PivotHeader;
import com.kingsrook.qbits.dataanalysis.result.PivotMeasure;
import com.kingsrook.qbits.dataanalysis.result.PivotMeasureInfo;
import com.kingsrook.qbits.dataanalysis.result.PivotRequest;
import com.kingsrook.qbits.dataanalysis.result.PivotResult;
import com.kingsrook.qbits.dataanalysis.result.ValueType;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucket;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucketUnit;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.NumericBucket;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.aggregates.AggregateAnalysis;

/*******************************************************************************
 ** Request-local pivot planner. Core executes every aggregate with ENFORCE.
 ** Merged cells are re-aggregated so non-additive measures remain exact.
 *******************************************************************************/
public class PivotExplorer
{
   private final AnalysisSecurity security;
   private final EffectiveConfig config;
   private final List<QueryJoin> joins = new ArrayList<>();
   private final LinkedHashSet<String> toManyTables = new LinkedHashSet<>();
   private final LinkedHashSet<String> multiHopTables = new LinkedHashSet<>();
   private PivotRequest request;
   private Aggregate count;
   private final List<Aggregate> measures = new ArrayList<>();
   private final List<PivotMeasureInfo> measureInfo = new ArrayList<>();
   private boolean partial;

   /*******************************************************************************
    ** Bind the current user's policy and bounded streamed fallback.
    *******************************************************************************/
   public PivotExplorer(AnalysisSecurity security, EffectiveConfig config)
   {
      this.security = security;
      this.config = config;
   }

   /*******************************************************************************
    ** One slot is a visible group or the exact complement of the kept groups.
    *******************************************************************************/
   private record Slot(Serializable value, String label, QQueryFilter condition, boolean other)
   {
   }

   /*******************************************************************************
    ** Dimension state owns predicates, so no generated filter names hidden fields.
    *******************************************************************************/
   private record Dimension(GroupBy group, QFieldMetaData field, Map<Serializable, Slot> kept, Map<Serializable, Long> counts, Slot other)
   {
      /*******************************************************************************
       ** Return the kept group, Other, or null for a privacy-suppressed group.
       *******************************************************************************/
      Slot slot(Serializable value, int minimum)
      {
         return (counts.getOrDefault(value, 0L) < minimum ? null : kept.getOrDefault(value, other));
      }
   }

   /*******************************************************************************
    ** Validate the complete plan before any source computation.
    *******************************************************************************/
   public PivotResult execute(PivotRequest request) throws QException
   {
      this.request = request;
      validate();
      String primaryKey = QContext.getQInstance().getTable(request.getTableName()).getPrimaryKeyField();
      count = new Aggregate(primaryKey, AggregateOperator.COUNT).withFieldType(QFieldType.INTEGER);
      List<PivotDimension> requested = new ArrayList<>(request.getRows());
      requested.addAll(request.getColumns());
      List<QFieldMetaData> fields = new ArrayList<>();
      for(PivotDimension dimension : requested)
      {
         fields.add(resolve(dimension.getField()));
      }
      for(int i = 0; i < request.getValues().size(); i++)
      {
         prepareMeasure(request.getValues().get(i), i);
      }
      List<Dimension> dimensions = new ArrayList<>();
      for(int i = 0; i < requested.size(); i++)
      {
         dimensions.add(dimension(requested.get(i), fields.get(i)));
      }
      int rowCount = request.getRows().size();
      List<Dimension> rows = dimensions.subList(0, rowCount);
      List<Dimension> columns = dimensions.subList(rowCount, dimensions.size());
      PivotResult result = new PivotResult().withAsOf(Instant.now()).withMeasures(measureInfo)
         .withTruncatedRows(rows.stream().anyMatch(d -> d.other() != null))
         .withTruncatedColumns(columns.stream().anyMatch(d -> d.other() != null));
      Map<List<Slot>, List<Metric>> matrix = level(dimensions);
      Map<List<Slot>, List<Metric>> rowTotals = request.getShowTotals() || config.getMinGroupSize() > 0 ? level(rows) : Map.of();
      Map<List<Slot>, List<Metric>> columnTotals = request.getShowTotals() || config.getMinGroupSize() > 0 ? level(columns) : Map.of();
      List<List<Slot>> rowKeys = new ArrayList<>();
      List<List<Slot>> columnKeys = new ArrayList<>();
      for(var entry : matrix.entrySet())
      {
         List<Slot> row = List.copyOf(entry.getKey().subList(0, rowCount));
         List<Slot> column = List.copyOf(entry.getKey().subList(rowCount, dimensions.size()));
         if(config.getMinGroupSize() > 0 && (rowTotals.getOrDefault(row, List.of()).stream().allMatch(m -> m.getSuppressed())
            || columnTotals.getOrDefault(column, List.of()).stream().allMatch(m -> m.getSuppressed())))
         {
            continue;
         }
         if(!rowKeys.contains(row))
         {
            rowKeys.add(row);
         }
         if(!columnKeys.contains(column))
         {
            columnKeys.add(column);
         }
         result.getCells().add(new PivotCell().withRowIndex(rowKeys.indexOf(row)).withColumnIndex(columnKeys.indexOf(column)).withValues(entry.getValue()));
      }
      result.withRowHeaders(headers(rowKeys)).withColumnHeaders(headers(columnKeys));
      if(request.getShowTotals())
      {
         rowKeys.forEach(key -> result.getRowTotals().add(rowTotals.getOrDefault(key, List.of())));
         columnKeys.forEach(key -> result.getColumnTotals().add(columnTotals.getOrDefault(key, List.of())));
         result.withGrandTotals(level(List.of()).getOrDefault(List.of(), List.of()));
      }
      for(String tableName : toManyTables)
      {
         result.getWarnings().add("Counts include one row per related " + QContext.getQInstance().getTable(tableName).getLabel()
            + "; drill-through is not available for this dimension.");
      }
      for(String tableName : multiHopTables)
      {
         result.getWarnings().add("Drill-through is not available for fields reached through more than one join (" + QContext.getQInstance().getTable(tableName).getLabel() + ").");
      }
      if(config.getMinGroupSize() > 0)
      {
         result.getWarnings().add("Groups below " + config.getMinGroupSize() + " records are omitted; small cells and totals are suppressed.");
      }
      if(partial)
      {
         result.withEstimate(true);
         result.getWarnings().add("Partial pivot: the streamed row limit was reached. Drills are disabled because aggregates may cover different prefixes.");
         result.getCells().forEach(c -> disableDrills(c.getValues()));
         result.getRowTotals().forEach(this::disableDrills);
         result.getColumnTotals().forEach(this::disableDrills);
         disableDrills(result.getGrandTotals());
      }
      return (result);
   }

   /*******************************************************************************
    ** Reject invalid shapes and types before aggregate execution.
    *******************************************************************************/
   private void validate() throws QException
   {
      if(request.getRows() == null || request.getRows().isEmpty() || request.getRows().size() > 3
         || request.getColumns() == null || request.getColumns().size() > 2 || request.getValues() == null
         || request.getValues().isEmpty() || request.getValues().size() > 6 || request.getShowTotals() == null
         || request.getLimitGroups() == null || request.getLimitGroups() < 1 || request.getLimitGroups() > 200)
      {
         throw new QBadRequestException("Choose 1�3 row dimensions, 0�2 column dimensions, 1�6 measures and a group limit from 1 to 200.");
      }
      List<PivotDimension> dimensions = new ArrayList<>(request.getRows());
      dimensions.addAll(request.getColumns());
      LinkedHashSet<String> names = new LinkedHashSet<>();
      for(PivotDimension dimension : dimensions)
      {
         if(dimension == null || dimension.getField() == null || !names.add(dimension.getField()))
         {
            throw new QBadRequestException("Choose distinct dimension fields.");
         }
         QFieldMetaData field = resolve(dimension.getField());
         var bins = dimension.getNumericBins();
         if(dimension.getDateBucket() != null && (bins != null || field.getType() != QFieldType.DATE && field.getType() != QFieldType.DATE_TIME)
            || bins != null && (!numeric(field) || bins.getCount() == null || bins.getCount() < 1 || bins.getCount() > 200
               || bins.getMin() != null && bins.getMax() != null && bins.getMin().compareTo(bins.getMax()) >= 0))
         {
            throw new QBadRequestException("Buckets must match the field type; numeric bins require 1�200 bins and min < max.");
         }
      }
   }

   /*******************************************************************************
    ** Only exposed, unambiguous paths may introduce a joined field.
    ** The installed snapshot has no related-record picker join-path helper.
    *******************************************************************************/
   private QFieldMetaData resolve(String name) throws QException
   {
      if(name == null || !name.contains("."))
      {
         return (security.field(request.getTableName(), name));
      }
      String[] parts = name.split("\\.", -1);
      var table = QContext.getQInstance().getTable(request.getTableName());
      List<ExposedJoin> exposed = table.getExposedJoins() == null ? List.of() : table.getExposedJoins();
      List<ExposedJoin> paths = exposed.stream().filter(j -> j.getJoinTable().equals(parts[0])).toList();
      if(parts.length != 2 || paths.size() != 1 || paths.getFirst().getJoinPath() == null || paths.getFirst().getJoinPath().isEmpty())
      {
         throw new QBadRequestException("Unknown analysis field.");
      }
      String current = table.getName();
      boolean toOne = true;
      for(String joinName : paths.getFirst().getJoinPath())
      {
         var metadata = QContext.getQInstance().getJoin(joinName);
         if(metadata == null || !current.equals(metadata.getLeftTable()) && !current.equals(metadata.getRightTable()))
         {
            throw new QBadRequestException("Unknown analysis field.");
         }
         String next = current.equals(metadata.getLeftTable()) ? metadata.getRightTable() : metadata.getLeftTable();
         JoinType type = metadata.getType();
         if(type != null && current.equals(metadata.getRightTable()))
         {
            type = type.flip();
         }
         toOne &= type == JoinType.MANY_TO_ONE || type == JoinType.ONE_TO_ONE;
         security.joinedTable(next);
         QueryJoin existing = joins.stream().filter(j -> next.equals(j.getJoinTable())).findFirst().orElse(null);
         if(existing == null)
         {
            joins.add(new QueryJoin(current, next).withJoinMetaData(metadata));
         }
         else if(!existing.getJoinMetaData().getName().equals(joinName))
         {
            throw new QBadRequestException("Ambiguous analysis join path.");
         }
         current = next;
      }
      if(!current.equals(parts[0]))
      {
         throw new QBadRequestException("Unknown analysis field.");
      }
      if(!toOne)
      {
         toManyTables.add(current);
      }
      else if(paths.getFirst().getJoinPath().size() > 1)
      {
         ///////////////////////////////////////////////////////////////////////////////
         // counts are per record, but core's implicit joins for a multi-hop filter     //
         // do not reproduce them on every backend yet (memory counts differ), so no   //
         // drill until that is fixed in core                                          //
         ///////////////////////////////////////////////////////////////////////////////
         multiHopTables.add(current);
      }
      return (security.joinedField(parts[0], parts[1]));
   }

   /*******************************************************************************
    ** Count records even when a supplied COUNT field is nullable.
    *******************************************************************************/
   private void prepareMeasure(PivotMeasure measure, int index) throws QException
   {
      if(measure == null || measure.getOperator() == null)
      {
         throw new QBadRequestException("Choose a measure operator.");
      }
      QFieldMetaData field = measure.getField() == null && measure.getOperator() == PivotMeasure.Operator.COUNT ? null : resolve(measure.getField());
      PivotMeasure.Operator operator = measure.getOperator();
      if((operator == PivotMeasure.Operator.SUM || operator == PivotMeasure.Operator.AVG || operator == PivotMeasure.Operator.PERCENTILE) && !numeric(field)
         || operator == PivotMeasure.Operator.PERCENTILE && (measure.getPercentile() == null || measure.getPercentile().signum() < 0 || measure.getPercentile().compareTo(BigDecimal.ONE) > 0)
         || operator != PivotMeasure.Operator.PERCENTILE && measure.getPercentile() != null)
      {
         throw new QBadRequestException("Numeric measures require a numeric field; percentile must be between 0 and 1.");
      }
      Aggregate aggregate = operator == PivotMeasure.Operator.COUNT ? count : new Aggregate(measure.getField(),
         operator == PivotMeasure.Operator.PERCENTILE ? AggregateOperator.PERCENTILE_CONT : AggregateOperator.valueOf(operator.name()));
      if(operator == PivotMeasure.Operator.PERCENTILE)
      {
         aggregate.withPercentile(measure.getPercentile());
      }
      if(operator == PivotMeasure.Operator.COUNT_DISTINCT)
      {
         aggregate.withFieldType(QFieldType.INTEGER);
      }
      measures.add(aggregate);
      ValueType type = operator == PivotMeasure.Operator.COUNT || operator == PivotMeasure.Operator.COUNT_DISTINCT ? ValueType.COUNT
         : numeric(field) ? ValueType.DECIMAL : field.getType() == QFieldType.DATE ? ValueType.DATE : field.getType() == QFieldType.DATE_TIME ? ValueType.DATE_TIME : ValueType.TEXT;
      measureInfo.add(new PivotMeasureInfo().withKey("value" + index).withLabel(measureLabel(measure, field)).withValueType(type));
   }

   /*******************************************************************************
    ** Human measure labels shared by headers, totals and cells.
    *******************************************************************************/
   private static String measureLabel(PivotMeasure measure, QFieldMetaData field)
   {
      String label = field == null ? "" : field.getLabel();
      return (switch(measure.getOperator())
      {
         case COUNT -> "Count";
         case AVG -> "Average " + label;
         case SUM -> "Sum of " + label;
         case COUNT_DISTINCT -> "Distinct " + label;
         case MIN -> "Minimum " + label;
         case MAX -> "Maximum " + label;
         case PERCENTILE -> measure.getPercentile().compareTo(new BigDecimal("0.5")) == 0 ? "Median " + label
            : ordinal(measure.getPercentile().multiply(BigDecimal.valueOf(100))) + " percentile of " + label;
      });
   }

   /*******************************************************************************
    ** English ordinal suffixes, including the eleventh through thirteenth.
    *******************************************************************************/
   private static String ordinal(BigDecimal value)
   {
      int number = value.intValue();
      String suffix = number % 100 >= 11 && number % 100 <= 13 ? "th" : switch(number % 10)
      {
         case 1 -> "st";
         case 2 -> "nd";
         case 3 -> "rd";
         default -> "th";
      };
      return (value.stripTrailingZeros().toPlainString() + suffix);
   }

   /*******************************************************************************
    ** Numeric buckets and moments accept the same numeric QQQ field types.
    *******************************************************************************/
   private static boolean numeric(QFieldMetaData field)
   {
      return (field != null && (field.getType() == QFieldType.INTEGER || field.getType() == QFieldType.LONG || field.getType() == QFieldType.DECIMAL));
   }

   /*******************************************************************************
    ** Select top groups first, then collect cardinality and privacy counts.
    *******************************************************************************/
   private Dimension dimension(PivotDimension requested, QFieldMetaData field) throws QException
   {
      GroupBy group = new GroupBy(field.getType(), requested.getField());
      if(requested.getDateBucket() != null)
      {
         group.withBucket(new DateBucket(requested.getDateBucket()));
      }
      if(requested.getNumericBins() != null)
      {
         var bins = requested.getNumericBins();
         BigDecimal low = bins.getMin();
         BigDecimal high = bins.getMax();
         if(low == null || high == null)
         {
            Aggregate min = new Aggregate(requested.getField(), AggregateOperator.MIN);
            Aggregate max = new Aggregate(requested.getField(), AggregateOperator.MAX);
            var extrema = aggregate(List.of(), request.getFilter(), List.of(min, max), null);
            Serializable first = extrema.isEmpty() ? null : extrema.getFirst().getAggregateValues().get(min);
            Serializable last = extrema.isEmpty() ? null : extrema.getFirst().getAggregateValues().get(max);
            NiceBins nice = NiceBins.covering(first == null ? BigDecimal.ZERO : new BigDecimal(first.toString()), last == null ? BigDecimal.ONE : new BigDecimal(last.toString()), bins.getCount());
            low = low == null ? nice.low() : low;
            high = high == null ? nice.high() : high;
         }
         if(low.compareTo(high) >= 0)
         {
            throw new QBadRequestException("Numeric bin min must be below max.");
         }
         group.withBucket(new NumericBucket(low, high, bins.getCount()));
      }
      List<AggregateResult> top = aggregate(List.of(group), request.getFilter(), List.of(count), request.getLimitGroups());
      List<AggregateResult> all = aggregate(List.of(group), request.getFilter(), List.of(count), null);
      Map<Serializable, Long> counts = new LinkedHashMap<>();
      all.forEach(r -> counts.put(r.getGroupByValues().get(group), count(r)));
      Map<Serializable, Slot> kept = new LinkedHashMap<>();
      for(AggregateResult row : top)
      {
         Serializable value = row.getGroupByValues().get(group);
         if(count(row) >= config.getMinGroupSize())
         {
            kept.put(value, new Slot(value, label(group, field, value), condition(group, field, value), false));
         }
      }
      List<Serializable> remaining = counts.keySet().stream().filter(v -> !kept.containsKey(v) && counts.get(v) >= config.getMinGroupSize()).toList();
      Slot other = null;
      if(!remaining.isEmpty())
      {
         QQueryFilter predicate;
         if(group.getBucket() == null && field.getType() != QFieldType.BOOLEAN && config.getMinGroupSize() == 0 && !kept.containsKey("") && kept.keySet().stream().noneMatch(v -> v instanceof Instant || v instanceof LocalDate))
         {
            List<Serializable> values = kept.keySet().stream().filter(Objects::nonNull).toList();
            predicate = values.isEmpty() ? new QQueryFilter() : new QQueryFilter().withCriteria(group.getFieldName(), QCriteriaOperator.NOT_IN, values);
            predicate = DrillFilters.and(predicate, DrillFilters.nonNull(group.getFieldName(), field.getType().isStringLike()));
            if(!kept.containsKey(null))
            {
               predicate = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR).withSubFilter(predicate).withSubFilter(DrillFilters.nulls(group.getFieldName(), field.getType().isStringLike()));
            }
            if(field.getType().isStringLike())
            {
               predicate = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR).withSubFilter(predicate).withSubFilter(DrillFilters.value(group.getFieldName(), "", true));
            }
         }
         else
         {
            predicate = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR);
            for(Serializable value : remaining)
            {
               predicate.withSubFilter(condition(group, field, value));
            }
         }
         other = new Slot(null, "Other (" + remaining.size() + " groups)", predicate, true);
      }
      return (new Dimension(group, field, kept, counts, other));
   }

   /*******************************************************************************
    ** Native group keys are displayed as labels without changing filter operands.
    *******************************************************************************/
   private String label(GroupBy group, QFieldMetaData field, Serializable value) throws QException
   {
      if(value == null)
      {
         return ("(Blank)");
      }
      if(group.getBucket() instanceof DateBucket date)
      {
         return (AnalysisFormatting.dateBucket(value, AnalysisBackend.DateUnit.valueOf(date.getUnit().name()), ValueUtils.getSessionOrInstanceZoneId()));
      }
      if(group.getBucket() instanceof NumericBucket bins)
      {
         BigDecimal[] edges = edges(bins, ((Number) value).intValue());
         return (AnalysisFormatting.interval(field, edges[0], edges[1]));
      }
      return (security.label(field, value));
   }

   /*******************************************************************************
    ** Match core's final closed numeric bin and exterior bins.
    *******************************************************************************/
   private static BigDecimal[] edges(NumericBucket bins, int index)
   {
      BigDecimal width = bins.getMax().subtract(bins.getMin()).divide(BigDecimal.valueOf(bins.getCount()), MathContext.DECIMAL128);
      return (new BigDecimal[] { index == 0 ? null : index > bins.getCount() ? bins.getMax() : bins.getMin().add(width.multiply(BigDecimal.valueOf(index - 1))),
         index == 0 ? bins.getMin() : index > bins.getCount() ? null : index == bins.getCount() ? bins.getMax() : bins.getMin().add(width.multiply(BigDecimal.valueOf(index))) });
   }

   /*******************************************************************************
    ** Calendar and numeric predicates use core bucket boundaries in the session zone.
    *******************************************************************************/
   private QQueryFilter condition(GroupBy group, QFieldMetaData field, Serializable value) throws QException
   {
      String name = group.getFieldName();
      if(value == null || group.getBucket() == null)
      {
         return (DrillFilters.value(name, value, field.getType().isStringLike()));
      }
      if(group.getBucket() instanceof NumericBucket bins)
      {
         int index = ((Number) value).intValue();
         BigDecimal[] edges = edges(bins, index);
         if(StandInAnalysisBackend.isMemory(request.getTableName()) && index <= bins.getCount())
         {
            GroupBy raw = new GroupBy(field.getType(), name);
            List<BigDecimal> members = aggregate(List.of(raw), request.getFilter(), List.of(count), null).stream()
               .map(r -> r.getGroupByValues().get(raw)).filter(Objects::nonNull).map(v -> new BigDecimal(v.toString())).toList();
            return (new StandInAnalysisBackend(config).numericRange(request.getTableName(), name, members, edges[0], edges[1], index == bins.getCount()));
         }
         return (index > bins.getCount() ? DrillFilters.condition(name, QCriteriaOperator.GREATER_THAN, bins.getMax())
            : DrillFilters.range(name, edges[0], edges[1], index == bins.getCount()));
      }
      DateBucketUnit unit = ((DateBucket) group.getBucket()).getUnit();
      if(unit == DateBucketUnit.HOUR)
      {
         Instant start = ValueUtils.getValueAsInstant(value);
         return (DrillFilters.range(name, start, start.plusSeconds(3600), false));
      }
      if(unit == DateBucketUnit.DAY_OF_WEEK || unit == DateBucketUnit.HOUR_OF_DAY)
      {
         GroupBy raw = new GroupBy(field.getType(), name);
         List<Serializable> members = aggregate(List.of(raw), request.getFilter(), List.of(count), null).stream().map(r -> r.getGroupByValues().get(raw))
            .filter(v -> Objects.equals(AggregateAnalysis.bucket(v, group.getBucket(), ValueUtils.getSessionOrInstanceZoneId()), value)).toList();
         return (DrillFilters.in(name, members));
      }
      LocalDate start = ValueUtils.getValueAsLocalDate(value);
      LocalDate end = switch(unit)
      {
         case YEAR -> start.plusYears(1);
         case QUARTER -> start.plusMonths(3);
         case MONTH -> start.plusMonths(1);
         case WEEK -> start.plusWeeks(1);
         default -> start.plusDays(1);
      };
      return (DrillFilters.dateRange(name, start, end, field.getType() == QFieldType.DATE_TIME, ValueUtils.getSessionOrInstanceZoneId()));
   }

   /*******************************************************************************
    ** A level is the full matrix, row totals, column totals or grand totals.
    *******************************************************************************/
   private Map<List<Slot>, List<Metric>> level(List<Dimension> dimensions) throws QException
   {
      List<Aggregate> requested = new ArrayList<>(measures);
      if(!requested.contains(count))
      {
         requested.add(count);
      }
      Map<List<Slot>, List<AggregateResult>> grouped = new LinkedHashMap<>();
      List<AggregateResult> aggregates = aggregate(dimensions.stream().map(Dimension::group).toList(), request.getFilter(), requested, null);
      if(dimensions.isEmpty() && aggregates.isEmpty())
      {
         AggregateResult empty = new AggregateResult();
         requested.forEach(a -> empty.withAggregateValue(a, a.getOperator() == AggregateOperator.COUNT || a.getOperator() == AggregateOperator.COUNT_DISTINCT ? 0L : null));
         aggregates = List.of(empty);
      }
      for(AggregateResult row : aggregates)
      {
         List<Slot> key = new ArrayList<>();
         for(Dimension dimension : dimensions)
         {
            key.add(dimension.slot(row.getGroupByValues().get(dimension.group()), config.getMinGroupSize()));
         }
         if(!key.contains(null))
         {
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
         }
      }
      Map<List<Slot>, List<Metric>> result = new LinkedHashMap<>();
      for(var entry : grouped.entrySet())
      {
         QQueryFilter predicate = new QQueryFilter();
         entry.getKey().forEach(slot -> predicate.withSubFilter(slot.condition()));
         AggregateResult row = entry.getValue().getFirst();
         if(entry.getKey().stream().anyMatch(Slot::other))
         {
            var merged = aggregate(List.of(), DrillFilters.and(request.getFilter(), predicate), requested, null);
            if(merged.isEmpty())
            {
               continue;
            }
            row = merged.getFirst();
         }
         List<Metric> values = new ArrayList<>();
         for(int i = 0; i < measures.size(); i++)
         {
            PivotMeasureInfo info = measureInfo.get(i);
            Serializable value = row.getAggregateValues().get(measures.get(i));
            Metric metric = new Metric().withKey(info.getKey()).withLabel(info.getLabel()).withValueType(info.getValueType()).withValue(value)
               .withFormatted(AnalysisFormatting.scalar(info.getKey(), value, info.getValueType()));
            if(toManyTables.isEmpty() && multiHopTables.isEmpty())
            {
               metric.withDrill(DrillFilters.drill(request.getTableName(), request.getFilter(), predicate));
            }
            values.add(metric);
         }
         new MinGroupSizeGuard(config.getMinGroupSize()).pivotMetrics(values, count(row));
         result.put(entry.getKey(), values);
      }
      return (result);
   }

   /*******************************************************************************
    ** Count is always present, even when the user selects only non-count measures.
    *******************************************************************************/
   private long count(AggregateResult row)
   {
      Object value = row.getAggregateValues().get(count);
      return (value == null ? 0 : ((Number) value).longValue());
   }

   /*******************************************************************************
    ** Core owns SQL pushdown, joins, record locks, field locks and bounded fallback.
    *******************************************************************************/
   private List<AggregateResult> aggregate(List<GroupBy> groups, QQueryFilter filter, List<Aggregate> aggregates, Integer limit) throws QException
   {
      QQueryFilter query = DrillFilters.restoreTypes(request.getTableName(), filter);
      if(limit != null)
      {
         query.withOrderBy(new QFilterOrderByAggregate(count, false));
      }
      AggregateInput input = new AggregateInput(request.getTableName()).withFilter(query).withQueryJoins(joins.stream().map(QueryJoin::clone).toList())
         .withAllowStreamedFallback(true).withStreamedRowLimit(config.getMaxRows()).withLimit(limit);
      groups.forEach(input::withGroupBy);
      new LinkedHashSet<>(aggregates).forEach(input::withAggregate);
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      var output = com.kingsrook.qbits.dataanalysis.saved.SqlHandoff.aggregate(input);
      partial |= output.getTruncated();
      return (limit == null ? output.getResults() : output.getResults().stream().sorted(java.util.Comparator.comparingLong(this::count).reversed()
         .thenComparing(r -> String.valueOf(r.getGroupByValues().values()))).limit(limit).toList());
   }

   /*******************************************************************************
    ** Header tuples retain typed values; Other is distinguished by its label.
    *******************************************************************************/
   private static List<List<PivotHeader>> headers(List<List<Slot>> keys)
   {
      return (keys.stream().map(key -> key.stream().map(slot -> new PivotHeader().withValue(slot.value()).withLabel(slot.label())).toList()).toList());
   }

   /*******************************************************************************
    ** Partial numbers must never claim an exact drill equivalence.
    *******************************************************************************/
   private void disableDrills(List<Metric> metrics)
   {
      metrics.forEach(m -> m.withDrill(null).withEstimate(true));
   }
}

