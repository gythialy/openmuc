package org.openmuc.framework.server.restws.servlets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthenticationServlet extends HttpServlet {

    private static final long serialVersionUID = 2441977846315845755L;
    private final static Logger logger = LoggerFactory.getLogger(AuthenticationServlet.class);

    @Override
    public void init() throws ServletException {

    }

    @Override
    public void destroy() {

    }

    // method is called when you type in http://localhost:8888/rest/authentications in your browser while openmuc is
    // running
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

}
