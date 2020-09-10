package com.epicGamecraft.dungeonCrawlDepths;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.couchbase.client.java.*;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.ReactiveQueryResult;

public class CouchbaseVerticle extends AbstractVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(CouchbaseVerticle.class);


	@Override
	public void start(Promise<Void> promise) throws Exception {
		final EventBus eb = vertx.eventBus();
		eb.consumer(couchbaseQuery.name(), this::handleQuery);
		final ReactiveCluster connection = ReactiveCluster.connect("localhost:11210", "Administrator", "password");
//		final Context context = vertx.getOrCreateContext();
		context.put(ContextKey.couchbaseConnection.name(), connection);

	}

   	final JsonObject empty = JsonObject.create();

	private void handleQuery(Message<String> message) {
		final ReactiveCluster connection = context.get(ContextKey.couchbaseConnection.name());
		
		//example of accessing couchbase the proper way:
		String username = "something";
		String hashword = "hash";
		connection.bucket("depths").defaultCollection().get("user::" + username);
		final JsonObject user = JsonObject.create();
		user.put("username", "jgurr");
		user.put("email", "jared@yahoo.com");
		user.put("hashword", hashword);
		connection.bucket("depths").defaultCollection().insert("user::" + username, user);

		
		LOGGER.debug("Couchbase Verticle received message: " + message.body());
        final Mono<ReactiveQueryResult> query = connection.query(message.body());
        query.subscribe(queryResult -> {
        	final Flux<JsonObject> rowsAsObject = queryResult.rowsAsObject();
			rowsAsObject.defaultIfEmpty(empty)
        	.subscribe(jsonObject -> {
        		if(jsonObject.equals(empty)) {
        			message.reply(null);
        		} else {
        			message.reply(jsonObject.toString());
        		}
        	}
        	, error -> {
        		message.fail(1, "failed to query Couchbase.");
        	});
        });
	}
}
