package us.kbase.groups.service.api;

import static us.kbase.groups.service.api.APIConstants.HEADER_TOKEN;
import static us.kbase.groups.service.api.APICommon.getToken;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupCreationParams;
import us.kbase.groups.core.GroupCreationParams.Builder;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupType;
import us.kbase.groups.core.Groups;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchUserException;
import us.kbase.groups.core.exceptions.NoTokenProvidedException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.exceptions.UnauthorizedException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

@Path(ServicePaths.GROUP)
public class GroupsAPI {

	// TODO JAVADOC / swagger
	// TODO NOW add endpoint for getting group types
	
	private final Groups groups;
	
	// normally instantiated by Jersey
	@Inject
	public GroupsAPI(final Groups groups) {
		this.groups = groups;
	}
	
	// this assumes there are a relatively small number of groups. If that proves false,
	// will need to filter somehow. Remember, deep paging was invented by Satan himself.
	// might want to remove description, members, and other fields
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, Object>> getGroups(
			@HeaderParam(HEADER_TOKEN) final String token)
			throws GroupsStorageException {
		return groups.getGroups().stream().map(g -> toGroupJSON(g)).collect(Collectors.toList());
	}
	
	public static class CreateGroupJSON extends IncomingJSON {
		
		private final String groupName;
		private final String type;
		private final String description;
		
		@JsonCreator
		public CreateGroupJSON(
				@JsonProperty(Fields.GROUP_NAME) final String name,
				@JsonProperty(Fields.GROUP_TYPE) final String type,
				@JsonProperty(Fields.GROUP_DESCRIPTION) final String desc) {
			this.groupName = name;
			this.type = type;
			this.description = desc;
		}
	}
	
	@PUT
	@Path(ServicePaths.GROUP_ID)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> createGroup(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			final CreateGroupJSON create)
			throws MissingParameterException, IllegalParameterException, InvalidTokenException,
				AuthenticationException, GroupExistsException, GroupsStorageException {
		checkIncomingJson(create);
		final Builder gbuilder = GroupCreationParams.getBuilder(
				new GroupID(groupID), new GroupName(create.groupName))
				.withDescription(create.description);
		if (!isNullOrEmpty(create.type)) {
			gbuilder.withType(GroupType.fromRepresentation(create.type));
		}
		return toGroupJSON(groups.createGroup(getToken(token, true), gbuilder.build()));
	}
	
	@GET
	@Path(ServicePaths.GROUP_ID)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> getGroup(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID)
			throws InvalidTokenException, NoSuchGroupException, NoTokenProvidedException,
				AuthenticationException, MissingParameterException, IllegalParameterException,
				GroupsStorageException {
		return toGroupJSON(groups.getGroup(getToken(token, false), new GroupID(groupID)));
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ServicePaths.GROUP_REQUEST_MEMBERSHIP)
	public Map<String, Object> requestGroupMembership(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID)
			throws InvalidTokenException, NoSuchGroupException, AuthenticationException,
				UserIsMemberException, MissingParameterException, IllegalParameterException,
				GroupsStorageException, RequestExistsException {
		return APICommon.toGroupRequestJSON(groups.requestGroupMembership(
				getToken(token, true), new GroupID(groupID)));
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ServicePaths.GROUP_USER_ID)
	public Map<String, Object> inviteMember(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			@PathParam(Fields.GROUP_MEMBER) final String member)
			throws InvalidTokenException, NoSuchGroupException, NoSuchUserException,
				AuthenticationException, UnauthorizedException, UserIsMemberException,
				RequestExistsException, MissingParameterException, IllegalParameterException,
				GroupsStorageException {
		return APICommon.toGroupRequestJSON(groups.inviteUserToGroup(
				getToken(token, true), new GroupID(groupID), new UserName(member)));
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ServicePaths.GROUP_REQUESTS)
	public List<Map<String, Object>> getRequestsForGroup(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID)
			throws InvalidTokenException, NoSuchGroupException, UnauthorizedException,
				AuthenticationException, MissingParameterException, IllegalParameterException,
				GroupsStorageException {
		return APICommon.toGroupRequestJSON(groups.getRequestsForGroupID(
				getToken(token, true), new GroupID(groupID)));
	}
	
	@DELETE
	@Path(ServicePaths.GROUP_USER_ID)
	public void removeMember(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			@PathParam(Fields.GROUP_MEMBER) final String member)
			throws NoSuchGroupException, InvalidTokenException, NoSuchUserException,
				AuthenticationException, UnauthorizedException, MissingParameterException,
				IllegalParameterException, GroupsStorageException {
		groups.removeMember(getToken(token, true), new GroupID(groupID), new UserName(member));
	}

	private Map<String, Object> toGroupJSON(final Group g) {
		final Map<String, Object> ret = new HashMap<>();
		ret.put(Fields.GROUP_ID, g.getGroupID().getName());
		ret.put(Fields.GROUP_NAME, g.getGroupName().getName());
		ret.put(Fields.GROUP_OWNER, g.getOwner().getName());
		ret.put(Fields.GROUP_TYPE, g.getType().getRepresentation());
		ret.put(Fields.GROUP_CREATION, g.getCreationDate().toEpochMilli());
		ret.put(Fields.GROUP_MODIFICATION, g.getModificationDate().toEpochMilli());
		ret.put(Fields.GROUP_DESCRIPTION, g.getDescription().orNull());
		ret.put(Fields.GROUP_MEMBERS, new TreeSet<>(g.getMembers()).stream().map(n -> n.getName())
				.collect(Collectors.toList()));
		return ret;
	}
	
	private void checkIncomingJson(final IncomingJSON json)
			throws IllegalParameterException, MissingParameterException {
		if (json == null) {
			throw new MissingParameterException("Missing JSON body");
		}
		json.exceptOnAdditionalProperties();
	}
	
}
