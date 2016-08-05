package com.weather.scalacass.util

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.{DockerContainer, DockerFactory, DockerKit, DockerReadyChecker}
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.collection.JavaConverters._

trait DockerCassandraService extends DockerKit {

  val DefaultCqlPort = 9042

  override implicit val dockerFactory: DockerFactory =
    new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  val cassandraContainer = DockerContainer("whisk/cassandra:2.1.8")
    .withPorts(DefaultCqlPort -> Some(DefaultCqlPort))
    .withReadyChecker(DockerReadyChecker.LogLineContains("Starting listening for CQL clients on"))

  abstract override def dockerContainers = cassandraContainer :: super.dockerContainers
}

trait DockerCassandra extends FlatSpec with Matchers with BeforeAndAfter with DockerCassandraService with DockerTestKit {
  implicit val pc = PatienceConfig(Span(60, Seconds), Span(1, Second))
  var client: CassandraClient = _
  override def beforeAll(): Unit = {
    super.beforeAll()
    client = CassandraClient(List("localhost"), Some(DefaultCqlPort))
  }

  after {
    val keyspaces = client.cluster.getMetadata.getKeyspaces.asScala.map(_.getName).filterNot(ks => ks == "system_traces" || ks == "system")
    keyspaces.foreach(k => client.session.execute(s"drop keyspace $k"))
  }

  override def afterAll(): Unit = {
    client.close()
    super.afterAll()
  }
}
