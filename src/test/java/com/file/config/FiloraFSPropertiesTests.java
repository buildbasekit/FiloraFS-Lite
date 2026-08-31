package com.file.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class FiloraFSPropertiesTests {

	@ParameterizedTest
	@ValueSource(strings = { "filorafs.api-key=", "filorafs.storage-path=" })
	void blankRequiredConfigurationFailsStartup(String invalidProperty) {
		new ApplicationContextRunner()
				.withUserConfiguration(PropertiesConfiguration.class)
				.withPropertyValues("filorafs.storage-path=./uploads", "filorafs.api-key=test-key", invalidProperty)
				.run(context -> assertThat(context).hasFailed());
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(FiloraFSProperties.class)
	static class PropertiesConfiguration {
	}
}
