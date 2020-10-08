package com.epicGamecraft.dungeonCrawlDepths;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;

import com.couchbase.client.java.kv.GetResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.*;
import io.vertx.reactivex.core.*;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;

import com.couchbase.client.java.*;
import com.couchbase.client.java.query.ReactiveQueryResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.couchbase.client.java.json.JsonObject;

public class CouchbaseVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(CouchbaseVerticle.class);


  @Override
  public Completable rxStart() {   //this line had "throws exception" in it before, ask if I need to implement that in some rx form.
    LOGGER.debug("Couchbase Verticle listening to: " + couchbaseQuery.name());
    final EventBus eb = vertx.eventBus();
    eb.consumer(couchbaseQuery.name(), this::handleQuery);
    final ReactiveCluster connection = ReactiveCluster.connect
      ("localhost:11210", "Administrator", "password");
    context.put(ContextKey.couchbaseConnection.name(), connection);
    return Completable.complete();
  }

  final JsonObject empty = JsonObject.create();

  private void handleQuery(Message<String> message) {
    LOGGER.debug("Couchbase Verticle received message: " + message.body());
    final ReactiveCluster connection = context.get(ContextKey.couchbaseConnection.name());

    //example of accessing couchbase the proper way:
    final JsonObject json = JsonObject.fromJson(message.body()); //TODO: Use codecs to make this just pass json object from user verticle in future.
    LOGGER.debug("json is: " + json);
    final String username = json.getString("usernameOrEmail");
    LOGGER.debug("username is: " + username);
    final String hashword = json.getString("password").hashCode() + "";
    LOGGER.debug("hashword is: " + hashword);
    connection.bucket("depths")
      .defaultCollection()
      .get("user::" + username)
      .log()
      .map(result -> {
        JsonObject row = result.contentAs(JsonObject.class);
        return row;
      })
      .subscribe(row -> {
          if (row.equals(empty)) {
            message.reply(null);
          } else {
            message.reply(row.toString());
          }
        }
        , error -> {
          message.reply(null);
        });
  }
  //retrieves the document with "user::username" id.
  //TODO: Figure out how to continue the process from here to check if the document that was retrieved
  // has the correct username and password. And then have it reply to UserVerticle with result.
}




//TODO: Make this work for userCreateHandler() to create new records in couchbase.

//    final JsonObject user = JsonObject.create();
//    user.put("name", username);
//    user.put("email", email);
//    user.put("hashword", hashword);
//    connection.bucket("depths").defaultCollection().insert("user::" + username, user);  //inserts data to couchbase.


//Here is old code that 100% works but we will replace it with easier methods:
//There is also old code in UserVerticle when we forced it to create query strings but that is
// bad practice.
//    final JsonObject empty = JsonObject.create();
//    final Mono<ReactiveQueryResult> query = connection.query(message.body());
//    query.subscribe(queryResult -> {
//      final Flux<JsonObject> rowsAsObject = queryResult.rowsAsObject();
//      rowsAsObject.defaultIfEmpty(empty)
//        .subscribe(jsonObject -> {
//            if (jsonObject.equals(empty)) {
//              message.reply(null);
//            } else {
//              message.reply(jsonObject.toString());
//            }
//          }
//          , error -> {
//            message.fail(1, "failed to query Couchbase.");
//          });
//    });

