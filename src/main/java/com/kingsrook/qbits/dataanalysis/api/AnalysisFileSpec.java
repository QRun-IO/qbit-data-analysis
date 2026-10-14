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

package com.kingsrook.qbits.dataanalysis.api;

import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dataanalysis.DataAnalysisRuntime;
import com.kingsrook.qbits.dataanalysis.saved.AnalysisExport;
import com.kingsrook.qbits.dataanalysis.saved.SavedDefinition;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.middleware.javalin.executors.AbstractMiddlewareExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareOutputInterface;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Response;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;

/*******************************************************************************
 ** Binary endpoints extend AbstractEndpointSpec directly, preserving core sessions.
 *******************************************************************************/
public class AnalysisFileSpec extends AbstractEndpointSpec<AnalysisFileSpec.Input, AnalysisFileSpec.Output, AnalysisFileSpec.Executor>
{
   /*******************************************************************************
    ** POST an arbitrary supported typed request plus CSV/XLSX format.
    *******************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation().withPath(DataAnalysisApi.PREFIX + "/export").withHttpMethod(HttpMethod.POST)
         .withShortSummary("Export an analysis result").withLongDescription("Execute a typed analysis as the signed-in viewer and download its result table as CSV or XLSX. Requires source READ/Analyze and FULL fields; uses core report formats."));
   }

   /*******************************************************************************
    ** Saved export has only id/format parameters; ad hoc export uses a JSON body.
    *******************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (List.of());
   }

   /*******************************************************************************
    ** Typed discriminated request plus file format.
    *******************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      return (new RequestBody().withRequired(true).withContent(Map.of("application/json", new Content()
         .withSchema(new Schema().withRef("#/components/schemas/DataAnalysisSavedRequestV1")))));
   }

   /*******************************************************************************
    ** Both file representations are declared, without JSON response wrapping.
    *******************************************************************************/
   @Override
   public Map<Integer, Response> defineResponses()
   {
      Schema file = new Schema().withRef("#/components/schemas/DataAnalysisFileV1");
      return (Map.of(200, new Response().withDescription("Analysis result file").withContent(new java.util.TreeMap<>(Map.of("text/csv", new Content().withSchema(file),
         "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new Content().withSchema(file))))));
   }

   /*******************************************************************************
    ** Read transport data only; execution waits for middleware session setup.
    *******************************************************************************/
   @Override
   public Input buildInput(Context context) throws Exception
   {
      Input input = new Input();
      if(this instanceof Saved)
      {
         input.id = SavedApi.id(context.pathParam("id"));
         input.format = context.queryParam("format");
      }
      else
      {
         if(context.contentType() == null || !context.contentType().toLowerCase(java.util.Locale.ROOT).startsWith("application/json"))
         {
            throw new QBadRequestException("The request body must be JSON.");
         }
         input.body = SavedDefinition.object(context.body());
         input.format = input.body.get("format") instanceof String value ? value : null;
      }
      AnalysisExport.format(input.format);
      return (input);
   }

   /*******************************************************************************
    ** Actual file bytes, a stable filename and the matching MIME type.
    *******************************************************************************/
   @Override
   public void handleOutput(Context context, Output output)
   {
      context.contentType(output.file.mimeType()).header("Content-Disposition", "attachment; filename=\"" + output.file.filename() + "\"")
         .header("Cache-Control", "no-store").result(output.file.bytes());
   }

   /*******************************************************************************
    ** Saved GET variant uses the same file executor.
    *******************************************************************************/
   public static class Saved extends AnalysisFileSpec
   {
      /*******************************************************************************
       ** Core-visible saved definition is re-run as viewer before export.
       *******************************************************************************/
      @Override
      public BasicOperation defineBasicOperation()
      {
         return (new BasicOperation().withPath(DataAnalysisApi.PREFIX + "/saved/{id}/export").withHttpMethod(HttpMethod.GET)
            .withShortSummary("Export a saved analysis").withLongDescription("Re-run a visible saved analysis as the viewer, applying current source permissions and dropping unavailable fields, then download its result table as CSV or XLSX."));
      }

      /*******************************************************************************
       ** Required id and CSV/XLSX query format.
       *******************************************************************************/
      @Override
      public List<Parameter> defineRequestParameters()
      {
         return (List.of(new Parameter().withName("id").withIn(In.PATH).withRequired(true).withDescription("Visible saved analysis id.").withSchema(new Schema().withType(Type.INTEGER)),
            new Parameter().withName("format").withIn(In.QUERY).withRequired(true).withDescription("Result file format.").withSchema(new Schema().withType(Type.STRING).withEnumValues(List.of("csv", "xlsx")))));
      }

      /*******************************************************************************
       ** GET has no request body.
       *******************************************************************************/
      @Override
      public RequestBody defineRequestBody()
      {
         return (null);
      }
   }

   /*******************************************************************************
    ** Input data only, inherited middleware fields carry the authenticated session.
    *******************************************************************************/
   public static class Input extends AbstractMiddlewareInput
   {
      private Integer id;
      private String format;
      private Map<String, Object> body;
   }

   /*******************************************************************************
    ** Binary output stays out of JSON serialization.
    *******************************************************************************/
   public static class Output implements AbstractMiddlewareOutputInterface
   {
      private AnalysisExport.File file;
   }

   /*******************************************************************************
    ** Invoked after the normal middleware session pipeline.
    *******************************************************************************/
   public static class Executor extends AbstractMiddlewareExecutor<Input, Output>
   {
      /*******************************************************************************
       ** No direct source access outside the existing authorized analysis engine.
       *******************************************************************************/
      @Override
      public void execute(Input input, Output output) throws QException
      {
         var result = input.id == null ? SavedApi.definition(input.body, null).run(DataAnalysisRuntime.service(), true)
            : DataAnalysisRuntime.saved().run(input.id);
         output.file = AnalysisExport.file(result, input.format);
      }
   }
}
