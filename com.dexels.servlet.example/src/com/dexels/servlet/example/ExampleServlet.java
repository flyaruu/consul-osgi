package com.dexels.servlet.example;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;

@Component(name="servlet.example.one", property={"alias=/hello","name=testservlet"})
public class ExampleServlet extends HttpServlet implements Servlet {

	private static final long serialVersionUID = 5835066732011816933L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.getWriter().write("Hello!");
	}


}
