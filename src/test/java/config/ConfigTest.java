package config;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigTest {
	private static final Logger log = LoggerFactory.getLogger(ConfigTest.class);

	@Test
	public void test() throws ConfigurationException {
		String[] configKeys = { "bitcoin.rpc.URL", "bitcoin.rpc.rpcuser", "bitcoin.rpc.rpcpassword", "db.host",
				"db.name", "db.user", "db.password" };
		Configuration config = new Configurations().properties(new File("bitcoin.properties"));
		for (String key : configKeys) {
			log.info("{}: {}", key, config.getString(key));
			assertNotNull(config.getString(key));
		}
	}

}
