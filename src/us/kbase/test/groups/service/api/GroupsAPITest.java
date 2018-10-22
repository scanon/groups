package us.kbase.test.groups.service.api;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupCreationParams;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupType;
import us.kbase.groups.core.Groups;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchUserException;
import us.kbase.groups.core.exceptions.NoTokenProvidedException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.exceptions.UnauthorizedException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.service.api.GroupsAPI;
import us.kbase.groups.service.api.GroupsAPI.CreateGroupJSON;
import us.kbase.test.groups.MapBuilder;
import us.kbase.test.groups.TestCommon;

public class GroupsAPITest {

	private static final Group GROUP_MIN;
	private static final Group GROUP_MAX;
	static {
		try {
			GROUP_MIN = Group.getBuilder(
					new GroupID("id"), new GroupName("name"), new UserName("u"),
					new CreateAndModTimes(Instant.ofEpochMilli(10000)))
					.build();
			GROUP_MAX = Group.getBuilder(
					new GroupID("id2"), new GroupName("name2"), new UserName("u2"),
					new CreateAndModTimes(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
					.withDescription("desc")
					.withMember(new UserName("foo"))
					.withMember(new UserName("bar"))
					.withType(GroupType.PROJECT)
					.build();
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new RuntimeException("Fix your tests newb", e);
		}
	}
	
	private static final Map<Object, Object> GROUP_MIN_JSON = MapBuilder.newHashMap()
			.with("id", "id")
			.with("name", "name")
			.with("owner", "u")
			.with("createdate", 10000L)
			.with("moddate", 10000L)
			.with("type", "Organization")
			.with("description", null)
			.with("members", Collections.emptyList())
			.build();
	
	private static final Map<Object, Object> GROUP_MAX_JSON = MapBuilder.newHashMap()
			.with("id", "id2")
			.with("name", "name2")
			.with("owner", "u2")
			.with("createdate", 20000L)
			.with("moddate", 30000L)
			.with("type", "Project")
			.with("description", "desc")
			.with("members", Arrays.asList("bar", "foo"))
			.build();
	
	@Test
	public void getGroups() throws Exception {
		final Groups g = mock(Groups.class);
		when(g.getGroups()).thenReturn(Arrays.asList(GROUP_MAX, GROUP_MIN));
		final List<Map<String, Object>> ret = new GroupsAPI(g).getGroups("unused for now");
		
		assertThat("incorrect groups", ret, is(Arrays.asList(GROUP_MAX_JSON, GROUP_MIN_JSON)));
	}
	
	@Test
	public void createGroupMinimalNulls() throws Exception {
		createGroupMinimal(null);
	}
	
	@Test
	public void createGroupMinimalWhitespace() throws Exception {
		createGroupMinimal("    \t    ");
	}
	
	private void createGroupMinimal(final String noInput) throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.createGroup(new Token("toke"), GroupCreationParams.getBuilder(
				new GroupID("gid"), new GroupName("name")).build()))
				.thenReturn(GROUP_MAX);
		
		final Map<String, Object> ret = new GroupsAPI(g).createGroup(
				"toke", "gid", new CreateGroupJSON("name", noInput, noInput));
		
		assertThat("incorrect group", ret, is(GROUP_MAX_JSON));
	}
	
