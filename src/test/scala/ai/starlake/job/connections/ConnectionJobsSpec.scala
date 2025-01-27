package ai.starlake.job.connections

import ai.starlake.TestHelper
import ai.starlake.config.Settings
import ai.starlake.job.transform.TransformConfig
import ai.starlake.schema.handlers.SchemaHandler
import ai.starlake.schema.model.{AutoJobDesc, AutoTaskDesc, JdbcSink, WriteMode}
import ai.starlake.workflow.IngestionWorkflow
import org.apache.hadoop.fs.Path
import org.apache.spark.sql.SaveMode

class ConnectionJobsSpec extends TestHelper {
  lazy val pathBusiness = new Path(starlakeMetadataPath + "/transform/user.sl.yml")
  new WithSettings() {
    "JDBC 2 JDBC Connection" should "succeed" in {
      pending
      val connection = "test-h2"
      val session = sparkSession
      import session.implicits._
      val usersDF = Seq(
        ("John", "Doe", 10),
        ("Sam", "Hal", 20),
        ("Martin", "Odersky", 30)
      ).toDF("firstname", "lastname", "age")
      val usersOptions =
        settings.appConfig.connections(connection).options + ("dbtable" -> "users.users")
      usersDF.write.format("jdbc").options(usersOptions).mode(SaveMode.Overwrite).save()

      val businessTask1 = AutoTaskDesc(
        "",
        Some(
          "(with user_View as (select * from users.users) select firstname, lastname from user_View where age = {{age}})"
        ),
        None,
        "users",
        "userout",
        Some(WriteMode.OVERWRITE),
        sink = Some(JdbcSink(connectionRef = Some(connection)).toAllSinks()),
        python = None,
        merge = None
      )
      val businessJob =
        AutoJobDesc(
          "user",
          List(businessTask1)
        )

      val businessJobDef = mapper
        .writer()
        .withAttribute(classOf[Settings], settings)
        .writeValueAsString(businessJob)

      storageHandler.write(businessJobDef, pathBusiness)

      val schemaHandler = new SchemaHandler(storageHandler, Map("age" -> "10"))

      val workflow =
        new IngestionWorkflow(storageHandler, schemaHandler)

      workflow.autoJob(TransformConfig("user"))

      val userOutOptions =
        settings.appConfig.connections(connection).options + ("dbtable" -> "users.userout")
      sparkSession.read.format("jdbc").options(userOutOptions).load.collect() should have size 1
    }
  }
}
