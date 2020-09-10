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
import io.vertx.reactivex.*;
import io.vertx.reactivex.core.*;
import io.vertx.reactivex.core.http.*;
import io.vertx.reactivex.ext.web.*;
import io.vertx.reactivex.ext.web.handler.SessionHandler;
import io.vertx.reactivex.ext.web.sstore.LocalSessionStore;
import io.vertx.reactivex.ext.web.sstore.SessionStore;
import io.vertx.reactivex.core.buffer.*;

public class HttpServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  @Override
  public Completable rxStart() {
    HttpServer server = vertx.createHttpServer();

    Router router = Router.router(vertx);

    router.get("/status").handler(this::statusHandler);
    router.get("/static/*").handler(this::staticHandler);
    //		router.route().handler(BodyHandler.create());
    //		router.post("/bus/*").handler(this::busHandler);
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
    LOGGER.debug("Get " + path);
    response.end();
  }
}

/*
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

@Nullable
String path = request.path();

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
