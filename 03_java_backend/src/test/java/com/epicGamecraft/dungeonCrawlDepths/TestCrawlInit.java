package com.epicGamecraft.dungeonCrawlDepths;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;

import io.vertx.junit5.Checkpoint;
import io.vertx.reactivex.core.*;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.reactivex.core.Vertx.*;
import io.reactivex.*;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.*;
import io.reactivex.observers.*;
import io.reactivex.schedulers.*;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.*;
import io.vertx.reactivex.core.*;
import io.vertx.reactivex.core.http.*;
import io.vertx.reactivex.ext.web.*;
import io.vertx.reactivex.core.buffer.*;
import io.vertx.reactivex.core.eventbus.EventBus;

import java.util.concurrent.TimeUnit;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.couchbaseQuery;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;

@ExtendWith(VertxExtension.class)
public class TestCrawlInit {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestCrawlInit.class);


  @Test
  void verticle_deployed(Vertx vertx, VertxTestContext context) throws Throwable {
    vertx.deployVerticle(new HttpServerVerticle(), context.succeeding(id -> context.completeNow()));
    context.completeNow();
  }

  @Test
  void loginSuccessMysql(Vertx vertx, VertxTestContext context) throws Throwable {
    FakeMysqlVerticle mysqlVerticle = new FakeMysqlVerticle();
    mysqlVerticle.response = "{\"email\":\"jared.gurr@yahoo.com\",\"hashword\":\"1216985755\",\"name\":\"Jared Gurr\"}";
    vertx.rxDeployVerticle(mysqlVerticle)
      .flatMap(e -> {
        return vertx.rxDeployVerticle(new UserVerticle());   //This is how you deploy more than one verticle. Just use more .flatmaps().
      })
      .subscribe(e -> {
          vertx.eventBus().rxRequest(userLogin.name(), "{\"usernameOrEmail\":\"jgurr\",\"password\":\"password\"}")
            .subscribe(ar -> {
                LOGGER.debug("Test Verticle received reply: " + ar.body());
              },
              err -> {
                LOGGER.debug("An error occurred retrieving data from Couchbase.");
              });
          context.completeNow();
        },
        err -> {
          context.failNow(err);
        });
  }

  @Test
  void loginFailureMysql(Vertx vertx, VertxTestContext context) throws Throwable {
    FakeMysqlVerticle mysqlVerticle = new FakeMysqlVerticle();
    mysqlVerticle.response = null;
    vertx.rxDeployVerticle(mysqlVerticle)
      .flatMap(e -> vertx.rxDeployVerticle(new UserVerticle()))
      .subscribe(e -> {
          vertx.eventBus().rxRequest(userLogin.name(), "{\"usernameOrEmail\":\"jgr\",\"password\":\"pass\"}")
            .subscribe(ar -> {
                LOGGER.debug("Test Verticle received reply: " + ar.body());
              },
              err -> {
                LOGGER.debug("An error occurred retrieving data from Couchbase." + err.getMessage());
              });
          context.completeNow();
        },
        err -> {
          context.failNow(new Exception("Test Verticle failed to handle login correctly."));
        });
  }

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

  /*
  //Below are two for the CouchbaseVerticle. Requires couchbase container running.
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
          LOGGER.debug("TestCrawlInit.queryCouchbase issue deploying verticle : " + err.getMessage());
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
          LOGGER.debug("TestCrawlInit.insertCouchbase issue communicating with " +
            "couchbase verticle. : " + err.getCause());
          context.failNow(err);
        });
  }

   */

