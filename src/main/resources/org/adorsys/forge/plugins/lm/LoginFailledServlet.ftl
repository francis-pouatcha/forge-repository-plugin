package org.adorsys.adpharma.server.lm;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet("/login-failed")
public class LoginFailledServlet extends HttpServlet {

	private static final long serialVersionUID = 394802460939755622L;

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
	}

	
}
