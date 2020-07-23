package com.epicGamecraft.dungeonCrawlDepths;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

public class HttpServerVerticle extends AbstractVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

	@Override
	public void start(Promise<Void> promise) throws Exception {

		HttpServer server = vertx.createHttpServer();

		Router router = Router.router(vertx);
		router.get("/status").handler(this::statusHandler);
		router.get("/static/*").handler(this::staticHandler);
		router.route().handler(BodyHandler.create());
		router.post("/bus/*").handler(this::busHandler);
//		router.get("/users/*").handler(this::userGetHandler);

		SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
		final PermittedOptions inbound = new PermittedOptions().setAddress(BusEvent.browserInput.name());
		BridgeOptions bridgeOptions = new BridgeOptions().addInboundPermitted(inbound);
		sockJSHandler.bridge(bridgeOptions);
		router.route("/eventbus/*").handler(sockJSHandler);

		final int port = 8080;
		server.requestHandler(router).listen(port, ar -> {
			if (ar.succeeded()) {
				LOGGER.info("HTTP server running on port " + port);
				promise.complete();
			} else {
				LOGGER.error("Could not start a HTTP server", ar.cause());
				promise.fail(ar.cause());
			}
		});

	}

	private void busHandler(RoutingContext context) {

     	final EventBus eb = vertx.eventBus();
//		final HttpServerResponse response = context.response();

		final HttpServerRequest request = context.request();
		final MultiMap params = request.params();

		JsonObject object = new JsonObject();
		for (Map.Entry<String, String> entry : params.entries()) {
			object.put(entry.getKey(), entry.getValue());
		}
		
		final String absoluteURI = request.absoluteURI();
		LOGGER.debug("absoluteURI=" + absoluteURI);
		final String busAddress = absoluteURI.replaceAll("^.*/bus/", "");
		LOGGER.debug("busAddress=" + busAddress);
		eb.request(busAddress, object.encode(), ar -> {
			if(ar.succeeded()) {
				LOGGER.debug("Received UUID: " + ar.result().body());
			}
		});
		
		// address will be whatever last part of action="bus/" is for example userLogin
		// see each login form
		// HTML page to see what they are listed as.
		// message should be the JSON object with all the parameters.

	}

	private void statusHandler(RoutingContext context) {

		final HttpServerResponse response = context.response();
		response.putHeader("Content-Type", "text/html");
		response.end("<html><body>All is well</body></html>");

	}

	private void staticHandler(RoutingContext context) {

		final HttpServerResponse response = context.response();
		final HttpServerRequest request = context.request();
		@Nullable
		String path = request.path();

		try {
			LOGGER.debug("GET " + path);
			path = path.substring(1);
			final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
			if (stream != null) {
				final String text = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines()
						.collect(Collectors.joining("\n"));
				if (path.endsWith(".html")) {
					response.putHeader("Content-Type", "text/html");
				} else if (path.endsWith(".css")) {
					response.putHeader("Content-Type", "text/css");
				} else {
					response.end("<html><body>Error filetype unknown: " + path + "</body></html>");
				}
				response.setStatusCode(200);
				response.end(text);
			} else {
				LOGGER.warn("Resource not found: " + path);
				response.setStatusCode(404);
				response.end();
			}
		} catch (Exception e) {
			LOGGER.error("Problem fetching static file: " + path, e);
			response.setStatusCode(502);
			response.end();
		}
	}

}
