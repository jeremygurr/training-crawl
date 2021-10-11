package com.epicGamecraft.dungeonCrawlDepths;

import io.reactivex.rxjava3.core.Completable;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.Message;
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


