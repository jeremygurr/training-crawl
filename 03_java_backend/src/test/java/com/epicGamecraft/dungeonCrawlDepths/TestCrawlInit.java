package com.epicGamecraft.dungeonCrawlDepths;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;

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
  void loginSuccess(Vertx vertx, VertxTestContext context) throws Throwable {
    FakeCouchbaseVerticle couchbaseVerticle = new FakeCouchbaseVerticle();
    couchbaseVerticle.response = "{\"email\":\"jared.gurr@yahoo.com\",\"hashword\":\"1216985755\",\"name\":\"Jared Gurr\"}";
    vertx.rxDeployVerticle(couchbaseVerticle)
      .flatMap(e -> {
        return vertx.rxDeployVerticle(new UserVerticle());   //This is how you set multiple deploy verticles. Use more .flatmaps().
      })
      .subscribe(e -> {
          vertx.eventBus().rxRequest(userLogin.name(), "{\"usernameOrEmail\":\"jgurr\",\"password\":\"password\"}")
            .subscribe(ar -> {
                LOGGER.debug("User Verticle received reply: " + ar.body());
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
  //TODO: Figure out what this actually tests for? I am confused...
  // shouldn't it test for making sure the verticle handles incorrect syntax correctly or something?
  @Test
  void loginFailure(Vertx vertx, VertxTestContext context) throws Throwable {
    FakeCouchbaseVerticle couchbaseVerticle = new FakeCouchbaseVerticle();
    couchbaseVerticle.response = null;
    vertx.rxDeployVerticle(couchbaseVerticle)
      .flatMap(e -> {
        return vertx.rxDeployVerticle(new UserVerticle());   //This is how you set multiple deploy verticles. Use more .flatmaps().
      })
      .subscribe(e -> {
          vertx.eventBus().rxRequest(userLogin.name(), "{\"usernameOrEmail\":\"jgurr\",\"password\":\"password\"}")
            .subscribe(ar -> {
                LOGGER.debug("User Verticle received reply: " + ar.body());
              },
              err -> {
                LOGGER.debug("An error occurred retrieving data from Couchbase.");
              });
          context.failNow(new Exception("User Verticle failed to handle login correctly."));
        },
        err -> {
          context.completeNow();
        });
  }
}
