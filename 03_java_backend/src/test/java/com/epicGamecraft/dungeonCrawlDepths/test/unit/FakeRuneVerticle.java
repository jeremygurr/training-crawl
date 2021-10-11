package com.epicGamecraft.dungeonCrawlDepths.test.unit;

import io.reactivex.rxjava3.core.Completable;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.core.eventbus.Message;
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
