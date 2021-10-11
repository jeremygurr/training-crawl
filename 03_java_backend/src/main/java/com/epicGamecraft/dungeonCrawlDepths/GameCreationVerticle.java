package com.epicGamecraft.dungeonCrawlDepths;

import io.reactivex.rxjava3.core.Completable;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;

public class GameCreationVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(PlayerVerticle.class);

  @Override
  public Completable rxStart() {
    vertx.eventBus().consumer(gameCreation.name(), this::handleGameCreate);
    return Completable.complete();
  }

  private void handleGameCreate(Message<String> message) {
    vertx.eventBus().rxRequest(player.name(), message.body())
      .flatMap(deployId -> {
        LOGGER.debug("Making request to create player.");
        return vertx.eventBus().rxRequest(player.name(), message.body());
      })
      .map(ar -> {
        LOGGER.debug("handleGameCreate received reply: " + ar.body());
        return ar;
      })
      .flatMap(deployID -> {
        LOGGER.debug("Making request to create rune.");
        return vertx.eventBus().rxRequest(rune.name(), message.body());
      })
      .map(ar -> {
        LOGGER.debug("handleGameCreate received reploy: " + ar.body());
        return ar;
      })
      .subscribe(f -> {
        message.reply("GameCreated");
      },
        err -> {
        message.reply("Failed to create game");
        });
  }

}
