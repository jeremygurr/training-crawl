package com.epicGamecraft.dungeonCrawlDepths.test.integration;

import com.epicGamecraft.dungeonCrawlDepths.CouchbaseVerticle;
import com.epicGamecraft.dungeonCrawlDepths.MysqlVerticle;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;

@ExtendWith(VertxExtension.class)
public class TestMysql {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestMysql.class);

  @Test
  void crudMysql(Vertx vertx, VertxTestContext context) throws Throwable {
    vertx.rxDeployVerticle(new MysqlVerticle())
      .subscribe(e -> {
          vertx.eventBus().rxRequest(mysqlInsert.name(), "{\"id\":0,\"username\":\"billybob\",\"password\":\"password\",\"email\":\"som@gmail.com\"}")
            .subscribe(ar -> {
                if (ar.body() == null) {
                  LOGGER.debug("Mysql successfully inserted record.");
                } else {
                  LOGGER.debug("Mysql failed to insert record : " + ar.body());
                }
                context.completeNow();
              },
              err -> {
                LOGGER.debug("Communication between TestMysql.crudMysql error : " + err.getMessage());
                context.failNow(err);
              });
          vertx.eventBus().rxRequest(mysqlQuery.name(), "{\"username\":\"billybob\",\"password\":\"password\"}")
            .subscribe(ar -> {
                LOGGER.debug("Test.crudMysql received reply : " + ar.body());
                context.completeNow();
              },
              err -> {
                LOGGER.debug("Communication between Test.crudMysql error : " + err.getMessage());
                context.failNow(err);
              });
          vertx.eventBus().rxRequest(mysqlDelete.name(), "{\"username\":\"billybob\",\"password\":\"password\"}")
            .subscribe(ar -> {
                LOGGER.debug("Test.crudMysql received reply : " + ar.body());
                if (ar.body() == null) {
                  LOGGER.debug("Mysql successfully deleted user: 'billybob'");
                } else {
                  //fixme: This code never runs even if the user/password combo doesn't exist in database.
                  LOGGER.debug("Mysql failed to delete record : that username/password combination doesn't exist.");
                }
                context.completeNow();
              },
              err -> {
                LOGGER.debug("Communication between Test.crudMysql error : " + err.getMessage());
                context.failNow(err);
              });
        },
        err -> {
          LOGGER.debug("TestMysql.crudMysql issue deploying verticle : " + err.getMessage());
          context.failNow(err);
        });
  }

  @Test
  void forgotPassword(Vertx vertx, VertxTestContext context) throws Throwable {
    vertx.rxDeployVerticle(new MysqlVerticle())
      .subscribe(e -> {
          vertx.eventBus().rxRequest(mysqlPass.name(), "{\"username\":\"billybob\",\"email\":\"som@gmail.com\"}")
            .subscribe(ar -> {
                LOGGER.debug("TestMysql.forgotPassword received reply : " + ar.body());
                context.completeNow();
              },
              err -> {
                LOGGER.debug("Communication between Test.forgotPassword error : " + err.getMessage());
                context.failNow(err);
              });
        },
        err -> {
          LOGGER.debug("TestMysql.forgotPassword issue deploying verticle : " + err.getMessage());
          context.failNow(err);
        });
  }

}
  /*
  //These two tests were used for running and stopping mysql container. It's no longer needed.
  @Test
  void runMysql(Vertx vertx, VertxTestContext context) throws Throwable {
    Process proc = null;
    try {
      proc = Runtime.getRuntime().exec("./start_mysql");
      proc.waitFor();
      BufferedReader reader = new BufferedReader(new InputStreamReader(
        proc.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println(line);
      }
      context.completeNow();
    } catch (IOException e) {
      e.printStackTrace();
      context.failNow(e);
    }
  }

  @Test
  void stopMysql(Vertx vertx, VertxTestContext context) throws Throwable {
    Process proc = null;
    try {
      proc = Runtime.getRuntime().exec("docker stop mysql");
      proc.waitFor();
      BufferedReader reader = new BufferedReader(new InputStreamReader(
        proc.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println(line);
      }
      context.completeNow();
    } catch (IOException e) {
      e.printStackTrace();
      context.failNow(e);
    }
  }

  //Below are two for the CouchbaseVerticle. Requires couchbase container running.
  //TODO: Make this auto-run couchbase container.
  //FIXME: Make it query and insert for something besides login.
  @Test
  void queryCouchbase(Vertx vertx, VertxTestContext context) throws Throwable {
    vertx.rxDeployVerticle(new CouchbaseVerticle())
      .subscribe(e -> {
          vertx.eventBus().rxRequest(couchbaseQuery.name(), "{\"username\":\"jgurr\",\"password\":\"password\"}")
            .subscribe(ar -> {
                LOGGER.debug("Test.queryCouchbase received reply : " + ar.body());
                context.completeNow();
              },
              err -> {
                LOGGER.debug("Communication between Test.queryCouchbase error : " + err.getMessage());
                context.failNow(err);
              });
        },
        err -> {
          LOGGER.debug("TestMysql.queryCouchbase issue deploying verticle : " + err.getMessage());
          context.failNow(err);
        });
  }


  @Test
  void insertCouchbase(Vertx vertx, VertxTestContext context) throws Throwable {
    vertx.rxDeployVerticle(new CouchbaseVerticle())
      .subscribe(e -> {
          vertx.eventBus().rxRequest(couchbaseInsert.name(), "{\"username\":\"jgurr\",\"password\":\"password\",\"email\":\"som@gmail.com\"}")
            .subscribe(ar -> {
                if (ar.body() == null) {
                  LOGGER.debug("Couchbase successfully inserted document.");
                } else {
                  LOGGER.debug("Couchbase failed to insert document : " + ar.body());
                }
                context.completeNow();
              },
              err -> {
                LOGGER.debug("Communication between Test.queryCouchbase error : " + err.getMessage());
                context.failNow(err);
              });
        },
        err -> {
          LOGGER.debug("TestMysql.insertCouchbase issue communicating with " +
            "couchbase verticle. : " + err.getCause());
          context.failNow(err);
        });
  }
*/

