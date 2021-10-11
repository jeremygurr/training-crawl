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
    vertx.eventBus().rxRequest(mysqlGameList.name(), message.body())
      .subscribe(e -> {
          LOGGER.debug("GameListVerticle.handleList received reply: " + e.body());
          if (e.body() != null) {
            //This means user did correct username and password.
            LOGGER.debug("Successfully retrieved lobby data: " + e.body());
            message.reply("success");
          } else {
            //This means mysql did not have any lobby info to give.
            LOGGER.debug("Error: no data available for lobby.");
            message.reply("failure");
          }
        },
        err -> {
          //This means the eventbus failed to communicate properly with the CouchbaseVerticle or vice versa.
          LOGGER.debug("UserVerticle Error communicating with MysqlVerticle: " + err.getMessage());
          message.reply("communication fail");
        }
      );
  }


}
