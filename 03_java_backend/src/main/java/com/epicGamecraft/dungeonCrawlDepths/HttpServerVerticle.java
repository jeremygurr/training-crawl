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
import io.reactivex.disposables.*;
import io.reactivex.observers.*;
import io.reactivex.schedulers.*;
import io.vertx.reactivex.*;
import io.vertx.reactivex.core.*;
import io.vertx.reactivex.core.http.*;
import io.vertx.reactivex.ext.web.*;
import io.vertx.reactivex.core.buffer.*;

public class HttpServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  @Override
  public Completable rxStart() {
    HttpServer server = vertx.createHttpServer();

    Router router = Router.router(vertx);

    router.get("/status").handler(this::statusHandler);
//		router.get("/static/*").handler(this::staticHandler);
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

}
