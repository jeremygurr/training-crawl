package com.epicGamecraft.dungeonCrawlDepths;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;
import static com.epicGamecraft.dungeonCrawlDepths.UserResult.*;
import static com.epicGamecraft.dungeonCrawlDepths.MessageKey.*;

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.*;
import io.vertx.reactivex.core.*;
import io.vertx.reactivex.core.eventbus.Message;

public class UserVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserVerticle.class);


  @Override
  public Completable rxStart() {
    vertx.eventBus().consumer(userLogin.name(), this::handleUserLogin);
    vertx.eventBus().consumer(createUser.name(), this::userCreateHandler);
    vertx.eventBus().consumer(forgotPassword.name(), this::userPassResHandler);
    return Completable.complete();
  }

  //here is new code:
  private void handleUserLogin(Message<String> message) {
    LOGGER.debug("UserVerticle.handleUserLogin received message: " + message.body());
    final JsonObject jsonReply = new JsonObject();
    vertx.eventBus().rxRequest(couchbaseQuery.name(), message.body())
      .subscribe(e -> {
          if (e.body() != null) {
            //This means user did correct username and password.
            LOGGER.debug("User Verticle received reply: " + e.body());
            jsonReply.put(redirect.name(), "jscrawl.html");

          } else {
            //This means user input wrong username or password.
            LOGGER.debug("Invalid Login");
            jsonReply.put(redirect.name(), "login.html");
            jsonReply.put(response.name(), "Invalid Login");
            //add a message to http verticle to notify user login information was incorrect.
            // (Perhaps add that as part of a json object message reply.)
          }
          message.reply(jsonReply);
        },
        err -> {
          //This means the eventbus failed to communicate properly with the CouchbaseVerticle or vice versa.
          LOGGER.debug("UserVerticle Error communicating with CouchbaseVerticle : " + err.getMessage());
          jsonReply.put(redirect.name(), "serverError.html");
          message.reply(jsonReply);
        }
      );
  }

  private void userCreateHandler(Message<String> message) {
    LOGGER.debug("UserVerticle.userCreateHandler received message: " + message.body());
    final JsonObject jsonReply = new JsonObject();
    vertx.eventBus().rxRequest(couchbaseInsert.name(), message.body())
      .subscribe(e -> {
          if (e.body() == null) {
            //This means that user account has successfully been created.
            LOGGER.debug("User Verticle has found no record of that user, so user has been created.");
            jsonReply.put(redirect.name(), "login.html");
            jsonReply.put(response.name(), "Your account was successfully created.");
            //Also add a message that lets them know it was successfully created.
            //And add a method that lets user click 'redirect to login page' if they want to login right then.
          } else {
            // This means that the couchbase query failed because user already exists.
            LOGGER.debug("Couchbase Verticle failed to create new user : " + e.body());
            jsonReply.put(redirect.name(), "createLogin.html");
            jsonReply.put(response.name(), "An account with that information already exists.");
            //Send message to user that says a user with that information already exists.
          }
          message.reply(jsonReply);
        },
        err -> {
          //This means the eventbus failed to communicate properly with the CouchbaseVerticle or vice versa.
          LOGGER.debug("User Verticle Error communicating with CouchbaseVerticle : " + err.getMessage());
          jsonReply.put(redirect.name(), "serverError.html");
          message.reply(jsonReply);
        });
  }

  private void userPassResHandler(Message<String> message) {
    LOGGER.debug("UserVerticle.userPassResHandler received message: " + message.body());
    final JsonObject jsonReply = new JsonObject();
    vertx.eventBus().rxRequest(couchbasePass.name(), message.body())
      .subscribe(e -> {
          if (e.body() == null) {
            LOGGER.debug("Username or email was incorrect.");
            jsonReply.put(redirect.name(), "forgotPassword.html");
            jsonReply.put(response.name(), "Username or email was incorrect.");
          } else {
            //This means username and email was correct and an email will be sent to user to reset password.
            LOGGER.debug("Received object: " + e.body());
            jsonReply.put(redirect.name(), "sentPassResEmail.html");
          }
          message.reply(jsonReply);
        },
        err -> {
          LOGGER.debug("User Verticle Error communicating with CouchbaseVerticle : " + err.getMessage());
          jsonReply.put(redirect.name(), "serverError.html");
          message.reply(jsonReply);
        });
  }

}
