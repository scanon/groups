package us.kbase.groups.service.api;

/** Paths to service endpoints.
 * @author gaprice@lbl.gov
 *
 */
public class ServicePaths {

	/* general strings */

	/** The URL path separator. */
	public static final String SEP = "/";
	
	/* Root endpoint */

	/** The root endpoint location. */
	public static final String ROOT = SEP;
	
	/* Groups endpoints */
	
	/** The group endpoint location. */
	public static final String GROUP = SEP + "group";
	/** The group ID */
	public static final String GROUP_ID = "{" + Fields.GROUP_ID + "}";
	/** The location to request membership in a group. */
	public static final String GROUP_REQUEST_MEMBERSHIP = GROUP_ID + SEP + "requestmembership";
	/** the location to get requests targeted at a group. */
	public static final String GROUP_REQUESTS = GROUP_ID + SEP + "requests";
	/** The location to remove a user from a group. */
	public static final String GROUP_USER_ID = GROUP_ID + SEP + "user" + SEP + "{" +
			Fields.GROUP_MEMBER + "}";
	
	/* Request endpoints */
	
	/** The request endpoint location. */
	public static final String REQUEST = SEP + "request";
	/** The location to access a request by ID. */
	public static final String REQUEST_ID = SEP + "id" + SEP + "{" + Fields.REQUEST_ID + "}";
	/** The location to cancel a request. */
	public static final String REQUEST_CANCEL = REQUEST_ID + SEP + "cancel";
	/** The location to accept a request. */
	public static final String REQUEST_ACCEPT = REQUEST_ID + SEP + "accept";
	/** The location to deny a request. */
	public static final String REQUEST_DENY = REQUEST_ID + SEP + "deny";
	/** The location to list requests created by the user. */
	public static final String REQUEST_CREATED = SEP + "created";
	/** The location to list requests targeted at the user. */
	public static final String REQUEST_TARGETED = SEP + "targeted";
	
	
}