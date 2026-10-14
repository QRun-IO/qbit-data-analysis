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
import com.fasterxml.jackson.annotation.JsonAutoDetect;

/*******************************************************************************
 ** Typed Significance contract for comparison and relationship analysis.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Significance implements Serializable
{
   private String test;
   private Double statistic;
   private Double df;
   private Double pValue;
   private Boolean applicable = false;
   private String sentence;
   private Double cramersV;



   /*******************************************************************************
    ** Getter for test.
    *******************************************************************************/
   public String getTest()
   {
      return (test);
   }



   /*******************************************************************************
    ** Fluent setter for test.
    *******************************************************************************/
   public Significance withTest(String test)
   {
      this.test = test;
      return (this);
   }



   /*******************************************************************************
    ** Getter for statistic.
    *******************************************************************************/
   public Double getStatistic()
   {
      return (statistic);
   }



   /*******************************************************************************
    ** Fluent setter for statistic.
    *******************************************************************************/
   public Significance withStatistic(Double statistic)
   {
      this.statistic = statistic;
      return (this);
   }



   /*******************************************************************************
    ** Getter for df.
    *******************************************************************************/
   public Double getDf()
   {
      return (df);
   }



   /*******************************************************************************
    ** Fluent setter for df.
    *******************************************************************************/
   public Significance withDf(Double df)
   {
      this.df = df;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pValue.
    *******************************************************************************/
   public Double getPValue()
   {
      return (pValue);
   }



   /*******************************************************************************
    ** Fluent setter for pValue.
    *******************************************************************************/
   public Significance withPValue(Double pValue)
   {
      this.pValue = pValue;
      return (this);
   }



   /*******************************************************************************
    ** Getter for applicable.
    *******************************************************************************/
   public Boolean getApplicable()
   {
      return (applicable);
   }



   /*******************************************************************************
    ** Fluent setter for applicable.
    *******************************************************************************/
   public Significance withApplicable(Boolean applicable)
   {
      this.applicable = applicable;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sentence.
    *******************************************************************************/
   public String getSentence()
   {
      return (sentence);
   }



   /*******************************************************************************
    ** Fluent setter for sentence.
    *******************************************************************************/
   public Significance withSentence(String sentence)
   {
      this.sentence = sentence;
      return (this);
   }



   /*******************************************************************************
    ** Getter for cramersV.
    *******************************************************************************/
   public Double getCramersV()
   {
      return (cramersV);
   }



   /*******************************************************************************
    ** Fluent setter for cramersV.
    *******************************************************************************/
   public Significance withCramersV(Double cramersV)
   {
      this.cramersV = cramersV;
      return (this);
   }
}
