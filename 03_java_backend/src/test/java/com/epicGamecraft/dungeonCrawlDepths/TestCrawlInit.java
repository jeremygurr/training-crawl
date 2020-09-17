//package com.epicGamecraft.dungeonCrawlDepths;
//
//import io.vertx.reactivex.core.*;
//import io.vertx.junit5.VertxExtension;
//import io.vertx.junit5.VertxTestContext;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.concurrent.TimeUnit;
//
//import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.couchbaseQuery;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;
//
//@ExtendWith(VertxExtension.class)
public class TestCrawlInit {
//
//  private static final Logger LOGGER = LoggerFactory.getLogger(TestCrawlInit.class);
//
//
//  @Test
//  void verticle_deployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
//    vertx.deployVerticle(new HttpServerVerticle(), testContext.succeeding(id -> testContext.completeNow()));
//    testContext.completeNow();
//  }
//
//
//  @Test
//  void login(Vertx vertx, VertxTestContext testContext) throws Throwable {
//	  vertx.deployVerticle(new FakeCouchbaseVerticle(), Handler<AsyncResult<String>>() {
//		vertx.eventBus().request(userLogin.name(), message)
//	  }
//  }
//    vertx.deployVerticle(new CouchbaseVerticle(), Handler<AsyncResult<String>>() {
//		vertx.deployVerticle(new UserVerticle(), Handler<AsyncResult<String>>() {
//		vertx.eventBus().request(userLogin.name(), "{ 'usernameOrEmail' : 'Jared Gurr', 'password' "
//				+ ": 'hashpassword' }", ar -> {
//					if (ar.succeeded()) {
//						LOGGER.debug("Test Verticle received reply: " + ar.result().body());
//					}
//				});
//		testContext.completeNow();
//		  });
//
//	}
}

//  @Test
//  void failedLoginAttempt(Vertx vertx, VertxTestContext testContext) throws Throwable {
//    vertx.eventBus().request(couchbaseQuery.name(), "select name from registration where name='jared' "
//    		+ "and ");
//    testContext.completeNow();
//  }
