package com.epicGamecraft.dungeonCrawlDepths.test.integration;

import com.epicGamecraft.dungeonCrawlDepths.MysqlVerticle;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;

@ExtendWith(VertxExtension.class)
public class TestMysql {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestMysql.class);

  //These mysql tests require the mysql container to be running.
  @Test
  void deleteMysql(Vertx vertx, VertxTestContext context) throws Throwable {
    vertx.rxDeployVerticle(new MysqlVerticle())
      .subscribe(e -> {
          vertx.eventBus().rxRequest(mysqlDelete.name(), "{\"username\":\"billybob\",\"password\":\"password\"}")
            .subscribe(ar -> {
                LOGGER.debug("Test.deleteMysql received reply : " + ar.body());
                if (ar.body() == null) {
                  LOGGER.debug("Mysql successfully deleted record.");
                } else {
                  //fixme: This code never runs even if the user/password combo doesn't exist in database.
                  LOGGER.debug("Mysql failed to delete record : that username/password combo doesn't exist." );
                }
                context.completeNow();
              },
              err -> {
                LOGGER.debug("Communication between Test.deleteMysql error : " + err.getMessage());
                context.failNow(err);
              });
        },
        err -> {
          LOGGER.debug("TestCrawlInit.deleteMysql issue deploying verticle : " + err.getMessage());
          context.failNow(err);
        });
  }

  @Test
  void queryMysql(Vertx vertx, VertxTestContext context) throws Throwable {
    vertx.rxDeployVerticle(new MysqlVerticle())
      .subscribe(e -> {
          vertx.eventBus().rxRequest(mysqlQuery.name(), "{\"username\":\"billybob\",\"password\":\"password\"}")
            .subscribe(ar -> {
                LOGGER.debug("Test.queryMysql received reply : " + ar.body());
                context.completeNow();
              },
              err -> {
                LOGGER.debug("Communication between Test.queryMysql error : " + err.getMessage());
                context.failNow(err);
              });
        },
        err -> {
          LOGGER.debug("TestCrawlInit.queryMysql issue deploying verticle : " + err.getMessage());
          context.failNow(err);
        });
  }

  @Test
  void insertMysql(Vertx vertx, VertxTestContext context) throws Throwable {
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
                LOGGER.debug("Communication between TestCrawlInit.insertMysql error : " + err.getMessage());
                context.failNow(err);
              });
        },
        err -> {
          LOGGER.debug("TestCrawlInit.insertMysql issue communicating with " +
            "MysqlVerticle. : " + err.getCause());
          context.failNow(err);
        });
  }

  @Test
  void forgotPassword(Vertx vertx, VertxTestContext context) throws Throwable {
    vertx.rxDeployVerticle(new MysqlVerticle())
      .subscribe(e -> {
          vertx.eventBus().rxRequest(mysqlPass.name(), "{\"username\":\"billybob\",\"email\":\"som@gmail.com\"}")
            .subscribe(ar -> {
                LOGGER.debug("TestCrawlInit.forgotPassword received reply : " + ar.body());
                context.completeNow();
              },
              err -> {
                LOGGER.debug("Communication between Test.forgotPassword error : " + err.getMessage());
                context.failNow(err);
              });
        },
        err -> {
          LOGGER.debug("TestCrawlInit.forgotPassword issue deploying verticle : " + err.getMessage());
          context.failNow(err);
        });
  }
}
