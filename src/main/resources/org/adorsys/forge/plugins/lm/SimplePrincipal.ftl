package ${topPackage}.lm;

import java.io.Serializable;
import java.security.Principal;

public class SimplePrincipal implements Principal, Serializable{

	private static final long serialVersionUID = -2221888790136398728L;
	private final String name;

	public SimplePrincipal(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		/*
		 * We accept any principal with the same name.
		 * 
		 * We do not allow principal with name==null.
		 */
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (name == null)
			return false;
		if (!(obj instanceof Principal))
			return false;
		Principal other = Principal.class.cast(obj);
		return name.equals(other.getName());
	}

	@Override
	public String toString() {
		return "SimplePrincipal [name=" + name + "]";
	}
	
}
