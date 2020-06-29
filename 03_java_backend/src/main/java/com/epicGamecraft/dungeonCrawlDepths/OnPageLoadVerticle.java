package com.epicGamecraft.dungeonCrawlDepths;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

public class OnPageLoadVerticle extends AbstractVerticle {

  @Override
  public void start() throws Exception {
	  checkSessionId();
  }
  
  public int checkSessionId() {
	  
	  //find cookie;
	  id == null ? createSessionId() : return id; 
  }
  
  public static int createSessionId() {
	  int id = n + 1;
	  return id;
  }
}
