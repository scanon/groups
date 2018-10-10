package us.kbase.test.groups.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.google.common.base.Optional;

import us.kbase.groups.config.GroupsConfig;
import us.kbase.groups.config.GroupsConfigurationException;
import us.kbase.groups.service.SLF4JAutoLogger;
import us.kbase.groups.util.FileOpener;
import us.kbase.test.groups.TestCommon;

public class GroupsConfigTest {

	private GroupsConfig getConfig(final FileOpener opener) throws Throwable {
		final Constructor<GroupsConfig> con =
				GroupsConfig.class.getDeclaredConstructor(FileOpener.class);
		con.setAccessible(true);
		try {
			return con.newInstance(opener);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}
	
	private GroupsConfig getConfig(
			final Path iniFilePath,
			final boolean nullLogger,
			final FileOpener opener)
			throws Throwable {
		final Constructor<GroupsConfig> con =
				GroupsConfig.class.getDeclaredConstructor(
						Path.class, boolean.class, FileOpener.class);
		con.setAccessible(true);
		try {
			return con.newInstance(iniFilePath, nullLogger, opener);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}
	
	@Test
	public void sysPropNoUserNoBools() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		final GroupsConfig cfg;
		try {
			System.setProperty(GroupsConfig.ENV_VAR_KB_DEP, "some file");
			TestCommon.getenv().put(GroupsConfig.ENV_VAR_KB_DEP, "some file2");
			when(fo.open(Paths.get("some file"))).thenReturn(new ByteArrayInputStream(
					("[groups]\n" +
					 "mongo-host=mongo\n" +
					 "mongo-db=database\n" +
					 "auth-url=     http://auth.com       \n" +
					 "workspace-url=http://ws.com\n")
					.getBytes()));
			cfg = getConfig(fo);
		} finally {
			System.clearProperty(GroupsConfig.ENV_VAR_KB_DEP);
			TestCommon.getenv().remove(GroupsConfig.ENV_VAR_KB_DEP);
		}
		
		assertThat("incorrect mongo host", cfg.getMongoHost(), is("mongo"));
		assertThat("incorrect mongo db", cfg.getMongoDatabase(), is("database"));
		assertThat("incorrect mongo user", cfg.getMongoUser(), is(Optional.absent()));
		assertThat("incorrect mongo pwd", cfg.getMongoPwd(), is(Optional.absent()));
		assertThat("incorrect auth url", cfg.getAuthURL(), is(new URL("http://auth.com")));
		assertThat("incorrect ws url", cfg.getWorkspaceURL(), is(new URL("http://ws.com")));
		assertThat("incorrect allow insecure", cfg.isAllowInsecureURLs(), is(false));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(false));
		testLogger(cfg.getLogger(), false);
	}
	
	@Test
	public void sysPropNoUserNoBoolsWhitespace() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		final GroupsConfig cfg;
		try {
			System.setProperty(GroupsConfig.ENV_VAR_KB_DEP, "some file2");
			TestCommon.getenv().put(GroupsConfig.ENV_VAR_KB_DEP, "some file");
			when(fo.open(Paths.get("some file2"))).thenReturn(new ByteArrayInputStream(
					("[groups]\n" +
					 "mongo-host=mongo\n" +
					 "mongo-db=database\n" +
					 "mongo-user=\n" +
					 "mongo-pwd=\n" +
					 "auth-url=http://auth.com\n" +
					 "workspace-url=http://ws.com\n" +
					 "allow-insecure-urls=true1\n" +
					 "dont-trust-x-ip-headers=true1\n")
					.getBytes()));
			cfg = getConfig(fo);
		} finally {
			System.clearProperty(GroupsConfig.ENV_VAR_KB_DEP);
			TestCommon.getenv().remove(GroupsConfig.ENV_VAR_KB_DEP);
		}
		
