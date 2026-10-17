package com.beanote.support;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 테스트 컨텍스트가 뜰 때 테스트 DB를 비우고 마이그레이션을 처음부터 다시 적용한다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestFlywayConfig {

	@Bean
	public FlywayMigrationStrategy cleanMigrateStrategy() {
		return flyway -> {
			flyway.clean();
			flyway.migrate();
		};
	}

}
