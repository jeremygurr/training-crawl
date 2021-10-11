package com.epicGamecraft.dungeonCrawlDepths;



import io.vertx.reactivex.core.http.HttpServerResponse;
import org.apache.sshd.common.util.io.IoUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Collectors;


public class WebUtils {
  public static void redirect(HttpServerResponse response, String target) {
    response.setStatusCode(303);
    response.putHeader("Location", target);
    response.end();
  }

  public static void failMessage(HttpServerResponse response, String message) {
    response.setStatusCode(200);
    response.write("<p style='color:red;'>" + message + "</p>");
  }

  public static String generateHtml(String path, String errMessage) throws IOException {
    File file = new File(path);
    return String.valueOf(file);
  }
}
//    path convert to content, take content and replace [[error]] with errMessage and return as String
// Whatever calls that takes the String and does response.write with it.
