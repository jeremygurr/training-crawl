package com.epicGamecraft.dungeonCrawlDepths;

import io.reactivex.Completable;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.reactivex.mysqlclient.MySQLPool;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
import io.vertx.reactivex.sqlclient.Tuple;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlResult;

import java.io.IOException;
import java.util.Properties;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;


public class MysqlVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MysqlVerticle.class);

  @Override
  public Completable rxStart() {
    LOGGER.debug("MysqlVerticle is listening to: " + mysqlQuery.name());
    final EventBus eb = vertx.eventBus();
    eb.consumer(mysqlQuery.name(), this::handleQuery);
    eb.consumer(mysqlInsert.name(), this::handleInsert);
    eb.consumer(mysqlPass.name(), this::handlePassReset);
    return Completable.complete();
  }

  private void handleQuery(Message<String> message) {
    LOGGER.debug("MysqlVerticle received message: " + message.body());
    final Properties config = new Properties();
    try {
      config.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    final String url = config.getProperty("mysqlurl");
    final String user = config.getProperty("mysqluser");
    final String pass = config.getProperty("mysqlpass");
    MySQLConnectOptions connectOptions = new MySQLConnectOptions()
      .setPort(3306)
      .setHost(url)
      .setDatabase("crawl")
      .setUser(user)
      .setPassword(pass);

    // Pool options
    PoolOptions poolOptions = new PoolOptions()
      .setMaxSize(5);

    // Create the client pool
    MySQLPool client = MySQLPool.pool(vertx, connectOptions, poolOptions);

    /*Now here I need to use retrieve the username, and password from the messsage.body object and
    put that into a prepared statement and in message.reply() return the results.
     */

    final JsonObject json = new JsonObject(message.body());
    final String username = json.getString("username");
    final String password = json.getString("password").hashCode() + "";
    //todo: If hashcode is always numbers then make this into an int and change the column inside mysql user table.

    client
      .preparedQuery("SELECT * FROM user WHERE username=? AND password=?")
      .execute(Tuple.of(username, password), ar -> {
        if (ar.succeeded()) {
          RowSet<Row> rows = ar.result();
          LOGGER.debug("Got " + rows.size() + " rows ");
          for (Row row : rows) {
            message.reply(row.getInteger(0) + " " + row.getString(1) + " " + row.getString(2) + " " + row.getInteger(3));
            //Alternatively you can use row.getValue() instead.
          }
        } else {
          LOGGER.debug("Failure: " + ar.cause().getMessage());
          message.reply("invalid query");
        }
      });

  }

    /* example of pure sql statement.
    client
      .query("SELECT * FROM user")
      .execute(ar -> {
        if (ar.succeeded()) {
          RowSet<Row> result = ar.result();
          System.out.println("Got " + result.size() + " rows ");
          message.reply("success message");
        } else {
          System.out.println("Failure: " + ar.cause().getMessage());
          message.reply("failed message");
        }

        // Now close the pool
        client.close();
      });
*/


  private void handleInsert(Message<String> message) {
    //see CouchbaseVerticle to see how we handled this with couchbase.
  }

  private void handlePassReset(Message<String> message) {
    //see CouchbaseVerticle to see how we handled this with couchbase.
  }
}
