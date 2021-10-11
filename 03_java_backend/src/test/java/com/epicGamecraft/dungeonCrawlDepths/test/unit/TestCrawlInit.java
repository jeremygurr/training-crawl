package com.epicGamecraft.dungeonCrawlDepths.test.unit;

import com.epicGamecraft.dungeonCrawlDepths.GameCreationVerticle;
import com.epicGamecraft.dungeonCrawlDepths.GameListVerticle;
import com.epicGamecraft.dungeonCrawlDepths.HttpServerVerticle;
import com.epicGamecraft.dungeonCrawlDepths.UserVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
//import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Authenticator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class TestCrawlInit {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestCrawlInit.class);

  @Test
  void verticle_deployed(Vertx vertx, VertxTestContext context) throws Throwable {
    vertx.deployVerticle(new HttpServerVerticle());
    context.completeNow();
  }

  @Test
  void loginSuccessMysql(Vertx vertx, VertxTestContext context) throws Throwable {
    FakeMysqlVerticle mysqlVerticle = new FakeMysqlVerticle();
    mysqlVerticle.response = "{\"email\":\"bob@yahoo.com\",\"hashword\":\"1216985755\",\"name\":\"Bob John\"}";
    vertx.rxDeployVerticle(mysqlVerticle)
      .flatMap(e -> {
        return vertx.rxDeployVerticle(new UserVerticle());   //This is how you deploy more than one verticle. Just use more .flatmaps().
      })
      .subscribe(e -> {
          vertx.eventBus().rxRequest(userLogin.name(), "{\"usernameOrEmail\":\"billybob\",\"password\":\"password\"}")
            .subscribe(ar -> {
                LOGGER.debug("Test Verticle received reply: " + ar.body());
                JsonObject json = new JsonObject(ar.body().toString());
                if (json.getString("redirect").equals("serverError.html")) {
                  context.failNow(new Exception("User Verticle failed to retrieve data from FakeMysqlVerticle."));
                } else {
                  context.completeNow();
                }
              },
              err -> {
                LOGGER.debug("An error occurred retrieving data from Mysql.");
              });
        },
        err -> {
          context.failNow(err);
        });
  }

  @Test
  void loginFailureMysql(Vertx vertx, VertxTestContext context) throws Throwable {
    FakeMysqlVerticle mysqlVerticle = new FakeMysqlVerticle();
    mysqlVerticle.response = null;
    vertx.rxDeployVerticle(mysqlVerticle)
      .flatMap(e -> vertx.rxDeployVerticle(new UserVerticle()))
      .subscribe(e -> {
          vertx.eventBus().rxRequest(userLogin.name(), "{\"usernameOrEmail\":\"b\",\"password\":\"pass\"}")
            .subscribe(ar -> {
                LOGGER.debug("Test Verticle received reply: " + ar.body());
                JsonObject json = new JsonObject(ar.body().toString());
                if (json.getString("redirect").equals("serverError.html")) {
                  context.failNow(new Exception("User Verticle failed to retrieve data from FakeMysqlVerticle."));
                } else {
                  context.completeNow();
                }
              },
              err -> {
                LOGGER.debug("An error occurred retrieving data from Mysql." + err.getMessage());
              });
        },
        err -> {
          context.failNow(err);
        });
  }

  @Test
  void gameListSuccess(Vertx vertx, VertxTestContext context) throws Throwable {
    FakeMysqlVerticle mysqlVerticle = new FakeMysqlVerticle();
    mysqlVerticle.response = "{\"game_id\":1,\"character_Name\":\"Ross the cold Enchanter\",\"status\":\"paused\",\"floorNumber\":\"1\",\"playtime\":\"00:00:00\",\"creation_time\":\"2021-04-10 18:28:57\",\"user\":\"billybob\",\"scenario\":\"normal\"}";
    vertx.rxDeployVerticle(mysqlVerticle)
      .flatMap(e -> vertx.rxDeployVerticle(new GameListVerticle()))
      .subscribe(e -> {
          vertx.eventBus().rxRequest(gameList.name(), "basic")
            .subscribe(ar -> {
              LOGGER.debug("Test Verticle received reply: " + ar.body());
                if (ar.body().toString().equals("failed")) {
                  context.failNow(new Exception("User Verticle failed to retrieve data from FakeMysqlVerticle."));
                } else {
                  context.completeNow();
                }
              },
              err -> {
                LOGGER.debug("An error occurred retrieving game list." + err.getMessage());
              });
        },
        err -> {
          context.failNow(err);
        });
  }

  @Test
  void gameListFailure(Vertx vertx, VertxTestContext context) throws Throwable {
    FakeMysqlVerticle mysqlVerticle = new FakeMysqlVerticle();
    mysqlVerticle.response = null;
    vertx.rxDeployVerticle(mysqlVerticle)
      .flatMap(e -> vertx.rxDeployVerticle(new GameListVerticle()))
      .subscribe(e -> {
          vertx.eventBus().rxRequest(gameList.name(), "basic")
            .subscribe(ar -> {
                LOGGER.debug("Test Verticle received reply: " + ar.body());
                if (ar.body().toString().equals("success")) {
                  context.failNow(new Exception("User Verticle failed to retrieve data from FakeMysqlVerticle."));
                } else {
                  context.completeNow();
                }
              },
              err -> {
                LOGGER.debug("An error occurred retrieving game list." + err.getMessage());
                context.failNow(err);
              });
        },
        err -> {
          context.failNow(err);
        });
  }

  @Test
  void GameCreateSuccess(Vertx vertx, VertxTestContext context) throws Throwable {
    FakePlayerVerticle playerVerticle = new FakePlayerVerticle();
    playerVerticle.response = "success";
    FakeRuneVerticle runeVerticle = new FakeRuneVerticle();
    runeVerticle.response = "success";
    vertx.rxDeployVerticle(playerVerticle)
      .flatMap(e -> vertx.rxDeployVerticle(runeVerticle))
      .flatMap(e -> vertx.rxDeployVerticle(new GameCreationVerticle()))
      .subscribe(e -> {
        vertx.eventBus().rxRequest(gameCreation.name(), "normal")
          .subscribe(ar -> {
            LOGGER.debug("Test.GameCreate received reply: " + ar.body());
            context.completeNow();
          },
            err -> {
            LOGGER.debug("An error occurred creating the Game.");
            context.failNow(err);
            });
      },
        err -> {
        context.failNow(err);
        });
  }

  @Test   // Source: https://examples.javacodegeeks.com/core-java/java-11-standardized-http-client-api-example/
  public void httpPostTest() throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create("http://localhost:8080/api/student"))
      .timeout(Duration.ofSeconds(15))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString("name=betsy&email=betsy@mail.com&dob=1992-08-06"))  // HttpRequest.BodyPublishers.ofFile(Paths.get("file.json")) This is how to get from a file instead of string.
      .build();
    HttpResponse response = client.send(request, HttpResponse.BodyHandlers.discarding());
    assertTrue(response.statusCode() == 201, "Status Code is not Created");
  }


  @Test
  public void httpGetTest() {
    HttpClient client = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_2)
      .followRedirects(HttpClient.Redirect.NORMAL)
      .connectTimeout(Duration.ofSeconds(10))
//      .proxy(ProxySelector.of(new InetSocketAddress("www-proxy.com", 8080)))
      .authenticator(Authenticator.getDefault())
      .build();
  }

}
