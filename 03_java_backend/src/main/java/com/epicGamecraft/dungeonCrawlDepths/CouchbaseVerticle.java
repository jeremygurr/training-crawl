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
	//right now there is an error where .subscribe will not action the message.reply if the input
	//doesn't match exactly what is found inside the database. What I want it to do is, even if the
	//input is not matching, it should still reply with something like "null" or "doesn't match" etc...
    //It probably does this because jsonObject parameters is empty/null, but actually, if that were the case
	//then message.reply should still at least attempt to send a message, even if empty. But it doesn't, 
	//If you set breakpoint on message.reply line, it never gets hit at all which means the process stops 
	//before it.
}
