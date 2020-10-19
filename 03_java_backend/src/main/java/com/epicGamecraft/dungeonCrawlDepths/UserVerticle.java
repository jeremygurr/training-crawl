package com.epicGamecraft.dungeonCrawlDepths;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;
import static com.epicGamecraft.dungeonCrawlDepths.UserResult.*;

import io.vertx.reactivex.ext.web.Session;
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
            message.reply(successLog.name());
            Context vertxContext = vertx.getOrCreateContext();
            Session session = vertxContext.get("session");
            if(session == null) {  //FIXME: This always returns true currently, which means we are not getting the context correctly.
              message.fail(500, "Session is null"); //FIXME: This doesn't seem to do anything.
              return;
            }
            session.put(SessionKey.username.name(), "username"); //TODO: Find a way to add a cookie to session here.
            LOGGER.debug("session equals : " + session.get(SessionKey.username.name()));
          } else {
            //This means user input wrong username or password.
            LOGGER.debug("Invalid Login");
            message.reply(invalid.name());
          }
        },
        err -> {
          //This means the eventbus failed to communicate properly with the CouchbaseVerticle or vice versa.
          LOGGER.debug("UserVerticle Error communicating with CouchbaseVerticle : " + err.getMessage());
          message.reply(messageErr.name());
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
            message.reply(registerUser.name());
          } else {
            // This means that the couchbase query failed either from syntax error or user already exists.
            LOGGER.debug("Couchbase Verticle failed to create new user : " + e.body());
            message.reply(invalid.name());
          }
        },
        err -> {
          //This means the eventbus failed to communicate properly with the CouchbaseVerticle or vice versa.
          LOGGER.debug("User Verticle Error communicating with CouchbaseVerticle : " + err.getMessage());
          message.reply(messageErr.name());
        });
  }

  private void userPassResHandler(Message<String> message) {
    LOGGER.debug("UserVerticle.userPassResHandler received message: " + message.body());
    vertx.eventBus().rxRequest(couchbasePass.name(), message.body())
      .subscribe(e -> {
          if (e.body() == null) {
            LOGGER.debug("username or email was incorrect.");
            message.reply(invalid.name());
          } else {
            LOGGER.debug("Received object: " + e.body());
            message.reply(resetPass.name());
          }
        },
        err -> {
          LOGGER.debug("User Verticle Error communicating with CouchbaseVerticle : " + err.getMessage());
          message.reply(messageErr.name());
        });
  }

}
