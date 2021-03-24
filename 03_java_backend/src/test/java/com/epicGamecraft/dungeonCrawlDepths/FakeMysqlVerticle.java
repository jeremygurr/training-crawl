package com.epicGamecraft.dungeonCrawlDepths;


import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.reactivex.mysqlclient.MySQLPool;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
import io.vertx.reactivex.sqlclient.Tuple;
import io.vertx.sqlclient.PoolOptions;

import io.reactivex.Completable;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.sqlclient.SqlResult;
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

    /*
//Example of retrieving data using mysql. Add this code to actual mysql verticle later.
    MySQLConnectOptions connectOptions = new MySQLConnectOptions()
      .setPort(3306)
      .setHost("localhost")
      .setDatabase("crawl")
      .setUser("root")
      .setPassword("password");

// Pool options
    PoolOptions poolOptions = new PoolOptions()
      .setMaxSize(5);

// Create the client pool
    MySQLPool client = MySQLPool.pool(connectOptions, poolOptions);

// A simple query
    client
      .query("SHOW DATABASES")
      .execute(ar -> {
        if (ar.succeeded()) {
          RowSet<Row> result = ar.result();
          System.out.println("Got " + result.size() + " rows ");
          LOGGER.debug("full result object: " + result);
        } else {
          System.out.println("Failure: " + ar.cause().getMessage());
        }

        // Now close the pool
        client.close();
      });
     */

/*
  @Override
  public Completable rxStart() {
    LOGGER.debug("FakeCouchbase Verticle listening to: " + couchbaseQuery.name());
    final EventBus eb = vertx.eventBus();
    eb.consumer(couchbaseQuery.name(), this::handleQuery);
    return Completable.complete();
  }

  final JsonObject empty = JsonObject.create();

  private void handleQuery(Message<String> message) {
    LOGGER.debug("FakeCouchbase Verticle received message: " + message.body());

    //example of accessing couchbase the proper way:
    final JsonObject json = JsonObject.fromJson(message.body());
    LOGGER.debug("json is: " + json);
    final String username = json.getString("username");
    LOGGER.debug("username is: " + username);
    final String hashword = json.getString("password").hashCode() + "";
    LOGGER.debug("hashword is: " + hashword);

    message.reply(response);

  }

  public String response = null;
}
 */
