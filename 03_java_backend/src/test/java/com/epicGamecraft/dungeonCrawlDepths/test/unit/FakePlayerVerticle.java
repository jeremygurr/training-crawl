package com.epicGamecraft.dungeonCrawlDepths.test.unit;

import io.reactivex.rxjava3.core.Completable;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.core.eventbus.Message;
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
