package com.epicGamecraft.dungeonCrawlDepths;

import static com.epicGamecraft.dungeonCrawlDepths.UserResult.*;
import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.vertx.reactivex.ext.web.Cookie;
import io.vertx.reactivex.ext.web.handler.CookieHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.reactivex.core.Vertx.*;
import io.reactivex.*;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.*;
import io.reactivex.observers.*;
import io.reactivex.schedulers.*;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.*;
import io.vertx.reactivex.core.*;
import io.vertx.reactivex.core.http.*;
import io.vertx.reactivex.ext.web.*;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.SessionHandler;
import io.vertx.reactivex.ext.web.sstore.LocalSessionStore;
import io.vertx.reactivex.ext.web.sstore.SessionStore;
import io.vertx.reactivex.core.buffer.*;
import io.vertx.reactivex.core.eventbus.EventBus;

public class HttpServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  @Override
  public Completable rxStart() {
    LOGGER.info("Http Verticle is starting. Make sure Couchbase container is running.");
    HttpServer server = vertx.createHttpServer();

    Router router = Router.router(vertx);
    SessionStore store = LocalSessionStore.create(vertx);
    SessionHandler mySesh = SessionHandler.create(store);
    mySesh.setSessionTimeout(86400000); //24 hours in milliseconds.

    router.get("/status").handler(this::statusHandler);
    router.route().handler(mySesh);
    router.get("/static/*").handler(this::staticHandler);
    router.route().handler(BodyHandler.create());
    router.post("/bus/*").handler(this::busHandler);
    final int port = 8080;
    final Single<HttpServer> rxListen = server
      .requestHandler(router)
      .rxListen(port)
      .doOnSuccess(e -> {
        LOGGER.info("HTTP server running on port " + port);
      })
      .doOnError(e -> {
        LOGGER.error("Could not start a HTTP server", e.getMessage());
      });

    return rxListen.ignoreElement();
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
      Session session = context.session();
      String username = session.get(SessionKey.username.name());
      if (username != null && path.equals("/static/login.html")) {
          // TODO: Figure out how to make || path.equals("/static/jscrawl.html") work here.
          WebUtils.redirect(response, "/static/jscrawl.html");
          return;
      }
      LOGGER.debug("GET " + path);
      path = path.substring(1);
      final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
      if (stream != null) {
        final String text = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
          .lines()
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

  private void busHandler(RoutingContext context) {

    try {
      final EventBus eb = vertx.eventBus();

      final HttpServerRequest request = context.request();
      final HttpServerResponse response = context.response();
      final MultiMap params = request.params();

      final String absoluteURI = request.absoluteURI();
      LOGGER.debug("absoluteURI=" + absoluteURI);
      final String busAddress = absoluteURI.replaceAll("^.*/bus/", "");
      LOGGER.debug("busAddress=" + busAddress);
      Session session = context.session();

      //This puts the params from http headers into json object.
      JsonObject object = new JsonObject();
      for (Map.Entry<String, String> entry : params.entries()) {
        object.put(entry.getKey(), entry.getValue());
      }

/*    //This adds the session variables(if there are any) to the same Json object so they all will be
      // sent in a message below to user verticle.
      for (Map.Entry<String, Object> entry : session.data().entrySet()) {
        object.put(entry.getKey(), entry.getValue());
      } */

      eb.rxRequest(busAddress, object.encode())
        .subscribe(e -> {
            final JsonObject json = JsonObject.mapFrom(e.body());
            final String message = json.getString("response");
            final String redirect = json.getString("redirect");
            LOGGER.debug("HttpServer Verticle Received reply: " + e.body());

            if (message != null) {
              WebUtils.failMessage(response, message);
              LOGGER.debug("message =" + message);
            } else if (redirect.equals("jscrawl.html")) {
              session.put(SessionKey.username.name(), object.getString("username"));
              LOGGER.debug("session equals: " + session.data());
              WebUtils.redirect(response, "/static/" + redirect);
            } else {
              WebUtils.redirect(response, "/static/" + redirect);
            }
          },
          err -> {
            LOGGER.debug("Failed login : " + err.getMessage());
            if (err.getMessage() == null) {
              LOGGER.debug("Error with busAddress " + busAddress + " " + err.getMessage());
              WebUtils.redirect(response, "/static/serverError.html");
            }
          });
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
    }
  }
}
