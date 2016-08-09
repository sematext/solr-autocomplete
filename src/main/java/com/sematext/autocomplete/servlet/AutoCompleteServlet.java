package com.sematext.autocomplete.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sematext.autocomplete.tst.AutoCompleteService;
import com.sematext.autocomplete.tst.DoublyLinkedList.DLLIterator;

public class AutoCompleteServlet extends HttpServlet {
    private static final long serialVersionUID = 9111106498492817985L;
    private ServletContext context;
    private static AutoCompleteService service;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        this.context = config.getServletContext();

        service = new AutoCompleteService("etc/hr-wiki-titles.txt");

    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String action = req.getParameter("action");
        String searchString = req.getParameter("searchString");
        JSONArray matches = null;

        if ("complete".equals(action)) {
            if (searchString != null && !searchString.equals("")) {
                searchString = searchString.trim();

                try {
                    JSONObject resultSet = new JSONObject();
                    JSONObject result = new JSONObject();

                    JSONObject JsonObject = null;
                    DLLIterator it = service.matchPrefix(searchString).iterator();

                    matches = new JSONArray();

                    while (it.hasNext()) {
                        String word = (String) it.next();

                        JsonObject = new JSONObject();

                        JsonObject.put("Title", word);
                        matches.put(JsonObject);
                    }

                    if (matches.length() > 0) {
                        resp.setContentType("text/plain");
                        resp.setHeader("Cache-Control", "no-cache");
                        result.put("Result", matches);
                        resultSet.put("ResultSet", result);
                        resp.getWriter().write(resultSet.toString(2));
                    }
                } catch (JSONException e) {
                    resp.getWriter().write("\"" + e.getMessage() + "\"");
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        }

        else if ("lookup".equals(action)) {
            searchString = req.getParameter("searchString").trim().toUpperCase();

            if ((searchString != null) && service.get(searchString) != null) {
                req.setAttribute("object", service.get(searchString));
            }

            context.getRequestDispatcher("/display.jsp").forward(req, resp);
        }
    }
}
