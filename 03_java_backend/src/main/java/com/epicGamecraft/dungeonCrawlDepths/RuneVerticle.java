package com.epicGamecraft.dungeonCrawlDepths;

import io.reactivex.Completable;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;

public class RuneVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(RuneVerticle.class);

  @Override
  public Completable rxStart() {
    vertx.eventBus().consumer(rune.name(), this::handleRune);
    return Completable.complete();
  }

  private void handleRune (Message<String> message) {
    message.reply("rune created");
  }
}


