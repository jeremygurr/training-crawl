package com.epicGamecraft.dungeonCrawlDepths;

import io.vertx.reactivex.core.http.HttpServerResponse;

public class WebUtils {
  public static void redirect(HttpServerResponse response, String target) {
    response.setStatusCode(303);
    response.putHeader("Location", target);
    response.end();
  }
}
