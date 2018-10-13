package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Optional;

public class Group {
	
	// TODO JAVADOC
	// TODO TESTS
	
	private final GroupID groupID;
	private final GroupName groupName;
	private final UserName owner;
	private final GroupType type;
	private final Instant creationDate;
	private final Instant modificationDate;
	private final Optional<String> description;
	
	private Group(
			final GroupID groupID,
			final GroupName groupName,
			final UserName owner,
			final GroupType type,
			final CreateAndModTimes times,
			final Optional<String> description) {
		this.groupID = groupID;
		this.groupName = groupName;
		this.owner = owner;
		this.type = type;
		this.creationDate = times.getCreationTime();
		this.modificationDate = times.getModificationTime();
		this.description = description;
	}

	public GroupID getGroupID() {
		return groupID;
	}

	public GroupName getGroupName() {
		return groupName;
	}

	public UserName getOwner() {
		return owner;
	}

	public GroupType getType() {
		return type;
	}

	public Instant getCreationDate() {
		return creationDate;
	}

	public Instant getModificationDate() {
		return modificationDate;
	}

	public Optional<String> getDescription() {
		return description;
	}
	
	public boolean isAdministrator(final UserName user) {
		checkNotNull("user", user);
		//TODO NOW deal with group admins
		return owner.equals(user);
	}
	
	public Set<UserName> getAdministrators() {
		//TODO NOW deal with group admins
		return new HashSet<>(Arrays.asList(owner));
	}
	
	public boolean isMember(final UserName user) {
		checkNotNull("user", user);
		// TODO NOW check admins and users list
		return owner.equals(user);
	}

	@Override
	public String toString() {
		StringBuilder builder2 = new StringBuilder();
		builder2.append("Group [groupID=");
		builder2.append(groupID);
		builder2.append(", groupName=");
		builder2.append(groupName);
		builder2.append(", owner=");
		builder2.append(owner);
		builder2.append(", type=");
		builder2.append(type);
		builder2.append(", creationDate=");
		builder2.append(creationDate);
		builder2.append(", modificationDate=");
		builder2.append(modificationDate);
		builder2.append(", description=");
		builder2.append(description);
		builder2.append("]");
		return builder2.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((groupID == null) ? 0 : groupID.hashCode());
		result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + ((modificationDate == null) ? 0 : modificationDate.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Group other = (Group) obj;
		if (creationDate == null) {
			if (other.creationDate != null) {
				return false;
			}
		} else if (!creationDate.equals(other.creationDate)) {
			return false;
		}
		if (description == null) {
			if (other.description != null) {
				return false;
			}
		} else if (!description.equals(other.description)) {
			return false;
		}
		if (groupID == null) {
			if (other.groupID != null) {
				return false;
			}
		} else if (!groupID.equals(other.groupID)) {
			return false;
		}
		if (groupName == null) {
			if (other.groupName != null) {
				return false;
			}
		} else if (!groupName.equals(other.groupName)) {
			return false;
		}
		if (modificationDate == null) {
			if (other.modificationDate != null) {
				return false;
			}
		} else if (!modificationDate.equals(other.modificationDate)) {
			return false;
		}
		if (owner == null) {
			if (other.owner != null) {
				return false;
			}
		} else if (!owner.equals(other.owner)) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}

	public static Builder getBuilder(
			final GroupID id,
			final GroupName name,
			final UserName owner,
			final CreateAndModTimes times) {
		return new Builder(id, name, owner, times);
	}
	
	public static class Builder {
		
		private final GroupID groupID;
		private final GroupName groupName;
		private final UserName owner;
		private final CreateAndModTimes times;
		private GroupType type = GroupType.organization;
		private Optional<String> description = Optional.absent();
		
		public Builder(
				final GroupID id,
				final GroupName name,
				final UserName owner,
				final CreateAndModTimes times) {
			checkNotNull(id, "id");
			checkNotNull(name, "name");
			checkNotNull(owner, "owner");
			checkNotNull(times, "times");
			this.groupID = id;
			this.groupName = name;
			this.owner = owner;
			this.times = times;
		}
		
		public Builder withType(final GroupType type) {
			checkNotNull(type, "type");
			this.type = type;
			return this;
		}
		
		// null or whitespace only == remove description
		public Builder withDescription(final String description) {
			if (isNullOrEmpty(description)) {
				this.description = Optional.absent();
			} else {
				this.description = Optional.of(description);
			}
			return this;
		}
		
		public Group build() {
			return new Group(groupID, groupName, owner, type, times, description);
		}
	}
	
}
