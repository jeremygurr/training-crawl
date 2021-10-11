package com.epicGamecraft.dungeonCrawlDepths;

import com.couchbase.client.java.ReactiveCluster;
import com.couchbase.client.java.json.JsonObject;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;

public class CouchbaseVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(CouchbaseVerticle.class);
  private final JsonObject empty = JsonObject.create();

  @Override
  public Completable rxStart() {   //this line had "throws exception" in it before, ask if I need to implement that in some rx form.
    LOGGER.debug("Couchbase Verticle listening to: " + couchbaseQuery.name());
    final EventBus eb = vertx.eventBus();
    eb.consumer(couchbaseQuery.name(), this::handleQuery);
    eb.consumer(couchbaseInsert.name(), this::handleInsert);
    eb.consumer(couchbasePass.name(), this::handlePassReset);
    final Properties config = new Properties();
    try {
      config.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    final String url = config.getProperty("url");
    final String username = config.getProperty("username");
    final String password = config.getProperty("password");
    final ReactiveCluster connection = ReactiveCluster.connect(url, username, password);
    context.put(ContextKey.couchbaseConnection.name(), connection);
    return Completable.complete();
  }


  private void handleQuery(Message<String> message) {
    LOGGER.debug("couchbaseVerticle.handleQuery received message: " + message.body());
    final ReactiveCluster connection = context.get(ContextKey.couchbaseConnection.name());
    final JsonObject json = JsonObject.fromJson(message.body());
    final String username = json.getString("username");
    final String hashword = json.getString("password").hashCode() + "";
    connection.bucket("depths")
      .defaultCollection()
      .get("user::" + username)
      .log()
      .map(result -> {
        JsonObject row = result.contentAs(JsonObject.class);
        return row;
      })
      .subscribe(row -> {
          if (row.containsValue(hashword)) {
            message.reply(row.toString());
            //means the username and password given are correct.
          } else {
            message.reply(null);
            //means the username is correct, but password was incorrect so cannot retrieve user.
          }
        }
        , err -> {
          LOGGER.debug("error : " + err.getMessage());
          message.reply(err.getCause());
          //returns null to indicate no document was found with the information given. Use err.getMessage() to see more details.
        });
  }


  private void handleInsert(Message<String> message) {
    LOGGER.debug("CouchbaseVerticle.handleInsert received message : " + message.body());
    final ReactiveCluster connection = context.get(ContextKey.couchbaseConnection.name());
    final JsonObject json = JsonObject.fromJson(message.body());
    final String username = json.getString("username");
    final String email = json.getString("email");
    final String hashword = json.getString("password").hashCode() + "";
    final JsonObject user = JsonObject.create();
    user.put("name", username);
    user.put("email", email);
    user.put("hashword", hashword);
    //inserts user JsonObject to couchbase based on the userid.
    connection.bucket("depths")
      .defaultCollection()
      .insert("user::" + username, user)
      .log()
      .subscribe(result -> {
          LOGGER.debug("Result of insertion: " + result);
          message.reply(null);
          //Means the insert was successful.
        },
        err -> {
          message.reply(err.getMessage());
          //Means the insert was a failure.
        });
  }

  private void handlePassReset(Message<String> message) {
    final ReactiveCluster connection = context.get(ContextKey.couchbaseConnection.name());
    final JsonObject json = JsonObject.fromJson(message.body());
    final String username = json.getString("username");
    final String email = json.getString("email");
    connection.bucket("depths")
      .defaultCollection()
      .get("user::" + username)
      .log()
      .map(result -> {
        JsonObject row = result.contentAs(JsonObject.class);
        return row;
      })
      .subscribe(row -> {
          if (row.containsValue(email)) {
            LOGGER.debug("" + row.toString());
            message.reply(row.toString());
            //means the username and email given are correct.
          } else {
            LOGGER.debug("email was incorrect.");
            message.reply(null);
            //means the username is correct, but email was incorrect so cannot retrieve user.
          }
        }
        , err -> {
          LOGGER.debug("error : " + err.getMessage());
          message.reply(err.getCause());
          //returns null to indicate no document was found with the information given. Use err.getMessage() to see more details.
        });
  }

}
