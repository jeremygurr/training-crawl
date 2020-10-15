package com.epicGamecraft.dungeonCrawlDepths;

import static com.epicGamecraft.dungeonCrawlDepths.UserResult.*;

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

    final EventBus eb = vertx.eventBus();

    final HttpServerRequest request = context.request();
    final HttpServerResponse response = context.response();
    final MultiMap params = request.params();

//    Session session = context.session();
//    String username = session.get(SessionKey.username.name());
//    if (username == null) {
//      WebUtils.redirect(response, "/static/login.html");
//      return;
//      //If username is null it forces user to login on login page.
//      //This means user is new to website:
//    }
//    session.put(SessionKey.username.name(), "username");
    //These will be variables. the values will come from database.
    // The key will never change it will just be user.
    //session id stuff you plug into session cookie. cookie is session id.
    //the id pulls up session that goes with the id and use all its variables.
    //if it doesn't find the session that goes with the id it will create a new
    //session if they login.

    JsonObject object = new JsonObject();
    for (Map.Entry<String, String> entry : params.entries()) {
      object.put(entry.getKey(), entry.getValue());
    }

    final String absoluteURI = request.absoluteURI();
    LOGGER.debug("absoluteURI=" + absoluteURI);
    final String busAddress = absoluteURI.replaceAll("^.*/bus/", "");
    LOGGER.debug("busAddress=" + busAddress);

    eb.rxRequest(busAddress, object.encode())  //sends the json object with request params to UserVerticle to whichever consumer specified by busAddress.
      .subscribe(e -> {
          LOGGER.debug("HttpServer Verticle Received reply: " + e.body());
          if (e.body() == successLog.name()) {
            WebUtils.redirect(response, "/static/jscrawl.html");
            //This means login was successful. Redirects user to main crawl page.
          } else if (e.body() == invalid.name()) {
            if (busAddress == "userLogin") {
              //Make JavaScript do an alert that says "username or password was incorrect." -for login page
            } else if (busAddress == "createUser") {
              //Make JavaScript do an alert that says "username already in use" -for Create Account page
            } else {
              //Make JavaScript do an alert that says "username or email was incorrect." -for PassReset page
              //TODO: add another else if so you can add the reset username code.
            }
          } else if (e.body() == messageErr.name()) {
            //This means User and couchbase verticles had communication errors.
            //Redirect user to our error page.
          } else if (e.body() == registerUser.name()) {
            //This means that user successfully registered a new account.
            //redirect user to the login page so they can login with new account.
          } else if (e.body() == resetPass.name()) {
            // This means the account was located with email and username user provided.
            // reset password email will be sent to the email the user specified.
          }
        },
        err -> {
          LOGGER.debug("Failed login : " + err.getMessage());
          if (err.getMessage() == null) {
            LOGGER.debug("Error with busAddress " + busAddress + " " + err.getMessage());
            //TODO: redirect user to an error page.
          }
        });
  }
}
// address will be whatever last part of action="bus/" is for example userLogin
// see each login form HTML page or UserVerticle to see what they are listed as.
// message should be the JSON object with all the parameters.

