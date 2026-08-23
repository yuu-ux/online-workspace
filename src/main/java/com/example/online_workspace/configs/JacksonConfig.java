package com.example.online_workspace.configs;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;

/**
 * JSON入力をAPI契約に合わせて厳密に扱う設定。
 */
@Configuration
public class JacksonConfig {

	/**
	 * 未定義のJSONプロパティを受け付けないMapper設定を提供する。
	 *
	 * @return JSON Mapperのカスタマイザー
	 */
	@Bean
	JsonMapperBuilderCustomizer strictJsonInput() {
		return builder -> builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}
}
