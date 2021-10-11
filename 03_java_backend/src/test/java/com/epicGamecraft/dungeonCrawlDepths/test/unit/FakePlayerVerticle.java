package com.epicGamecraft.dungeonCrawlDepths.test.unit;

import io.reactivex.Completable;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;

public class FakePlayerVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(FakePlayerVerticle.class);
  public String response = null;

  @Override
  public Completable rxStart() {
    LOGGER.debug("FakePlayerVerticle is listening to: " + player.name());
    final EventBus eb = vertx.eventBus();
    eb.consumer(player.name(), this::handlePlayer);
    return Completable.complete();
  }

  private void handlePlayer(Message<String> message) {
    LOGGER.debug("FakePlayerVerticle received message: " + message.body());
    message.reply(response);
  }

}
