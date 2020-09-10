package com.epicGamecraft.dungeonCrawlDepths;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.vertx.core.json.JsonObject;

import com.couchbase.client.java.*;
import com.couchbase.client.java.query.ReactiveQueryResult;

public class UserVerticle extends AbstractVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserVerticle.class);

	@Override
	public void start(Promise<Void> promise) throws Exception {
		LOGGER.debug("User Verticle is listening to: " + userLogin.name());
		vertx.eventBus().consumer(userLogin.name(), this::handleUser);
		vertx.eventBus().consumer(createUser.name(), this::userCreateHandler);
		vertx.eventBus().consumer(forgotPassword.name(), this::userPasswordResetHandler);
	}

	private void handleUser(Message<String> message) {
		LOGGER.debug("User Verticle received message: " + message.body());
		JsonObject json = new JsonObject(message.body());
		final String user = "'" + json.getString("usernameOrEmail") + "'";
		final String hash = "'" + json.getString("password").hashCode() + "'";
		vertx.eventBus().request(couchbaseQuery.name(), "select name from registration where name=" 
		+ user + " and hashword=" + hash, ar -> {
			if (ar.succeeded()) {
				if (ar.result().body() == null) {
					LOGGER.debug("Invalid Login");
				} else if (ar.result().body() != null) {
					LOGGER.debug("User Verticle received reply: " + ar.result().body());
					final UUID sessionId = UUID.randomUUID();
					message.reply(sessionId.toString());
					//context.put(ContextKey.sessionMap.name(), sessionId);  
					// Go this route if you decide to put
					// session and cookie handling inside UserVerticle instead of HttpVerticle.
				} else {
					LOGGER.debug("An error occured retrieving data from Couchbase.");
				}
			}
		});

	}

	private void userCreateHandler(Message<String> message) {
		LOGGER.debug("User Verticle received message: " + message.body());
		JsonObject json = new JsonObject(message.body());
		final String user = "'" + json.getString("username") + "'";
		final String email = "'" + json.getString("email") + "'";
		final String hash = "'" + json.getString("password") + "'";
		vertx.eventBus().request(couchbaseQuery.name(), "select name, email, hashword from registration where name="
		+ user + " or hashword=" + hash + " or email=" + email, ar -> {
			if (ar.succeeded()) {
				if (ar.result().body() == null) {
					LOGGER.debug("User Verticle has found no record of that user, so user can now be created.");
				} else if (ar.result().body() != null) {
					LOGGER.debug(
							"User Verticle has found record of already existing user: " + ar.result().body());
				} else {
					LOGGER.debug("An error occured retrieving data from Couchbase.");
				}
			}
		});
	}

	private void userPasswordResetHandler(Message<String> message) {
		LOGGER.debug("User Verticle received message: " + message.body());
		JsonObject json = new JsonObject(message.body());
		final String email = "'" + json.getString("email") + "'";
		vertx.eventBus().request(couchbaseQuery.name(), "select name, email from registration where email=" + email, ar -> {
			if (ar.succeeded()) {
				if (ar.result().body() == null) {
					LOGGER.debug("User Verticle has found no record containing that email, so user should create account.");
				} else if (ar.result().body() != null) {
					LOGGER.debug("User Verticle has found a record containing that email: " + ar.result().body());
				} else {
					LOGGER.debug("An error occured retrieving data from Couchbase.");
				}
			}
		});
	}

}