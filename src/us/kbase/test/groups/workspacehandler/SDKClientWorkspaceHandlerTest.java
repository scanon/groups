package us.kbase.test.groups.workspacehandler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static us.kbase.test.groups.TestCommon.set;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentMatcher;

import com.google.common.collect.ImmutableMap;

import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.Tuple9;
import us.kbase.common.service.UObject;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.NoSuchWorkspaceException;
import us.kbase.groups.core.exceptions.WorkspaceHandlerException;
import us.kbase.groups.core.workspace.WorkspaceID;
import us.kbase.groups.core.workspace.WorkspaceIDSet;
import us.kbase.groups.core.workspace.WorkspaceInfoSet;
import us.kbase.groups.core.workspace.WorkspaceInformation;
import us.kbase.groups.workspacehandler.SDKClientWorkspaceHandler;
import us.kbase.test.groups.TestCommon;
import us.kbase.workspace.WorkspaceClient;

public class SDKClientWorkspaceHandlerTest {
	
	private static boolean DEBUG = false;
	
	private static class UObjectArgumentMatcher implements ArgumentMatcher<UObject> {

		private final Map<String, Object> adminPackage;

		public UObjectArgumentMatcher(final Map<String, Object> adminPackage) {
			this.adminPackage = adminPackage;
		}
		
		@Override
		public boolean matches(final UObject uo) {
			final Object obj = uo.asInstance();
			final boolean match = adminPackage.equals(obj);
			if (DEBUG && !match) {
				System.out.println(String.format("UObject match failed. Expected:\n%s\nGot:\n%s",
						adminPackage, obj));
			}
			return match;
		}
	}
	
	@Test
	public void constructFailNull() throws Exception {
		failConstruct(null, new NullPointerException("client"));
	}
	
	@Test
	public void constructFailVersion() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.7.999999");
		
