package com.epicGamecraft.dungeonCrawlDepths;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.BrowserInput;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
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
		vertx.eventBus().consumer(BrowserInput.name(), this::handleKey);

	}

	private void handleKey(Message<String> message) {
		LOGGER.debug("Received message: " + message.body());
	}

}
