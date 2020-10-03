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
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.core.eventbus.MessageConsumer;

public class UserVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserVerticle.class);

  @Override
  public Completable rxStart() {
    LOGGER.debug("User Verticle is listening to: " + userLogin.name());
    vertx.eventBus().consumer(userLogin.name(), this::handleUser);   //For some reason this doesn't get hit yet.
    vertx.eventBus().consumer(createUser.name(), this::userCreateHandler);
    vertx.eventBus().consumer(forgotPassword.name(), this::userPasswordResetHandler);
    return Completable.complete();
  }

  private void handleUser(Message<String> message) {
    LOGGER.debug("User Verticle received message: " + message.body());
    JsonObject json = new JsonObject(message.body());
    final String user = "'" + json.getString("usernameOrEmail") + "'";
    final String hash = "'" + json.getString("password").hashCode() + "'";
    vertx.eventBus().rxRequest(couchbaseQuery.name(), "select name from registration where name="
      + user + " and hashword=" + hash)
      .doOnSuccess(e -> {
        if (e.body() == null) {
          LOGGER.debug("Invalid Login");
        } else if (e.body() != null) {
          LOGGER.debug("User Verticle received reply: " + e.body());
        } else {
          LOGGER.debug("An error occurred retrieving data from Couchbase.");
        }
      });
  }

  private void userCreateHandler(Message<String> message) {
    LOGGER.debug("User Verticle received message: " + message.body());
    JsonObject json = new JsonObject(message.body());
    final String user = "'" + json.getString("username") + "'";
    final String email = "'" + json.getString("email") + "'";
    final String hash = "'" + json.getString("password") + "'";
    vertx.eventBus().rxRequest(couchbaseQuery.name(), "select name, email, hashword from registration where name="
      + user + " or hashword=" + hash + " or email=" + email)
      .doOnSuccess(e -> {             //Check this later in tests and confirm if it does what you want or if it needs changes.
        if (e.body() == null) {
          LOGGER.debug("User Verticle has found no record of that user, so user can now be created.");
        } else if (e.body() != null) {
          LOGGER.debug("User Verticle has found record of already existing user: " + e.body());
        } else {
          LOGGER.debug("An error occurred retrieving data from Couchbase.");
        }
      })
      .doOnError(e -> {
        LOGGER.debug("User Verticle Error retrieving Couchbase response." + e.getMessage());
        //Decide if you need to notify user that this happened to have them try again or not.
      });
  }

  private void userPasswordResetHandler(Message<String> message) {
    LOGGER.debug("User Verticle received message: " + message.body());
    JsonObject json = new JsonObject(message.body());
    final String email = "'" + json.getString("email") + "'";
    vertx.eventBus().rxRequest(couchbaseQuery.name(),
      "select name, email from registration where email=" + email)
      .doOnSuccess(e -> {
        if (e.body() == null) {
          LOGGER.debug("User Verticle has found no record containing that email, so user should create account.");
        } else if (e.body() != null) {
          LOGGER.debug("User Verticle has found a record containing that email: " + e.body());
        } else {
          LOGGER.debug("An error occurred retrieving data from Couchbase.");
        }
      });
  }

}
