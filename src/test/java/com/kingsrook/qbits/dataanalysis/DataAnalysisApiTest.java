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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.dataanalysis.api.DataAnalysisApi;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import com.kingsrook.qqq.middleware.javalin.specs.invoke.UiApiInvoker;
import com.kingsrook.qqq.middleware.javalin.specs.invoke.UiApiRequest;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import com.kingsrook.qqq.middleware.javalin.tools.UiApiContract;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/*******************************************************************************
 ** Real HTTP session, schema and drill round-trip acceptance tests.
 *******************************************************************************/
class DataAnalysisApiTest extends EngineFixture
{
   private QApplicationJavalinServer server;
   private UiApiInvoker invoker;
   private String caller = "analysis-user";

   /*******************************************************************************
    ** Give MOCK sessions only the grants represented by their caller cookie.
    *******************************************************************************/
   public static class Sessions implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       ** Build the host's ordinary user permission and record-lock policy.
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         QSession grants = TestSupport.admin().withPermissions("orders.read", "customer.read").withSecurityKeyValue("orders.all", true);
         session.setPermissions(grants.getPermissions());
         session.setSecurityKeyValues(grants.getSecurityKeyValues());
         if(!"denied".equals(session.getUuid()))
         {
            session.withPermission("orders.analyze");
         }
         session.withSecurityKeyValue("data_analysis_user", session.getUser().getIdReference());
      }
   }

   /*******************************************************************************
    ** Start on an OS-selected port using the actual spec router and authentication.
    *******************************************************************************/
   private void start() throws Exception
   {
      instance.withInstanceDefaultAuthentication(new QAuthenticationMetaData().withName("http-test").withType(QAuthenticationType.MOCK)
         .withCustomizer(new QCodeReference(Sessions.class)));
      QInstance fixture = instance;
      server = new QApplicationJavalinServer(new AbstractQQQApplication()
      {
         @Override
         public QInstance defineQInstance()
         {
            return (fixture);
         }
      }).withPort(0).withServeFrontendNext(false).withServeFrontendMaterialDashboard(false)
         .withServeLegacyUnversionedMiddlewareAPI(false).withStrictUiApiRoutes(true);
      server.start();
      invoker = UiApiInvoker.forServer(server);
   }

   /*******************************************************************************
    ** Stop the server before the fixture releases context.
    *******************************************************************************/
   @AfterEach
   void stop()
   {
      if(server != null)
      {
         server.stop();
      }
      QJavalinImplementation.setSessionCookieHttpOnly(false);
   }

   /*******************************************************************************
    ** Invoke through loopback, forwarding the actual session cookie.
    *******************************************************************************/
   private JsonNode call(HttpMethod method, String path, Object body, int status) throws Exception
   {
      var request = new UiApiRequest().withMethod(method).withPath(DataAnalysisApi.PREFIX + path)
         .withPathParam("table", "orders").withSessionUUID(caller).withUserTimezone("America/Chicago");
      if(body != null)
      {
         request.withJsonBody(body);
      }
      var response = invoker.invoke(request);
      assertThat(response.getStatus()).as(response.getBody()).isEqualTo(status);
      return (response.getJson());
   }

   /*******************************************************************************
    ** All operations, assistant discovery, OpenAPI and count-drill equivalence.
    *******************************************************************************/
   @Test
   void happyPathsAndDrills() throws Exception
   {
      start();
      JsonNode capabilities = call(HttpMethod.GET, "/tables/{table}/capabilities", null, 200);
      assertThat(capabilities.path("canAnalyze").asBoolean()).isTrue();
      assertThat(capabilities.path("backendCapabilities").toString()).contains("NUMERIC_BUCKET");
      assertThat(capabilities.path("fields").toString()).doesNotContain("secret").contains("CATEGORY", "HISTOGRAM");
      for(String field : List.of("amount", "note", "created", "order_date", "paid", "status"))
      {
         checkDrills(call(HttpMethod.POST, "/tables/{table}/column-profile", Map.of("fieldName", field), 200));
      }
      checkDrills(call(HttpMethod.POST, "/tables/{table}/column-profile", Map.of("fieldName", "amount", "filter",
         Map.of("criteria", List.of(Map.of("fieldName", "created", "operator", "EQUALS", "values", List.of("2026-01-01T12:00:00Z"))))), 200));
      checkDrills(call(HttpMethod.POST, "/tables/{table}/distinct-values", Map.of("fieldName", "status", "pageSize", 1), 200));
      checkDrills(call(HttpMethod.POST, "/tables/{table}/table-profile", Map.of("fields", List.of("amount", "note")), 200));
      checkDrills(call(HttpMethod.POST, "/tables/{table}/duplicates", Map.of("fields", List.of("quantity"), "limit", 2), 200));
      for(String operation : List.of("COLUMN_PROFILE", "TABLE_PROFILE"))
      {
         JsonNode started = call(HttpMethod.POST, "/runs", Map.of("operation", operation, "tableName", "orders", "request",
            operation.equals("COLUMN_PROFILE") ? Map.of("fieldName", "quantity") : Map.of("fields", List.of("paid"))), 200);
         JsonNode run = null;
         for(int i = 0; i < 100; i++)
         {
            var response = invoker.invoke(new UiApiRequest().withMethod(HttpMethod.GET).withPath(DataAnalysisApi.PREFIX + "/runs/{runId}")
               .withPathParam("runId", started.path("runId").asText()).withSessionUUID(caller).withUserTimezone("America/Chicago"));
            assertThat(response.getStatus()).as(response.getBody()).isEqualTo(200);
            run = response.getJson();
            if(!run.path("status").asText().equals("RUNNING"))
            {
               break;
            }
            Thread.sleep(20);
         }
         assertThat(run.path("status").asText()).isEqualTo("COMPLETE");
         var denied = invoker.invoke(new UiApiRequest().withMethod(HttpMethod.GET).withPath(DataAnalysisApi.PREFIX + "/runs/{runId}")
            .withPathParam("runId", started.path("runId").asText()).withSessionUUID("another-owner"));
         assertThat(denied.getStatus()).isEqualTo(403);
         assertThat(run.path(operation.equals("COLUMN_PROFILE") ? "columnProfile" : "tableProfile").isObject()).isTrue();
      }
      assertThat(invoker.listOperations().stream().filter(o -> o.pathTemplate().startsWith(DataAnalysisApi.PREFIX)).toList()).hasSize(8);
      var client = java.net.http.HttpClient.newHttpClient();
      var response = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:" + server.getBoundPort() + "/qqq/v1/openapi.json")).build(), java.net.http.HttpResponse.BodyHandlers.ofString());
      assertThat(response.statusCode()).isEqualTo(200);
      JsonNode api = new ObjectMapper().readTree(response.body());
      assertThat(api.path("paths").has("/qqq/v1/qbit/data-analysis/tables/{table}/column-profile")).isTrue();
      assertThat(api.path("components").path("schemas").has("DataAnalysisMetricV1")).isTrue();
   }

   /*******************************************************************************
    ** Estimate payloads preserve exact totals and send drills to all matching rows.
    *******************************************************************************/
   @Test
   void estimateDrills() throws Exception
   {
      config.withDefaultSampleThresholdRows(2).withDefaultSampleRows(4);
      instance.withSupplementalMetaData(new DataAnalysisRuntime(config));
      start();
      JsonNode profile = call(HttpMethod.POST, "/tables/{table}/column-profile", Map.of("fieldName", "amount"), 200);
      assertThat(profile.path("estimate").asBoolean()).isTrue();
      assertThat(profile.path("sampleRows").asLong()).isEqualTo(4);
      JsonNode rows = profile.path("counts").get(0);
      assertThat(rows.path("value").asLong()).isEqualTo(12);
      assertThat(rows.path("estimate").asBoolean()).isFalse();
      assertThat(profile.path("counts").get(1).path("formatted").asText()).startsWith("≈ ");
      assertThat(profile.toString()).doesNotContain("\"operator\":\"IN\"");
      var response = invoker.invoke(new UiApiRequest().withMethod(HttpMethod.POST).withPath("/table/{tableName}/count")
         .withPathParam("tableName", "orders").withJsonBody(Map.of("filter", rows.path("drill").path("filter")))
         .withSessionUUID(caller).withUserTimezone("America/Chicago"));
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(response.getJson().path("count").asLong()).isEqualTo(12);
      checkDrills(profile);
      JsonNode table = call(HttpMethod.POST, "/tables/{table}/table-profile", Map.of("fields", List.of("amount", "quantity")), 200);
      assertThat(table.path("estimate").asBoolean()).isTrue();
      assertThat(table.path("rowCount").path("value").asLong()).isEqualTo(12);
      checkDrills(table);
   }

   /*******************************************************************************
    ** Recursively post returned count drills to the core count endpoint.
    *******************************************************************************/
   private void checkDrills(JsonNode node) throws Exception
   {
      if(node.path("estimate").asBoolean(false))
      {
         return;
      }
      if(node.isObject() && node.hasNonNull("drill"))
      {
         JsonNode expected = node.has("count") ? node.get("count") : node.has("y") ? node.get("y") : "COUNT".equals(node.path("valueType").asText()) ? node.get("value") : null;
         if(expected != null && expected.isNumber())
         {
            JsonNode drill = node.get("drill");
            var response = invoker.invoke(new UiApiRequest().withMethod(HttpMethod.POST).withPath("/table/{tableName}/count")
               .withPathParam("tableName", drill.path("tableName").asText()).withJsonBody(Map.of("filter", drill.get("filter"))).withSessionUUID(caller).withUserTimezone("America/Chicago"));
            assertThat(response.getStatus()).as(response.getBody()).isEqualTo(200);
            assertThat(response.getJson().path("count").asLong()).as(drill.toString()).isEqualTo(expected.asLong());
         }
      }
      if(node.isContainerNode())
      {
         for(JsonNode child : node)
         {
            checkDrills(child);
         }
      }
   }

   /*******************************************************************************
    ** Denials, locked fields, missing tables and malformed request bodies.
    *******************************************************************************/
   @Test
   void errors() throws Exception
   {
      start();
      call(HttpMethod.POST, "/tables/{table}/column-profile", Map.of("fieldName", "secret"), 400);
      call(HttpMethod.POST, "/tables/{table}/column-profile", Map.of("fieldName", "missing"), 400);
      call(HttpMethod.POST, "/tables/{table}/column-profile", Map.of(), 400);
      call(HttpMethod.POST, "/tables/{table}/column-profile", Map.of("fieldName", 1), 400);
      call(HttpMethod.POST, "/tables/{table}/column-profile", Map.of("fieldName", "amount", "exact", "yes"), 400);
      call(HttpMethod.POST, "/tables/{table}/distinct-values", Map.of("fieldName", "note", "pageSize", 0), 400);
      call(HttpMethod.POST, "/tables/{table}/duplicates", Map.of("fields", List.of()), 400);
      call(HttpMethod.POST, "/tables/{table}/table-profile", Map.of("fields", "amount"), 400);
      call(HttpMethod.POST, "/runs", Map.of("operation", "PIVOT"), 400);
      call(HttpMethod.POST, "/runs", com.fasterxml.jackson.databind.node.NullNode.getInstance(), 400);
      call(HttpMethod.POST, "/tables/{table}/column-profile", Map.of("fieldName", "amount", "filter",
         Map.of("criteria", List.of(Map.of("fieldName", "secret", "operator", "EQUALS", "values", List.of("hidden"))))), 403);
      var absent = invoker.invoke(new UiApiRequest().withMethod(HttpMethod.GET).withPath(DataAnalysisApi.PREFIX + "/tables/{table}/capabilities")
         .withPathParam("table", "missing").withSessionUUID(caller).withUserTimezone("America/Chicago"));
      assertThat(absent.getStatus()).isEqualTo(404);
      caller = "denied";
      assertThat(call(HttpMethod.GET, "/tables/{table}/capabilities", null, 200).path("canAnalyze").asBoolean()).isFalse();
      call(HttpMethod.POST, "/tables/{table}/column-profile", Map.of("fieldName", "amount"), 403);
      call(HttpMethod.POST, "/tables/{table}/distinct-values", Map.of("fieldName", "amount"), 403);
      call(HttpMethod.POST, "/tables/{table}/table-profile", Map.of(), 403);
      call(HttpMethod.POST, "/tables/{table}/duplicates", Map.of("fields", List.of("amount")), 403);
      call(HttpMethod.POST, "/runs", Map.of("operation", "COLUMN_PROFILE", "tableName", "orders", "request", Map.of("fieldName", "amount")), 403);
   }

   /*******************************************************************************
    ** Generated fragment is committed verbatim and has only the eight operations.
    *******************************************************************************/
   @Test
   void contract() throws Exception
   {
      MiddlewareVersionV1 version = new MiddlewareVersionV1();
      version.setQInstance(instance);
      String yaml = UiApiContract.generateExtensionYaml(version, DataAnalysisApi.extension());
      Path path = Path.of("src/main/resources/openapi/qqq-v1/data-analysis.yaml");
      if(Boolean.getBoolean("openapi.write"))
      {
         Files.createDirectories(path.getParent());
         Files.writeString(path, yaml);
      }
      assertThat(Files.exists(path)).as("Regenerate with ./mvnw -o test -Dtest=DataAnalysisApiTest#contract -Dopenapi.write=true -Dqqq.snapshot.version=4.1.0-DATAANALYSIS-SNAPSHOT").isTrue();
      assertThat(Files.readString(path).replace("\r\n", "\n")).as("OpenAPI changed: regenerate with -Dopenapi.write=true").isEqualTo(yaml.replace("\r\n", "\n"));
   }
   @Test
   void pivotHttp() throws Exception
   {
      start();
      var body = Map.of("rows", List.of(Map.of("field", "status")), "columns", List.of(Map.of("field", "customer")),
         "values", List.of(Map.of("operator", "COUNT")));
      JsonNode result = call(HttpMethod.POST, "/tables/{table}/pivot", body, 200);
      assertThat(result.path("cells").size()).isEqualTo(4);
      checkDrills(result);
      checkDrills(call(HttpMethod.POST, "/tables/{table}/pivot", Map.of("rows", List.of(Map.of("field", "paid")),
         "values", List.of(Map.of("operator", "COUNT")), "limitGroups", 1), 200));
      for(String field : List.of("note", "amount", "quantity"))
      {
         checkDrills(call(HttpMethod.POST, "/tables/{table}/pivot", Map.of("rows", List.of(Map.of("field", field)),
            "values", List.of(Map.of("operator", "COUNT")), "limitGroups", 1), 200));
      }
      for(String unit : List.of("DAY", "HOUR", "DAY_OF_WEEK", "HOUR_OF_DAY"))
      {
         checkDrills(call(HttpMethod.POST, "/tables/{table}/pivot", Map.of("rows", List.of(Map.of("field", "created", "dateBucket", unit)),
            "values", List.of(Map.of("operator", "COUNT"))), 200));
      }
      checkDrills(call(HttpMethod.POST, "/tables/{table}/pivot", Map.of("rows", List.of(Map.of("field", "amount", "numericBins", Map.of("count", 10))),
         "values", List.of(Map.of("operator", "COUNT"))), 200));
      for(String field : List.of("secret", "missing"))
      {
         call(HttpMethod.POST, "/tables/{table}/pivot", Map.of("rows", List.of(Map.of("field", field)), "values", List.of(Map.of("operator", "COUNT"))), 400);
         call(HttpMethod.POST, "/tables/{table}/pivot", Map.of("rows", List.of(Map.of("field", "status")), "values", List.of(Map.of("operator", "MIN", "field", field))), 400);
      }
      call(HttpMethod.POST, "/tables/{table}/pivot", Map.of(), 400);
      call(HttpMethod.POST, "/tables/{table}/pivot", Map.of("rows", "status", "values", List.of(Map.of("operator", "COUNT"))), 400);
      call(HttpMethod.POST, "/tables/{table}/pivot", Map.of("rows", List.of(Map.of("field", "status")), "values", List.of(Map.of("operator", "COUNT")), "limitGroups", 1.5), 400);
      caller = "denied";
      call(HttpMethod.POST, "/tables/{table}/pivot", body, 403);
   }
}
