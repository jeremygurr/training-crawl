package com.epicGamecraft.dungeonCrawlDepths;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class CrawlServer {

	public static void main(final String[] args) {
	  final CrawlServer crawlServer = new CrawlServer();
	}

	private final Vertx vertx;
	private final boolean debug = true;

	CrawlServer() {

		final VertxOptions options = new VertxOptions();
		if(debug) {
			options.setBlockedThreadCheckInterval(Long.MAX_VALUE >> 2);
		}

		vertx = Vertx.vertx(options);
		vertx.deployVerticle(new HttpServerVerticle());
		vertx.deployVerticle(new BrowserInputVerticle());
		vertx.deployVerticle(new UserVerticle());
		vertx.deployVerticle(new CouchbaseVerticle());

	}

}
