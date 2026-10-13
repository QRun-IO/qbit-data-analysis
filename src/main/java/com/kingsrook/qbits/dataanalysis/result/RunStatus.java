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
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonAutoDetect;


/*******************************************************************************
 ** Typed RunStatus contract; independent of any presentation framework.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class RunStatus implements Serializable
{
   private String runId;
   private String status;
   private String error;
   private ColumnProfile columnProfile;
   private TableProfile tableProfile;
   private Instant startedAt;
   private Instant finishedAt;



   /*******************************************************************************
    ** Getter for runId.
    *******************************************************************************/
   public String getRunId()
   {
      return (runId);
   }



   /*******************************************************************************
    ** Fluent setter for runId.
    *******************************************************************************/
   public RunStatus withRunId(String runId)
   {
      this.runId = runId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for status.
    *******************************************************************************/
   public String getStatus()
   {
      return (status);
   }



   /*******************************************************************************
    ** Fluent setter for status.
    *******************************************************************************/
   public RunStatus withStatus(String status)
   {
      this.status = status;
      return (this);
   }



   /*******************************************************************************
    ** Getter for error.
    *******************************************************************************/
   public String getError()
   {
      return (error);
   }



   /*******************************************************************************
    ** Fluent setter for error.
    *******************************************************************************/
   public RunStatus withError(String error)
   {
      this.error = error;
      return (this);
   }



   /*******************************************************************************
    ** Getter for columnProfile.
    *******************************************************************************/
   public ColumnProfile getColumnProfile()
   {
      return (columnProfile);
   }



   /*******************************************************************************
    ** Fluent setter for columnProfile.
    *******************************************************************************/
   public RunStatus withColumnProfile(ColumnProfile columnProfile)
   {
      this.columnProfile = columnProfile;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableProfile.
    *******************************************************************************/
   public TableProfile getTableProfile()
   {
      return (tableProfile);
   }



   /*******************************************************************************
    ** Fluent setter for tableProfile.
    *******************************************************************************/
   public RunStatus withTableProfile(TableProfile tableProfile)
   {
      this.tableProfile = tableProfile;
      return (this);
   }



   /*******************************************************************************
    ** Getter for startedAt.
    *******************************************************************************/
   public Instant getStartedAt()
   {
      return (startedAt);
   }



   /*******************************************************************************
    ** Fluent setter for startedAt.
    *******************************************************************************/
   public RunStatus withStartedAt(Instant startedAt)
   {
      this.startedAt = startedAt;
      return (this);
   }



   /*******************************************************************************
    ** Getter for finishedAt.
    *******************************************************************************/
   public Instant getFinishedAt()
   {
      return (finishedAt);
   }



   /*******************************************************************************
    ** Fluent setter for finishedAt.
    *******************************************************************************/
   public RunStatus withFinishedAt(Instant finishedAt)
   {
      this.finishedAt = finishedAt;
      return (this);
   }
}
