package com.epicGamecraft.dungeonCrawlDepths.test.unit;

import com.couchbase.client.java.ReactiveCluster;
import com.couchbase.client.java.json.JsonObject;
import com.epicGamecraft.dungeonCrawlDepths.CouchbaseVerticle;
import io.reactivex.Completable;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.couchbaseQuery;

public class FakeCouchbaseVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(CouchbaseVerticle.class);


  @Override
  public Completable rxStart() {   //TODO: this line had "throws exception" in it before, ask if I need to implement that in some rx form.
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





//    final JsonObject user = JsonObject.create();
//    user.put("name", username);
//    user.put("email", email);
//    user.put("hashword", hashword);
//    connection.bucket("depths").defaultCollection().insert("user::" + username, user);  //inserts data to couchbase.
