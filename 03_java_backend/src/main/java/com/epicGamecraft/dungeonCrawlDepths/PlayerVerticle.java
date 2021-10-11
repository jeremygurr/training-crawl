package com.epicGamecraft.dungeonCrawlDepths;

import io.reactivex.Completable;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;

public class PlayerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(PlayerVerticle.class);

  @Override
  public Completable rxStart() {
    vertx.eventBus().consumer(player.name(), this::handlePlayer);
    return Completable.complete();
  }

  private void handlePlayer(Message<String> message) {
    message.reply("player created");
  }

}
