package ${topPackage}.lm;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

	private static final long serialVersionUID = 6646314403682818472L;

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setHeader("Cache-Control", "no-cache, no-store");
		resp.setHeader("Pragma", "no-cache");
		resp.setHeader("Expires", new java.util.Date().toString());
		if(req.getSession(false)!=null){
			req.getSession(false).invalidate();//remove session.
        }
        if(req.getSession()!=null){
        	req.getSession().invalidate();//remove session.
        }		
		req.logout();
		resp.setStatus(HttpServletResponse.SC_OK);
	}
}
