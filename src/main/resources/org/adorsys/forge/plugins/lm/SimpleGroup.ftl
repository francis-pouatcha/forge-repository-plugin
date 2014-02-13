package org.adorsys.adpharma.server.lm;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

/**
 * An implementation of Group that manages a collection of Principal objects
 * based on their hashCode() and equals() methods. This class is not thread
 * safe.
 * 
 * @author Scott.Stark@jboss.org
 * @author francis pouatcha
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SimpleGroup extends SimplePrincipal implements Group, Cloneable {

	private static final long serialVersionUID = -3895238309655268742L;

	private HashMap<Principal, Principal> members;

	public SimpleGroup(String groupName) {
		super(groupName);
		members = new HashMap<Principal, Principal>(3);
	}

	/**
	 * Adds the specified member to the group.
	 * 
	 * @param user
	 *            the principal to add to this group.
	 * @return true if the member was successfully added, false if the principal
	 *         was already a member.
	 */
	public boolean addMember(Principal user) {
		boolean isMember = members.containsKey(user);
		if (isMember == false)
			members.put(user, user);
		return isMember == false;
	}

	/**
	 * Returns true if the passed principal is a member of the group. This
	 * method does a recursive search, so if a principal belongs to a group
	 * which is a member of this group, true is returned.
	 * 
	 * A special check is made to see if the member is an instance of
	 * org.jboss.security.AnybodyPrincipal or org.jboss.security.NobodyPrincipal
	 * since these classes do not hash to meaningful values.
	 * 
	 * @param member
	 *            the principal whose membership is to be checked.
	 * @return true if the principal is a member of this group, false otherwise.
	 */
	public boolean isMember(Principal member) {
		// First see if there is a key with the member name
		boolean isMember = members.containsKey(member);

		if (isMember == false) { // Check any Groups for membership
			Collection values = members.values();
			Iterator iter = values.iterator();
			while (isMember == false && iter.hasNext()) {
				Object next = iter.next();
				if (next instanceof Group) {
					Group group = (Group) next;
					isMember = group.isMember(member);
				}
			}
		}
		return isMember;
	}

	/**
	 * Returns an enumeration of the members in the group. The returned objects
	 * can be instances of either Principal or Group (which is a subinterface of
	 * Principal).
	 * 
	 * @return an enumeration of the group members.
	 */
	public Enumeration<Principal> members() {
		return Collections.enumeration(members.values());
	}

	/**
	 * Removes the specified member from the group.
	 * 
	 * @param user
	 *            the principal to remove from this group.
	 * @return true if the principal was removed, or false if the principal was
	 *         not a member.
	 */
	public boolean removeMember(Principal user) {
		Object prev = members.remove(user);
		return prev != null;
	}

	public String toString() {
		StringBuffer tmp = new StringBuffer(getName());
		tmp.append("(members:");
		Iterator iter = members.keySet().iterator();
		while (iter.hasNext()) {
			tmp.append(iter.next());
			tmp.append(',');
		}
		tmp.setCharAt(tmp.length() - 1, ')');
		return tmp.toString();
	}

	public synchronized Object clone() throws CloneNotSupportedException {
		SimpleGroup clone = (SimpleGroup) super.clone();
		if (clone != null)
			clone.members = (HashMap) this.members.clone();
		return clone;
	}
}