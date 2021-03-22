package org.springframework.cloud.gateway.sample;

import org.apache.commons.lang.math.RandomUtils;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.HeaderRoutePredicateFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.util.function.Predicate;

public class RandomRoutePredicateFactory extends AbstractRoutePredicateFactory<RandomRoutePredicateFactory.Config> {
	public RandomRoutePredicateFactory() {
		super(Config.class);
	}

	@Override
	public Predicate<ServerWebExchange> apply(Config config) {
		// grab configuration from Config object
		return exchange -> {
			//grab the request
			ServerHttpRequest request = exchange.getRequest();
			//take information from the request to see if it
			//matches configuration.
			int number = RandomUtils.nextInt();
//			System.out.println("number:" + number);
			return number > 5;
		};
	}

	public static class Config {
//		private Integer number;
//
//		public Integer getNumber() {
//			return number;
//		}
//
//		public void setNumber(Integer number) {
//			this.number = number;
//		}
	}
}