package com.epicGamecraft.dungeonCrawlDepths;

import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.core.eventbus.Message;
import io.vertx.rxjava3.mysqlclient.MySQLPool;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.RowSet;
import io.vertx.rxjava3.sqlclient.Tuple;
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
    LOGGER.debug("MysqlVerticle is listening for queries.");
    final EventBus eb = vertx.eventBus();
    eb.consumer(mysqlQuery.name(), this::findUser);
    eb.consumer(mysqlInsert.name(), this::handleInsert);
    eb.consumer(mysqlPass.name(), this::handleForgotPass);
    eb.consumer(mysqlResetPass.name(), this::handleResetPass);
    eb.consumer(mysqlDelete.name(), this::handleDelete);
    eb.consumer(mysqlGameList.name(), this::handleGameList);
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

  private void findUser(Message<String> message) {
    LOGGER.debug("MysqlVerticle.handleQuery received message: " + message.body());
    final MySQLPool client = context.get(ContextKey.mysqlConnection.name());
    final JsonObject json = new JsonObject(message.body());
    final String username = json.getString("username");
    final String password = json.getString("password").hashCode() + "";
    client
      .preparedQuery("SELECT * FROM player WHERE username=? AND hashword=?")
      .execute(Tuple.of(username, password))
      .subscribe(ar -> {
        if (ar.size() > 0) {
          RowSet<Row> rows = ar.value();
          LOGGER.debug("Got " + rows.size() + " rows ");
          if (rows.size() != 0) {
            for (Row row : rows) {
              message.reply(row.getInteger(0) + " " + row.getString(1) + " " + row.getInteger(2) + " " + row.getString(3));
            }
          } else {
            message.reply("zero results for that username and password.");
            //Need to make sure if this happens, the code must let user retry login.
          }
        }
      }, throwable -> {
        LOGGER.debug("Failure: " + throwable.getCause().getMessage());
        message.reply("invalid query");
      });
  }

  private void handleInsert(Message<String> message) {
    LOGGER.debug("MysqlVerticle.handleInsert received message: " + message.body());
    final MySQLPool client = context.get(ContextKey.mysqlConnection.name());
    try {
      final JsonObject json = new JsonObject(message.body());
      final String username = json.getString("username");
      final String password = json.getString("password").hashCode() + "";
      final String email = json.getString("email");
      client
        .preparedQuery("INSERT INTO player VALUES (?,?,?,?)")
        .execute(Tuple.of(0, username, password, email))
        .subscribe(ar -> {
            LOGGER.debug("Successfully inserted record for: " + username);
            message.reply(null);
          },
          throwable -> {
            message.fail(500, "invalid insert statement " + throwable.getMessage());
          });
    } catch (Exception e) {
      message.fail(500, "invalid insert statement");
    }
  }

  private void handleForgotPass(Message<String> message) {
    LOGGER.debug("MysqlVerticle.handleForgotPass received message: " + message.body());
    final MySQLPool client = context.get(ContextKey.mysqlConnection.name());
    final JsonObject json = new JsonObject(message.body());
    final String username = json.getString("username");
    final String email = json.getString("email");
    client
      .preparedQuery("SELECT * FROM player WHERE username=? AND email=?")
      .execute(Tuple.of(username, email))
      .subscribe(ar -> {
        //send email to user account with password reset option.
        RowSet<Row> rows = ar.value();
        LOGGER.debug("Got " + rows.size() + " rows ");
        if (rows.size() != 0) {
          for (Row row : rows) {
            message.reply(row.getInteger(0) + " " + row.getString(1) + " " + row.getInteger(2) + " " + row.getString(3));
          }
        } else {
          message.reply("zero results for that username and email.");
        }
      }, throwable -> {
        LOGGER.debug("Failure: " + throwable.getCause().getMessage());
        message.reply("invalid query");
      });
  }

  private void handleResetPass(Message<String> message) {
    LOGGER.debug("MysqlVerticle.handleResetPass received message: " + message.body());
    final MySQLPool client = context.get(ContextKey.mysqlConnection.name());
    final JsonObject json = new JsonObject(message.body());
    final String username = json.getString("username");
    final String email = json.getString("email");
    final int hashword = json.getString("password").hashCode();
    client
      .preparedQuery("UPDATE player SET hashword=? WHERE username=? AND email=?")
      .execute(Tuple.of(hashword, username, email))
      .subscribe(ar -> {
        //Send result to javascript to output text to the page with AJAX, and also to give link to
        // login page so user can attempt to login with their new password.
        message.reply("successfully updated password to: " + hashword + " for " + username);
      }, throwable -> {
        // This only happens as a result of failure to connect to mysql container.
        LOGGER.debug("Failure: " + throwable.getCause().getMessage());
        message.reply("invalid query");
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
      .execute(Tuple.of(username, password)).subscribe(ar -> {
        message.reply("Successfully deleted record for: " + username);
      }, throwable -> {
        LOGGER.debug("Failure: " + throwable.getCause().getMessage());
        message.reply("invalid query");
      });
  }

  private void handleGameList(Message<String> message) {
    LOGGER.debug("MysqlVerticle.handleGameList received message: " + message.body());
    final MySQLPool client = context.get(ContextKey.mysqlConnection.name());
    String sql = "";
    if (message.body().equals("basic")) {
      sql = "SELECT * FROM lobby";
    } else {
      sql = "SELECT * FROM lobby ORDER BY " + message.body();
    }
    LOGGER.debug("Sending sql statement: " + sql);
    client
      .query(sql)
      .execute()
      .subscribe(ar -> {
        if (ar.value().size() != 0) {
          RowSet<Row> rows = ar.value();
          System.out.println("rows returned: " + rows.size());
          for (Row row : rows) {
            message.reply(row.getInteger(0) + " " + row.getString(1)
              + " " + row.getString(2) + " " + row.getInteger(3)
              + " " + row.getLocalTime(4) + " " + row.getLocalDateTime(5)
              + " " + row.getString(6) + " " + row.getString(7) + " " + row.getInteger(8));
          }
        } else {
          message.reply("No results to show.");
        }
      }, throwable -> {
        LOGGER.debug("Failure: " + throwable.getCause().getMessage());
        message.reply("invalid query");
      });
  }

}
