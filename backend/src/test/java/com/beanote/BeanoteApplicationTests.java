package com.beanote;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.beanote.support.IntegrationTest;

@IntegrationTest
class BeanoteApplicationTests {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void contextLoadsAndFlywayMigrationIsApplied() {
		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_info", Integer.class);
		assertThat(count).isEqualTo(1);
	}

}
