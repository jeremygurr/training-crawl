package com.epicGamecraft.dungeonCrawlDepths.test.unit;

import io.reactivex.Completable;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;

public class FakeRuneVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(FakeRuneVerticle.class);
  public String response = null;

  @Override
  public Completable rxStart() {
    LOGGER.debug("FakeRuneVerticle is listening to: " + rune.name());
    final EventBus eb = vertx.eventBus();
    eb.consumer(rune.name(), this::handleRune);
    return Completable.complete();
  }

  private void handleRune(Message<String> message) {
    LOGGER.debug("FakeRuneVerticle received message: " + message.body());
    message.reply(response);
  }

}
