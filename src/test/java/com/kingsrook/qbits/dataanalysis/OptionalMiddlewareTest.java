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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/*******************************************************************************
 ** A separate JVM proves the optional middleware is not needed by the engine.
 *******************************************************************************/
class OptionalMiddlewareTest
{
   /*******************************************************************************
    ** Remove Javalin and middleware artifacts from the actual test classpath.
    *******************************************************************************/
   @Test
   void engineWithoutMiddleware() throws Exception
   {
      String classpath = Arrays.stream(System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")).split(java.io.File.pathSeparator))
         .filter(entry -> !entry.contains("qqq-middleware-javalin") && !Path.of(entry).getFileName().toString().startsWith("javalin-"))
         .collect(Collectors.joining(java.io.File.pathSeparator));
      Path log = Path.of("target", "core-only-smoke.log");
      Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-cp", classpath, CoreOnlySmoke.class.getName())
         .redirectErrorStream(true).redirectOutput(log.toFile()).start();
      try
      {
         assertThat(process.waitFor(30, TimeUnit.SECONDS)).as("Core-only JVM timed out").isTrue();
         assertThat(process.exitValue()).as(java.nio.file.Files.readString(log)).isZero();
      }
      finally
      {
         if(process.isAlive())
         {
            process.destroyForcibly();
         }
      }
   }

   /*******************************************************************************
    ** Entry point in the classpath with middleware removed.
    *******************************************************************************/
   public static class CoreOnlySmoke
   {
      /*******************************************************************************
       ** Metadata and runtime must initialize without loading any API classes.
       *******************************************************************************/
      public static void main(String[] args) throws Exception
      {
         var instance = TestSupport.instance();
         assertThat(instance.getSupplementalMetaData("dataAnalysisRuntime")).isNotNull();
         assertThat(instance.getSupplementalMetaData("middlewareSpecExtensions")).isNull();
         assertThat(instance.getTable("data_analysis_run")).isNotNull();
      }
   }
}
