package com.epicGamecraft.dungeonCrawlDepths;

import io.reactivex.Completable;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;

public class GameListVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MysqlVerticle.class);

  @Override
  public Completable rxStart() {
    vertx.eventBus().consumer(gameList.name(), this::handleList);
    return Completable.complete();
  }

  private void handleList(Message<String> message) {
    LOGGER.debug("GameListVerticle.handleList received message: " + message.body());
    vertx.eventBus().rxRequest(mysqlQuery.name(), message.body())
      .subscribe(e -> {
          if (e.body() != null) {
            //This means user did correct username and password.
            LOGGER.debug("User Verticle received reply: " + e.body());
          } else {
            //This means user input wrong username or password.
            LOGGER.debug("Invalid Login");
            //add a message to http verticle to notify user login information was incorrect.
            // (Perhaps add that as part of a json object message reply.)
          }
          message.reply("");
        },
        err -> {
          //This means the eventbus failed to communicate properly with the CouchbaseVerticle or vice versa.
          LOGGER.debug("UserVerticle Error communicating with CouchbaseVerticle: " + err.getMessage());
          message.reply("");
        }
      );
  }


}
