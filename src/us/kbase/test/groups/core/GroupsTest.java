package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static us.kbase.test.groups.TestCommon.set;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
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
import us.kbase.groups.core.Notifications;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.UUIDGenerator;
import us.kbase.groups.core.UserHandler;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.WorkspaceHandler;
import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.ErrorType;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.NoSuchUserException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.exceptions.UnauthorizedException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.GroupRequestUserAction;
import us.kbase.groups.core.request.GroupRequestWithActions;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.storage.GroupsStorage;
import us.kbase.test.groups.TestCommon;

public class GroupsTest {
	
	private static final GroupCreationParams PARAMS;
	static {
		try {
			PARAMS = GroupCreationParams.getBuilder(
					new GroupID("i"), new GroupName("n")).build();
		} catch (Exception e) {
			throw new RuntimeException("Fix yer tests newb", e);
		}
	}

	private static TestMocks initTestMocks() throws Exception {
		final GroupsStorage storage = mock(GroupsStorage.class);
		final UserHandler uh = mock(UserHandler.class);
		final WorkspaceHandler wh = mock(WorkspaceHandler.class);
		final Notifications notis = mock(Notifications.class);
		final UUIDGenerator uuidGen = mock(UUIDGenerator.class);
		final Clock clock = mock(Clock.class);
		
		
		final Constructor<Groups> c = Groups.class.getDeclaredConstructor(
				GroupsStorage.class, UserHandler.class, WorkspaceHandler.class,
				Notifications.class, UUIDGenerator.class, Clock.class);
		c.setAccessible(true);
		final Groups instance = c.newInstance(storage, uh, wh, notis, uuidGen, clock);
		return new TestMocks(instance, storage, uh, wh, notis, uuidGen, clock);
	}
	
	public static class TestMocks {
		
		public final Groups groups;
		public final GroupsStorage storage;
		public final UserHandler userHandler;
		public final WorkspaceHandler wsHandler;
		public final Notifications notifs;
		public final UUIDGenerator uuidGen;
		public final Clock clock;
		
		private TestMocks(
				final Groups groups,
				final GroupsStorage storage,
				final UserHandler userHandler,
				final WorkspaceHandler wsHandler,
				final Notifications notifs,
				final UUIDGenerator uuidGen,
				final Clock clock) {
			this.groups = groups;
			this.storage = storage;
			this.userHandler = userHandler;
			this.wsHandler = wsHandler;
			this.notifs = notifs;
			this.uuidGen = uuidGen;
			this.clock = clock;
		}
	}
	
	@Test
	public void constructFail() throws Exception {
		final GroupsStorage s = mock(GroupsStorage.class);
		final UserHandler u = mock(UserHandler.class);
		final WorkspaceHandler w = mock(WorkspaceHandler.class);
		final Notifications n = mock(Notifications.class);
		
		failConstruct(null, u, w, n, new NullPointerException("storage"));
		failConstruct(s, null, w, n, new NullPointerException("userHandler"));
		failConstruct(s, u, null, n, new NullPointerException("wsHandler"));
		failConstruct(s, u, w, null, new NullPointerException("notifications"));
	}
	