		failConstruct(c, new WorkspaceHandlerException(
				"Workspace version 0.8.0 or greater is required"));
	}
	
	@Test
	public void constructFailJsonClientException() throws Exception {
		constructFail(new JsonClientException("User foo is not an admin"));
	}
	
	@Test
	public void constructFailIOException() throws Exception {
		constructFail(new IOException("blearg"));
	}


	private void constructFail(final Exception exception) throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.8.0");
		when(c.getURL()).thenReturn(new URL("http://bar.com"));
		
		when(c.administer(argThat(new UObjectArgumentMatcher(
				ImmutableMap.of("command", "listAdmins")))))
				.thenThrow(exception);
		
		failConstruct(c, new WorkspaceHandlerException(
				"Error contacting workspace at http://bar.com"));
	}
	
	private void failConstruct(final WorkspaceClient c, final Exception expected) {
		try {
			new SDKClientWorkspaceHandler(c);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void isAdminAdmin() throws Exception {
		isAdmin("user1", true);
	}
	
	@Test
	public void isAdminWrite() throws Exception {
		isAdmin("user2", false);
	}
	
	@Test
	public void isAdminNoUser() throws Exception {
		isAdmin("user3", false);
	}

	private void isAdmin(final String user, final boolean expected) throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.8.0");
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		when(c.administer(argThat(getPermissionsCommandMatcher(24))))
				.thenReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
						ImmutableMap.of("user1", "a", "user2", "w")))));
		
		assertThat("incorrect admin", h.isAdministrator(
				new WorkspaceID(24), new UserName(user)), is(expected));
	}

	private UObjectArgumentMatcher getPermissionsCommandMatcher(final int wsid) {
		return new UObjectArgumentMatcher(ImmutableMap.of(
				"command", "getPermissionsMass",
				"params", ImmutableMap.of("workspaces",
						Arrays.asList(ImmutableMap.of("id", wsid)))));
	}
	
	@Test
	public void isAdminFailNulls() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.8.0");
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		failIsAdmin(h, null, new UserName("u"), new NullPointerException("wsid"));
		failIsAdmin(h, new WorkspaceID(1), null, new NullPointerException("user"));
	}
	
	@Test
	public void isAdminFailDeletedWS() throws Exception {
		isAdminFail(new ServerException("Workspace 24 is deleted", -1, "n"),
				new NoSuchWorkspaceException("24"));
	}
	
	@Test
	public void isAdminFailMissingWS() throws Exception {
		isAdminFail(new ServerException("No workspace with id 24 exists", -1, "n"),
				new NoSuchWorkspaceException("24"));
	}
	
	@Test
	public void isAdminFailOtherServerException() throws Exception {
		isAdminFail(new ServerException("You pootied real bad I can smell it", -1, "n"),
				new WorkspaceHandlerException("Error contacting workspace at http://foo.com"));
	}
	
	@Test
	public void isAdminFailJsonClientException() throws Exception {
		isAdminFail(new JsonClientException("You pootied real bad I can smell it"),
				new WorkspaceHandlerException("Error contacting workspace at http://foo.com"));
	}
	
	@Test
	public void isAdminFailIOException() throws Exception {
		isAdminFail(new IOException("You pootied real bad I can smell it"),
				new WorkspaceHandlerException("Error contacting workspace at http://foo.com"));
	}
	
	@Test
	public void isAdminFailIllegalStateException() throws Exception {
		isAdminFail(new IllegalStateException("You pootied real bad I can smell it"),
				new WorkspaceHandlerException("Error contacting workspace at http://foo.com"));
	}

	private void isAdminFail(final Exception exception, final Exception expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.8.0");
		when(c.getURL()).thenReturn(new URL("http://foo.com"));
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		when(c.administer(argThat(getPermissionsCommandMatcher(24)))).thenThrow(exception);
		
		failIsAdmin(h, new WorkspaceID(24), new UserName("user"), expected);
	}
	
	private void failIsAdmin(
			final SDKClientWorkspaceHandler h,
			final WorkspaceID id,
			final UserName user,
			final Exception expected) {
		try {
			h.isAdministrator(id, user);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getWorkspaceInformationNoWS() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.8.0");
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		final WorkspaceInfoSet wi = h.getWorkspaceInformation(
				new UserName("u"),
				WorkspaceIDSet.fromInts(set()),
				false);
		
		assertThat("incorrect wsi", wi, is(WorkspaceInfoSet.getBuilder(new UserName("u"))
				.build()));
	}

	@Test
	public void getWorkspaceInformationFull() throws Exception {
		getWorkspaceInformation(
				new UserName("user1"),
				false,
				WorkspaceInfoSet.getBuilder(new UserName("user1"))
						.withNonexistentWorkspace(8)
						.withNonexistentWorkspace(9)
						.withNonexistentWorkspace(20)
						.withNonexistentWorkspace(21)
						.withWorkspaceInformation(WorkspaceInformation.getBuilder(5, "name5")
								.withNullableNarrativeName("narr_name").build(),
								true)
						.withWorkspaceInformation(WorkspaceInformation.getBuilder(7, "name7")
								.build(),
								false)
						.withWorkspaceInformation(WorkspaceInformation.getBuilder(10, "name10")
								.withIsPublic(true).build(),
								false)
						.withWorkspaceInformation(WorkspaceInformation.getBuilder(11, "name11")
								.withIsPublic(true).build(),
								false)
						.build());
	}
	
	@Test
	public void getWorkspaceInformationAdministratedOnly() throws Exception {
		getWorkspaceInformation(
				new UserName("user1"),
				true,
				WorkspaceInfoSet.getBuilder(new UserName("user1"))
						.withNonexistentWorkspace(9)
						.withNonexistentWorkspace(20)
						.withNonexistentWorkspace(21)
						.withWorkspaceInformation(WorkspaceInformation.getBuilder(5, "name5")
								.withNullableNarrativeName("narr_name").build(),
								true)
						.withWorkspaceInformation(WorkspaceInformation.getBuilder(10, "name10")
								.withIsPublic(true).build(),
								false)
						.withWorkspaceInformation(WorkspaceInformation.getBuilder(11, "name11")
								.withIsPublic(true).build(),
								false)
						.build());
	}
	
	@Test
	public void getWorkspaceInformationAnonUser() throws Exception {
		getWorkspaceInformationAnonUser(false);
	}
	
	@Test
	public void getWorkspaceInformationAnonUserAdministratedWSOnly() throws Exception {
		getWorkspaceInformationAnonUser(true);
	}

	private void getWorkspaceInformationAnonUser(final boolean administratedWorkspacesOnly)
			throws Exception {
		getWorkspaceInformation(
				null,
				administratedWorkspacesOnly,
				WorkspaceInfoSet.getBuilder(null)
						.withNonexistentWorkspace(9)
						.withNonexistentWorkspace(20)
						.withNonexistentWorkspace(21)
						.withWorkspaceInformation(WorkspaceInformation.getBuilder(10, "name10")
								.withIsPublic(true).build(),
								false)
						.withWorkspaceInformation(WorkspaceInformation.getBuilder(11, "name11")
								.withIsPublic(true).build(),
								false)
						.build());
	}

	private void getWorkspaceInformation(
			final UserName user,
			final boolean administratedWorkspacesOnly,
			final WorkspaceInfoSet expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.8.0");
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "a", "user2", "w")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(5)));
		
		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "w", "user2", "w")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(7)));

		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "w", "user2", "w")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(8)));

		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "r", "user2", "w", "*", "r")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(9)));
		
		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "r", "user2", "w", "*", "r")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(10)));
		
		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "r", "user2", "w", "*", "r")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(11)));
		
		doThrow(new ServerException("Workspace 20 is deleted", -1, "n"))
				.when(c).administer(argThat(getPermissionsCommandMatcher(20)));
		doThrow(new ServerException("No workspace with id 21 exists", -1, "n"))
				.when(c).administer(argThat(getPermissionsCommandMatcher(21)));

		doReturn(getWorkspaceInfoResponse(5, "name5", false, ImmutableMap.of(
				"is_temporary", "false",
				"narrative_nice_name", "narr_name")))
				.when(c).administer(argThat(getWSInfoCommandMatcher(5)));

		doReturn(getWorkspaceInfoResponse(7, "name7", false, ImmutableMap.of(
				"is_temporary", "true",
				"narrative_nice_name", "narr_name2")))
				.when(c).administer(argThat(getWSInfoCommandMatcher(7)));
		
		doThrow(new ServerException("Workspace 8 is deleted", -1, "n"))
				.when(c).administer(argThat(getWSInfoCommandMatcher(8)));
		doThrow(new ServerException("No workspace with id 9 exists", -1, "n"))
				.when(c).administer(argThat(getWSInfoCommandMatcher(9)));
		
		doReturn(getWorkspaceInfoResponse(10, "name10", true, ImmutableMap.of(
				"is_temporary", "false")))
				.when(c).administer(argThat(getWSInfoCommandMatcher(10)));

		doReturn(getWorkspaceInfoResponse(11, "name11", true, Collections.emptyMap()))
				.when(c).administer(argThat(getWSInfoCommandMatcher(11)));
		
		final WorkspaceInfoSet wi = h.getWorkspaceInformation(
				user,
				WorkspaceIDSet.fromInts(set(5, 7, 8, 9, 10, 11, 20, 21)),
				administratedWorkspacesOnly);
		
		assertThat("incorrect wsinfo", wi, is(expected));
	}

	private UObjectArgumentMatcher getWSInfoCommandMatcher(final int wsid) {
		return new UObjectArgumentMatcher(ImmutableMap.of(
				"command", "getWorkspaceInfo",
				"params", ImmutableMap.of("id", wsid)));
	}
	
	private UObject getWorkspaceInfoResponse(
			final int id,
			final String name,
			final boolean isPublic,
			final Map<String, String> meta) {
		return new UObject(new Tuple9<Long, String, String, String, Long, String, String, String,
				Map<String, String>>()
				.withE1((long) id)
				.withE2(name)
				.withE7(isPublic ? "r" : "n")
				.withE9(meta));
		// other fields are currently unused in the handler
	}
	
	@Test
	public void getWorkspaceInfoFailNull() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.8.0");
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		failGetWorkspaceInfo(h, null, new NullPointerException("ids"));
	}
	
	@Test
	public void getWorkspaceInformationFailPermsOtherServerException() throws Exception {
		failGetWorkspaceInfoOnPermissionsCall(
				new ServerException("You pootied real bad I can smell it", -1, "n"),
				new WorkspaceHandlerException("Error contacting workspace at http://baz.com"));
	}
	
	@Test
	public void getWorkspaceInformationFailPermsJsonClientException() throws Exception {
		failGetWorkspaceInfoOnPermissionsCall(
				new JsonClientException("You pootied real bad I can smell it"),
				new WorkspaceHandlerException("Error contacting workspace at http://baz.com"));
	}
	
	@Test
	public void getWorkspaceInformationFailPermsIOException() throws Exception {
		failGetWorkspaceInfoOnPermissionsCall(
				new IOException("You pootied real bad I can smell it"),
				new WorkspaceHandlerException("Error contacting workspace at http://baz.com"));
	}
	
	@Test
	public void getWorkspaceInformationFailPermsIllegalStateException() throws Exception {
		failGetWorkspaceInfoOnPermissionsCall(
				new IllegalStateException("You pootied real bad I can smell it"),
				new WorkspaceHandlerException("Error contacting workspace at http://baz.com"));
	}
	
	private void failGetWorkspaceInfoOnPermissionsCall(
			final Exception thrown,
			final Exception expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.8.0");
		when(c.getURL()).thenReturn(new URL("http://baz.com"));
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		when(c.administer(argThat(getPermissionsCommandMatcher(24)))).thenThrow(thrown);
		
		failGetWorkspaceInfo(h, WorkspaceIDSet.fromInts(set(24)), expected);
	}
	
	@Test
	public void getWorkspaceInformationFailGetWSOtherServerException() throws Exception {
		failGetWorkspaceInfoOnGetWSCall(
				new ServerException("You pootied real bad I can smell it", -1, "n"),
				new WorkspaceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	@Test
	public void getWorkspaceInformationFailGetWSJsonClientException() throws Exception {
		failGetWorkspaceInfoOnGetWSCall(
				new JsonClientException("You pootied real bad I can smell it"),
				new WorkspaceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	@Test
	public void getWorkspaceInformationFailGetWSIOException() throws Exception {
		failGetWorkspaceInfoOnGetWSCall(
				new IOException("You pootied real bad I can smell it"),
				new WorkspaceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	@Test
	public void getWorkspaceInformationFailGetWSIllegalStateException() throws Exception {
		failGetWorkspaceInfoOnGetWSCall(
				new IllegalStateException("You pootied real bad I can smell it"),
				new WorkspaceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	private void failGetWorkspaceInfoOnGetWSCall(
			final Exception thrown,
			final Exception expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.8.0");
		when(c.getURL()).thenReturn(new URL("http://bat.com"));
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "a", "*", "r")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(24)));

		doThrow(thrown).when(c).administer(argThat(getWSInfoCommandMatcher(24)));
		
		failGetWorkspaceInfo(h, WorkspaceIDSet.fromInts(set(24)), expected);
	}
	
	private void failGetWorkspaceInfo(
			final SDKClientWorkspaceHandler h,
			final WorkspaceIDSet ids,
			final Exception expected) {
		try {
			// no way to cause a fail via user or adminOnly param
			h.getWorkspaceInformation(null, ids, false);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getAdministratedWorkspaces() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.8.0");
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		when(c.administer(argThat(getListWorkspaceIDsCommandMatcher("user"))))
				.thenReturn(new UObject(ImmutableMap.of("workspaces", Arrays.asList(
						4L, 6L, 8L, 42L, 86L))));

		assertThat("incorrect ids", h.getAdministratedWorkspaces(new UserName("user")),
				is(WorkspaceIDSet.fromInts(set(4, 6, 8, 42, 86))));
	}
	
	@Test
	public void getAdministratedWorkspacesFailNull() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.8.0");
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		failGetAdministratedWorkspaces(h, null, new NullPointerException("user"));
	}
	
	@Test
	public void getAdministratedWorkspacesFailIOException() throws Exception {
		getAdministratedWorkspacesFail(new IOException("oh dookybutts"),
				new WorkspaceHandlerException(
						"Error contacting workspace at http://nudewombats.com"));
	}

	@Test
	public void getAdministratedWorkspacesFailJSONClientException() throws Exception {
		getAdministratedWorkspacesFail(new JsonClientException("oh dookybutts"),
				new WorkspaceHandlerException(
						"Error contacting workspace at http://nudewombats.com"));
	}

	private void getAdministratedWorkspacesFail(final Exception thrown, final Exception expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.8.0");
		when(c.getURL()).thenReturn(new URL("http://nudewombats.com"));
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		when(c.administer(argThat(getListWorkspaceIDsCommandMatcher("user"))))
				.thenThrow(thrown);

		failGetAdministratedWorkspaces(h, new UserName("user"), expected);
	}
	
	private void failGetAdministratedWorkspaces(
			final SDKClientWorkspaceHandler h,
			final UserName user,
			final Exception expected) {
		try {
			h.getAdministratedWorkspaces(user);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

	private UObjectArgumentMatcher getListWorkspaceIDsCommandMatcher(final String user) {
		return new UObjectArgumentMatcher(ImmutableMap.of(
				"command", "listWorkspaceIDs",
				"user", user,
				"params", ImmutableMap.of("perm", "a")));
	}
	
}
