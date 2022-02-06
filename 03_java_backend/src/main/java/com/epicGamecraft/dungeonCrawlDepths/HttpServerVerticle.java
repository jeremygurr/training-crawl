package com.epicGamecraft.dungeonCrawlDepths;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.annotations.Nullable;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.Session;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.SessionHandler;
import io.vertx.reactivex.ext.web.sstore.LocalSessionStore;
import io.vertx.reactivex.ext.web.sstore.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  @Override
  public Completable rxStart() {
    LOGGER.info("Http Verticle is starting. ");
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

  public static void writeStaticHtml(HttpServerResponse response, String path) {
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
      } else if (path.endsWith(".js")) {
          response.putHeader("Content-Type", "text/javascript");
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
  }

  private void statusHandler(RoutingContext context) {

    final HttpServerResponse response = context.response();
    response.putHeader("Content-Type", "text/html");
    response.end("<html><body>All is well</body></html>");

  }

  private void staticHandler(RoutingContext context) {

    final HttpServerResponse response = context.response();
    final HttpServerRequest request = context.request();
    response.setChunked(true);
    @Nullable
    String path = request.path();
    try {
      Session session = context.session();
      String username = session.get(SessionKey.username.name());
      if (username != null && path.equals("/static/login.html") || username != null && path.equals("/static/jscrawl.html")) {
        WebUtils.redirect(response, "/static/jscrawl.html");
        return;
      }
      writeStaticHtml(response, path);
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
      response.setChunked(true);

      final String absoluteURI = request.absoluteURI();
      LOGGER.debug("absoluteURI=" + absoluteURI);
      final String busAddress = absoluteURI.replaceAll("^.*/bus/", "");
      LOGGER.debug("busAddress=" + busAddress);
      Session session = context.session();
      JsonObject object = new JsonObject();
      for (Map.Entry<String, String> entry : params.entries()) {
        object.put(entry.getKey(), entry.getValue());
      }
      eb.rxRequest(busAddress, object.encode())
        .subscribe(e -> {
            final JsonObject json = JsonObject.mapFrom(e.body());
            final String message = json.getString("response");
            final String redirect = json.getString("redirect");
            LOGGER.debug("HttpServer Verticle Received reply: " + e.body());

            if (redirect.equals("login.html")) {
              // redirect to login page.
/* TODO: Figure out why this isn't working.
              String html = WebUtils.generateHtml("/home/jaredgurr/training-crawl/03_java_backend/src/main/resources/static/login.html", message);
              response.write(html);
              writeStaticHtml(response, "/static/login.html");
              LOGGER.debug("message =" + message);
              LOGGER.debug("html = " + html);
*/
            } else if (redirect.equals("jscrawl.html")) {
              session.put(SessionKey.username.name(), object.getString("username"));
              LOGGER.debug("session equals: " + session.data());
            }
            WebUtils.redirect(response, "/static/" + redirect);
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
