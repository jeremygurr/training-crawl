package com.epicGamecraft.dungeonCrawlDepths;

import io.vertx.reactivex.core.http.HttpServerResponse;
import io.reactivex.annotations.Nullable;

public class WebUtils {
  public static void redirect(HttpServerResponse response, String target) {
    response.setStatusCode(303);
    response.putHeader("Location", target);
    response.end();
  }

  public static void failMessage(HttpServerResponse response, String message) {
    response.setStatusCode(200);
    response.putHeader("Content-Length", "100");
    response.write("<html><body><p style='color:red;'>" + message + "</p></body></html>");
  }
}
