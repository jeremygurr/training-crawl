package com.epicGamecraft.dungeonCrawlDepths;



import io.reactivex.Completable;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.mysqlQuery;

public class FakeMysqlVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MysqlVerticle.class);

  @Override
  public Completable rxStart() {
    LOGGER.debug("FakeMysqlVerticle is listening to: " + mysqlQuery.name());
    final EventBus eb = vertx.eventBus();
    eb.consumer(mysqlQuery.name(), this::handleQuery);
    return Completable.complete();
  }
  private void handleQuery(Message<String> message) {
    LOGGER.debug("FakeMysqlVerticle received message: " + message.body());

    message.reply(response);
  }

  public String response = null;
}