	@Test
	public void createGroupMaximal() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.createGroup(new Token("toke"), GroupCreationParams.getBuilder(
				new GroupID("gid"), new GroupName("name"))
				.withDescription("my desc")
				.withType(GroupType.TEAM)
				.build()))
				.thenReturn(GROUP_MIN);
		
		final Map<String, Object> ret = new GroupsAPI(g).createGroup(
				"toke", "gid", new CreateGroupJSON("name", "Team", "my desc"));
		
		assertThat("incorrect group", ret, is(GROUP_MIN_JSON));
	}
	
	@Test
	public void createGroupFailNullsAndWhitespace() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateGroupJSON b = new CreateGroupJSON("n", null, null);
		
		failCreateGroup(g, null, "i", b, new NoTokenProvidedException("No token provided"));
		failCreateGroup(g, "  \t  ", "i", b, new NoTokenProvidedException("No token provided"));
		failCreateGroup(g, "t", null, b, new MissingParameterException("group id"));
		failCreateGroup(g, "t", "   \t  ", b, new MissingParameterException("group id"));
		failCreateGroup(g, "t", "i", null, new MissingParameterException("Missing JSON body"));
		failCreateGroup(g, "t", "i", new CreateGroupJSON(null, null, null),
				new MissingParameterException("group name"));
		failCreateGroup(g, "t", "i", new CreateGroupJSON("   \t    ", null, null),
				new MissingParameterException("group name"));
		
	}
	
	@Test
	public void createGroupFailExtraProperties() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateGroupJSON b = new CreateGroupJSON("n", null, null);
		b.setAdditionalProperties("foo", "bar");

		failCreateGroup(g, "t", "i", b, new IllegalParameterException(
				"Unexpected parameters in request: foo"));
	}
	
	@Test
	public void createGroupFailBadType() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateGroupJSON b = new CreateGroupJSON("n", "Teem", null);

		failCreateGroup(g, "t", "i", b, new IllegalParameterException(
				"Invalid group type: Teem"));
	}
	
	@Test
	public void createGroupFailGroupExists() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateGroupJSON b = new CreateGroupJSON("n", null, null);

		when(g.createGroup(new Token("t"), GroupCreationParams.getBuilder(
				new GroupID("i"), new GroupName("n")).build()))
				.thenThrow(new GroupExistsException("i"));
				
		failCreateGroup(g, "t", "i", b, new GroupExistsException("i"));
	}
	
	private void failCreateGroup(
			final Groups g,
			final String token,
			final String groupID,
			final CreateGroupJSON body,
			final Exception expected) {
		try {
			new GroupsAPI(g).createGroup(token, groupID, body);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getGroupNoTokenNull() throws Exception {
		getGroup(null, null);
	}
	
	@Test
	public void getGroupNoTokenWhitespace() throws Exception {
		getGroup("   \t    ", null);
	}
	
	@Test
	public void getGroupWithToken() throws Exception {
		getGroup("foo", new Token("foo"));
	}
	
	private void getGroup(final String token, final Token expected) throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getGroup(expected, new GroupID("id"))).thenReturn(GROUP_MAX);
		
		final Map<String, Object> ret = new GroupsAPI(g).getGroup(token, "id");
		
		assertThat("incorrect group", ret, is(GROUP_MAX_JSON));
	}
	
	@Test
	public void getGroupFailMissingID() {
		final Groups g = mock(Groups.class);
		
		failGetGroup(g, null, null, new MissingParameterException("group id"));
		failGetGroup(g, null, "   \t   ", new MissingParameterException("group id"));
	}
	
	@Test
	public void getGroupFailInvalidToken() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getGroup(new Token("t"), new GroupID("i"))).thenThrow(new InvalidTokenException());
		
		failGetGroup(g, "t", "i", new InvalidTokenException());
	}
	
	private void failGetGroup(
			final Groups g,
			final String token,
			final String groupid,
			final Exception expected) {
		try {
			new GroupsAPI(g).getGroup(token, groupid);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void requestGroupMembership() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.requestGroupMembership(new Token("t"), new GroupID("gid")))
				.thenReturn(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("foo"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(30000))
								.build())
						.withRequestGroupMembership()
						.build());
		
		final Map<String, Object> ret = new GroupsAPI(g).requestGroupMembership("t", "gid");
		
		assertThat("incorrect request", ret, is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "foo")
				.with("type", "Request group membership")
				.with("targetuser", null)
				.with("status", "Open")
				.with("createdate", 10000L)
				.with("moddate", 10000L)
				.with("expiredate", 30000L)
				.build()));
	}
	
	@Test
	public void requestGroupMembershipFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failRequestGroupMembership(g, null, "i",
				new NoTokenProvidedException("No token provided"));
		failRequestGroupMembership(g, "    \t    ", "i",
				new NoTokenProvidedException("No token provided"));
		failRequestGroupMembership(g, "t", null,
				new MissingParameterException("group id"));
		failRequestGroupMembership(g, "t", "   \t   ",
				new MissingParameterException("group id"));
	}
	
	@Test
	public void requestGroupMembershipFailUserIsMember() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.requestGroupMembership(new Token("t"), new GroupID("i"))).thenThrow(
				new UserIsMemberException("foo"));
		
		failRequestGroupMembership(g, "t", "i", new UserIsMemberException("foo"));
	}
	
	private void failRequestGroupMembership(
			final Groups g,
			final String token,
			final String groupid,
			final Exception expected) {
		try {
			new GroupsAPI(g).requestGroupMembership(token, groupid);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void inviteMember() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.inviteUserToGroup(new Token("t"), new GroupID("gid"), new UserName("bar")))
				.thenReturn(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("foo"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(30000))
								.build())
						.withInviteToGroup(new UserName("bar"))
						.build());
		
		final Map<String, Object> ret = new GroupsAPI(g).inviteMember("t", "gid", "bar");
		
		assertThat("incorrect request", ret, is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "foo")
				.with("type", "Invite to group")
				.with("targetuser", "bar")
				.with("status", "Open")
				.with("createdate", 10000L)
				.with("moddate", 10000L)
				.with("expiredate", 30000L)
				.build()));
	}
	
	@Test
	public void inviteMemberFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failInviteMember(g, null, "i", "u",
				new NoTokenProvidedException("No token provided"));
		failInviteMember(g, "    \t    ", "i", "u",
				new NoTokenProvidedException("No token provided"));
		failInviteMember(g, "t", null, "u",
				new MissingParameterException("group id"));
		failInviteMember(g, "t", "   \t   ", "u",
				new MissingParameterException("group id"));
		failInviteMember(g, "t", "i", null,
				new MissingParameterException("user name"));
		failInviteMember(g, "t", "i", "  \t    ",
				new MissingParameterException("user name"));
	}
	
	@Test
	public void inviteMemberRequestExists() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.inviteUserToGroup(new Token("t"), new GroupID("i"), new UserName("foo"))).thenThrow(
				new RequestExistsException("reeeeeeee"));
		
		failInviteMember(g, "t", "i", "foo", new RequestExistsException("reeeeeeee"));
	}
	
	
	private void failInviteMember(
			final Groups g,
			final String token,
			final String groupid,
			final String user,
			final Exception expected) {
		try {
			new GroupsAPI(g).inviteMember(token, groupid, user);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getRequestsForGroup() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		
		
		when(g.getRequestsForGroupID(new Token("t"), new GroupID("id"))).thenReturn(Arrays.asList(
				GroupRequest.getBuilder(new RequestID(id1), new GroupID("id"), new UserName("foo"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(40000))
								.build())
						.build(),
				GroupRequest.getBuilder(new RequestID(id2), new GroupID("id"), new UserName("bar"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
								.withModificationTime(Instant.ofEpochMilli(25000))
								.build())
						.withInviteToGroup(new UserName("baz"))
						.withStatus(GroupRequestStatus.canceled())
						.build()
				));
		
		final List<Map<String, Object>> ret = new GroupsAPI(g).getRequestsForGroup("t", "id");
		
		assertThat("incorrect requests", ret, is(Arrays.asList(
				MapBuilder.newHashMap()
						.with("id", id1.toString())
						.with("groupid", "id")
						.with("requester", "foo")
						.with("type", "Request group membership")
						.with("targetuser", null)
						.with("status", "Open")
						.with("createdate", 10000L)
						.with("moddate", 10000L)
						.with("expiredate", 40000L)
						.build(),
				MapBuilder.newHashMap()
						.with("id", id2.toString())
						.with("groupid", "id")
						.with("requester", "bar")
						.with("type", "Invite to group")
						.with("targetuser", "baz")
						.with("status", "Canceled")
						.with("createdate", 20000L)
						.with("moddate", 25000L)
						.with("expiredate", 30000L)
						.build()
				)));
	}
	
	@Test
	public void getRequestsForGroupFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failGetRequestsForGroup(g, null, "i",
				new NoTokenProvidedException("No token provided"));
		failGetRequestsForGroup(g, "    \t    ", "i",
				new NoTokenProvidedException("No token provided"));
		failGetRequestsForGroup(g, "t", null,
				new MissingParameterException("group id"));
		failGetRequestsForGroup(g, "t", "   \t   ",
				new MissingParameterException("group id"));
	}

	@Test
	public void getRequestsForGroupFailUnauthorized() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getRequestsForGroupID(new Token("t"), new GroupID("i"))).thenThrow(
				new UnauthorizedException("yay"));
		
		failGetRequestsForGroup(g, "t", "i", new UnauthorizedException("yay"));
	}
	
	private void failGetRequestsForGroup(
			final Groups g,
			final String token,
			final String groupid,
			final Exception expected) {
		try {
			new GroupsAPI(g).getRequestsForGroup(token, groupid);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void removeMember() throws Exception {
		final Groups g = mock(Groups.class);
		
		new GroupsAPI(g).removeMember("t", "i", "foo");
		
		verify(g).removeMember(new Token("t"), new GroupID("i"), new UserName("foo"));
	}
	
	@Test
	public void removeMemberFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failRemoveMember(g, null, "i", "u",
				new NoTokenProvidedException("No token provided"));
		failRemoveMember(g, "    \t    ", "i", "u",
				new NoTokenProvidedException("No token provided"));
		failRemoveMember(g, "t", null, "u",
				new MissingParameterException("group id"));
		failRemoveMember(g, "t", "   \t   ", "u",
				new MissingParameterException("group id"));
		failRemoveMember(g, "t", "i", null,
				new MissingParameterException("user name"));
		failRemoveMember(g, "t", "i", "  \t    ",
				new MissingParameterException("user name"));
	}
	
	@Test
	public void removeMemberNoSuchUser() throws Exception {
		final Groups g = mock(Groups.class);
		
		doThrow(new NoSuchUserException("foo")).when(g)
				.removeMember(new Token("t"), new GroupID("i"), new UserName("foo"));
		
		failRemoveMember(g, "t", "i", "foo", new NoSuchUserException("foo"));
	}
	
	
	private void failRemoveMember(
			final Groups g,
			final String token,
			final String groupid,
			final String user,
			final Exception expected) {
		try {
			new GroupsAPI(g).removeMember(token, groupid, user);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}