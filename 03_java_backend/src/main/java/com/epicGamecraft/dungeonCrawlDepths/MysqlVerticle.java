package com.epicGamecraft.dungeonCrawlDepths;

import io.reactivex.Completable;
import io.reactivex.disposables.Disposable;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.mysqlclient.MySQLPool;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
import io.vertx.reactivex.sqlclient.Tuple;
import io.vertx.sqlclient.PoolOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    eb.consumer(mysqlDelete.name(), this::handleDelete);
    final Disposable fs = vertx.fileSystem().rxReadFile("config.json")
      .subscribe(buffer -> {
        JsonObject json = buffer.toJsonObject();
        final String url = json.getString("mysqlUrl");
        final String user = json.getString("mysqlUser");
        final String pass = json.getString("mysqlPass");
      }, err -> {
        LOGGER.debug("failure: " + err.getMessage());
      });

    final Properties config = new Properties();
    try {
      config.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties"));
    } catch (
      IOException e) {
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

    PoolOptions poolOptions = new PoolOptions()
      .setMaxSize(5);
    final MySQLPool client = MySQLPool.pool(vertx, connectOptions, poolOptions);
    context.put(ContextKey.mysqlConnection.name(), client);
    return Completable.complete();
  }

  private void handleQuery(Message<String> message) {
    LOGGER.debug("MysqlVerticle.handleQuery received message: " + message.body());
    final MySQLPool client = context.get(ContextKey.mysqlConnection.name());
    final JsonObject json = new JsonObject(message.body());
    final String username = json.getString("username");
    final String password = json.getString("password").hashCode() + "";
    client
      .preparedQuery("SELECT * FROM player WHERE username=? AND hashword=?")
      .execute(Tuple.of(username, password), ar -> {
        if (ar.succeeded()) {
          RowSet<Row> rows = ar.result();
          LOGGER.debug("Got " + rows.size() + " rows ");
          if (rows.size() != 0) {
            for (Row row : rows) {
              message.reply(row.getInteger(0) + " " + row.getString(1) + " " + row.getInteger(2) + " " + row.getString(3));
              //Alternatively you can use row.getValue() instead.
            }
          }
          else {
            message.reply("zero results for that username and password.");
          }
        } else {
          LOGGER.debug("Failure: " + ar.cause().getMessage());
          message.reply("invalid query");
        }
      });
  }

  private void handleInsert(Message<String> message) {
    LOGGER.debug("MysqlVerticle.handleInsert received message: " + message.body());
    final MySQLPool client = context.get(ContextKey.mysqlConnection.name());
    final JsonObject json = new JsonObject(message.body());
    final int id = json.getInteger("id");
    final String username = json.getString("username");
    final String password = json.getString("password").hashCode() + "";
    final String email = json.getString("email");
    client
      .preparedQuery("INSERT INTO player VALUES (?,?,?,?)")
      .execute(Tuple.of(id, username, password, email), ar -> {
        if (ar.succeeded()) {
          RowSet<Row> rows = ar.result();
          System.out.println(rows.rowCount());
          message.reply(null);
        } else {
          LOGGER.debug("Failure: " + ar.cause().getMessage());
          message.reply("invalid query");
        }
      });
  }

  private void handlePassReset(Message<String> message) {
    LOGGER.debug("MysqlVerticle.handlePassReset received message: " + message.body());
    final MySQLPool client = context.get(ContextKey.mysqlConnection.name());
    final JsonObject json = new JsonObject(message.body());
    final String username = json.getString("username");
    final String email = json.getString("email");
    client
      .preparedQuery("SELECT * FROM player WHERE username=? AND email=?")
      .execute(Tuple.of(username, email), ar -> {
        if (ar.succeeded()) {
          RowSet<Row> rows = ar.result();
          LOGGER.debug("Got " + rows.size() + " rows ");
          if (rows.size() != 0) {
            for (Row row : rows) {
              message.reply(row.getInteger(0) + " " + row.getString(1) + " " + row.getInteger(2) + " " + row.getString(3));
              //Alternatively you can use row.getValue() instead.
            }
          }
          else {
            message.reply("zero results for that username and email.");
          }
        } else {
          LOGGER.debug("Failure: " + ar.cause().getMessage());
          message.reply("invalid query");
        }
      });
  }

  private void handleDelete(Message<String> message) {
    LOGGER.debug("MysqlVerticle.handleDelete received message: " + message.body());
    final MySQLPool client = context.get(ContextKey.mysqlConnection.name());
    final JsonObject json = new JsonObject(message.body());
    final String username = json.getString("username");
    final String password = json.getString("password").hashCode() + "";
    client
      .preparedQuery("DELETE FROM player WHERE username=? AND hashword=?")
      .execute(Tuple.of(username, password), ar -> {
        if (ar.succeeded()) {
          RowSet<Row> rows = ar.result();
          System.out.println("rows returned: " + rows.rowCount());
          message.reply(rows.next());
        } else {
          LOGGER.debug("Failure: " + ar.cause().getMessage());
          message.reply("invalid query");
        }
      });
  }

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

/*
// another way to obtain credentials with config.json:

    final Disposable fs = vertx.fileSystem().rxReadFile("config.json")
      .subscribe(buffer -> {
        LOGGER.debug("result is: " + buffer.toString());
        JsonObject json = buffer.toJsonObject();
        final String url = json.getString("mysqlUrl");
        final String user = json.getString("mysqlUser");
        final String pass = json.getString("mysqlPass");
      }, err -> {
        LOGGER.debug("failure: " + err.getMessage());
      });
 */
