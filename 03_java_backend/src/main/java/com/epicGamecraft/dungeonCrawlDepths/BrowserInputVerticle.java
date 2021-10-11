package com.epicGamecraft.dungeonCrawlDepths;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.browserInput;

public class BrowserInputVerticle extends AbstractVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(BrowserInputVerticle.class);

	@Override
	public void start(Promise<Void> promise) throws Exception {
		vertx.eventBus().consumer(browserInput.name(), this::handleKey);
	}

	private void handleKey(Message<String> message) {
		LOGGER.debug("Received message: " + message.body());
	}

}
