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

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

public class UserVerticle extends AbstractVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserVerticle.class);

	@Override
	public void start(Promise<Void> promise) throws Exception {
		LOGGER.debug("User Verticle is listening to: " + userLogin.name());
		vertx.eventBus().consumer(userLogin.name(), this::handleUser);
	}

	private void handleUser(Message<String> message) {
		LOGGER.debug("User Verticle received message: " + message.body());
		JsonObject json = new JsonObject(message.body());
		String user = "'" + json.getString("usernameOrEmail") + "'";
		String pass = "'" + json.getString("password") + "'";
		vertx.eventBus().request(couchbaseQuery.name(),
				"select name from registration where name=" + user + " and hashword=" + pass, ar -> {
					if (ar.succeeded()) {
						if (ar.result().body() == null) {
							LOGGER.debug("Invalid Login");
						} else if (ar.result().body() != null) {
							LOGGER.debug("Received reply: " + ar.result().body());
							final UUID sessionId = UUID.randomUUID();
							message.reply(sessionId.toString());
//							context.put(ContextKey.sessionMap.name(), sessionId);  //Go this route if you decide to put 
//session/cookie handling inside UserVerticle instead of HttpVerticle.
						} else {
							LOGGER.debug("An error occured retrieving data from Couchbase.");
						}
					}
				});

	}
//  receives { "usernameOrEmail": "Jared", "password": "Gurr" }

	public void userLookupHandler(RoutingContext context) {

	}

	public void userNewPasswordHandler(RoutingContext context) {

	}

	public void userCreateHandler(RoutingContext context) {

		final HttpServerRequest request = context.request();

		final String username = request.getParam("username");
		final String email = request.getParam("email");
		final String password = request.getParam("password");
		final int hashCode = password.hashCode();
		final String usernameOrEmail = request.getParam("usernameOrEmail");
		LOGGER.debug("Received login request: " + usernameOrEmail);

		final String queryUser = "select user from registration where user=" + username;
		final String queryEmail = "select email from registration where user=" + username + "and email=" + email;
		final String queryPassword = "select email from registration where user=" + username + "and password="
				+ hashCode;
		final String queryUserAndEmail = "select user, email from registration where user=" + usernameOrEmail
				+ "or email=" + usernameOrEmail;

		final JsonObject jsonQueryUser = jsonObjectCreator(queryUser);
		final JsonObject jsonQueryEmail = jsonObjectCreator(queryEmail);
		final JsonObject jsonQueryPassword = jsonObjectCreator(queryPassword);
		final JsonObject jsonQueryUserAndEmail = jsonObjectCreator(queryUserAndEmail);

		vertx.eventBus().request("couchbase.query", "" + jsonQueryUser + "", ar -> {
			if (ar.succeeded()) {
				LOGGER.info("Received reply: " + ar.result().body());
			}
		});
		vertx.eventBus().request("couchbase.query", "" + jsonQueryEmail + "", ar -> {
			if (ar.succeeded()) {
				LOGGER.info("Received reply: " + ar.result().body());
			}
		});
		vertx.eventBus().request("couchbase.query", "" + jsonQueryPassword + "", ar -> {
			if (ar.succeeded()) {
				LOGGER.info("Received reply: " + ar.result().body());
			}
		});
		vertx.eventBus().request("couchbase.query", "" + jsonQueryUserAndEmail + "", ar -> {
			if (ar.succeeded()) {
				LOGGER.info("Received reply: " + ar.result().body());
			}
		});
	}

	public JsonObject jsonObjectCreator(String value) {

		String jsonString = "{\"query\":\"" + value + "\"}";
		JsonObject object = new JsonObject(jsonString);
		return object;

	}

//		if (object.containsKey("usernameOrEmail")) {
//			eb.request("userLogin", object, ar -> {
//				if (ar.succeeded()) {
//					LOGGER.info("Received reply: " + ar.result().body());
//				}
//			});
//		} else if (object.containsKey("email")) {
//			eb.request("resetPassword", object, ar -> {
//				if (ar.succeeded()) {
//					LOGGER.info("Received reply: " + ar.result().body());
//				}
//			});
//		} else if (object.containsKey("username") && object.containsKey("password") && object.containsKey("email")) {
//			eb.request("createUser", object, ar -> {
//				if (ar.succeeded()) {
//					LOGGER.info("Received reply: " + ar.result().body());
//				}
//			});
//		} else {
//			LOGGER.info("Object contains zero matching keys.");
//		}

//		final HttpServerResponse response = context.response();
//		response.putHeader("Content-Type", "text/html");
//		response.end("<html><body>Username = " + username + " email = " + email + " Password = " 
//		+ password + " usernameOrEmail = " + usernameOrEmail + "</body></html>");

}
