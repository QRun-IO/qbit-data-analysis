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

package com.kingsrook.qbits.dataanalysis;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qbits.dataanalysis.result.CorrelationRequest;
import com.kingsrook.qbits.dataanalysis.result.CompareRequest;
import com.kingsrook.qqq.backend.core.actions.interfaces.AggregateInterface;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateCapability;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryAggregateAction;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/*******************************************************************************
 ** Force unavailable capabilities without replacing the actual core execution.
 *******************************************************************************/
class RelationshipFallbackTest extends EngineFixture
{
   /*******************************************************************************
    ** Verify restore.
    *******************************************************************************/
   @AfterEach
   void restore()
   {
      QBackendModuleDispatcher.registerBackendModule(new MemoryBackendModule());
   }

   /*******************************************************************************
    ** Verify core streams unsupported correlation and rejects truncation.
    *******************************************************************************/
   @Test
   void coreStreamsUnsupportedCorrelationAndRejectsTruncation() throws Exception
   {
      var request = new CorrelationRequest().withTableName("orders").withFields(List.of("id", "amount", "quantity"));
      var nativeResult = service.correlation(request);
      TrackingModule.fallback = true;
      QBackendModuleDispatcher.registerBackendModule(new TrackingModule());
      var streamed = service.correlation(request);
      for(int i = 0; i < nativeResult.getCells().size(); i++)
      {
         assertThat(streamed.getCells().get(i).getR()).isCloseTo(nativeResult.getCells().get(i).getR(), within(1e-12));
         assertThat(streamed.getCells().get(i).getN()).isEqualTo(nativeResult.getCells().get(i).getN());
      }
      assertThat(service.compare(new CompareRequest().withTableName("orders").withFields(List.of("amount"))).getFields().getFirst().getSignificance().getPValue()).isEqualTo(1);
      config.withDefaultMaxRows(3);
      service.getConfigResolver().invalidate();
      assertThatThrownBy(() -> service.correlation(request)).isInstanceOf(QBadRequestException.class).hasMessageContaining("streamed row limit");
   }

   /*******************************************************************************
    ** Verify all correlations use one aggregate call.
    *******************************************************************************/
   @Test
   void allCorrelationsUseOneAggregateCall() throws Exception
   {
      TrackingModule.fallback = false;
      TrackingModule.calls.set(0);
      QBackendModuleDispatcher.registerBackendModule(new TrackingModule());
      service.correlation(new CorrelationRequest().withTableName("orders").withFields(List.of("id", "amount", "quantity")));
      assertThat(TrackingModule.calls.get()).isEqualTo(1);
   }

   /*******************************************************************************
    ** Verify backend capability execution.
    *******************************************************************************/
   public static class TrackingModule extends MemoryBackendModule
   {
      static boolean fallback;
      static final AtomicInteger calls = new AtomicInteger();

      /*******************************************************************************
       ** Verify get aggregate interface.
       *******************************************************************************/
      @Override
      public AggregateInterface getAggregateInterface()
      {
         return (new MemoryAggregateAction()
         {
            /*******************************************************************************
             ** Verify get supported capabilities.
             *******************************************************************************/
            @Override
            public Set<AggregateCapability> getSupportedCapabilities(AggregateInput input)
            {
               return (fallback ? Set.of() : super.getSupportedCapabilities(input));
            }

            /*******************************************************************************
             ** Verify execute.
             *******************************************************************************/
            @Override
            public AggregateOutput execute(AggregateInput input) throws com.kingsrook.qqq.backend.core.exceptions.QException
            {
               if(!fallback && input.getTableName().equals("orders"))
               {
                  calls.incrementAndGet();
                  assertThat(input.getAggregates()).hasSize(6).allSatisfy(a -> assertThat(a.getOperator()).isEqualTo(AggregateOperator.CORR));
               }
               return (super.execute(input));
            }
         });
      }
   }
}
