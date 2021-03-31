package com.epicGamecraft.dungeonCrawlDepths.test.unit;

import com.epicGamecraft.dungeonCrawlDepths.HttpServerVerticle;
import com.epicGamecraft.dungeonCrawlDepths.UserVerticle;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.userLogin;

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

