package us.kbase.groups.storage.mongo;

/** This class defines the field names used in MongoDB documents for storing groups data.
 * @author gaprice@lbl.gov
 *
 */
public class Fields {
	
	/** The separator between mongo field names. */
	public static final String FIELD_SEP = ".";

	/** The key for the MongoDB ID in documents. */
	public static final String MONGO_ID = "_id";

	/* ***********************
	 * groups fields
	 * ***********************
	 */
	
	/** The group id. */
	public static final String GROUP_ID = "id";
	/** The group name. */
	public static final String GROUP_NAME = "name";
	/** The group type. */
	public static final String GROUP_TYPE = "type";
	/** The username of the group owner. */
	public static final String GROUP_OWNER = "own";
	/** The group administrators. */
	public static final String GROUP_ADMINS = "admin";
	/** The group members. */
	public static final String GROUP_MEMBERS = "memb";
	/** The group creation date. */
	public static final String GROUP_CREATION = "create";
	/** The group modification date. */
	public static final String GROUP_MODIFICATION = "mod";
	/** The group description. */
	public static final String GROUP_DESCRIPTION = "desc";
	/** The group custom fields. */
	public static final String GROUP_CUSTOM_FIELDS = "custom";
	
	// resources fields
	/** Resources associated with the group. */
	public static final String GROUP_RESOURCES = "resources";
	/** A resource ID. */
	public static final String GROUP_RESOURCE_ID = "rid";
	/** A resource administrative ID. */
	public static final String GROUP_RESOURCE_ADMINISTRATIVE_ID = "aid";
	
	/* ***********************
	 * request fields
	 * ***********************
	 */
	
	/** The request ID */
	public static final String REQUEST_ID = "id";
	/** The group ID for the request */
	public static final String REQUEST_GROUP_ID = "gid";
	/** The user the request targets, if any. */
	public static final String REQUEST_TARGET = "target";
	/** The workspace the request targets, if any. */
	public static final String REQUEST_TARGET_WORKSPACE = "wstarg";
	/** The catalog module the request targets, if any. */
	public static final String REQUEST_TARGET_CATALOG_MODULE = "modtarg";
	/** The catalog method the request targets, if any. */
	public static final String REQUEST_TARGET_CATALOG_METHOD = "methtarg";
	/** The user that made the request. */
	public static final String REQUEST_REQUESTER = "requester";
	/** The type of the request. */
	public static final String REQUEST_TYPE = "type";
	/** The the status of the request. */
	public static final String REQUEST_STATUS = "status";
	/** The user that closed the request. */
	public static final String REQUEST_CLOSED_BY = "closedby";
	/** The the reason the request was closed. */
	public static final String REQUEST_REASON_CLOSED = "reasonclosed";
	/** The creation date of the request. */
	public static final String REQUEST_CREATION = "create";
	/** The modification date of the request. */
	public static final String REQUEST_MODIFICATION = "mod";
	/** The expiration date of the request. */
	public static final String REQUEST_EXPIRATION = "expire";
	/** The characteristic string for a request. */
	public static final String REQUEST_CHARACTERISTIC_STRING = "charstr";
	
	/* ***********************
	 * database schema fields
	 * ***********************
	 */
	
	/** The key for the schema field. The key and value are used to ensure there is
	 * never more than one schema record.
	 */
	public static final String DB_SCHEMA_KEY = "schema";
	/** The value for the schema field. The key and value are used to ensure there is
	 * never more than one schema record.
	 */
	public static final String DB_SCHEMA_VALUE = "schema";
	/** Whether the database schema is in the process of being updated. */
	public static final String DB_SCHEMA_UPDATE = "inupdate";
	/** The version of the database schema. */
	public static final String DB_SCHEMA_VERSION = "schemaver";

}
