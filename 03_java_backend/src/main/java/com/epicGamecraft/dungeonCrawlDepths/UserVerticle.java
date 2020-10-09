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
    vertx.eventBus().consumer(forgotPassword.name(), this::userPasswordResetHandler);
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
    vertx.eventBus().rxRequest(couchbaseQuery.name(), message.body())
      .subscribe(e -> {
          LOGGER.debug("Received object: " + e.body());
          if (e.body() == "empty") {
            //This means that no record exists for the user, so a new record can now be created.
            LOGGER.debug("User Verticle has found no record of that user, so user can now be created.");
            message.reply("4");
            //TODO: Here use a method to add user to database through couchbaseVerticle, and redirect
            // the user to the login page through httpServerVerticle.
          } else if (e.body() == null) {
            //This means that the couchbase query failed due to syntax error in query.
            LOGGER.debug("Couchbase Verticle failed to retreive data.");
            //TODO: Redirect user to "Sorry, we are experiencing technical difficulties." page.
          } else {
            //This means that the username/password/email already exists in database, and user should login.
            LOGGER.debug("User Verticle has found record of already existing user: " + e.body());
            message.reply("5");
            //TODO: Have Javascript alert user that the "username or email already in use with an account."
            // Then consider redirecting them to login page or ask if they want to reset password.
          }
        },
        err -> {
          //This means the eventbus failed to communicate properly with the CouchbaseVerticle or vice versa.
          LOGGER.debug("User Verticle Error communicating with CouchbaseVerticle : " + err.getMessage());
          message.reply("3");
        });
  }

  private void userPasswordResetHandler(Message<String> message) {
    LOGGER.debug("UserVerticle.userPasswordResetHandler received message: " + message.body());
    vertx.eventBus().rxRequest(couchbaseQuery.name(), message.body())
      .subscribe(e -> {
          LOGGER.debug("Received object: " + e.body());
          if (e.body() == "empty") {
            //This means that the email given did not match any records so user can try another email or create account.
            LOGGER.debug("User Verticle has found no record containing that email, so user should create account or try another email.");
            message.reply("6");
            //TODO: Have javascript respond with alert that says "We found no account matching that email"
          } else if (e.body() == null) {
            //This means that the couchbase query failed due to syntax error in query.
          } else {
            //This means that users account was located in db and now they will be sent email with password reset.
            LOGGER.debug("User Verticle has found a record containing that email: " + e.body());
            message.reply("7");
            //TODO: Use a method to notify user that a email has been sent to let them reset their password
            // and use a method to send them the email and receive their response of new password. Then redirect
            // user to the login page so they can try the new password.
          }
        },
        err -> {
          //This means the eventbus failed to communicate properly with the CouchbaseVerticle or vice versa.
          LOGGER.debug("User Verticle Error communicating with CouchbaseVerticle : " + err.getMessage());
          message.reply("3");
          //TODO: Use a method to respond with a error page saying "Sorry, our site is having trouble
          // retrieving data from the server."
          // Decide if you need to notify user that this happened to have them try again or not.
        });
  }

}
