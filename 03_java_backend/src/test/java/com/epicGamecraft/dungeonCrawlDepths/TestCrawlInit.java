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

  /*todo: Improve this unit test, by making it do conditional for the possible MysqlVerticle reply message.
    You can also improve it by making this unit test work without mysql container required to be running if possible.
  */
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

}

  /*
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

  @Test
  void passwordCouchbase(Vertx vertx, VertxTestContext context) throws Throwable {
    vertx.rxDeployVerticle(new CouchbaseVerticle())
      .subscribe(e -> {
          vertx.eventBus().rxRequest(couchbasePass.name(), "{\"username\":\"jgurr\",\"email\":\"som@gmail.com\"}")
            .subscribe(ar -> {
                LOGGER.debug("Test.passwordCouchbase received reply : " + ar.body());
                context.completeNow();
              },
              err -> {
                LOGGER.debug("Communication between Test.passwordCouchbase error : " + err.getMessage());
                context.failNow(err);
              });
        },
        err -> {
          LOGGER.debug("TestCrawlInit.queryCouchbase issue deploying verticle : " + err.getMessage());
          context.failNow(err);
        });
  }

  //Below is a test for the UserVerticle using a fake couchbase verticle.
  @Test
  void loginSuccess(Vertx vertx, VertxTestContext context) throws Throwable {
    FakeCouchbaseVerticle couchbaseVerticle = new FakeCouchbaseVerticle();
    couchbaseVerticle.response = "{\"email\":\"jared.gurr@yahoo.com\",\"hashword\":\"1216985755\",\"name\":\"Jared Gurr\"}";
    vertx.rxDeployVerticle(couchbaseVerticle)
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
  void loginFailure(Vertx vertx, VertxTestContext context) throws Throwable {
    FakeCouchbaseVerticle couchbaseVerticle = new FakeCouchbaseVerticle();
    couchbaseVerticle.response = null;
    vertx.rxDeployVerticle(couchbaseVerticle)
      .flatMap(e -> {
        return vertx.rxDeployVerticle(new UserVerticle());
      })
      .subscribe(e -> {
          vertx.eventBus().rxRequest(userLogin.name(), "{\"usernameOrEmail\":\"jgr\",\"password\":\"pass\"}")
            .subscribe(ar -> {
                LOGGER.debug("Test Verticle received reply: " + ar.body());
              },
              err -> {
                LOGGER.debug("An error occurred retrieving data from Couchbase." + err.getMessage());
              });
          context.failNow(new Exception("Test Verticle failed to handle login correctly."));
        },
        err -> {
          context.completeNow();
        });
  }
   */

