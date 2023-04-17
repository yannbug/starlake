/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 *
 */
package ai.starlake.extract

import ai.starlake.utils.CliConfig
import scopt.OParser

case class ExtractDataConfig(
  mapping: String = "",
  outputDir: Option[String] = None,
  limit: Int = 0,
  separator: Char = ';',
  numPartitions: Int = 1,
  parallelism: Option[Int] = None,
  clean: Boolean = false,
  fullExport: Boolean = false
)

object ExtractDataConfig extends CliConfig[ExtractDataConfig] {
  val command = "extract-data"
  val parser: OParser[Unit, ExtractDataConfig] = {
    val builder = OParser.builder[ExtractDataConfig]
    import builder._
    OParser.sequence(
      programName(s"starlake $command"),
      head("starlake", command, "[options]"),
      note(""),
      opt[String]("mapping")
        .action((x, c) => c.copy(mapping = x))
        .required()
        .text("Database tables & connection info"),
      opt[Int]("limit")
        .action((x, c) => c.copy(limit = x))
        .optional()
        .text("Limit number of records"),
      opt[Int]("numPartitions")
        .action((x, c) => c.copy(numPartitions = x))
        .optional()
        .text("parallelism level regarding partitionned tables"),
      opt[Int]("parallelism")
        .action((x, c) => c.copy(parallelism = Some(x)))
        .optional()
        .text(
          s"parallelism level of the extraction process. By default equals to the available cores: ${Runtime.getRuntime().availableProcessors()}"
        ),
      opt[Char]("separator")
        .action((x, c) => c.copy(separator = x))
        .optional()
        .text("Column separator"),
      opt[Unit]("clean")
        .action((x, c) => c.copy(clean = true))
        .optional()
        .text("Cleanup output directory first ?"),
      opt[String]("output-dir")
        .action((x, c) => c.copy(outputDir = Some(x)))
        .required()
        .text("Where to output csv files"),
      opt[Unit]("fullExport")
        .action((x, c) => c.copy(fullExport = true))
        .optional()
        .text("Force full export to all tables")
    )
  }

  /** @param args
    *   args list passed from command line
    * @return
    *   Option of case class JDBC2YmlConfig.
    */
  def parse(args: Seq[String]): Option[ExtractDataConfig] =
    OParser.parse(parser, args, ExtractDataConfig())
}
