package com.epicGamecraft.dungeonCrawlDepths;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    HttpServer server = vertx.createHttpServer();

    Router router = Router.router(vertx);

    router.get("/status").handler(this::statusHandler);
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

  private void staticHandler(RoutingContext context) {  //needs to be tested

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

  private void busHandler(RoutingContext context) {  //needs to be tested

    final EventBus eb = vertx.eventBus();
    final HttpServerResponse response = context.response();

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
    eb.rxRequest(busAddress, object.encode())
    .doOnSuccess(e -> {
      LOGGER.debug("HttpServer Verticle Received UUID: " +  e.body());
      context.put(ContextKey.sessionMap.name(), e.body());
    })
    .doOnError(e -> {
      //put some method to notify browser that the login was unsuccessful, and try again.
      eb.send("loginForm", "That username or password is invalid.");
      //make sure to uncomment login.html script when time to test this.
      
    });
  }
}
// address will be whatever last part of action="bus/" is for example userLogin
// see each login form
// HTML page to see what they are listed as.
// message should be the JSON object with all the parameters.

/*

After I get everything else working, add this code and try to get cookies/sessions working.

Cookie userCookie = context.getCookie("myCookie");
String cookieValue = userCookie.getValue();
context.addCookie(Cookie.cookie("othercookie", "somevalue"));
Session session = context.session();
SessionStore store = LocalSessionStore.create(vertx);  //Creates session store. Required to create sessionHandler.
SessionHandler sessionHandler = SessionHandler.create(store);   //Used to configure and set cookies for the session.
sessionHandler.setSessionCookieName("User123");
sessionHandler.setSessionCookiePath("/something");
sessionHandler.setSessionTimeout(5000000); //5 minutes. Not needed since default is 30 min.

String username = session.get("name");
String webpage = session.get("path");




*/
