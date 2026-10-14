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

package com.kingsrook.qbits.dataanalysis.metadata;


import java.util.List;
import com.kingsrook.qbits.dataanalysis.DataAnalysisQBitConfig;
import com.kingsrook.qbits.dataanalysis.DataAnalysisQBitProducer;
import com.kingsrook.qbits.dataanalysis.app.DataAnalysisHome;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitComponentMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** The qbit's component producer, discovered only under the metadata package.
 *******************************************************************************/
public class DataAnalysisMetaDataProducer extends QBitComponentMetaDataProducer<MetaDataProducerMultiOutput, DataAnalysisQBitConfig>
{
   /*******************************************************************************
    ** Skip standalone host scans until configured by the owning qbit.
    *******************************************************************************/
   @Override
   public boolean isEnabled()
   {
      return (getQBitConfig() != null);
   }



   /*******************************************************************************
    ** Produce visible tables, sources, locks, relations and the Analyses home.
    *******************************************************************************/
   @Override
   public MetaDataProducerMultiOutput produce(QInstance instance)
   {
      MetaDataProducerMultiOutput output = new MetaDataProducerMultiOutput();
      DataAnalysisTables.possibleValueSources().forEach(output::add);
      DataAnalysisTables.joins().forEach(output::add);
      DataAnalysisTables.childListWidgets().forEach(output::add);
      output.add(DataAnalysisTables.userKeyType());
      List<QTableMetaData> tables = DataAnalysisTables.tables(getQBitConfig());
      for(QTableMetaData table : tables)
      {
         table.setSourceQBitName(instance.getQBits().values().stream()
            .filter(qbit -> DataAnalysisQBitProducer.ARTIFACT_ID.equals(qbit.getArtifactId())).findFirst().orElseThrow().getName());
         output.add(table);
      }
      DataAnalysisHome.widgets().forEach(output::add);
      output.add(com.kingsrook.qbits.dataanalysis.app.SavedAnalysisWidget.widget());
      output.add(DataAnalysisHome.app(getQBitConfig(), tables));
      return (output);
   }
}
