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
    LOGGER.debug("User Verticle is listening to: " + userLogin.name()); //TODO: Either add createUser.name() + forgotPassword.name(), or ask why we just did userLogin.name() only? What purpose does this serve to have busEvent enums?
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
    vertx.eventBus().rxRequest(couchbaseQuery.name(), "select name from depths where name="
      + user + " and hashword=" + hash)
      .doOnSuccess(e -> {
        if (e.body() == null) {
          LOGGER.debug("Invalid Login");
          //TODO: make javascript do an alert that says "invalid login" to user.
        } else if (e.body() != null) {
          LOGGER.debug("User Verticle received reply: " + e.body());
          //TODO: Use a method to direct to main crawl page.
        } else {
          LOGGER.debug("An error occurred retrieving data from Couchbase.");
          //TODO: Respond with a error page that says "Sorry, site is having problems retrieving server data. Please come back later"
          // Or maybe remove this else code and do this for the .doOnError line.
        }
      }).doOnError(e -> {
        LOGGER.debug("User Verticle Error retrieving Couchbase response. " + e.getMessage());
    })
    .subscribe(ar -> {
      LOGGER.debug("Received object: " + ar.body());
    });
  }

  private void userCreateHandler(Message<String> message) {
    LOGGER.debug("User Verticle received message: " + message.body());
    JsonObject json = new JsonObject(message.body());
    final String user = "'" + json.getString("username") + "'";
    final String email = "'" + json.getString("email") + "'";
    final String hash = "'" + json.getString("password") + "'";
    vertx.eventBus().rxRequest(couchbaseQuery.name(), "select name, email, hashword from depths where name="
      + user + " or hashword=" + hash + " or email=" + email)
      .doOnSuccess(e -> {             //Check this later in tests and confirm if it does what you want or if it needs changes.
        if (e.body() == null) {
          LOGGER.debug("User Verticle has found no record of that user, so user can now be created.");
          //TODO: Here use a method to add user to database through couchbase verticle, and redirect
          // the user to the login page.
        } else if (e.body() != null) {
          LOGGER.debug("User Verticle has found record of already existing user: " + e.body());
          //TODO: Have Javascript alert user that the "username or email already in use with an account."
          // Then consider redirecting them to login page or ask if they want to reset password.
        } else {
          LOGGER.debug("An error occurred retrieving data from Couchbase.");
          //TODO: respond with error page that says "Sorry, site is having problems retrieving server data. Please come back later"
          // Or maybe remove this else code and do this for the .doOnError line.
        }
      })
      .doOnError(e -> {
        LOGGER.debug("User Verticle Error retrieving Couchbase response." + e.getMessage());
      })
      .subscribe(ar -> {
        LOGGER.debug("Received object: " + ar.body());
      });
  }

  private void userPasswordResetHandler(Message<String> message) {
    LOGGER.debug("User Verticle received message: " + message.body());
    JsonObject json = new JsonObject(message.body());
    final String email = "'" + json.getString("email") + "'";
    vertx.eventBus().rxRequest(couchbaseQuery.name(),
      "select name, email from depths where email=" + email)
      .doOnSuccess(e -> {
        if (e.body() == null) {
          LOGGER.debug("User Verticle has found no record containing that email, so user should create account or try another email.");
          //TODO: Have javascript respond with alert that says "We found no account matching that email"
        } else if (e.body() != null) {
          LOGGER.debug("User Verticle has found a record containing that email: " + e.body());
          //TODO: Use a method to notify user that a email has been sent to let them reset their password
          // and use a method to send them the email and receive their response of new password. Then redirect
          // user to the login page so they can try the new password.
        } else {
          LOGGER.debug("An error occurred retrieving data from Couchbase.");
          //TODO: Use a method to respond with a error page saying "Sorry, our site is having trouble
          // retrieving data from the server." Or get rid of this code and put it in .doOnError() section.
        }
      })
      .doOnError(e -> {
      LOGGER.debug("User Verticle Error retrieving Couchbase response." + e.getMessage());
      //Decide if you need to notify user that this happened to have them try again or not.
    })
      .subscribe(ar -> {
        LOGGER.debug("Received object: " + ar.body());
      });
  }

}
