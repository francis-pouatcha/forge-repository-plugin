package ${topPackage}.lm;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is the representation of the login form for a from login process.
 * 
 * @author francis
 * 
 */
@WebServlet("/login")
public class LoginFormServlet extends HttpServlet {

	private static final long serialVersionUID = -3629781750937980222L;

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.sendError(HttpServletResponse.SC_FORBIDDEN);
	}
}
