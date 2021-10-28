package com.epicGamecraft.dungeonCrawlDepths;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;
import static com.epicGamecraft.dungeonCrawlDepths.MessageKey.redirect;
import static com.epicGamecraft.dungeonCrawlDepths.MessageKey.response;

public class UserVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserVerticle.class);


  @Override
  public Completable rxStart() {
    vertx.eventBus().consumer(userLogin.name(), this::handleUserLogin);
    vertx.eventBus().consumer(createUser.name(), this::createUserAccount);
    vertx.eventBus().consumer(forgotPassword.name(), this::userPassResHandler);
    return Completable.complete();
  }

  private void handleUserLogin(Message<String> message) {
    LOGGER.debug("UserVerticle.handleUserLogin received message: " + message.body());
    final JsonObject jsonReply = new JsonObject();
    vertx.eventBus().rxRequest(mysqlQuery.name(), message.body())
      .subscribe(e -> {
          if (e.body() != null) {
            LOGGER.debug("User Verticle received reply: " + e.body());
            jsonReply.put(redirect.name(), "jscrawl.html");
          } else {
            LOGGER.debug("Invalid Login");
            jsonReply.put(redirect.name(), "login.html");
            jsonReply.put(response.name(), "Invalid Login");
          }
          message.reply(jsonReply);
        },
        err -> {
          LOGGER.debug("UserVerticle Error communicating with CouchbaseVerticle: " + err.getMessage());
          jsonReply.put(redirect.name(), "serverError.html");
          message.reply(jsonReply);
        }
      );
  }

  private void createUserAccount(Message<String> message) {
    LOGGER.debug("UserVerticle.userCreateHandler received message: " + message.body());
    final JsonObject jsonReply = new JsonObject();
    vertx.eventBus().rxRequest(mysqlInsert.name(), message.body())
      .subscribe(e -> {
          if (e.body() == null) {
            LOGGER.debug("User Verticle has found no record of that user, so user has been created.");
            jsonReply.put(redirect.name(), "login.html");
            jsonReply.put(response.name(), "Your account was successfully created.");
          } else {
            LOGGER.debug("Mysql Verticle failed to create new user : " + e.body());
            jsonReply.put(redirect.name(), "createLogin.html");
            jsonReply.put(response.name(), "An account with that information already exists.");
          }
          message.reply(jsonReply);
        },
        err -> {
          LOGGER.debug("User Verticle Error communicating with MySQLVerticle : " + err.getMessage());
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
