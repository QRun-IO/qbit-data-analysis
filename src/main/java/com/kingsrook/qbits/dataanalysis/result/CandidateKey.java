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

package com.kingsrook.qbits.dataanalysis.result;


import java.io.Serializable;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAutoDetect;


/*******************************************************************************
 ** Typed CandidateKey contract; independent of any presentation framework.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CandidateKey implements Serializable
{
   private List<String> fields;
   private Long rowCount;
   private Drill drill;



   /*******************************************************************************
    ** Getter for fields.
    *******************************************************************************/
   public List<String> getFields()
   {
      return (fields);
   }



   /*******************************************************************************
    ** Fluent setter for fields.
    *******************************************************************************/
   public CandidateKey withFields(List<String> fields)
   {
      this.fields = fields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rowCount.
    *******************************************************************************/
   public Long getRowCount()
   {
      return (rowCount);
   }



   /*******************************************************************************
    ** Fluent setter for rowCount.
    *******************************************************************************/
   public CandidateKey withRowCount(Long rowCount)
   {
      this.rowCount = rowCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for drill.
    *******************************************************************************/
   public Drill getDrill()
   {
      return (drill);
   }



   /*******************************************************************************
    ** Fluent setter for drill.
    *******************************************************************************/
   public CandidateKey withDrill(Drill drill)
   {
      this.drill = drill;
      return (this);
   }
}
