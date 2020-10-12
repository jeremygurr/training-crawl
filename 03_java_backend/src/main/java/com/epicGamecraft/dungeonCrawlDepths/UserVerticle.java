package com.epicGamecraft.dungeonCrawlDepths;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.*;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.*;
import io.vertx.reactivex.core.eventbus.Message;

public class UserVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserVerticle.class);


  @Override
  public Completable rxStart() {
    LOGGER.debug("User Verticle is listening to: " + userLogin.name());
    vertx.eventBus().consumer(userLogin.name(), this::handleUser);
    vertx.eventBus().consumer(createUser.name(), this::userCreateHandler);
    vertx.eventBus().consumer(forgotPassword.name(), this::userPassResHandler);
    return Completable.complete();
  }

  //here is new code:
  private void handleUser(Message<String> message) {
    LOGGER.debug("UserVerticle.handleUser received message: " + message.body());
    vertx.eventBus().rxRequest(couchbaseQuery.name(), message.body())
      .subscribe(e -> {
          LOGGER.debug("Received object: " + e.body());
          if (e.body() != null) {
            //This means user did correct username and password.
            LOGGER.debug("User Verticle received reply: " + e.body());
            message.reply("1");
            //TODO: Use a method to direct to main crawl page.
          } else {
            //This means user input wrong username or password.
            LOGGER.debug("Invalid Login");
            message.reply("2");
            //TODO: make javascript do an alert that says "invalid login" to user.
          }
        },
        err -> {
          //This means the eventbus failed to communicate properly with the CouchbaseVerticle or vice versa.
          LOGGER.debug("UserVerticle Error communicating with CouchbaseVerticle : " + err.getMessage());
          //TODO: add failure code here.
          message.reply("3");
        }
      );
  }

  private void userCreateHandler(Message<String> message) {
    LOGGER.debug("UserVerticle.userCreateHandler received message: " + message.body());
    vertx.eventBus().rxRequest(couchbaseInsert.name(), message.body())
      .subscribe(e -> {
          LOGGER.debug("Received object: " + e.body());
          if (e.body() == null) {
            LOGGER.debug("User Verticle has found no record of that user, so user has been created.");
            message.reply("4");
            //TODO: Here use a method to add user to database through couchbaseVerticle, and redirect
            // the user to the login page through httpServerVerticle.
          } else {
            // This means that the couchbase query failed either from syntax error or user already exists.
            LOGGER.debug("Couchbase Verticle failed to create new user : " + e.body());
            //TODO: Make user try a different username/email/password combo.
            message.reply("2");
          }
        },
        err -> {
          //This means the eventbus failed to communicate properly with the CouchbaseVerticle or vice versa.
          LOGGER.debug("User Verticle Error communicating with CouchbaseVerticle : " + err.getMessage());
          message.reply("3");
        });
  }

  private void userPassResHandler(Message<String> message) {
    LOGGER.debug("UserVerticle.userPassResHandler received message: " + message.body());
    vertx.eventBus().rxRequest(couchbasePass.name(), message.body())
      .subscribe(e -> {
          if (e.body() == null) {
            LOGGER.debug("username or email was incorrect.");
            message.reply("2");
          } else {
            LOGGER.debug("Received object: " + e.body());
            message.reply("5");
            // TODO: put method here that sends reset password email to user.
          }
        },
        err -> {
          LOGGER.debug("User Verticle Error communicating with CouchbaseVerticle : " + err.getMessage());
          message.reply("3");
        });
  }

}
