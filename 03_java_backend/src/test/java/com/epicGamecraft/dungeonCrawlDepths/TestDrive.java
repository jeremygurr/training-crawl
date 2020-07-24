package com.epicGamecraft.dungeonCrawlDepths;

import static com.epicGamecraft.dungeonCrawlDepths.BusEvent.*;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.Promise;
import io.vertx.core.AbstractVerticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestDrive extends AbstractVerticle {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UserVerticle.class);

	@Override
	public void start(Promise<Void> promise) {
		final EventBus eb = vertx.eventBus();
		eb.request(couchbaseQuery.name(), "select name from registration where name = 'Jared Gurr' "
				+ "and hashword = 'hashpassword'", ar -> {
			if(ar.succeeded()) {
				LOGGER.debug("Result:" + ar.result().body());
			}
		});

	}
	//ask why this doesn't work..
}