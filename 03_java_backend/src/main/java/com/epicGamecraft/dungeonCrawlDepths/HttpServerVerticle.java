package com.epicGamecraft.dungeonCrawlDepths;

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
    HttpServer server = vertx.createHttpServer();

    Router router = Router.router(vertx);
    SessionStore store = LocalSessionStore.create(vertx);
    SessionHandler mySesh = SessionHandler.create(store);
    mySesh.setSessionTimeout(86400000); //24 hours in milliseconds.

    router.get("/status").handler(this::statusHandler);
    router.get("/static/*").handler(this::staticHandler);
    router.route().handler(BodyHandler.create());
    router.route().handler(mySesh); //FIXME: Ask if this is right place, because it only happens for post requests with html login forms. And not when user finds website.
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

  private void busHandler(RoutingContext context) {

    //uncomment when ready to work on sessions/coookies.
//    Session session = context.session();
//    String sessionToken = session.get(SessionKey.username.name());  //TODO: If username is null force user to login on login page.
//    // get the token from the session
//    if (sessionToken != null) {
//      // attempt to parse the value
//      int idx = sessionToken.indexOf('/');
//      if (idx != -1 && session.id() != null && session.id().equals(sessionToken.substring(0, idx))) {
//        String parsedToken = sessionToken.substring(idx + 1);
//        vertx.eventBus().rxRequest(couchbaseQuery.name(), parsedToken) //TODO: Find out why this works in other vertciles but not this one.
//          .doOnSuccess(e -> {
//            //TODO: Put method here that logs into account of user that has that SessionToken.
//          })
//          .doOnError(e -> {
//            //TODO: Respond with a error page that says "Sorry, site is having problems retrieving server data.
//          })
//          .subscribe();
//      }
//      // If sessionToken is null they need to login or create an account.
//      //TODO: Place method here that sends user to login page.
//    }
//
//    session.put(SessionKey.username.name(), "username");
    //These will be variables. the values will come from database.
    // The key will never change it will just be user.

    final EventBus eb = vertx.eventBus();

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

    eb.rxRequest(busAddress, object.encode())                   //sends the json object with request params to UserVerticle to whichever consumer specified by busAddress.
      .doOnSuccess(e -> {
        LOGGER.debug("HttpServer Verticle Received username: " + e.body());
      })
      .doOnError(e -> {
        LOGGER.debug("Error with busAddress " + busAddress + " " + e.getMessage());
        //put some method to notify browser that the login was unsuccessful, and try again.
        eb.send("loginForm", "That username or password is invalid.");
      })
      .subscribe(ar -> {
        LOGGER.debug("Received username: " + ar.body());
      });
  }
}
// address will be whatever last part of action="bus/" is for example userLogin
// see each login form  HTML page to see what they are listed as.
// message should be the JSON object with all the parameters.