	private void failConstruct(
			final GroupsStorage storage,
			final UserHandler userHandler,
			final WorkspaceHandler wsHandler,
			final Notifications notifications,
			final Exception expected) {
		try {
			new Groups(storage, userHandler, wsHandler, notifications);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void createGroupMinimal() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("foo"));
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(10000));
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build());
		
		final Group ret = mocks.groups.createGroup(new Token("token"), GroupCreationParams
				.getBuilder(new GroupID("bar"), new GroupName("name")).build());
		
		verify(mocks.storage).createGroup(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build());
		
		assertThat("incorrect group", ret, is(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build()));
	}
	
	@Test
	public void createGroupMaximal() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("foo"));
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(10000));
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("desc")
				.withType(GroupType.TEAM)
				.build());
		
		final Group ret = mocks.groups.createGroup(new Token("token"), GroupCreationParams
				.getBuilder(new GroupID("bar"), new GroupName("name"))
				.withDescription("desc")
				.withType(GroupType.TEAM)
				.build());
		
		verify(mocks.storage).createGroup(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("desc")
				.withType(GroupType.TEAM)
				.build());
		
		assertThat("incorrect group", ret, is(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("desc")
				.withType(GroupType.TEAM)
				.build()));
	}
	
	@Test
	public void createGroupFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;

		failCreateGroup(g, null, PARAMS, new NullPointerException("userToken"));
		failCreateGroup(g, new Token("t"), null, new NullPointerException("createParams"));
	}
	
	@Test
	public void createGroupFailInvalidToken() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenThrow(new InvalidTokenException());
		
		failCreateGroup(mocks.groups, new Token("token"), PARAMS, new InvalidTokenException());
	}
	
	@Test
	public void createGroupFailGroupExists() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("foo"));
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(10000));
		doThrow(new GroupExistsException("bar")).when(mocks.storage).createGroup(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build());
		
		failCreateGroup(mocks.groups, new Token("token"), GroupCreationParams
				.getBuilder(new GroupID("bar"), new GroupName("name")).build(),
				new GroupExistsException("bar"));
	}
	
	@Test
	public void createGroupFailMissingGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("foo"));
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(10000));
		when(mocks.storage.getGroup(new GroupID("bar")))
				.thenThrow(new NoSuchGroupException("bar"));
		
		failCreateGroup(mocks.groups, new Token("token"), GroupCreationParams
				.getBuilder(new GroupID("bar"), new GroupName("name")).build(),
				new RuntimeException(
						"Just created a group and it's already gone. Something's really broken"));
		
		verify(mocks.storage).createGroup(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build());
	}
	
	private void failCreateGroup(
			final Groups g,
			final Token t,
			final GroupCreationParams p,
			final Exception expected) {
		try {
			g.createGroup(t, p);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getGroupNoToken() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("desc")
				.withType(GroupType.TEAM)
				.withMember(new UserName("baz"))
				.build());
		
		final Group g = mocks.groups.getGroup(null, new GroupID("bar"));
		
		assertThat("incorrect group", g, is(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("desc")
				.withType(GroupType.TEAM)
				.build()));
	}
	
	@Test
	public void getGroupNonMemberToken() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
				.withMember(new UserName("baz"))
				.build());
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("whee"));
		
		final Group g = mocks.groups.getGroup(new Token("token"), new GroupID("bar"));
		
		assertThat("incorrect group", g, is(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
				.build()));
	}
	
	@Test
	public void getGroupMemberToken() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("other desc")
				.withType(GroupType.PROJECT)
				.withMember(new UserName("baz"))
				.build());
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("baz"));
		
		final Group g = mocks.groups.getGroup(new Token("token"), new GroupID("bar"));
		
		assertThat("incorrect group", g, is(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("other desc")
				.withType(GroupType.PROJECT)
				.withMember(new UserName("baz"))
				.build()));
	}
	
	@Test
	public void getGroupFailNoSuchGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroup(new GroupID("bar")))
				.thenThrow(new NoSuchGroupException("bar"));
		
		failGetGroup(mocks.groups, null, new GroupID("bar"), new NoSuchGroupException("bar"));
	}
	
	@Test
	public void getGroupFailAuthException() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build());
		when(mocks.userHandler.getUser(new Token("token")))
				.thenThrow(new AuthenticationException(
						ErrorType.AUTHENTICATION_FAILED, "oh hecky darn"));
		
		failGetGroup(mocks.groups, new Token("token"), new GroupID("bar"),
				new AuthenticationException(ErrorType.AUTHENTICATION_FAILED, "oh hecky darn"));
	}
	
	private void failGetGroup(
			final Groups g,
			final Token t,
			final GroupID i,
			final Exception expected) {
		try {
			g.getGroup(t, i);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getGroupsEmpty() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroups()).thenReturn(Collections.emptyList());
		
		assertThat("incorrect groups", mocks.groups.getGroups(), is(Collections.emptyList()));
	}
	
	@Test
	public void getGroups() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroups()).thenReturn(Arrays.asList(
				Group.getBuilder(new GroupID("id1"), new GroupName("name1"), new UserName("u1"),
						new CreateAndModTimes(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
						.build(),
				Group.getBuilder(new GroupID("id2"), new GroupName("name2"), new UserName("u2"),
						new CreateAndModTimes(Instant.ofEpochMilli(10000)))
						.withDescription("desc")
						.withType(GroupType.PROJECT)
						.withMember(new UserName("whee"))
						.build()
				));
		
		assertThat("incorrect groups", mocks.groups.getGroups(), is(Arrays.asList(
				Group.getBuilder(new GroupID("id1"), new GroupName("name1"), new UserName("u1"),
						new CreateAndModTimes(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
						.build(),
				Group.getBuilder(new GroupID("id2"), new GroupName("name2"), new UserName("u2"),
						new CreateAndModTimes(Instant.ofEpochMilli(10000)))
						.withDescription("desc")
						.withType(GroupType.PROJECT)
						.build()
				)));
	}
	
	@Test
	public void requestGroupMembership() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("foo"));
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(10000));
		when(mocks.uuidGen.randomUUID()).thenReturn(id);
		
		final GroupRequest req = mocks.groups.requestGroupMembership(
				new Token("token"), new GroupID("bar"));
		
		verify(mocks.storage).storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("bar"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(1209610000))
						.build())
				.withRequestGroupMembership()
				.build());
		
		verify(mocks.notifs).notify(
				set(new UserName("own")),
				Group.getBuilder(
						new GroupID("bar"), new GroupName("name"), new UserName("own"),
						new CreateAndModTimes(Instant.ofEpochMilli(10000)))
						.withMember(new UserName("u1"))
						.withMember(new UserName("u3"))
						.build(),
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("bar"), new UserName("foo"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(1209610000))
								.build())
						.withRequestGroupMembership()
						.build()
				);
		
		assertThat("incorrect request", req, is(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("bar"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(1209610000))
						.build())
				.withRequestGroupMembership()
				.build()
				));
	}
	
	@Test
	public void requestGroupMembershipFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		
		failRequestGroupMembership(g, null, new GroupID("i"),
				new NullPointerException("userToken"));
		failRequestGroupMembership(g, new Token("t"), null,
				new NullPointerException("groupID"));
	}
	
	@Test
	public void requestGroupMembershipFailInvalidToken() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenThrow(new InvalidTokenException());
		
		failRequestGroupMembership(mocks.groups, new Token("token"), new GroupID("i"),
				new InvalidTokenException());
	}
	
	@Test
	public void requestGroupMembershipFailNoSuchGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("foo"));
		when(mocks.storage.getGroup(new GroupID("bar")))
				.thenThrow(new NoSuchGroupException("bar"));
		
		failRequestGroupMembership(mocks.groups, new Token("token"), new GroupID("bar"),
				new NoSuchGroupException("bar"));
	}
	
	@Test
	public void requestGroupMembershipFailUserIsOwner() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		failRequestGroupMembership(mocks.groups, new Token("token"), new GroupID("bar"),
				new UserIsMemberException("User own is already a member of group bar"));
	}
	
	@Test
	public void requestGroupMembershipFailUserIsMember() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("u3"));
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		failRequestGroupMembership(mocks.groups, new Token("token"), new GroupID("bar"),
				new UserIsMemberException("User u3 is already a member of group bar"));
	}
	
	private void failRequestGroupMembership(
			final Groups g,
			final Token t,
			final GroupID i,
			final Exception expected) {
		try {
			g.requestGroupMembership(t, i);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void inviteUserToGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.userHandler.isValidUser(new UserName("foo"))).thenReturn(true);
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(10000));
		when(mocks.uuidGen.randomUUID()).thenReturn(id);
		
		final GroupRequest req = mocks.groups.inviteUserToGroup(
				new Token("token"), new GroupID("bar"), new UserName("foo"));
		
		verify(mocks.storage).storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("bar"), new UserName("own"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(1209610000))
						.build())
				.withInviteToGroup(new UserName("foo"))
				.build());
		
		verify(mocks.notifs).notify(
				set(new UserName("foo")),
				Group.getBuilder(
						new GroupID("bar"), new GroupName("name"), new UserName("own"),
						new CreateAndModTimes(Instant.ofEpochMilli(10000)))
						.withMember(new UserName("u1"))
						.withMember(new UserName("u3"))
						.build(),
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("bar"), new UserName("own"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(1209610000))
								.build())
						.withInviteToGroup(new UserName("foo"))
						.build()
				);
		
		assertThat("incorrect request", req, is(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("bar"), new UserName("own"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(1209610000))
						.build())
				.withInviteToGroup(new UserName("foo"))
				.build()
				));
	}
	
	@Test
	public void inviteUserToGroupFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		final GroupID i = new GroupID("i");
		final Token t = new Token("t");
		final UserName u = new UserName("u");
		
		failInviteUserToGroup(g, null, i, u, new NullPointerException("userToken"));
		failInviteUserToGroup(g, t, null, u, new NullPointerException("groupID"));
		failInviteUserToGroup(g, t, i, null, new NullPointerException("newMember"));
	}
	
	@Test
	public void inviteUserToGroupFailInvalidToken() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenThrow(new InvalidTokenException());
		
		failInviteUserToGroup(mocks.groups, new Token("token"), new GroupID("i"),
				new UserName("u"), new InvalidTokenException());
	}
	
	@Test
	public void inviteUserToGroupFailInvalidUser() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.userHandler.isValidUser(new UserName("foo"))).thenReturn(false);
		
		failInviteUserToGroup(mocks.groups, new Token("token"), new GroupID("i"),
				new UserName("foo"), new NoSuchUserException("foo"));
	}
	
	@Test
	public void inviteUserToGroupFailNoSuchGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.userHandler.isValidUser(new UserName("foo"))).thenReturn(true);
		when(mocks.storage.getGroup(new GroupID("bar")))
				.thenThrow(new NoSuchGroupException("bar"));
		
		failInviteUserToGroup(mocks.groups, new Token("token"), new GroupID("bar"),
				new UserName("foo"), new NoSuchGroupException("bar"));
	}
	
	@Test
	public void inviteUserToGroupFailNotAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("notown"));
		when(mocks.userHandler.isValidUser(new UserName("foo"))).thenReturn(true);
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		failInviteUserToGroup(mocks.groups, new Token("token"), new GroupID("bar"),
				new UserName("foo"), new UnauthorizedException(
						"User notown may not administrate group bar"));
	}
	
	@Test
	public void inviteUserToGroupFailIsMember() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.userHandler.isValidUser(new UserName("u1"))).thenReturn(true);
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		failInviteUserToGroup(mocks.groups, new Token("token"), new GroupID("bar"),
				new UserName("u1"), new UserIsMemberException(
						"User u1 is already a member of group bar"));
	}
	
	//TODO TEST add test for admin trying to add themselves
	
	@Test
	public void inviteUserToGroupFailRequestExists() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.userHandler.isValidUser(new UserName("foo"))).thenReturn(true);
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(10000));
		when(mocks.uuidGen.randomUUID()).thenReturn(id);
		doThrow(new RequestExistsException("someid")).when(mocks.storage).storeRequest(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("bar"), new UserName("own"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(1209610000))
								.build())
						.withInviteToGroup(new UserName("foo"))
						.build()
				);
		
		failInviteUserToGroup(mocks.groups, new Token("token"), new GroupID("bar"),
				new UserName("foo"), new RequestExistsException("someid"));
	}
	
	private void failInviteUserToGroup(
			final Groups g,
			final Token t,
			final GroupID i,
			final UserName invite,
			final Exception expected) {
		try {
			g.inviteUserToGroup(t, i, invite);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getRequestMembershipCreatorOpen() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withRequestGroupMembership()
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		final GroupRequestWithActions req = mocks.groups.getRequest(
				new Token("token"), new RequestID(id));
		
		assertThat("incorrect request", req, is(new GroupRequestWithActions(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withRequestGroupMembership()
						.build(),
				set(GroupRequestUserAction.CANCEL))));
	}
	
	@Test
	public void getRequestMembershipCreatorClosed() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withRequestGroupMembership()
				.withStatus(GroupRequestStatus.canceled())
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		final GroupRequestWithActions req = mocks.groups.getRequest(
				new Token("token"), new RequestID(id));
		
		assertThat("incorrect request", req, is(new GroupRequestWithActions(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withRequestGroupMembership()
						.withStatus(GroupRequestStatus.canceled())
						.build(),
				set())));
	}
	
	@Test
	public void getRequestMembershipAdminOpen() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withRequestGroupMembership()
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		final GroupRequestWithActions req = mocks.groups.getRequest(
				new Token("token"), new RequestID(id));
		
		assertThat("incorrect request", req, is(new GroupRequestWithActions(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withRequestGroupMembership()
						.build(),
				set(GroupRequestUserAction.ACCEPT, GroupRequestUserAction.DENY))));
	}
	
	@Test
	public void getRequestMembershipAdminClosed() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withRequestGroupMembership()
				.withStatus(GroupRequestStatus.expired())
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		final GroupRequestWithActions req = mocks.groups.getRequest(
				new Token("token"), new RequestID(id));
		
		assertThat("incorrect request", req, is(new GroupRequestWithActions(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withRequestGroupMembership()
						.withStatus(GroupRequestStatus.expired())
						.build(),
				set())));
	}
	
	@Test
	public void getRequestInviteCreatorOpen() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withInviteToGroup(new UserName("invite"))
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		final GroupRequestWithActions req = mocks.groups.getRequest(
				new Token("token"), new RequestID(id));
		
		assertThat("incorrect request", req, is(new GroupRequestWithActions(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withInviteToGroup(new UserName("invite"))
						.build(),
				set(GroupRequestUserAction.CANCEL))));
	}
	
	@Test
	public void getRequestInviteCreatorClosed() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withInviteToGroup(new UserName("invite"))
				.withStatus(GroupRequestStatus.canceled())
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		final GroupRequestWithActions req = mocks.groups.getRequest(
				new Token("token"), new RequestID(id));
		
		assertThat("incorrect request", req, is(new GroupRequestWithActions(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withInviteToGroup(new UserName("invite"))
						.withStatus(GroupRequestStatus.canceled())
						.build(),
				set())));
	}
	
	@Test
	public void getRequestInviteTargetOpen() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("invite"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withInviteToGroup(new UserName("invite"))
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		final GroupRequestWithActions req = mocks.groups.getRequest(
				new Token("token"), new RequestID(id));
		
		assertThat("incorrect request", req, is(new GroupRequestWithActions(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withInviteToGroup(new UserName("invite"))
						.build(),
				set(GroupRequestUserAction.ACCEPT, GroupRequestUserAction.DENY))));
	}
	
	@Test
	public void getRequestInviteTargetClosed() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("invite"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withInviteToGroup(new UserName("invite"))
				.withStatus(GroupRequestStatus.expired())
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		final GroupRequestWithActions req = mocks.groups.getRequest(
				new Token("token"), new RequestID(id));
		
		assertThat("incorrect request", req, is(new GroupRequestWithActions(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withInviteToGroup(new UserName("invite"))
						.withStatus(GroupRequestStatus.expired())
						.build(),
				set())));
	}
	
	@Test
	public void getRequestFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		final UUID id = UUID.randomUUID();
		
		failGetRequest(g, null, new RequestID(id), new NullPointerException("userToken"));
		failGetRequest(g, new Token("t"), null, new NullPointerException("requestID"));
	}
	
	@Test
	public void getRequestFailInvalidToken() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenThrow(new InvalidTokenException());
		
		failGetRequest(mocks.groups, new Token("token"), new RequestID(UUID.randomUUID()),
				new InvalidTokenException());
	}
	
	@Test
	public void getRequestFailNoSuchRequest() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("invite"));
		when(mocks.storage.getRequest(new RequestID(id))).thenThrow(
				new NoSuchRequestException(id.toString()));
		
		failGetRequest(mocks.groups, new Token("token"), new RequestID(id),
				new NoSuchRequestException(id.toString()));
	}
	
	@Test
	public void getRequestFailNoSuchGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("invite"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withInviteToGroup(new UserName("invite"))
				.withStatus(GroupRequestStatus.expired())
				.build());
		when(mocks.storage.getGroup(new GroupID("gid")))
				.thenThrow(new NoSuchGroupException("gid"));
		
		failGetRequest(mocks.groups, new Token("token"), new RequestID(id),
				new RuntimeException(String.format(
						"Request %s's group doesn't exist: 50000 No such group: gid", id)));
	}
	
	@Test
	public void getRequestFailMembershipCantView() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("requester"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withRequestGroupMembership()
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		failGetRequest(mocks.groups, new Token("token"), new RequestID(id),
				new UnauthorizedException("User user cannot access request " + id));
	}
	
	@Test
	public void getRequestFailInviteCantViewAsAdmin() throws Exception {
		// see notes in the getRequest subfunction code about this.
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withInviteToGroup(new UserName("invite"))
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		failGetRequest(mocks.groups, new Token("token"), new RequestID(id),
				new UnauthorizedException("User own cannot access request " + id));
	}
	
	private void failGetRequest(
			final Groups g,
			final Token t,
			final RequestID i,
			final Exception expected) {
		try {
			g.getRequest(t, i);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getRequestsForRequesterEmpty() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getRequestsByRequester(new UserName("user")))
				.thenReturn(Collections.emptyList());
		
		assertThat("incorrect requests", mocks.groups.getRequestsForRequester(new Token("token")),
				is(Collections.emptyList()));
	}
	
	@Test
	public void getRequestsForRequester() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getRequestsByRequester(new UserName("user"))).thenReturn(Arrays.asList(
				GroupRequest.getBuilder(
						new RequestID(id1), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withInviteToGroup(new UserName("invite"))
						.build(),
				GroupRequest.getBuilder(
						new RequestID(id2), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
								.withModificationTime(Instant.ofEpochMilli(25000))
								.build())
						.withRequestGroupMembership()
						.withStatus(GroupRequestStatus.accepted(new UserName("admin")))
						.build()
				));
		
		assertThat("incorrect requests", mocks.groups.getRequestsForRequester(new Token("token")),
				is(Arrays.asList(
						GroupRequest.getBuilder(
								new RequestID(id1), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
										.build())
								.withInviteToGroup(new UserName("invite"))
								.build(),
						GroupRequest.getBuilder(
								new RequestID(id2), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
										.withModificationTime(Instant.ofEpochMilli(25000))
										.build())
								.withRequestGroupMembership()
								.withStatus(GroupRequestStatus.accepted(new UserName("admin")))
								.build()
						)));
	}
	
	@Test
	public void getRequestsForRequesterFail() throws Exception {
		final TestMocks mocks = initTestMocks();
		try {
			mocks.groups.getRequestsForRequester(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("userToken"));
		}
	}
	
	@Test
	public void getRequestsForTargetEmpty() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getRequestsByTarget(new UserName("user")))
				.thenReturn(Collections.emptyList());
		
		assertThat("incorrect requests", mocks.groups.getRequestsForTarget(new Token("token")),
				is(Collections.emptyList()));
	}
	
	@Test
	public void getRequestsForTarget() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("target"));
		when(mocks.storage.getRequestsByTarget(new UserName("target"))).thenReturn(Arrays.asList(
				GroupRequest.getBuilder(
						new RequestID(id1), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withInviteToGroup(new UserName("target"))
						.build(),
				GroupRequest.getBuilder(
						new RequestID(id2), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
								.withModificationTime(Instant.ofEpochMilli(25000))
								.build())
						.withInviteToGroup(new UserName("target"))
						.withStatus(GroupRequestStatus.accepted(new UserName("target")))
						.build()
				));
		
		assertThat("incorrect requests", mocks.groups.getRequestsForTarget(new Token("token")),
				is(Arrays.asList(
						GroupRequest.getBuilder(
								new RequestID(id1), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
										.build())
								.withInviteToGroup(new UserName("target"))
								.build(),
						GroupRequest.getBuilder(
								new RequestID(id2), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
										.withModificationTime(Instant.ofEpochMilli(25000))
										.build())
								.withInviteToGroup(new UserName("target"))
								.withStatus(GroupRequestStatus.accepted(new UserName("target")))
								.build()
						)));
	}
	
	@Test
	public void getRequestsForTargetFail() throws Exception {
		final TestMocks mocks = initTestMocks();
		try {
			mocks.groups.getRequestsForTarget(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("userToken"));
		}
	}
	
	@Test
	public void getRequestsForGroupEmpty() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		when(mocks.storage.getRequestsByGroup(new GroupID("gid")))
				.thenReturn(Collections.emptyList());
		
		assertThat("incorrect requests", mocks.groups.getRequestsForGroup(
				new Token("token"), new GroupID("gid")), is(Collections.emptyList()));
	}
	
	@Test
	public void getRequestsForGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		when(mocks.storage.getRequestsByGroup(new GroupID("gid"))).thenReturn(Arrays.asList(
				GroupRequest.getBuilder(
						new RequestID(id1), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withRequestGroupMembership()
						.build(),
				GroupRequest.getBuilder(
						new RequestID(id2), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
								.withModificationTime(Instant.ofEpochMilli(25000))
								.build())
						.withRequestGroupMembership()
						.withStatus(GroupRequestStatus.accepted(new UserName("admin")))
						.build()
				));
		
		assertThat("incorrect requests", mocks.groups.getRequestsForGroup(
				new Token("token"), new GroupID("gid")),
				is(Arrays.asList(
						GroupRequest.getBuilder(
								new RequestID(id1), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
										.build())
								.withRequestGroupMembership()
								.build(),
						GroupRequest.getBuilder(
								new RequestID(id2), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
										.withModificationTime(Instant.ofEpochMilli(25000))
										.build())
								.withRequestGroupMembership()
								.withStatus(GroupRequestStatus.accepted(new UserName("admin")))
								.build()
				)));
	}
	
	@Test
	public void getRequestsForGroupFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		
		failGetRequestsForGroup(g, null, new GroupID("i"), new NullPointerException("userToken"));
		failGetRequestsForGroup(g, new Token("t"), null, new NullPointerException("groupID"));
	}
	
	@Test
	public void getRequestForGroupFailInvalidToken() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenThrow(new InvalidTokenException());
		
		failGetRequestsForGroup(mocks.groups, new Token("token"), new GroupID("i"),
				new InvalidTokenException());
	}
	
	@Test
	public void getRequestForGroupFailNoSuchGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.storage.getGroup(new GroupID("gid")))
				.thenThrow(new NoSuchGroupException("gid"));
		
		failGetRequestsForGroup(mocks.groups, new Token("token"), new GroupID("gid"),
				new NoSuchGroupException("gid"));
	}
	
	@Test
	public void getRequestForGroupFailNotAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("u1"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		failGetRequestsForGroup(mocks.groups, new Token("token"), new GroupID("gid"),
				new UnauthorizedException("User u1 cannot view requests for group gid"));
	}
	
	private void failGetRequestsForGroup(
			final Groups g,
			final Token t,
			final GroupID i,
			final Exception expected) {
		try {
			g.getRequestsForGroup(t, i);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void cancelRequest() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withInviteToGroup(new UserName("invite"))
						.build(),
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
								.withModificationTime(Instant.ofEpochMilli(15000))
								.build())
						.withInviteToGroup(new UserName("invite"))
						.withStatus(GroupRequestStatus.canceled())
						.build());
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(15000));
		
		final GroupRequest req = mocks.groups.cancelRequest(new Token("token"), new RequestID(id));
		
		verify(mocks.storage).closeRequest(new RequestID(id), GroupRequestStatus.canceled(),
				Instant.ofEpochMilli(15000));
		verify(mocks.notifs).cancel(new RequestID(id));
		
		assertThat("incorrect request", req, is(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
								.withModificationTime(Instant.ofEpochMilli(15000))
								.build())
						.withInviteToGroup(new UserName("invite"))
						.withStatus(GroupRequestStatus.canceled())
						.build()));
	}
	
	@Test
	public void cancelRequestFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		
		failCancelRequest(g, null, new RequestID(UUID.randomUUID()),
				new NullPointerException("userToken"));
		failCancelRequest(g, new Token("t"), null, new NullPointerException("requestID"));
	}
	
	@Test
	public void cancelRequestFailNoSuchRequest() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getRequest(new RequestID(id)))
				.thenThrow(new NoSuchRequestException(id.toString()));
		
		failCancelRequest(mocks.groups, new Token("token"), new RequestID(id),
				new NoSuchRequestException(id.toString()));
	}
	
	@Test
	public void cancelRequestFailUnauthed() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("otheruser"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withInviteToGroup(new UserName("invite"))
						.build());
		
		failCancelRequest(mocks.groups, new Token("token"), new RequestID(id),
				new UnauthorizedException("User otheruser may not cancel request " + id));
	}
	
	private void failCancelRequest(
			final Groups g,
			final Token t,
			final RequestID i,
			final Exception expected) {
		try {
			g.cancelRequest(t, i);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