		assertThat("incorrect mongo host", cfg.getMongoHost(), is("mongo"));
		assertThat("incorrect mongo db", cfg.getMongoDatabase(), is("database"));
		assertThat("incorrect mongo user", cfg.getMongoUser(), is(Optional.absent()));
		assertThat("incorrect mongo pwd", cfg.getMongoPwd(), is(Optional.absent()));
		assertThat("incorrect auth url", cfg.getAuthURL(), is(new URL("http://auth.com")));
		assertThat("incorrect ws url", cfg.getWorkspaceURL(), is(new URL("http://ws.com")));
		assertThat("incorrect allow insecure", cfg.isAllowInsecureURLs(), is(false));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(false));
		testLogger(cfg.getLogger(), false);
	}
	
	@Test
	public void envVarWithUserWithBools() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		final GroupsConfig cfg;
		try {
			TestCommon.getenv().put(GroupsConfig.ENV_VAR_KB_DEP, "some file");
			when(fo.open(Paths.get("some file"))).thenReturn(new ByteArrayInputStream(
					("[groups]\n" +
					 "mongo-host=mongo\n" +
					 "mongo-db=database\n" +
					 "mongo-user=userfoo\n" +
					 "mongo-pwd=somepwd\n" +
					 "auth-url=https://auth.com\n" +
					 "workspace-url=https://ws.com\n" +
					 "allow-insecure-urls=true\n" +
					 "dont-trust-x-ip-headers=true\n")
					.getBytes()));
			cfg = getConfig(fo);
		} finally {
			TestCommon.getenv().remove(GroupsConfig.ENV_VAR_KB_DEP);
		}
		
		assertThat("incorrect mongo host", cfg.getMongoHost(), is("mongo"));
		assertThat("incorrect mongo db", cfg.getMongoDatabase(), is("database"));
		assertThat("incorrect mongo user", cfg.getMongoUser(), is(Optional.of("userfoo")));
		assertThat("incorrect mongo pwd", cfg.getMongoPwd().get(),
				equalTo("somepwd".toCharArray()));
		assertThat("incorrect auth url", cfg.getAuthURL(), is(new URL("https://auth.com")));
		assertThat("incorrect ws url", cfg.getWorkspaceURL(), is(new URL("https://ws.com")));
		assertThat("incorrect allow insecure", cfg.isAllowInsecureURLs(), is(true));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(true));
		testLogger(cfg.getLogger(), false);
	}
	
	@Test
	public void pathNoUserNoBoolsStdLogger() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file2"))).thenReturn(new ByteArrayInputStream(
				("[groups]\n" +
				 "mongo-host=mongo\n" +
				 "mongo-db=database\n" +
				 "auth-url=https://auth.com\n" +
				 "workspace-url=https://ws.com\n")
				.getBytes()));
		final GroupsConfig cfg = getConfig(Paths.get("some file2"), false, fo);
		
		assertThat("incorrect mongo host", cfg.getMongoHost(), is("mongo"));
		assertThat("incorrect mongo db", cfg.getMongoDatabase(), is("database"));
		assertThat("incorrect mongo user", cfg.getMongoUser(), is(Optional.absent()));
		assertThat("incorrect mongo pwd", cfg.getMongoPwd(), is(Optional.absent()));
		assertThat("incorrect auth url", cfg.getAuthURL(), is(new URL("https://auth.com")));
		assertThat("incorrect ws url", cfg.getWorkspaceURL(), is(new URL("https://ws.com")));
		assertThat("incorrect allow insecure", cfg.isAllowInsecureURLs(), is(false));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(false));
		testLogger(cfg.getLogger(), false);
	}
	
	@Test
	public void pathWithUserWithBoolsNullLogger() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file2"))).thenReturn(new ByteArrayInputStream(
				("[groups]\n" +
				 "mongo-host=mongo\n" +
				 "mongo-db=database\n" +
				 "mongo-user=userfoo\n" +
				 "mongo-pwd=somepwd\n" +
				 "auth-url=https://auth.com\n" +
				 "workspace-url=https://ws.com\n" +
				 "allow-insecure-urls=true\n" +
				 "dont-trust-x-ip-headers=true\n")
				.getBytes()));
		final GroupsConfig cfg = getConfig(Paths.get("some file2"), true, fo);
		
		assertThat("incorrect mongo host", cfg.getMongoHost(), is("mongo"));
		assertThat("incorrect mongo db", cfg.getMongoDatabase(), is("database"));
		assertThat("incorrect mongo user", cfg.getMongoUser(), is(Optional.of("userfoo")));
		assertThat("incorrect mongo pwd", cfg.getMongoPwd().get(),
				equalTo("somepwd".toCharArray()));
		assertThat("incorrect auth url", cfg.getAuthURL(), is(new URL("https://auth.com")));
		assertThat("incorrect ws url", cfg.getWorkspaceURL(), is(new URL("https://ws.com")));
		assertThat("incorrect allow insecure", cfg.isAllowInsecureURLs(), is(true));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(true));
		testLogger(cfg.getLogger(), true);
	}
	
	private void testLogger(final SLF4JAutoLogger logger, final boolean nullLogger) {
		// too much of a pain to really test. Just test manually which is trivial.
		logger.setCallInfo("GET", "foo", "0.0.0.0");
		
		assertThat("incorrect ID", logger.getCallID(), is(nullLogger ? (String) null : "foo"));
	}
	
	@Test
	public void configFailNoEnvPath() throws Throwable {
		failConfig(new FileOpener(), new GroupsConfigurationException(
				"Could not find deployment configuration file from the " +
				"environment variable / system property KB_DEPLOYMENT_CONFIG"));
	}
	
	@Test
	public void configFailWhiteSpaceEnvPath() throws Throwable {
		// can't put nulls into the sysprops or env
		failConfig("     \t    ", new FileOpener(), new GroupsConfigurationException(
				"Could not find deployment configuration file from the " +
						"environment variable / system property KB_DEPLOYMENT_CONFIG"));
	}
	
	@Test
	public void configFail1ArgExceptionOnOpen() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file"))).thenThrow(new IOException("yay"));
		
		failConfig("some file", fo, new GroupsConfigurationException(
				"Could not read configuration file some file: yay"));
	}
	
	@Test
	public void configFail3ArgExceptionOnOpen() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file"))).thenThrow(new IOException("yay"));
		
		failConfig(Paths.get("some file"), fo, new GroupsConfigurationException(
				"Could not read configuration file some file: yay"));
	}
	
	@Test
	public void configFailBadIni() throws Throwable {
		failConfigBoth("foobar", new GroupsConfigurationException(
				"Could not read configuration file some file: parse error (at line: 1): foobar"));
	}
	
	@Test
	public void configFailNoSection() throws Throwable {
		failConfigBoth("", new GroupsConfigurationException(
				"No section groups in config file some file"));
	}
	
	@Test
	public void configFailNoHost() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-db=bar\n" +
				"auth-url=https://auth.com\n" +
				"workspace-url=https://ws.com\n",
				new GroupsConfigurationException(
						"Required parameter mongo-host not provided in configuration file " +
						"some file, section groups"));
		
		failConfigBoth(
				"[groups]\n" +
				"mongo-db=bar\n" +
				"mongo-host=     \t     \n" +
				"auth-url=https://auth.com\n" +
				"workspace-url=https://ws.com\n",
				new GroupsConfigurationException(
						"Required parameter mongo-host not provided in configuration file " +
						"some file, section groups"));
	}
	
	@Test
	public void configFailNoDB() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"auth-url=https://auth.com\n" +
				"workspace-url=https://ws.com\n",
				new GroupsConfigurationException(
						"Required parameter mongo-db not provided in configuration file " +
						"some file, section groups"));
		
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=     \t     \n" +
				"auth-url=https://auth.com\n" +
				"workspace-url=https://ws.com\n",
				new GroupsConfigurationException(
						"Required parameter mongo-db not provided in configuration file " +
						"some file, section groups"));
	}
	
	@Test
	public void configFailUserNoPwd() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"auth-url=https://auth.com\n" +
				"workspace-url=https://ws.com\n" +
				"mongo-user=user",
				new GroupsConfigurationException(
						"Must provide both mongo-user and mongo-pwd params in config file " +
						"some file section groups if MongoDB authentication is to " +
						"be used"));
		
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"auth-url=https://auth.com\n" +
				"workspace-url=https://ws.com\n" +
				"mongo-user=user\n" +
				"mongo-pwd=   \t    ",
				new GroupsConfigurationException(
						"Must provide both mongo-user and mongo-pwd params in config file " +
						"some file section groups if MongoDB authentication is to " +
						"be used"));
	}
	
	@Test
	public void configFailPwdNoUser() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"auth-url=https://auth.com\n" +
				"workspace-url=https://ws.com\n" +
				"mongo-pwd=pwd",
				new GroupsConfigurationException(
						"Must provide both mongo-user and mongo-pwd params in config file " +
						"some file section groups if MongoDB authentication is to " +
						"be used"));
		
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"auth-url=https://auth.com\n" +
				"workspace-url=https://ws.com\n" +
				"mongo-pwd=pwd\n" +
				"mongo-user=   \t    ",
				new GroupsConfigurationException(
						"Must provide both mongo-user and mongo-pwd params in config file " +
						"some file section groups if MongoDB authentication is to " +
						"be used"));
	}
	
	@Test
	public void configFailNoAuth() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"workspace-url=https://ws.com\n",
				new GroupsConfigurationException(
						"Required parameter auth-url not provided in configuration file " +
						"some file, section groups"));
		
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"auth-url=     \t     \n" +
				"workspace-url=https://ws.com\n",
				new GroupsConfigurationException(
						"Required parameter auth-url not provided in configuration file " +
						"some file, section groups"));
	}
	
	@Test
	public void configFailBadAuth() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"auth-url=   htp://foo.com\n" +
				"workspace-url=https://ws.com\n",
				new GroupsConfigurationException("Value htp://foo.com of parameter auth-url " +
						"in section groups of config file some file is not a valid URL"));
	}
	
	@Test
	public void configFailNoWS() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"auth-url=https://auth.com\n",
				new GroupsConfigurationException(
						"Required parameter workspace-url not provided in configuration file " +
						"some file, section groups"));
		
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"auth-url=https://auth.com\n" +
				"workspace-url=     \t     \n",
				new GroupsConfigurationException(
						"Required parameter workspace-url not provided in configuration file " +
						"some file, section groups"));
	}
	
	@Test
	public void configFailBadWS() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"auth-url=https://auth.com\n" +
				"workspace-url=htp://foo.com\n",
				new GroupsConfigurationException("Value htp://foo.com of parameter " +
						"workspace-url in section groups of config file some file is not a " +
						"valid URL"));
	}
	
	private InputStream toStr(final String input) {
		return new ByteArrayInputStream(input.getBytes());
	}
	
	private void failConfig(final FileOpener opener, final Exception expected) throws Throwable {
		try {
			getConfig(opener);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	private void failConfig(
			final String filename,
			final FileOpener opener,
			final Exception expected)
			throws Throwable {
		try {
			TestCommon.getenv().put(GroupsConfig.ENV_VAR_KB_DEP, filename);
			failConfig(opener, expected);
		} finally {
			TestCommon.getenv().remove(GroupsConfig.ENV_VAR_KB_DEP);
		}
	}
	
	private void failConfig1Arg(final String fileContents, final Exception expected)
			throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file"))).thenReturn(toStr(fileContents));
		
		failConfig("some file", fo, expected);
	}
	
	private void failConfig(
			final Path pathToIni,
			final FileOpener opener,
			final Exception expected)
			throws Throwable {
		try {
			getConfig(pathToIni, false, opener);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	private void failConfig3Arg(final String fileContents, final Exception expected)
			throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file"))).thenReturn(toStr(fileContents));
		
		failConfig(Paths.get("some file"), fo, expected);
	}
	
	private void failConfigBoth(final String fileContents, final Exception expected)
			throws Throwable {
		failConfig1Arg(fileContents, expected);
		failConfig3Arg(fileContents, expected);
	}
}
